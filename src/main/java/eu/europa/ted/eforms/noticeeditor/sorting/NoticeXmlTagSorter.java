package eu.europa.ted.eforms.noticeeditor.sorting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeInfo;
import eu.europa.ted.eforms.noticeeditor.helper.notice.FieldsAndNodes;
import eu.europa.ted.eforms.noticeeditor.service.XmlWriteService;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Sorts notice XML tags (elements) in the order defined by the corresponding SDK XSD sequences.
 *
 * <p>
 * NOTE: For a sample usage there is a unit test which uses this.
 * </p>
 */
public class NoticeXmlTagSorter {

  private static final Logger logger = LoggerFactory.getLogger(NoticeXmlTagSorter.class);

  private static final String CBC_CUSTOMIZATION_ID = "cbc:CustomizationID";

  private final DocumentTypeInfo docTypeInfo;
  private final Path sdkFolder;
  private final XPath xpathInst;
  private final FieldsAndNodes fieldsAndNodes;

  /**
   * The instance is reusable but specific to a given SDK version.
   *
   * @param docBuilder The document builder is passed as it can be reused
   * @param xpathInst Reusable xpath preconfigured instance
   * @param docTypeInfo SDK document type info
   * @param sdkFolder The folder of the downloaded SDK
   * @param fieldsAndNodes The SDK fields and nodes metadata (including sort order)
   */
  public NoticeXmlTagSorter(final DocumentBuilder docBuilder, final XPath xpathInst,
      final DocumentTypeInfo docTypeInfo, final Path sdkFolder,
      final FieldsAndNodes fieldsAndNodes) {

    Validate.notNull(docBuilder);
    Validate.notNull(xpathInst);
    Validate.notNull(docTypeInfo);
    Validate.notNull(sdkFolder);

    // SDK specific.
    this.docTypeInfo = docTypeInfo;
    this.xpathInst = xpathInst;
    this.sdkFolder = sdkFolder;

    this.fieldsAndNodes = fieldsAndNodes;
  }

  /**
   * Sorts the passed notice XML document starting from the root. This depends on the SDK version of
   * the notice.
   *
   * @param doc A notice XML as a W3C DOM.
   *
   * @throws SAXException If any parse error occurs.
   * @throws IOException If any IO error occurs.
   */
  public void sortXml(final Document doc) throws SAXException, IOException {
    sortXml(doc.getDocumentElement());
  }

  /**
   * Sorts the passed notice XML elements and sub-elements. This depends on the SDK version of the
   * notice.
   *
   * @param xmlRoot The xml root element is the entry point
   * @throws SAXException If any parse error occurs.
   * @throws IOException If any IO error occurs.
   */
  public void sortXml(final Element xmlRoot) throws SAXException, IOException {

    // Compare sdkVersion of the element to the SDK version of this instance.
    final String sdkVersionOfNoticeStr =
        XmlUtils.getDirectChild(xmlRoot, CBC_CUSTOMIZATION_ID).getTextContent();

    final SdkVersion sdkVersionOfNotice =
        new SdkVersion(XmlWriteService.parseEformsSdkVersionText(sdkVersionOfNoticeStr));
    final SdkVersion sdkVersionOfSorter = getSorterSdkVersion();
    if (!sdkVersionOfSorter.equals(sdkVersionOfNotice)) {
      throw new RuntimeException(
          String.format("Incompatible version: sorterInstance=%s, noticeToSort=%s",
              sdkVersionOfSorter, sdkVersionOfNotice));
    }

    logger.info("Attempting to sort tags in the XML, starting from root element={}",
        xmlRoot.getTagName());
    logger.info("XML uri={}", xmlRoot.getOwnerDocument().getBaseURI());

    // The used map does not need to be a LinkedHashMap but it helps to see in which order the
    // entries have been added during when debugging (root first, ...).
    // final Map<String, List<String>> elemOrderByXsdElemType = new LinkedHashMap<>(128);
    // final Map<String, String> xsdElemTypeByXsdElemName = new LinkedHashMap<>(256);

    //
    // Example:
    // <xsd:element name="BusinessRegistrationInformationNotice"
    // type="BusinessRegistrationInformationNoticeType"/>
    //

    final JsonNode rootNode = this.fieldsAndNodes.getRootNode();
    sortRecursive(xmlRoot, rootNode);
    // NOTE: we do not normalize the document, this can be done later if desired.
  }

  /**
   * @param xmlElement The XML element to sort
   * @param fieldOrNode Field or node corresponding to the
   */
  public void sortRecursive(final Element xmlElement, final JsonNode fieldOrNode) {
    Validate.notNull(xmlElement);
    Validate.notNull(fieldOrNode);

    final String id = JsonUtils.getTextStrict(fieldOrNode, FieldsAndNodes.FIELD_OR_NODE_ID_KEY);
    final String xpathAbsolute =
        JsonUtils.getTextStrict(fieldOrNode, FieldsAndNodes.XPATH_ABSOLUTE);

    final Map<String, List<JsonNode>> fieldOrNodeByParentNodeId =
        fieldsAndNodes.buildMapOfFieldOrNodeByParentNodeId();

    // REORDER TREE ITEMS BY ORDER OF TOP LEVEL ELEMENT.

    // All the fields or nodes found under the same parent.
    // Example:
    // id = "ND-BusinessParty" but in the xml it is "cac:BusinessParty"
    // We want the XML child elements of "cac:BusinessParty" in the correct sequence order.
    final List<JsonNode> childItems = fieldOrNodeByParentNodeId.get(id);
    if (childItems == null) {
      return; // Nothing to sort.
    }
    final List<OrderItem> orderItemsForParent = new ArrayList<>(childItems.size());
    for (final JsonNode childItem : childItems) {
      final String itemId = JsonUtils.getTextStrict(childItem, FieldsAndNodes.FIELD_OR_NODE_ID_KEY);
      final JsonNode arr = childItem.get(FieldsAndNodes.XSD_SEQUENCE_ORDER_KEY);
      if (arr != null) {
        final ArrayNode xsdSequenceOrder = (ArrayNode) arr;
        final JsonNode firstItem = xsdSequenceOrder.get(0);
        final String key = firstItem.fieldNames().next();
        final int order = firstItem.get(key).asInt();
        final OrderItem orderItem = new OrderItem(itemId, key, order);
        orderItemsForParent.add(orderItem);
      }
    }
    // The order items are not ordered yet, they contain the order, and we naturally sort on it.
    Collections.sort(orderItemsForParent);

    // Find parent elements in the XML.
    final List<Element> xmlParentElements =
        XmlUtils.evaluateXpathAsElemList(xpathInst, xmlElement, xpathAbsolute, xpathAbsolute);
    for (final Element xmlParentElement : xmlParentElements) {

      // Find child elements relative to the parent context.
      final Element xpathContext = xmlParentElement;

      for (final OrderItem orderItem : orderItemsForParent) {
        final String xpathExpr = orderItem.getXmlName();
        final List<Element> elemsFoundByTag =
            XmlUtils.evaluateXpathAsElemList(xpathInst, xpathContext, xpathExpr, xpathExpr);

        // Reorder XML elements.
        // Also note that XML attributes have no order.
        for (final Element childElementsFound : elemsFoundByTag) {

          // PRESERVE POSITION OF COMMENTS OR XML TEXTS NODES (formatting...).
          // Find comments or text nodes above the element.
          final List<Node> commentsOrTextsAbove = new ArrayList<>();
          Node previousSibling = childElementsFound.getPreviousSibling();
          while (previousSibling != null) {
            if (previousSibling.getNodeType() == Node.TEXT_NODE) {
              commentsOrTextsAbove.add(previousSibling);
            } else if (previousSibling.getNodeType() == Node.COMMENT_NODE) {
              commentsOrTextsAbove.add(previousSibling);
            } else {
              break;
            }
            previousSibling = previousSibling.getPreviousSibling();
          }
          Collections.reverse(commentsOrTextsAbove); // To keep the order.

          for (final Node commentOrTextAbove : commentsOrTextsAbove) {
            removeAndAppend(xpathContext, commentOrTextAbove);
          }

          // This sorts the xml elements by removing them and appending them back.
          removeAndAppend(xpathContext, childElementsFound);
        }
      }
    }

    // THIS WILL NOT WORK FOR CASES HAVING PREDICATES IF ORDER MATTERS BY PREDICATE ???
    // "xpathRelative" :
    // "cac:ContractExecutionRequirement
    // /cbc:ExecutionRequirementCode[@listName='ecatalog-submission']",
    // "xsdSequenceOrder" : [ { "cac:ContractExecutionRequirement" : 38 }, {
    // "cbc:ExecutionRequirementCode" : 3 } ],

    // Continue on child items in the field and node hierarchy.
    for (final JsonNode childItem : childItems) {
      sortRecursive(xmlElement, childItem);
    }
  }

  /**
   * @return The path of the main XSD file, this is not supported in some older versions, in that
   *         case it returns empty.
   */
  public Optional<Path> getMainXsdPathOpt() {
    final Optional<String> sdkXsdPathOpt = this.docTypeInfo.getSdkXsdPathOpt();
    if (sdkXsdPathOpt.isEmpty()) {
      logger.info("Sorting not supported for version={}", getSorterSdkVersion());
      return Optional.empty();
    }
    return Optional.of(sdkFolder.resolve(sdkXsdPathOpt.get()));
  }

  private SdkVersion getSorterSdkVersion() {
    return docTypeInfo.getSdkVersion();
  }

  /**
   * @param elemParent The element in which to append
   * @param elemToSort The element to remove and append
   */
  private static void removeAndAppend(final Element elemParent, final Node elemToSort) {
    elemParent.removeChild(elemToSort); // Removes child from old location.
    elemParent.appendChild(elemToSort); // Appends child at the new location.
  }
}
