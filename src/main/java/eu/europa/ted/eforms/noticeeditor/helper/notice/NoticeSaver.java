package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.util.JsonUtils.getIntStrict;
import static eu.europa.ted.eforms.noticeeditor.util.JsonUtils.getTextStrict;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.util.JavaTools;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;
import net.sf.saxon.lib.NamespaceConstant;

public class NoticeSaver {

  private static final Logger logger = LoggerFactory.getLogger(NoticeSaver.class);

  static final String ND_ROOT = "ND-Root";
  static final String ND_ROOT_EXTENSION = "ND-RootExtension";

  /**
   * Notice field id having the eformsSdkVersion as a value.
   */
  public static final String FIELD_ID_SDK_VERSION = "OPT-002-notice";

  /**
   * Notice field id having the notice sub type as a value.
   */
  public static final String FIELD_ID_NOTICE_SUB_TYPE = "OPP-070-notice";

  /**
   * A special case that we have to solve.
   */
  static final String NATIONAL = "national";

  static final String NODE_PARENT_ID = "parentId";
  static final String NODE_XPATH_RELATIVE = "xpathRelative";
  static final String NODE_IDENTIFIER_FIELD_ID = "identifierFieldId";

  static final String FIELD_CODE_LIST_ID = "codeListId";
  static final String FIELD_ID_SCHEME = "idScheme";
  static final String FIELD_PARENT_NODE_ID = "parentNodeId";
  static final String FIELD_TYPE_CODE = "code";
  static final String FIELD_XPATH_RELATIVE = "xpathRelative";
  static final String FIELD_TYPE = "type";

  static final String VIS_TYPE = "type";
  static final String VIS_TYPE_FIELD = "field";
  static final String VIS_TYPE_GROUP = "group";
  static final String VIS_VALUE = "value";
  static final String VIS_CONTENT_PARENT_COUNT = "contentParentCount";
  static final String VIS_CONTENT_COUNT = "contentCount";
  static final String VIS_CONTENT_ID = "contentId";

  private static final String XML_ATTR_EDITOR_COUNTER_SELF = "editorCounterSelf";
  private static final String XML_ATTR_EDITOR_COUNTER_PRNT = "editorCounterPrnt";
  private static final String XML_ATTR_EDITOR_FIELD_ID = "editorFieldId";
  private static final String XML_ATTR_EDITOR_NODE_ID = "editorNodeId";
  private static final String XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID = "editorIdentifierFieldId";
  private static final String XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID_SCHEME =
      "editorIdentifierFieldIdScheme";
  private static final String XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID_COUNTER =
      "editorIdentifierFieldIdCounter";
  private static final String XML_ATTR_SCHEME_NAME = "schemeName";
  private static final String XML_ATTR_LIST_NAME = "listName";

  private static final String XPATH_REPLACEMENT = "~"; // ONE CHAR ONLY!

  /**
   * For ids like ORG-0001, 0 is used to fill left of 1.
   */
  private static final String SCHEME_ID_PADDING_CHAR = "0";
  /**
   * For ids like ORG-0001, 0001 is 4 chars total.
   */
  private static final int SCHEME_ID_PADDING_SIZE = 4;

  public static Map<String, ConceptNode> buildConceptualModel(final FieldsAndNodes fieldsAndNodes,
      final JsonNode visualRoot) {
    Validate.notNull(visualRoot, "visualRoot");

    logger.info("Attempting to build conceptual model.");
    final Map<String, ConceptNode> conceptNodeById = new HashMap<>();

    for (final JsonNode visualItem : visualRoot) {
      final String type = getTextStrict(visualItem, VIS_TYPE);
      if (VIS_TYPE_FIELD.equals(type)) {

        // Here we mostly care about the value and the field id.

        // If the type is field then the contentId is a field id.
        final String fieldId = getTextStrict(visualItem, VIS_CONTENT_ID);
        final JsonNode fieldMeta = fieldsAndNodes.getFieldById(fieldId);
        Validate.notNull(fieldMeta, "fieldMeta is null for fieldId=%s", fieldId);

        final String value = JsonUtils.getTextMaybeBlank(visualItem, VIS_VALUE);
        final int counter = getIntStrict(visualItem, VIS_CONTENT_COUNT);
        final int parentCounter = getIntStrict(visualItem, VIS_CONTENT_PARENT_COUNT);
        final ConceptField conceptField = new ConceptField(fieldId, value, counter, parentCounter);

        // Build the parent hierarchy.
        final String parentNodeId = getTextStrict(fieldMeta, FIELD_PARENT_NODE_ID);
        final ConceptNode conceptNode =
            buildAncestryRec(conceptNodeById, parentNodeId, fieldsAndNodes);

        conceptNode.addConceptField(conceptField);

      } else {
        // TODO 'group' later for repeatability of groups ??
        logger.warn("TODO type=" + type);
      }
    }
    return conceptNodeById;
  }

  /**
   * Builds the node and the parents until the root is reached.
   *
   * @param conceptNodeById is populated as a side effect
   * @param nodeId The node identifier
   * @param fieldsAndNodes Information about SDK fields and nodes
   *
   * @return The concept node
   */
  private static ConceptNode buildAncestryRec(final Map<String, ConceptNode> conceptNodeById,
      final String nodeId, final FieldsAndNodes fieldsAndNodes) {
    final JsonNode nodeMeta = fieldsAndNodes.getNodeById(nodeId);
    ConceptNode conceptNode = conceptNodeById.get(nodeId);
    if (conceptNode == null) {
      // It does not exist, create it.
      conceptNode = new ConceptNode(nodeId);
      conceptNodeById.put(nodeId, conceptNode);
      final Optional<String> parentNodeIdOpt = JsonUtils.getTextOpt(nodeMeta, NODE_PARENT_ID);
      if (parentNodeIdOpt.isPresent()) {
        // This node has a parent. Build the parent until the root is reached.
        final ConceptNode parentConceptNode =
            buildAncestryRec(conceptNodeById, parentNodeIdOpt.get(), fieldsAndNodes);
        // Add it to the parent.
        parentConceptNode.addConceptNode(conceptNode);
      }
    }
    return conceptNode;
  }

  /**
   * Builds the physical model.
   *
   * @param fieldsAndNodes Information about SDK fields and nodes
   * @param noticeInfoBySubtype Map with info about notice metadata by notice sub type
   * @param documentInfoByType Map with info about document metadata by document type
   * @param concept The conceptual model from the previous step
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for
   *        production
   * @param buildFields Allows to disable field building, for debugging purposes. Note that if xpath
   *        relies on the presence of fields or attribute of fields this could be problematic
   * @param schemaInfo Information about the XML schema, mostly for the field order
   *
   * @return The physical model as an object containing the XML with a few extras
   */
  public static PhysicalModel buildPhysicalModelXml(final FieldsAndNodes fieldsAndNodes,
      final Map<String, JsonNode> noticeInfoBySubtype,
      final Map<String, JsonNode> documentInfoByType, final ConceptualModel concept,
      final boolean debug, final boolean buildFields, final SchemaInfo schemaInfo)
      throws ParserConfigurationException {
    logger.info("Attempting to build physical model.");

    final DocumentBuilder safeDocBuilder =
        SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);
    logger.info("XML DOM namespaceAware={}", safeDocBuilder.isNamespaceAware());
    logger.info("XML DOM validating={}", safeDocBuilder.isValidating());
    final Document xmlDoc = safeDocBuilder.newDocument();
    xmlDoc.setXmlStandalone(true);

    final DocumentTypeInfo docTypeInfo =
        getDocumentTypeInfo(noticeInfoBySubtype, documentInfoByType, concept);

    final String namespaceUri = docTypeInfo.getNamespaceUri();
    final String rootElementType = docTypeInfo.getRootElementTagName();

    // Create the root element, top level element.
    final Element xmlDocRoot = createElemXml(xmlDoc, rootElementType);
    xmlDoc.appendChild(xmlDocRoot);

    // TEDEFO-1426
    // For the moment do as if it was there.
    final String xsdPath = docTypeInfo.getXsdFile();
    final XPath xPathInst = setXmlNamespaces(namespaceUri, xmlDocRoot);

    // Attempt to put schemeName first.
    // buildPhysicalModelXmlRec(fieldsAndNodes, doc, concept.getRoot(), rootElem, debug,
    // buildFields,
    // 0, true, xPathInst);

    if (debug) {
      try {
        // Generate dot file for the conceptual model.
        // Visualizing it can help understand how it works or find problems.
        final boolean includeFields = false;
        final String dotText = concept.toDot(fieldsAndNodes, includeFields);
        final Path pathToFolder = Path.of("target/dot/");
        Files.createDirectories(pathToFolder);
        final Path pathToFile = pathToFolder.resolve(concept.getNoticeSubType() + "-concept.dot");
        JavaTools.writeTextFile(pathToFile, dotText);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // Recursion: start with the concept root.
    buildPhysicalModelXmlRec(fieldsAndNodes, xmlDoc, concept.getRoot(), xmlDocRoot, debug,
        buildFields, 0, false, xPathInst);

    // Reorder the physical model.
    reorderPhysicalModel(xmlDocRoot, xPathInst, schemaInfo);

    return new PhysicalModel(xmlDoc);
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
    logger.debug("Attempting to read document type info.");
    final JsonNode noticeInfo = getNoticeSubTypeInfo(noticeInfoBySubtype, concept);

    // Get the document type info from the SDK data.
    final String documentType =
        JsonUtils.getTextStrict(noticeInfo, SdkConstants.NOTICE_TYPES_JSON_DOCUMENT_TYPE_KEY);

    final JsonNode documentTypeInfo = documentInfoByType.get(documentType);
    return new DocumentTypeInfo(documentTypeInfo);
  }

  /**
   * Get information about the notice sub type from the SDK.
   */
  public static JsonNode getNoticeSubTypeInfo(final Map<String, JsonNode> noticeInfoBySubtype,
      final ConceptualModel concept) {
    final String noticeSubType = concept.getNoticeSubType();
    return noticeInfoBySubtype.get(noticeSubType);
  }

  /**
   * Changes the order of the elements to the schema sequence order. The XML DOM model is modified
   * as a side effect.
   */
  private static void reorderPhysicalModel(final Element rootElem, final XPath xPathInst,
      final SchemaInfo schemaInfo) {
    final List<String> rootOrder = schemaInfo.getRootOrder();
    for (final String tagName : rootOrder) {
      final NodeList elementsFound = evaluateXpath(xPathInst, rootElem, tagName);
      for (int i = 0; i < elementsFound.getLength(); i++) {
        final Node elem = elementsFound.item(i);
        rootElem.removeChild(elem);
        rootElem.appendChild(elem);
      }
    }
  }

  /**
   * Set XML namespaces.
   *
   * @param namespaceUriRoot This depends on the notice sub type
   * @param rootElement The root element of the XML
   * @return XPath instance with prefix to namespace awareness
   */
  static XPath setXmlNamespaces(final String namespaceUriRoot, final Element rootElement) {
    Validate.notBlank(namespaceUriRoot);
    Validate.notNull(rootElement);

    final Map<String, String> map = new LinkedHashMap<>();

    // If these namespaces evolve they could start to differ by SDK version.
    // TODO load from XSDs?
    // TODO maybe provide a simple mapping in notice-types.json or an index json file in /schemas?
    map.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    map.put("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
    map.put("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
    map.put("efext", "http://data.europa.eu/p27/eforms-ubl-extensions/1");
    map.put("efac", "http://data.europa.eu/p27/eforms-ubl-extension-aggregate-components/1");
    map.put("efbc", "http://data.europa.eu/p27/eforms-ubl-extension-basic-components/1");
    map.put("ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");

    //
    // NAMESPACES FOR THE XML DOCUMENT.
    //
    final String xmlnsPrefix = "xmlns";
    rootElement.setAttribute(xmlnsPrefix, namespaceUriRoot);

    final String xmlnsUri = "http://www.w3.org/2000/xmlns/";
    final Set<Entry<String, String>> entrySet = map.entrySet();
    for (Entry<String, String> entry : entrySet) {
      rootElement.setAttributeNS(xmlnsUri, xmlnsPrefix + ":" + entry.getKey(), entry.getValue());
    }

    //
    // NAMESPACES FOR XPATH.
    //
    try {
      // Why Saxon HE lib: namespaces were not working with the JDK (Java 15).
      final String objectModelSaxon = NamespaceConstant.OBJECT_MODEL_SAXON;
      System.setProperty("javax.xml.xpath.XPathFactory:" + objectModelSaxon,
          "net.sf.saxon.xpath.XPathFactoryImpl");
      final XPath xPathInst = XPathFactory.newInstance(objectModelSaxon).newXPath();

      // Custom namespace context.
      // https://stackoverflow.com/questions/13702637/xpath-with-namespace-in-java
      final NamespaceContext namespaceCtx = new NamespaceContext() {
        @Override
        public String getNamespaceURI(final String prefix) {
          final String namespaceUri = map.get(prefix);
          Validate.notBlank(namespaceUri, "Namespace is blank for prefix=%s", prefix);
          return namespaceUri;
        }

        @Override
        public String getPrefix(final String uri) {
          return null;
        }

        @Override
        public Iterator<String> getPrefixes(final String namespaceURI) {
          return null;
        }
      };
      xPathInst.setNamespaceContext(namespaceCtx);
      return xPathInst;

    } catch (XPathFactoryConfigurationException ex) {
      throw new RuntimeException(ex);
    }

  }

  /**
   * Recursive function used to build the physical model.
   *
   * @param fieldsAndNodes Field and node meta information (no form values)
   * @param doc The XML document
   * @param conceptElem The current conceptual element
   * @param xmlNodeElem The current xml node element
   * @param debug Adds extra debugging info in the XML if true
   * @param buildFields True if fields have to be built, false otherwise
   * @param depth Passed for debugging and logging purposes
   */
  private static void buildPhysicalModelXmlRec(final FieldsAndNodes fieldsAndNodes,
      final Document doc, final ConceptNode conceptElem, final Element xmlNodeElem,
      final boolean debug, final boolean buildFields, final int depth, final boolean onlyIfPriority,
      final XPath xPathInst) {
    Validate.notNull(conceptElem, "conceptElem is null");
    Validate.notNull(xmlNodeElem, "xmlElem is null, conceptElem=%s", conceptElem.getId());

    final String depthStr = StringUtils.leftPad(" ", depth * 4);
    System.out.println(depthStr + " -----------------------");
    System.out.println(depthStr + " BUILD PHYSICAL " + depth);
    System.out.println(depthStr + " -----------------------");
    System.out.println(depthStr + " " + xmlNodeElem.getTagName() + ", id=" + conceptElem.getId());

    // NODES.
    for (final ConceptNode conceptNode : conceptElem.getConceptNodes()) {
      buildNodesAndFields(fieldsAndNodes, doc, conceptNode, xPathInst, xmlNodeElem, debug, depth,
          onlyIfPriority, buildFields);
    }

    // FIELDS.
    for (final ConceptField conceptField : conceptElem.getConceptFields()) {
      buildFields(fieldsAndNodes, doc, conceptField, xPathInst, xmlNodeElem, debug, depth,
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
   * Build the nodes.
   *
   * @param fieldsAndNodes Field and node meta information (no form values)
   * @param doc The XML document (w3c DOM)
   * @param conceptElem The current conceptual element
   * @param xPathInst Allows to evaluate xpath expressions
   * @param xmlNodeElem The current xml node element
   * @param debug Adds extra debugging info in the XML if true, for humans or unit tests, the XML
   *        may become invalid
   * @param depth The current depth level passed for debugging and logging purposes
   * @param onlyIfPriority
   * @param buildFields True if fields have to be built, false otherwise
   */
  private static void buildNodesAndFields(final FieldsAndNodes fieldsAndNodes, final Document doc,
      final ConceptNode conceptNode, final XPath xPathInst, final Element xmlNodeElem,
      final boolean debug, final int depth, boolean onlyIfPriority, final boolean buildFields) {

    final String depthStr = StringUtils.leftPad(" ", depth * 4);

    // Get the node meta-data from the SDK.
    final String nodeId = conceptNode.getId();
    final JsonNode nodeMeta = fieldsAndNodes.getNodeById(nodeId);

    // If a field or node is repeatable, the the XML element to repeat is the first XML
    // element in the xpathRelative.
    final boolean nodeMetaRepeatable = FieldsAndNodes.isNodeRepeatable(nodeMeta);

    //
    // Handle identifier field id of node.
    //
    final Optional<NodeIdentifierFieldId> nodeIdentifierFieldIdOpt;
    final Optional<String> identifierFieldIdOpt =
        JsonUtils.getTextOpt(nodeMeta, NODE_IDENTIFIER_FIELD_ID);
    if (identifierFieldIdOpt.isPresent()) {
      // Example: "ND-Organization"
      // "repeatable" : true,

      // "identifierFieldId" : "OPT-200-Organization-Company"
      final String identifierFieldId = identifierFieldIdOpt.get();

      // Example: get meta info for "OPT-200-Organization-Company"
      final JsonNode fieldMeta = fieldsAndNodes.getFieldById(identifierFieldId);
      final String scheme = getTextStrict(fieldMeta, FIELD_ID_SCHEME);

      // TODO tttt get counter value from the form.
      // NOTE: this is relative to the context as TPO can be inside ORG and both repeat ...
      // Example: for TPO, we are on the parent, so look for descendants having
      // XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID_SCHEME attribute being TPO.
      // If one is found the last one and the XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID_COUNTER
      // count.
      // Increment the highest count by one.
      // All of this has to rely on the current context, so on the XML dom.
      final int counter = 1;

      // xmlNodeElem.get
      // []
      // "@" + "XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID_SCHEME"

      // "@" + "XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID_COUNTER"

      // NODE PARTS SIZE: 1
      // NODE PARTS: efac:TouchPoint
      // tag=efac:TouchPoint
      // xmlTag=efac:Organization
      // ttttefac:TouchPoint
      // ND-Touchpoint, xml=efac:TouchPoint
      // --------------------------
      // --- BUILD PHYSICAL 4
      // --- -----------------------
      // --- efac:TouchPoint, id=ND-Touchpoint
      // --- -----------------------
      // --- BUILD NODES AND FIELDS
      // --- -----------------------
      // --- NODE PARTS SIZE: 1
      // --- NODE PARTS: cac:PostalAddress
      // --- tag=cac:PostalAddress
      // --- xmlTag=efac:TouchPoint
      // --- ND-TouchpointAddress, xml=cac:PostalAddress

      final String id = buildIdWithSchemeAndCount(scheme, counter);
      nodeIdentifierFieldIdOpt = Optional.of(new NodeIdentifierFieldId(id, scheme, counter));
    } else {
      nodeIdentifierFieldIdOpt = Optional.empty();
    }

    final String xpathRel = getTextStrict(nodeMeta, NODE_XPATH_RELATIVE);
    Element previousElem = xmlNodeElem;
    Element partElem = null;

    // xpathRelative can contain many xml elements. We must build the hierarchy.
    // TODO Use ANTLR xpath grammar later? Avoid parsing the xpath altogether?
    // TODO maybe use xpath to locate the tag in the doc ? What xpath finds is where to add the
    // data.

    final String[] partsArr = getXpathPartsArr(xpathRel);
    final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
    // parts.remove(0); // If absolute.
    // parts.remove(0); // If absolute.
    if (debug) {
      // System out is used here because it is more readable than the logger lines.
      // This is not a replacement for logger.debug(...)
      System.out.println(depthStr + " NODE PARTS SIZE: " + parts.size());
      System.out.println(depthStr + " NODE PARTS: " + JavaTools.listToString(parts));
    }

    for (final String partXpath : parts) {
      final PhysicalXpath px = handleXpathPart(partXpath);
      final Optional<String> schemeNameOpt = px.getSchemeNameOpt();
      String xpathExpr = px.getXpathExpr();
      final String tag = px.getTag();
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

      if (nodeIdentifierFieldIdOpt.isPresent()) {
        final NodeIdentifierFieldId nif = nodeIdentifierFieldIdOpt.get();
        final String idWithCount = nif.getIdWithCount();

        // Modify xpathExpr to find only ORG-0001
        // Example: [@editorIdentifierFieldId='GLO-0001']
        final String predicate =
            String.format("[@%s='%s']", XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID, idWithCount);
        xpathExpr = xpathExpr + predicate;
      }
      foundElements = evaluateXpath(xPathInst, previousElem, xpathExpr);

      if (foundElements.getLength() > 0) {
        assert foundElements.getLength() == 1;

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

      previousElem.appendChild(partElem);
      if (schemeNameOpt.isPresent()) {
        partElem.setAttribute(XML_ATTR_SCHEME_NAME, schemeNameOpt.get());
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
    Validate.notNull(nodeElem, "partElem is null, conceptElem=%s", conceptNode.getId());

    // This could make the XML invalid, this is meant to be read by humans.
    if (debug) {
      nodeElem.setAttribute(XML_ATTR_EDITOR_NODE_ID, nodeId);
    }

    if (nodeIdentifierFieldIdOpt.isPresent()) {
      final NodeIdentifierFieldId nif = nodeIdentifierFieldIdOpt.get();
      nodeElem.setAttribute(XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID, nif.getIdWithCount());
      nodeElem.setAttribute(XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID_SCHEME, nif.getScheme());
      nodeElem.setAttribute(XML_ATTR_EDITOR_NODE_IDENTIFIER_FIELD_ID_COUNTER,
          Integer.toString(nif.getCounter()));
    }

    // Build child nodes recursively.
    buildPhysicalModelXmlRec(fieldsAndNodes, doc, conceptNode, nodeElem, debug, buildFields,
        depth + 1, onlyIfPriority, xPathInst);
  }

  /**
   * @return An identifier. Example: Padding ORG and 1 would return ORG-0001
   */
  private static String buildIdWithSchemeAndCount(final String scheme, final int counter) {
    final String numStr = Integer.toString(counter);
    final String padding =
        StringUtils.leftPad(numStr, SCHEME_ID_PADDING_SIZE, SCHEME_ID_PADDING_CHAR);
    return scheme + "-" + padding;
  }

  public static void main(String[] args) {
    System.out.println(buildIdWithSchemeAndCount("ORG", 1));
    System.out.println(buildIdWithSchemeAndCount("ORG", 10));
    System.out.println(buildIdWithSchemeAndCount("ORG", 100));
  }

  /**
   * Builds the fields, some fields have nodes in their xpath, those will also be built. As a
   * side-effect the doc and the passed xml element will be modified.
   *
   * @param debug special debug mode for humans and unit tests (XML may be invalid)
   * @param depth The current depth level passed for debugging and logging purposes
   * @param onlyIfPriority add only elements that have priority
   * @param buildFields
   */
  private static void buildFields(final FieldsAndNodes fieldsAndNodes, final Document doc,
      final ConceptField conceptField, final XPath xPathInst, final Element xmlNodeElem,
      final boolean debug, final int depth, final boolean onlyIfPriority,
      final boolean buildFields) {

    if (!buildFields) {
      return;
    }
    final String depthStr = StringUtils.leftPad(" ", depth * 4);

    // logger.debug("xmlEleme=" + EditorXmlUtils.getNodePath(xmlNodeElem));

    final String value = conceptField.getValue();
    final String fieldId = conceptField.getId();

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
    final boolean fieldMetaRepeatable = FieldsAndNodes.isFieldRepeatable(fieldMeta);

    Element previousElem = xmlNodeElem;
    Element partElem = null;

    // TODO Use ANTLR xpath grammar later.
    final String[] partsArr = getXpathPartsArr(xpathRel);
    final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
    if (debug) {
      System.out.println(depthStr + " FIELD PARTS SIZE: " + parts.size());
      System.out.println(depthStr + " FIELD PARTS: " + JavaTools.listToString(parts));
    }

    final String attrTemp = "temp";
    for (final String partXpath : parts) {

      final PhysicalXpath px = handleXpathPart(partXpath);
      final Optional<String> schemeNameOpt = px.getSchemeNameOpt();
      final Optional<String> xpathExprOpt =
          fieldMetaRepeatable ? Optional.empty() : Optional.of(px.getXpathExpr());
      final String tagOrAttr = px.getTag();

      // In this case the field is an attribute of a field in the XML, technicall this makes a
      // difference and we have to handle this with specific code.
      // Example: "@listName"
      // IDEA: "attribute" : "listName" in the fields.json for fields at are attributes in the
      // XML.
      final boolean isAttribute = tagOrAttr.startsWith("@") && tagOrAttr.length() > 1;

      final Optional<NodeList> foundElementsOpt = !isAttribute && xpathExprOpt.isPresent()
          ? Optional.of(evaluateXpath(xPathInst, previousElem, xpathExprOpt.get()))
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
          System.out.println(depthStr + " Creating attribute=" + tagOrAttr);
          previousElem.setAttribute(tagOrAttr.substring(1), value);
          // partElem = ... NO we do not want to reassign the partElem. This ensures that after we
          // exit the loop the partElem still points to the last XML element.
          // We also cannot set an attribute on an attribute!
        } else {
          // Create an XML element.
          System.out.println(depthStr + " Creating tag=" + tagOrAttr);
          partElem = createElemXml(doc, tagOrAttr);
          partElem.setAttribute(attrTemp, attrTemp);
        }
      }

      // This check is to avoid a problem with attributes.
      if (!isAttribute && partElem != null) {
        previousElem.appendChild(partElem);
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

      fieldElem.setAttribute(XML_ATTR_EDITOR_COUNTER_PRNT,
          Integer.toString(conceptField.getParentCounter()));
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
    Validate.notNull(value, "value is null for fieldId=%s", fieldId);
    fieldElem.setTextContent(value);

    final String fieldType = JsonUtils.getTextStrict(fieldMeta, FIELD_TYPE);
    if (fieldType == FIELD_TYPE_CODE) {
      // Convention: in the XML the codelist is set in the listName attribute.
      String listName = JsonUtils.getTextStrict(fieldMeta, FIELD_CODE_LIST_ID);
      if ("OPP-105-Business".equals(fieldId)) {
        // TODO sector, temporary fix here, this information should be provided in the SDK.
        // Maybe via a special key/value.
        listName = "sector";
      }
      fieldElem.setAttribute(XML_ATTR_LIST_NAME, listName);
    }
  }

  /**
   * Evaluates xpath and returns a nodelist. Note: this works when the required notice values are
   * present as some xpath may rely on their presence (codes, indicators, ...).
   *
   * @param xPathInst The XPath instance (reusable)
   * @param contextElem The XML context element in which the xpath is evaluated
   * @param xpathExpr The XPath expression relative to the passed context
   * @return The result of evaluating the XPath expression as a NodeList
   */
  static NodeList evaluateXpath(final XPath xPathInst, final Object contextElem,
      final String xpathExpr) {
    Validate.notBlank(xpathExpr, "xpathExpr is blank for %s", contextElem);
    try {
      // A potential optimization would be to reuse some of the compiled xpath.
      // final NodeList nodeList =
      // (NodeList) xPathInst.compile(xpathExpr).evaluate(contextElem, XPathConstants.NODESET);

      final NodeList nodeList =
          (NodeList) xPathInst.evaluate(xpathExpr, contextElem, XPathConstants.NODESET);

      return nodeList;
    } catch (XPathExpressionException e) {
      logger.error("Problem with xpathExpr={}", xpathExpr);
      throw new RuntimeException(e);
    }
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
      sb.append(ch == '/' && stacked > 0 ? XPATH_REPLACEMENT : ch);
    }
    return sb.toString().split("/");
  }

  private static PhysicalXpath handleXpathPart(final String partParam) {
    // NOTE: ideally we would want to fully avoid using xpath.
    String tag = partParam;

    final Optional<String> schemeNameOpt;

    if (tag.contains("[not(@schemeName = 'EU')]")) {
      // TODO This is a TEMPORARY FIX until we have a proper solution inside of the SDK. National is
      // only indirectly described by saying not EU, but the text itself is not given.
      tag = tag.replace("[not(@schemeName = 'EU')]", "[@schemeName = '" + NATIONAL + "']");
    }

    if (tag.contains("[@schemeName = '")) {
      // TODO investigate
      // efx-toolkit-java/XPathAttributeLocator.java at develop · OP-TED/efx-toolkit-java
      // (github.com)
      // Example:
      // "xpathAbsolute" : "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName =
      // 'EU']",

      // Example: Here we want to extract EU text.
      final int indexOfSchemeName = tag.indexOf("[@schemeName = '");
      String schemeName = tag.substring(indexOfSchemeName + "[@schemeName = '".length());
      // Remove the ']
      schemeName = schemeName.substring(0, schemeName.length() - "']".length());
      Validate.notBlank(schemeName);
      tag = tag.substring(0, indexOfSchemeName);
      schemeNameOpt = Optional.of(schemeName);
    } else {
      schemeNameOpt = Optional.empty();
    }

    // We want to remove the predicate from the tag.
    if (tag.contains("[")) {
      // TEMPORARY FIX.
      // Ignore predicate with negation as it is not useful for XML generation.
      // Example:
      // "xpathAbsolute" :
      // "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName =
      // 'EU')]/cbc:RegistrationName",
      tag = tag.substring(0, tag.indexOf('['));
    }

    if (tag.contains(XPATH_REPLACEMENT)) {
      tag = tag.substring(0, tag.indexOf(XPATH_REPLACEMENT));
    }

    // For the xpath expression keep the original param, only do the replacement.
    final String xpathExpr = partParam.replaceAll(XPATH_REPLACEMENT, "/");

    return new PhysicalXpath(xpathExpr, tag, schemeNameOpt);
  }

  /**
   * @param tagName The XML element tag name
   *
   * @return A w3c dom element (note that it is not attached to the DOM yet)
   */
  private static final Element createElemXml(final Document doc, final String tagName) {
    // This removes the xmlns="" that Saxon adds.
    try {
      if (tagName.startsWith("@")) {
        throw new RuntimeException(String.format("This is an attribute", tagName));
      }
      return doc.createElementNS("", tagName);
    } catch (org.w3c.dom.DOMException ex) {
      logger.error("Problem creating element with tagName={}", tagName);
      throw ex;
    }
  }

}
