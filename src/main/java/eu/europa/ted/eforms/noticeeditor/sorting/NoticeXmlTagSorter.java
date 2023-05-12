package eu.europa.ted.eforms.noticeeditor.sorting;

import java.io.IOException;
import java.nio.file.Path;
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
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeInfo;
import eu.europa.ted.eforms.noticeeditor.helper.notice.FieldsAndNodes;
import eu.europa.ted.eforms.noticeeditor.service.XmlWriteService;
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

  private final DocumentBuilder docBuilder;
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

    this.docBuilder = docBuilder;

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

    // TODO

    // We need to go from XML to field id or node id to get the "xsdSequenceOrder".
    // 1. The best would be to mark the fields and nodes using extra attributes.
    // 2. Find attribute, get field id or node id, get xsdSequenceOrder
    // 3. Remove all the attributes later (unless debug).

    sortElementRec(xmlRoot);
  }

  public void sortElementRec(final Node xmlElement) {

    final JsonNode rootNode = this.fieldsAndNodes.getRootNode();
    fieldsAndNodes.buildMapOfFieldOrNodeByParentNodeId();

    // Maybe start from ND-ROOT instead, get child nodes, search using xpath ...
    // Each time you find XML elements, sort

    // TODO cannot get child nodes or fields here
    // TODO enrich json with child nodes and child fields
    // childNodes : ["ND-xyz", ...]
    // childFields : ["BT-abc", ...]

    // "xpathRelative" : "cac:CorporateRegistrationScheme/cac:JurisdictionRegionAddress",

    // "xsdSequenceOrder" : [
    // { "cac:CorporateRegistrationScheme" : 13 },
    // {"cac:JurisdictionRegionAddress" : 5 }
    // ],

    // TODO recursive from root, find nodes using xpath, ...
    // this.xpathInst.

    // final NodeList childNodes = xmlElement.getChildNodes();
    // for (int i = 0; i < childNodes.getLength(); ++i) {
    // final Node child = childNodes.item(i);
    // if (Node.ELEMENT_NODE == child.getNodeType()) {
    // // System.out.println(child.getNodeName());
    // sortElementRec(child);
    // }
    // // Find xpath relative ??? works for root, what about intermediary nodes found in xpaths
    // // xpathInst
    // // Node.ATTRIBUTE_NODE
    // // Node.COMMENT_NODE
    // }
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

  private Document buildDoc(final Path xsdPath) throws SAXException, IOException {
    return docBuilder.parse(xsdPath.toFile());
  }
}
