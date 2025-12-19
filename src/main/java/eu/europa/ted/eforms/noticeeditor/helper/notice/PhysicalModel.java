package eu.europa.ted.eforms.noticeeditor.helper.notice;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.helper.VersionHelper;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.XpathUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;
import eu.europa.ted.eforms.xpath.XPathInfo;
import eu.europa.ted.eforms.xpath.XPathProcessor;
import eu.europa.ted.eforms.xpath.XPathStep;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

/**
 * The physical model (PM) holds the XML representation. This class also provides static methods to
 * build the physical model from the conceptual model and a method to serialize it to XML text. The
 * SDK version is taken into account.
 *
 * <p>
 * Note that the XML elements are sorted, reordered, according to information found in the SDK.
 * </p>
 */
public class PhysicalModel {

  private static final Logger logger = LoggerFactory.getLogger(PhysicalModel.class);

  public static final String CBC_CUSTOMIZATION_ID = "cbc:CustomizationID";
  private static final String CBC_ID = "cbc:ID"; // Notice id, related to BT-701-notice.
  private static final String XMLNS = "xmlns";

  /**
   * A special case that we have to solve. HARDCODED. TODO
   */
  static final String NATIONAL = "national";

  public static final String FIELD_CODE_LIST = "codeList";
  private static final String FIELD_TYPE = "type";

  private static final String XML_ATTR_EDITOR_COUNTER_SELF = "editorCounterSelf";
  private static final String XML_ATTR_EDITOR_FIELD_ID = "editorFieldId";
  private static final String XML_ATTR_EDITOR_NODE_ID = "editorNodeId";

  private static final String ORDER_ATTRIBUTE = "x-order-num";

  /**
   * W3C Document Object Model (DOM), holds the XML representation. This can be queried using xpath
   * and is also easy to serialize.
   */
  private final Document domDocument;

  private final FieldsAndNodes fieldsAndNodes;
  private final XPath xpathInst;
  private final Optional<Path> mainXsdPathOpt;

  /**
   * @param document W3C DOM document representing the notice XML
   * @param xpathInst Used for xpath evaluation
   * @param fieldsAndNodes Holds SDK field and node metadata
   * @param mainXsdPathOpt Path to the main XSD file to use, may be empty if the feature is not
   *        supported in an older SDK
   */
  public PhysicalModel(final Document document, final XPath xpathInst,
      final FieldsAndNodes fieldsAndNodes, final Optional<Path> mainXsdPathOpt) {
    this.domDocument = document;

    // You may have the patch until this point, which could help debug the application.
    // But before the XML is written the patch version is removed.
    this.setSdkVersionWithoutPatch(getSdkVersion());

    this.fieldsAndNodes = fieldsAndNodes;
    this.xpathInst = xpathInst;
    this.mainXsdPathOpt = mainXsdPathOpt;
  }

  public Document getDomDocument() {
    return domDocument;
  }

  public FieldsAndNodes getFieldsAndNodes() {
    return fieldsAndNodes;
  }

  public Optional<Path> getMainXsdPathOpt() {
    return mainXsdPathOpt;
  }

  public UUID getNoticeId() {
    final String tagName = CBC_ID;
    // Get the direct child as we know it is directly under the root.
    final Element child = XmlUtils.getDirectChild(this.domDocument.getDocumentElement(), tagName);
    Validate.notNull(child, "The physical model notice id cannot be found by tagName=%s", tagName);
    final String text = child.getTextContent();
    Validate.notBlank(text, "The physical model notice id is blank via tagName=%s", tagName);
    return UUID.fromString(text);
  }

  public final void setSdkVersionWithoutPatch(final SdkVersion sdkVersion) {
    final Node xmlElem = getSdkVersionElement();
    xmlElem.setTextContent(VersionHelper.prefixSdkVersionWithoutPatch(sdkVersion));
  }

  public SdkVersion getSdkVersion() {
    final Node jsonNode = getSdkVersionElement();
    Validate.notNull(jsonNode, "The physical model SDK version element cannot be found!");
    final String text = jsonNode.getTextContent();
    return VersionHelper.parsePrefixedSdkVersion(text);
  }

  private Node getSdkVersionElement() {
    return this.domDocument.getElementsByTagName(CBC_CUSTOMIZATION_ID).item(0);
  }

  /**
   * This is provided for convenience for the unit tests.
   *
   * <p>
   * Evaluates xpath and returns a nodelist. Note: this works when the required notice values are
   * present as some xpath may rely on their presence (codes, indicators, ...).
   * </p>
   *
   * @param xpathExpr The XPath expression relative to the passed context
   * @param idForError An identifier which is shown in case of errors
   * @return The result of evaluating the XPath expression as a list of elements
   */
  List<Element> evaluateXpathForTests(final String xpathExpr, String idForError) {
    return XmlUtils.evaluateXpathAsElemList(this.xpathInst, this.getDomDocument(), xpathExpr,
        idForError);
  }

  /**
   * @param indented True if the xml text should be indented, false otherwise.
   *
   * @return The XML as text.
   */
  public String toXmlText(final boolean indented) {
    return EditorXmlUtils.asText(domDocument, indented);
  }

  @Override
  public String toString() {
    return toXmlText(true);
  }

  /**
   * Builds the physical model.
   * @param conceptualModel The conceptual Model
   * @param fieldsAndNodes The FieldAndNodes object for the specific SDK version
   * @param documentTypeInfo The DocumentTypeInfo object for the specific SDK version
   * @param sdkFolder Path for the SDK folder
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for production
   * @return The physical model as an object containing the XML with a few extras
   */
  public static PhysicalModel buildPhysicalModel(ConceptualModel conceptualModel, FieldsAndNodes fieldsAndNodes, DocumentTypeInfo documentTypeInfo,
          Path sdkFolder, boolean debug) throws ParserConfigurationException {

    logger.info("Attempting to build physical model.");

    final DocumentBuilder safeDocBuilder =
        SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

    logger.info("XML DOM namespaceAware={}", safeDocBuilder.isNamespaceAware());
    logger.info("XML DOM validating={}", safeDocBuilder.isValidating());

    final Document xmlDoc = safeDocBuilder.newDocument();
    xmlDoc.setXmlStandalone(true);

    visitNode(conceptualModel.getTreeRootNode(), null, xmlDoc, fieldsAndNodes, documentTypeInfo, debug);
    sortXml((Element) xmlDoc.getFirstChild());

    return new PhysicalModel(xmlDoc, XpathUtils.setupXpathInst(documentTypeInfo, Optional.of(documentTypeInfo.buildAdditionalNamespaceUriByPrefix())),
            fieldsAndNodes, Optional.of(sdkFolder.resolve(documentTypeInfo.getSdkXsdPathOpt().get())));

  }

  /**
   * Recursive method that process the nodes of the conceptual model and created the xml elements
   *
   * @param conceptTreeNode Conceptual node to process
   * @param xmlParent Already created XML element of the parent of the visiting node
   * @param document The XML document
   * @param fieldsAndNodes The FieldAndNodes object for the specific SDK version
   * @param documentTypeInfo The DocumentTypeInfo object for the specific SDK version
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for production
   */
  private static void visitNode(ConceptTreeNode conceptTreeNode, Element xmlParent, Document document, FieldsAndNodes fieldsAndNodes,
          DocumentTypeInfo documentTypeInfo, boolean debug) {
    Element xmlElement = createXmlElement(conceptTreeNode, xmlParent, document, List.of(), fieldsAndNodes, documentTypeInfo, debug);

    for (ConceptTreeNode childNode : conceptTreeNode.getConceptNodes()) {
      visitNode(childNode, xmlElement, document, fieldsAndNodes, documentTypeInfo, debug);
    }

    for (ConceptTreeField childField : conceptTreeNode.getConceptFields()) {
      if (XPathProcessor.parse(getRelativeXPath(childField, fieldsAndNodes)).isAttribute()) {
        continue;
      }
      Element childElement = createXmlElement(childField, xmlElement, document, conceptTreeNode.getConceptFields(), fieldsAndNodes, documentTypeInfo, debug);
      childElement.setTextContent(childField.getValue());
    }

  }

  /**
   * Create the XML elements (for each XPath part) for the given conceptual node/field as children of the given xmlParent element
   * @param conceptTreeItem The conceptual field/node
   * @param xmlParent The XML element to create the new elements as children of
   * @param document The XML document
   * @param siblings A list of all the conceptual fields that have the same parent node with the given conceptual field/node (Used for setting xml attributes)
   * @param fieldsAndNodes The FieldAndNodes object for the specific SDK version
   * @param documentTypeInfo The DocumentTypeInfo object for the specific SDK version
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for production
   * @return The new Element. If multiple elements are created (for each XPath part), returns the last element (Deepest in the XML)
   */
  private static Element createXmlElement(ConceptTreeItem conceptTreeItem, Element xmlParent, Document document, List<ConceptTreeField> siblings,
          FieldsAndNodes fieldsAndNodes, DocumentTypeInfo documentTypeInfo, boolean debug) {
    logger.debug("Creating element: {}", conceptTreeItem.idInSdkFieldsJson);

    if (conceptTreeItem.idInSdkFieldsJson.equals(ConceptualModel.ND_ROOT)) {
      //Create Root
      Element xmlRoot = document.createElement(documentTypeInfo.getRootElementTagName());
      PhysicalModel.setXmlNamespaces(documentTypeInfo, xmlRoot);
      document.appendChild(xmlRoot);
      return xmlRoot;
    }

    Element curParent = xmlParent;
    XPathInfo xPathInfo = XPathProcessor.parse(getRelativeXPath(conceptTreeItem, fieldsAndNodes));
    Map<String, Integer> xsdSequenceOrders = createXsdSequenceOrderMap(conceptTreeItem, fieldsAndNodes);
    int totalSteps = xPathInfo.getSteps().size();

    for (int i = 0; i < totalSteps; i++) {
      XPathStep xPathStep = xPathInfo.getSteps().get(i);

      //to cover most cases with common relative XPath parts
      if (!isRepeatable(conceptTreeItem, fieldsAndNodes) && i < totalSteps - 1) {
        Element existing = XmlUtils.getDirectChild(curParent, xPathStep.getStepText());
        if (existing != null) {
          curParent = existing;
          continue;
        }
      }

      Element curElement = document.createElementNS("", xPathStep.getStepText());

      if (xsdSequenceOrders.containsKey(xPathStep.getStepText())) {
        curElement.setAttribute(ORDER_ATTRIBUTE, xsdSequenceOrders.get(xPathStep.getStepText()).toString());
      }

      curParent.appendChild(curElement);
      curParent = curElement;
    }

    //Add debug attributes for testing
    if (debug) {
      if (conceptTreeItem instanceof ConceptTreeField) {
        curParent.setAttribute(XML_ATTR_EDITOR_FIELD_ID, conceptTreeItem.idInSdkFieldsJson);
        if (isRepeatable(conceptTreeItem, fieldsAndNodes)) {
          curParent.setAttribute(XML_ATTR_EDITOR_COUNTER_SELF, String.valueOf(conceptTreeItem.getCounter()));
        }
      } else if (conceptTreeItem instanceof ConceptTreeNode) {
        curParent.setAttribute(XML_ATTR_EDITOR_NODE_ID, conceptTreeItem.idInSdkFieldsJson);
        curParent.setAttribute(XML_ATTR_EDITOR_COUNTER_SELF, String.valueOf(conceptTreeItem.getCounter()));
      }
    }

    if (conceptTreeItem instanceof ConceptTreeField) {
      setAttributes(curParent, (ConceptTreeField) conceptTreeItem, siblings, fieldsAndNodes);
    }

    return curParent;
  }

  /**
   * Resolves if a field or node is repeatable
   * @param conceptTreeItem The conceptual node or field
   * @param fieldsAndNodes The FieldAndNodes object for the specific SDK version
   * @return True if the given node/field is repeatable, otherwise False
   */
  private static boolean isRepeatable(ConceptTreeItem conceptTreeItem, FieldsAndNodes fieldsAndNodes) {
    JsonNode elementsMeta = fieldsAndNodes.getFieldOrNodeById(conceptTreeItem.idInSdkFieldsJson);
    if (conceptTreeItem instanceof ConceptTreeNode) {
      return JsonUtils.getBoolStrict(elementsMeta, "repeatable");
    } else if (conceptTreeItem instanceof ConceptTreeField) {
      return JsonUtils.getBoolStrict(elementsMeta.get("repeatable"),"value");
    }

    return false;
  }

  /**
   * Sets the attributes to the given XML element
   * @param element The XML element
   * @param conceptTreeField The conceptual field of the element
   * @param siblings A list of all the conceptual fields that have the same parent node with the given conceptual field/node
   * @param fieldsAndNodes The FieldAndNodes object for the specific SDK version
   */
  private static void setAttributes(Element element, ConceptTreeField conceptTreeField, List<ConceptTreeField> siblings, FieldsAndNodes fieldsAndNodes) {
    JsonNode elementsMeta = fieldsAndNodes.getFieldOrNodeById(conceptTreeField.idInSdkFieldsJson);
    List<String> attributesFieldIds = JsonUtils.getListOfString(elementsMeta, "attributes");
    String fieldType = JsonUtils.getTextStrict(elementsMeta, FIELD_TYPE);

    for (String id : attributesFieldIds) {
      JsonNode attributeMeta = fieldsAndNodes.getFieldById(id);
      String name = JsonUtils.getTextStrict(attributeMeta, "attributeName");
      Optional<String> presetValue = JsonUtils.getTextOpt(attributeMeta, VisualModel.PRESET_VALUE);

      if (presetValue.isPresent()) {
        element.setAttribute(name, presetValue.get());
      } else if (fieldType.equals("text-multilingual") && name.equals("languageID")) {
        element.setAttribute(name, conceptTreeField.getLanguageId());
      } else {
        Optional<ConceptTreeField> attributeField = siblings.stream().filter(field -> field.getFieldId().equals(id)).findFirst();

        if (attributeField.isPresent()) {
          element.setAttribute(name, attributeField.get().getValue());
        } else {
          //TODO: attribute fields missing from notice type
          element.setAttribute(name, "");
        }
      }
    }
  }

  /**
   * Creates a map containing the XSD sequence order data of the given node/field
   * @param conceptTreeItem The conceptual node/field
   * @param fieldsAndNodes The FieldAndNodes object for the specific SDK version
   * @return Map: key is the xml element, value is the order number
   */
  private static Map<String, Integer> createXsdSequenceOrderMap(ConceptTreeItem conceptTreeItem, FieldsAndNodes fieldsAndNodes) {
    JsonNode meta = fieldsAndNodes.getFieldOrNodeById(conceptTreeItem.idInSdkFieldsJson);
    final List<JsonNode> nodeList = JsonUtils.getList(meta.get(FieldsAndNodes.XSD_SEQUENCE_ORDER_KEY));
    Map<String, Integer> xsdSequenceOrders = new HashMap<>();
    for (JsonNode node : nodeList) {
      node.fields().forEachRemaining(entry -> xsdSequenceOrders.put(entry.getKey(), entry.getValue().asInt()));
    }

    return xsdSequenceOrders;
  }

  /**
   * Gets the relative XPath of a node/field
   * @param conceptTreeItem The conceptual node/field
   * @param fieldsAndNodes The FieldAndNodes object for the specific SDK version
   * @return A string of the relative XPath
   */
  private static String getRelativeXPath(ConceptTreeItem conceptTreeItem, FieldsAndNodes fieldsAndNodes) {
    JsonNode meta = fieldsAndNodes.getFieldOrNodeById(conceptTreeItem.idInSdkFieldsJson);
    return JsonUtils.getTextStrict(meta, FieldsAndNodes.XPATH_RELATIVE);
  }

  /**
   * Recursive method that sorts the XML. The XSD sequence order is attached as attribute to each XML element that gets removed after the sorting
   * @param parent The XML element
   */
  private static void sortXml(Element parent) {
    NodeList children = parent.getChildNodes();
    List<Element> childrenElements = new ArrayList<>();

    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        childrenElements.add((Element) node);
      }
    }

    childrenElements.forEach(parent::removeChild);
    childrenElements.sort(Comparator.comparingInt(element -> Integer.parseInt(element.getAttribute(ORDER_ATTRIBUTE))));
    childrenElements.forEach(element -> {
      parent.appendChild(element);
      element.removeAttribute(ORDER_ATTRIBUTE);
    });
    childrenElements.forEach(PhysicalModel::sortXml);

  }

  /**
   * Set XML namespaces.
   *
   * @param docTypeInfo SDK document type info
   * @param rootElement The root element of the XML
   * @return XPath instance with prefix to namespace awareness
   */
  public static XPath setXmlNamespaces(final DocumentTypeInfo docTypeInfo,
      final Element rootElement) {
    Validate.notNull(rootElement);

    final String namespaceUriRoot = docTypeInfo.getNamespaceUri();
    Validate.notBlank(namespaceUriRoot);

    //
    // NAMESPACES FOR THE XML DOCUMENT.
    //
    rootElement.setAttribute(XMLNS, namespaceUriRoot);

    // Since SDK 1.6.0 the SDK provides this information (TEDEFO-1744).
    // If these namespaces evolve they could start to differ by SDK version.
    // This is why they have been moved to the SDK metadata.
    final Map<String, String> map = docTypeInfo.buildAdditionalNamespaceUriByPrefix();

    final String xmlnsUri = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
    for (final Entry<String, String> entry : map.entrySet()) {
      rootElement.setAttributeNS(xmlnsUri, XMLNS + ":" + entry.getKey(), entry.getValue());
    }
    return XpathUtils.setupXpathInst(docTypeInfo, Optional.of(map));
  }

}
