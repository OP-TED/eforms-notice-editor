package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.util.JsonUtils.getTextStrict;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.sorting.NoticeXmlTagSorter;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.XpathUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;

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

  private static final String XMLNS = "xmlns";

  /**
   * A special case that we have to solve. HARDCODED. TODO
   */
  static final String NATIONAL = "national";

  private static final String NODE_XPATH_RELATIVE = "xpathRelative";

  public static final String FIELD_CODE_LIST = "codeList";
  private static final String FIELD_TYPE_CODE = "code";
  private static final String FIELD_XPATH_RELATIVE = "xpathRelative";
  private static final String FIELD_TYPE = "type";

  private static final String XML_ATTR_EDITOR_COUNTER_SELF = "editorCounterSelf";
  private static final String XML_ATTR_EDITOR_FIELD_ID = "editorFieldId";
  private static final String XML_ATTR_EDITOR_NODE_ID = "editorNodeId";
  private static final String XML_ATTR_SCHEME_NAME = "schemeName";
  private static final String XML_ATTR_LIST_NAME = "listName";

  private static final String XPATH_TEMP_REPLACEMENT = "~"; // ONE CHAR ONLY!

  /**
   * W3C Document Object Model (DOM), holds the XML representation. This can be queried using xpath
   * and is also easy to serialize.
   */
  private final Document domDocument;

  private final FieldsAndNodes fieldsAndNodes;
  private final XPath xpathInst;

  /**
   * @param document W3C DOM document
   * @param xpathInst Used for xpath evaluation
   * @param fieldsAndNodes Holds SDK field and node metadata
   */
  public PhysicalModel(final Document document, final XPath xpathInst,
      final FieldsAndNodes fieldsAndNodes) {
    this.domDocument = document;
    this.fieldsAndNodes = fieldsAndNodes;
    this.xpathInst = xpathInst;
  }

  public Document getDomDocument() {
    return domDocument;
  }

  public FieldsAndNodes getFieldsAndNodes() {
    return fieldsAndNodes;
  }

  /**
   * This is provided for convenience for the unit tests.
   *
   * <p>
   * Evaluates xpath and returns a nodelist. Note: this works when the required notice values are
   * present as some xpath may rely on their presence (codes, indicators, ...).
   * </p>
   *
   * @param contextElem The XML context element in which the xpath is evaluated
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

  /**
   * Builds the physical model.
   *
   * @param conceptModel The conceptual model from the previous step
   * @param fieldsAndNodes Information about SDK fields and nodes
   * @param noticeInfoBySubtype Map with info about notice metadata by notice sub type
   * @param documentInfoByType Map with info about document metadata by document type
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for
   *        production
   * @param buildFields Allows to disable field building, for debugging purposes. Note that if xpath
   *        relies on the presence of fields or attribute of fields this could be problematic
   *
   * @return The physical model as an object containing the XML with a few extras
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS",
      justification = "Checked to Runtime OK here")
  public static PhysicalModel buildPhysicalModel(final ConceptualModel conceptModel,
      final FieldsAndNodes fieldsAndNodes, final Map<String, JsonNode> noticeInfoBySubtype,
      final Map<String, JsonNode> documentInfoByType, final boolean debug,
      final boolean buildFields, final Path sdkRootFolder)
      throws ParserConfigurationException, SAXException, IOException {

    logger.info("Attempting to build physical model.");

    final DocumentBuilder safeDocBuilder =
        SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

    logger.info("XML DOM namespaceAware={}", safeDocBuilder.isNamespaceAware());
    logger.info("XML DOM validating={}", safeDocBuilder.isValidating());

    final Document xmlDoc = safeDocBuilder.newDocument();
    xmlDoc.setXmlStandalone(true);

    final DocumentTypeInfo docTypeInfo =
        getDocumentTypeInfo(noticeInfoBySubtype, documentInfoByType, conceptModel);
    final String rootElementType = docTypeInfo.getRootElementTagName();

    // Create the root element, top level element.
    final Element xmlDocRoot = createElemXml(xmlDoc, rootElementType);
    xmlDoc.appendChild(xmlDocRoot);

    // TEDEFO-1426
    // For the moment do as if it was there.
    final XPath xpathInst =
        setXmlNamespaces(docTypeInfo, xmlDocRoot, fieldsAndNodes.getSdkVersion());

    // Attempt to put schemeName first.
    // buildPhysicalModelXmlRec(fieldsAndNodes, doc, concept.getRoot(), rootElem, debug,
    // buildFields,
    // 0, true, xPathInst);

    if (debug) {
      conceptModel.writeDotFile(fieldsAndNodes);
    }

    // Recursion: start with the concept root.
    final ConceptTreeNode conceptualModelTreeRootNode = conceptModel.getTreeRootNode();
    final boolean onlyIfPriority = false;
    final int depth = 0;
    buildPhysicalModelRec(xmlDoc, fieldsAndNodes, conceptualModelTreeRootNode, xmlDocRoot, debug,
        buildFields, depth, onlyIfPriority, xpathInst);

    // Reorder the physical model.
    reorderPhysicalModel(safeDocBuilder, xmlDocRoot, xpathInst, docTypeInfo, sdkRootFolder);

    return new PhysicalModel(xmlDoc, xpathInst, fieldsAndNodes);
  }

  /**
   * Recursive function used to build the physical model.
   *
   * @param doc The XML document, modified as a SIDE-EFFECT!
   * @param fieldsAndNodes Field and node meta information (no form values)
   * @param conceptElem The current conceptual element
   * @param xmlNodeElem The current xml node element
   * @param debug Adds extra debugging info in the XML if true
   * @param buildFields True if fields have to be built, false otherwise
   * @param depth Passed for debugging and logging purposes
   */
  private static void buildPhysicalModelRec(final Document doc, final FieldsAndNodes fieldsAndNodes,
      final ConceptTreeNode conceptElem, final Element xmlNodeElem, final boolean debug,
      final boolean buildFields, final int depth, final boolean onlyIfPriority,
      final XPath xpathInst) {
    Validate.notNull(conceptElem, "conceptElem is null");
    Validate.notNull(xmlNodeElem, "xmlElem is null, conceptElem=%s", conceptElem.getIdUnique());

    final String depthStr = StringUtils.leftPad(" ", depth * 4);
    System.out.println(depthStr + " -----------------------");
    System.out.println(depthStr + " BUILD PHYSICAL " + depth);
    System.out.println(depthStr + " -----------------------");
    System.out
        .println(depthStr + " " + xmlNodeElem.getTagName() + ", id=" + conceptElem.getIdUnique());

    // NODES.
    for (final ConceptTreeNode conceptNode : conceptElem.getConceptNodes()) {
      buildNodesAndFields(doc, fieldsAndNodes, conceptNode, xpathInst, xmlNodeElem, debug, depth,
          onlyIfPriority, buildFields);
    }

    // FIELDS.
    for (final ConceptTreeField conceptField : conceptElem.getConceptFields()) {
      buildFields(doc, fieldsAndNodes, conceptField, xpathInst, xmlNodeElem, debug, depth,
          onlyIfPriority, buildFields);
    }

    // if (debug) {
    // Display the XML steps:
    // System out is used here because it is more readable than the logger lines.
    // This is not a replacement for logger.debug(...)
    // System.out.println("");
    // System.out.println(EditorXmlUtils.asText(doc, true));
    // }
  }

  /**
   * Build the physical nodes (and fields).
   *
   * @param doc The XML document, modified as a SIDE-EFFECT!
   * @param fieldsAndNodes Field and node meta information (no form values)
   * @param conceptNode The current conceptual node
   * @param xpathInst Allows to evaluate xpath expressions
   * @param xmlNodeElem The current xml node element (modified as a SIDE-EFFECT!)
   * @param debug Adds extra debugging info in the XML if true, for humans or unit tests, the XML
   *        may become invalid
   * @param depth The current depth level passed for debugging and logging purposes
   * @param onlyIfPriority Only build priority items (for xpath of other items which refer to them
   *        later)
   * @param buildFields True if fields have to be built, false otherwise
   */
  private static boolean buildNodesAndFields(final Document doc,
      final FieldsAndNodes fieldsAndNodes, final ConceptTreeNode conceptNode, final XPath xpathInst,
      final Element xmlNodeElem, final boolean debug, final int depth, boolean onlyIfPriority,
      final boolean buildFields) {

    final String depthStr = StringUtils.leftPad(" ", depth * 4);

    // Get the node meta-data from the SDK.
    final String nodeId = conceptNode.getNodeId();
    final JsonNode nodeMeta = fieldsAndNodes.getNodeById(nodeId);

    // If a field or node is repeatable, the the XML element to repeat is the first XML
    // element in the xpathRelative.
    final boolean nodeMetaRepeatable = FieldsAndNodes.isNodeRepeatableStatic(nodeMeta);

    final String xpathRel = getTextStrict(nodeMeta, NODE_XPATH_RELATIVE);
    Element previousElem = xmlNodeElem;
    Element partElem = null;

    // xpathRelative can contain many xml elements. We must build the hierarchy.
    // TODO Use ANTLR xpath grammar later? Avoid parsing the xpath altogether?
    // TODO maybe use xpath to locate the tag in the doc ? What xpath finds is where to add the
    // data.

    // Split the XPATH into parts.
    final String[] partsArr = getXpathPartsArr(xpathRel);
    final List<String> xpathParts = new ArrayList<>(Arrays.asList(partsArr));
    // parts.remove(0); // If absolute.
    // parts.remove(0); // If absolute.
    if (debug) {
      // System out is used here because it is more readable than the logger lines.
      // This is not a replacement for logger.debug(...)
      System.out.println(depthStr + " NODE PARTS SIZE: " + xpathParts.size());
      System.out.println(depthStr + " NODE PARTS: " + listToString(xpathParts));
    }

    for (final String xpathPart : xpathParts) {
      Validate.notBlank(xpathPart, "partXpath is blank for nodeId=%s, xmlNodeElem=%s", nodeId,
          xmlNodeElem);
      final PhysicalXpathPart px = handleXpathPart(xpathPart);
      final Optional<String> schemeNameOpt = px.getSchemeNameOpt();
      String xpathExpr = px.getXpathExpr();
      final String tag = px.getTagOrAttribute();
      if (debug) {
        System.out.println(depthStr + " tag=" + tag);
        System.out.println(depthStr + " xmlTag=" + xmlNodeElem.getTagName());
      }

      // TODO if the element is not repeatable, reuse it. Maybe here is the right place to
      // use the counter.

      // Find existing elements in the context of the previous element.
      final NodeList foundElements;

      if (previousElem.getTagName().equals(tag) && xpathExpr.equals(tag)) {
        // Sometimes the xpath absolute part already matches the previous element.
        // If there is no special xpath expression, just skip the part.
        // This avoids nesting of the same .../tag/tag/...
        // TODO this may be fixed by TEDEFO-1466
        continue; // Skip this tag.
      }
      foundElements = XmlUtils.evaluateXpathAsNodeList(xpathInst, previousElem, xpathExpr, nodeId);

      if (foundElements.getLength() > 0) {
        assert foundElements.getLength() == 1;
        // TODO investigate what should be done if more than one is present!?

        // Node is a w3c dom node, nothing to do with the SDK node.
        final Node xmlNode = foundElements.item(0);
        if (Node.ELEMENT_NODE == xmlNode.getNodeType()) {
          // An existing element was found, reuse it.
          partElem = (Element) xmlNode;
        } else {
          throw new RuntimeException(String.format("NodeType=%s not an Element", xmlNode));
        }

      } else {
        // Create an XML element for the node.
        if (debug) {
          final String msg = String.format("%s, xml=%s", nodeId, tag);
          System.out.println(depthStr + " " + msg);
        }
        partElem = createElemXml(doc, tag);
      }

      previousElem.appendChild(partElem); // SIDE-EFFECT! Adding item to the tree.

      if (schemeNameOpt.isPresent()) {
        partElem.setAttribute(XML_ATTR_SCHEME_NAME, schemeNameOpt.get()); // SIDE-EFFECT!
      }
      previousElem = partElem;

    } // End of for loop on parts of relative xpath.

    // We arrived at the end of the relative xpath.
    // We are interested in the last xml element as this represents the current node.
    // For example:
    // "id" : "ND-RegistrarAddress"
    // "xpathRelative" : "cac:CorporateRegistrationScheme/cac:JurisdictionRegionAddress"
    // The element nodeElem is cac:JurisdictionRegionAddress, so it is the node.
    final Element nodeElem = partElem;
    Validate.notNull(nodeElem, "partElem is null, conceptElem=%s", conceptNode.getIdUnique());

    // This could make the XML invalid, this is meant to be read by humans.
    if (debug) {
      nodeElem.setAttribute(XML_ATTR_EDITOR_NODE_ID, nodeId); // SIDE-EFFECT!

      nodeElem.setAttribute(XML_ATTR_EDITOR_COUNTER_SELF,
          Integer.toString(conceptNode.getCounter())); // SIDE-EFFECT!
    }

    // Build child nodes recursively.
    buildPhysicalModelRec(doc, fieldsAndNodes, conceptNode, nodeElem, debug, buildFields, depth + 1,
        onlyIfPriority, xpathInst);

    return nodeMetaRepeatable;
  }

  /**
   * Builds the fields, some fields have nodes in their xpath, those will also be built. As a
   * side-effect the doc and the passed xml element will be modified.
   *
   * @param doc The XML document, modified as a SIDE-EFFECT!
   * @param xmlNodeElem current XML element (modified as a SIDE-EFFECT!)
   * @param debug special debug mode for humans and unit tests (XML may be invalid)
   * @param depth The current depth level passed for debugging and logging purposes
   * @param onlyIfPriority add only elements that have priority
   * @param buildFields If false it will abort (only exists to simplify the code elsewhere)
   */
  private static void buildFields(final Document doc, final FieldsAndNodes fieldsAndNodes,
      final ConceptTreeField conceptField, final XPath xpathInst, final Element xmlNodeElem,
      final boolean debug, final int depth, final boolean onlyIfPriority,
      final boolean buildFields) {

    if (!buildFields) {
      return;
    }
    final String depthStr = StringUtils.leftPad(" ", depth * 4);

    // logger.debug("xmlEleme=" + EditorXmlUtils.getNodePath(xmlNodeElem));

    final String value = conceptField.getValue();
    final String fieldId = conceptField.getFieldId();

    if (debug) {
      System.out.println("");
      System.out.println(depthStr + " fieldId=" + fieldId);
    }

    // Get the field meta-data from the SDK.
    final JsonNode fieldMeta = fieldsAndNodes.getFieldById(fieldId);
    Validate.notNull(fieldMeta, "fieldMeta null for fieldId=%s", fieldId);

    // IMPORTANT: !!! The relative xpath of fields can contain intermediary xml elements !!!
    // Example: "cac:PayerParty/cac:PartyIdentification/cbc:ID" contains more than just the field.
    // These intermediary elements are very simple items and have no nodeId.
    final String xpathRel = getTextStrict(fieldMeta, FIELD_XPATH_RELATIVE);

    // If a field or node is repeatable, the the XML element to repeat is the first XML
    // element in the xpathRelative.
    final boolean fieldMetaRepeatable = FieldsAndNodes.isFieldRepeatableStatic(fieldMeta);

    Element previousElem = xmlNodeElem;
    Element partElem = null;

    // TODO Use ANTLR xpath grammar later.
    final String[] partsArr = getXpathPartsArr(xpathRel);
    final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
    if (debug) {
      System.out.println(depthStr + " FIELD PARTS SIZE: " + parts.size());
      System.out.println(depthStr + " FIELD PARTS: " + listToString(parts));
    }

    final String attrTemp = "temp";
    for (final String partXpath : parts) {

      final PhysicalXpathPart px = handleXpathPart(partXpath);
      final Optional<String> schemeNameOpt = px.getSchemeNameOpt();
      final Optional<String> xpathExprOpt =
          fieldMetaRepeatable ? Optional.empty() : Optional.of(px.getXpathExpr());
      final String tagOrAttr = px.getTagOrAttribute();

      // In this case the field is an attribute of a field in the XML, technically this makes a
      // difference and we have to handle this with specific code.
      // Example: "@listName"
      // IDEA: "attribute" : "listName" in the fields.json for fields at are attributes in the
      // XML.
      final boolean isAttribute = tagOrAttr.startsWith("@") && tagOrAttr.length() > 1;

      final Optional<NodeList> foundElementsOpt = !isAttribute && xpathExprOpt.isPresent()
          ? Optional.of(XmlUtils.evaluateXpathAsNodeList(xpathInst, previousElem,
              xpathExprOpt.get(), fieldId))
          : Optional.empty();

      if (foundElementsOpt.isPresent() && foundElementsOpt.get().getLength() > 0) {
        final NodeList foundElements = foundElementsOpt.get();
        assert foundElements.getLength() == 1 : "foundElements length != 1";

        // One or more pre-existing XML items were found. Try to reuse.
        final Node xmlNode;
        if (foundElements.getLength() > 1) {
          xmlNode = foundElements.item(0); // Which? 0, 1, ...???
          logger.warn("  FOUND MULTIPLE ELEMENTS for: {}", fieldId);
        } else {
          xmlNode = foundElements.item(0);
        }

        // Node is a w3c dom node, nothing to do with SDK node.
        if (Node.ELEMENT_NODE == xmlNode.getNodeType()) {
          // An existing element was found, reuse it.
          partElem = (Element) xmlNode;
        } else {
          throw new RuntimeException(String.format("NodeType=%s not an Element", xmlNode));
        }

      } else {
        if (isAttribute) {
          // Set attribute on previous element.
          // Example:
          // @listName or @currencyID
          // In the case we cannot create a new XML element.
          // We have to add this attribute to the previous element.
          logger.debug(depthStr + " Creating attribute=" + tagOrAttr);
          previousElem.setAttribute(tagOrAttr.substring(1), value); // SIDE-EFFECT!
          // partElem = ... NO we do not want to reassign the partElem. This ensures that after we
          // exit the loop the partElem still points to the last XML element.
          // We also cannot set an attribute on an attribute!
        } else {
          // Create an XML element.
          logger.debug(depthStr + " Creating tag=" + tagOrAttr);
          partElem = createElemXml(doc, tagOrAttr);
          partElem.setAttribute(attrTemp, attrTemp);
        }
      }

      // This check is to avoid a problem with attributes.
      if (!isAttribute && partElem != null) {
        previousElem.appendChild(partElem); // SIDE-EFFECT! Adding item to the tree.

        if (schemeNameOpt.isPresent()) {
          partElem.setAttribute(XML_ATTR_SCHEME_NAME, schemeNameOpt.get());
        }
        previousElem = partElem;
      }

    } // End of for loop on parts of relative xpath.

    // We arrived at the end of the relative xpath.
    // By design of the above algorithm the last element is always a leaf: the current field.
    final Element fieldElem = partElem != null ? partElem : previousElem;

    Validate.notNull(fieldElem, "fieldElem is null for fieldId=%s, xpathRel=%s", fieldId, xpathRel);

    if (debug) {
      // This could make the XML invalid, this is meant to be read by humans.
      // These attributes are also useful in unit tests for easy checking of field by id.
      fieldElem.setAttribute(XML_ATTR_EDITOR_FIELD_ID, fieldId);

      fieldElem.setAttribute(XML_ATTR_EDITOR_COUNTER_SELF,
          Integer.toString(conceptField.getCounter()));
    }

    if (onlyIfPriority && StringUtils.isBlank(fieldElem.getAttribute(XML_ATTR_SCHEME_NAME))) {
      // Remove created and appended child elements.
      Element elem = fieldElem;
      while (true) {
        if (elem.hasAttribute(attrTemp)) {
          final Node parentNode = elem.getParentNode();
          if (parentNode != null) {
            parentNode.removeChild(elem);
            elem = (Element) parentNode;
          } else {
            break;
          }
        } else {
          break;
        }
      }
      return; // Skip, it will be added later.

    } else {
      // Remove temporary attribute.
      Element elem = fieldElem;
      while (true) {
        if (elem.hasAttribute(attrTemp)) {
          final Node parentNode = elem.getParentNode();
          if (parentNode != null) {
            elem.removeAttribute(attrTemp);
            elem = (Element) parentNode;
          } else {
            break;
          }
        } else {
          break;
        }
      }
    }

    // Set value of the field.
    Validate.notNull(value, "value is null for fieldId=%s", fieldId, "fieldId=" + fieldId);
    fieldElem.setTextContent(value);

    final String fieldType = JsonUtils.getTextStrict(fieldMeta, FIELD_TYPE);
    if (FIELD_TYPE_CODE.equals(fieldType)) {

      // Find the SDK codelist identifier.
      final JsonNode codelistValue =
          FieldsAndNodes.getFieldPropertyValue(fieldMeta, FIELD_CODE_LIST);
      String codelistName = JsonUtils.getTextStrict(codelistValue, "id", "fieldId=" + fieldId);
      if (ConceptualModel.OPP_105_BUSINESS.equals(fieldId)) {
        // TODO sector, temporary hardcoded fix here, this information should be provided in the
        // SDK. Maybe via a special key/value.
        codelistName = "sector";
      }

      // Convention: in the XML the codelist is set in the listName attribute.
      fieldElem.setAttribute(XML_ATTR_LIST_NAME, codelistName);
    }
  }

  /**
   * Get information about the document type from the SDK.
   *
   * @param noticeInfoBySubtype Map with info about notice metadata by notice sub type
   * @param documentInfoByType Map with info about document metadata by document type
   */
  public static DocumentTypeInfo getDocumentTypeInfo(
      final Map<String, JsonNode> noticeInfoBySubtype,
      final Map<String, JsonNode> documentInfoByType, final ConceptualModel concept) {

    logger.info("Attempting to read document type info");
    final JsonNode noticeInfo = noticeInfoBySubtype.get(concept.getNoticeSubType());

    // Get the document type info from the SDK data.
    final String documentType =
        JsonUtils.getTextStrict(noticeInfo, SdkConstants.NOTICE_TYPES_JSON_DOCUMENT_TYPE_KEY);

    final JsonNode documentTypeInfo = documentInfoByType.get(documentType);
    return new DocumentTypeInfo(documentTypeInfo, concept.getSdkVersion());
  }

  /**
   * Changes the order of the elements to the schema sequence order. The XML DOM model is modified
   * as a side effect.
   */
  private static void reorderPhysicalModel(final DocumentBuilder safeDocBuilder,
      final Element noticeRootElem, final XPath xpathInst, final DocumentTypeInfo docTypeInfo,
      final Path sdkRootFolder) throws SAXException, IOException {
    final NoticeXmlTagSorter sorter =
        new NoticeXmlTagSorter(safeDocBuilder, xpathInst, docTypeInfo, sdkRootFolder);
    sorter.sortXml(noticeRootElem);
  }

  /**
   * Set XML namespaces.
   *
   * @param docTypeInfo SDK document type info
   * @param rootElement The root element of the XML
   * @return XPath instance with prefix to namespace awareness
   */
  public static XPath setXmlNamespaces(final DocumentTypeInfo docTypeInfo,
      final Element rootElement, final SdkVersion sdkVersion) {
    Validate.notNull(rootElement);

    final String namespaceUriRoot = docTypeInfo.getNamespaceUri();
    Validate.notBlank(namespaceUriRoot);

    //
    // NAMESPACES FOR THE XML DOCUMENT.
    //
    rootElement.setAttribute(XMLNS, namespaceUriRoot);

    final Map<String, String> map;
    if (sdkVersion.compareTo(new SdkVersion("1.6")) >= 0) {
      // Since SDK 1.6.0 the SDK provides this information (TEDEFO-1744).
      // If these namespaces evolve they could start to differ by SDK version.
      // This is why they have been moved to the SDK metadata.
      map = docTypeInfo.buildAdditionalNamespaceUriByPrefix();
    } else {
      // Pre SDK 1.6.0 logic:
      map = new LinkedHashMap<>();
      map.put("xsi", XMLConstants.W3C_XML_SCHEMA_NS_URI);
      map.put("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
      map.put("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
      map.put("efext", "http://data.europa.eu/p27/eforms-ubl-extensions/1");
      map.put("efac", "http://data.europa.eu/p27/eforms-ubl-extension-aggregate-components/1");
      map.put("efbc", "http://data.europa.eu/p27/eforms-ubl-extension-basic-components/1");
      map.put("ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
    }

    final String xmlnsUri = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
    for (final Entry<String, String> entry : map.entrySet()) {
      rootElement.setAttributeNS(xmlnsUri, XMLNS + ":" + entry.getKey(), entry.getValue());
    }
    return XpathUtils.setupXpathInst(docTypeInfo, Optional.of(map));
  }

  /**
   * @param xpath A valid xpath string
   * @return The xpath string split by slash but with predicates ignored.
   */
  private static String[] getXpathPartsArr(final String xpath) {
    final StringBuilder sb = new StringBuilder(xpath.length());
    int stacked = 0;
    for (int i = 0; i < xpath.length(); i++) {
      final char ch = xpath.charAt(i);
      if (ch == '[') {
        stacked++;
      } else if (ch == ']') {
        stacked--;
        Validate.isTrue(stacked >= 0, "stacked is < 0 for %s", xpath);
      }
      sb.append(ch == '/' && stacked > 0 ? XPATH_TEMP_REPLACEMENT : ch);
    }
    return sb.toString().split("/");
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "UCPM_USE_CHARACTER_PARAMETERIZED_METHOD",
      justification = "OK here, used in other places a string")
  private static PhysicalXpathPart handleXpathPart(final String partParam) {
    Validate.notBlank(partParam, "partParam is blank");

    // NOTE: ideally we would want to fully avoid using xpath.
    String tagOrAttr = partParam;

    final Optional<String> schemeNameOpt;

    if (tagOrAttr.contains("[not(@schemeName = 'EU')]")) {
      // HARDCODED
      // TODO This is a TEMPORARY FIX until we have a proper solution inside of the SDK. National is
      // only indirectly described by saying not EU, but the text itself is not given.
      tagOrAttr =
          tagOrAttr.replace("[not(@schemeName = 'EU')]", "[@schemeName = '" + NATIONAL + "']");
    }

    if (tagOrAttr.contains("[@schemeName = '")) {
      // TODO investigate
      // efx-toolkit-java/XPathAttributeLocator.java at develop · OP-TED/efx-toolkit-java
      // (github.com)
      // Example:
      // "xpathAbsolute" : "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName =
      // 'EU']",

      // Example: Here we want to extract EU text.
      final int indexOfSchemeName = tagOrAttr.indexOf("[@schemeName = '");
      String schemeName = tagOrAttr.substring(indexOfSchemeName + "[@schemeName = '".length());
      // Remove the ']
      schemeName = schemeName.substring(0, schemeName.length() - "']".length());
      Validate.notBlank(schemeName);
      tagOrAttr = tagOrAttr.substring(0, indexOfSchemeName);
      schemeNameOpt = Optional.of(schemeName);
    } else {
      schemeNameOpt = Optional.empty();
    }

    // We want to remove the predicate from the tag.
    if (tagOrAttr.contains("[")) {
      // TEMPORARY FIX.
      // Ignore predicate with negation as it is not useful for XML generation.
      // Example:
      // "xpathAbsolute" :
      // "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName =
      // 'EU')]/cbc:RegistrationName",
      tagOrAttr = tagOrAttr.substring(0, tagOrAttr.indexOf('['));
    }

    if (tagOrAttr.contains(XPATH_TEMP_REPLACEMENT)) {
      tagOrAttr = tagOrAttr.substring(0, tagOrAttr.indexOf(XPATH_TEMP_REPLACEMENT));
    }

    // For the xpath expression keep the original param, only do the replacement.
    final String xpathExpr = partParam.replaceAll(XPATH_TEMP_REPLACEMENT, "/");

    Validate.notBlank(xpathExpr, "xpathExpr is blank for tag=%s, partParam=%s", tagOrAttr,
        partParam);
    return new PhysicalXpathPart(xpathExpr, tagOrAttr, schemeNameOpt);
  }

  /**
   * Builds a W3C DOM element.
   *
   * @param tagName The XML element tag name
   *
   * @return A W3C DOM element (note that it is not attached to the DOM yet)
   */
  private static final Element createElemXml(final Document doc, final String tagName) {
    // This removes the xmlns="" that Saxon adds.
    try {
      if (tagName.startsWith("@")) {
        throw new RuntimeException(
            String.format("Expecting a tag but this is an attribute: %s", tagName));
      }
      return doc.createElementNS("", tagName);
    } catch (org.w3c.dom.DOMException ex) {
      logger.error("Problem creating element with tagName={}", tagName);
      throw ex;
    }
  }

  /**
   * @return list to string without the brackets.
   */
  public static String listToString(final List<String> list) {
    if (list.isEmpty()) {
      return "";
    }
    final String text = list.toString();
    return text.substring(1, text.length() - 1);
  }

}
