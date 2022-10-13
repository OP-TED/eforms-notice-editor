package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.util.JsonUtils.getIntStrict;
import static eu.europa.ted.eforms.noticeeditor.util.JsonUtils.getTextStrict;
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
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import net.sf.saxon.lib.NamespaceConstant;

public class NoticeSaver {

  private static final Logger logger = LoggerFactory.getLogger(NoticeSaver.class);

  private static final String REPLACEMENT = "~~~";

  public static Map<String, ConceptNode> buildConceptualModel(final FieldsAndNodes fieldsAndNodes,
      final JsonNode visualRoot) {
    Validate.notNull(visualRoot, "visualRoot");

    logger.info("Attempting to build conceptual model.");
    final Map<String, ConceptNode> conceptNodeById = new HashMap<>();

    for (final JsonNode visualItem : visualRoot) {
      final String type = getTextStrict(visualItem, "type");
      if ("field".equals(type)) {

        // Here we mostly care about the value and the field id.

        // If the type is field then the contentId is a field id.
        final String fieldId = getTextStrict(visualItem, "contentId");
        final JsonNode fieldMeta = fieldsAndNodes.getFieldById(fieldId);
        Validate.notNull(fieldMeta, "fieldMeta is null for fieldId=%s", fieldId);

        final String value = getTextStrict(visualItem, "value");
        final int counter = getIntStrict(visualItem, "contentCount");
        final int parentCounter = getIntStrict(visualItem, "contentParentCount");
        final ConceptField conceptField = new ConceptField(fieldId, value, counter, parentCounter);

        // Build the parent hierarchy.
        final String parentNodeId = getTextStrict(fieldMeta, "parentNodeId");
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
   *
   * @return The concept node.
   */
  private static ConceptNode buildAncestryRec(final Map<String, ConceptNode> conceptNodeById,
      final String nodeId, final FieldsAndNodes fieldsAndNodes) {
    final JsonNode nodeMeta = fieldsAndNodes.getNodeById(nodeId);
    ConceptNode conceptNode = conceptNodeById.get(nodeId);
    if (conceptNode == null) {
      // It does not exist, create it.
      conceptNode = new ConceptNode(nodeId);
      conceptNodeById.put(nodeId, conceptNode);
      final Optional<String> parentNodeIdOpt = JsonUtils.getTextOpt(nodeMeta, "parentId");
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

  public static final Element createElem(final Document doc, final String tagName) {
    // This removes the xmlns="" that Saxon adds.
    return doc.createElementNS("", tagName);
  }

  /**
   * Builds the physical model.
   *
   * @param debug Adds special debug info to the XML, useful for humans and unit tests
   * @param buildFields Allows to disable field building, for debugging purposes
   *
   * @return The physical model as an object containing the XML with a few extras
   */
  public static PhysicalModel buildPhysicalModelXml(final FieldsAndNodes fieldsAndNodes,
      final Map<String, JsonNode> noticeInfoBySubtype,
      final Map<String, JsonNode> documentInfoByType, final ConceptualModel concept,
      final boolean debug, final boolean buildFields) throws ParserConfigurationException {
    logger.info("Attempting to build physical model.");

    final DocumentBuilder safeDocBuilder =
        SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);
    logger.info("XML DOM namespaceAware={}", safeDocBuilder.isNamespaceAware());
    logger.info("XML DOM validating={}", safeDocBuilder.isValidating());
    final Document doc = safeDocBuilder.newDocument();
    doc.setXmlStandalone(true);

    final DocumentTypeInfo docTypeInfo =
        getDocumentTypeInfo(noticeInfoBySubtype, documentInfoByType, concept);
    final String namespaceUri = docTypeInfo.getNamespaceUri();
    final String rootElementType = docTypeInfo.getRootElementTagName();

    // TODO tttt use path to xsd, try local changes for now.
    // https://citnet.tech.ec.europa.eu/CITnet/jira/browse/TEDEFO-1426
    // For the moment do as if it was there.
    final String xsdPath = docTypeInfo.getXsdPath();

    // Create the root element, top level element.
    final Element rootElem = createElem(doc, rootElementType);
    doc.appendChild(rootElem);

    final XPath xPathInst = setXmlNamespaces(namespaceUri, rootElem);

    // Attempt to put schemeName first.
    // buildPhysicalModelXmlRec(fieldsAndNodes, doc, concept.getRoot(), rootElem, debug,
    // buildFields,
    // 0, true, xPathInst);

    buildPhysicalModelXmlRec(fieldsAndNodes, doc, concept.getRoot(), rootElem, debug, buildFields,
        0, false, xPathInst);

    // Sort order.
    reorderElements(rootElem, xPathInst);

    return new PhysicalModel(doc);
  }

  /**
   * Get information about the document type from the SDK.
   */
  public static DocumentTypeInfo getDocumentTypeInfo(
      final Map<String, JsonNode> noticeInfoBySubtype,
      final Map<String, JsonNode> documentInfoByType, final ConceptualModel concept) {
    final JsonNode noticeInfo = getNoticeSubTypeInfo(noticeInfoBySubtype, concept);

    // Get the document type info from the SDK data.
    final String documentType = JsonUtils.getTextStrict(noticeInfo, "documentType");
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
   * Fix the sort order.
   */
  private static void reorderElements(final Element rootElem, final XPath xPathInst) {
    final List<String> setupCbcOrder = setupCbcOrder();
    for (final String tag : setupCbcOrder) {
      final NodeList elementsFound = evaluateXpath(xPathInst, rootElem, tag);
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
      // Why Saxon: It was not working with the default Java / JDK lib.
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

    logger.debug("--- " + depth + " " + xmlNodeElem.getTagName() + ", id=" + conceptElem.getId());

    // NODES.
    buildNodesAndFields(fieldsAndNodes, doc, conceptElem, xmlNodeElem, debug, buildFields, depth,
        xPathInst, onlyIfPriority);

    // FIELDS.
    if (buildFields) {
      buildFields(fieldsAndNodes, conceptElem, doc, xPathInst, xmlNodeElem, debug, onlyIfPriority);
      System.out.println("");
      System.out.println(EditorXmlUtils.asText(doc, true));
    }

    // System.out.println("");
    // System.out.println(EditorXmlUtils.asText(doc, true));

  }

  /**
   * Build the nodes.
   *
   * @param fieldsAndNodes Field and node meta information (no form values)
   * @param doc The XML document
   * @param conceptElem The current conceptual element
   * @param xmlNodeElem The current xml node element
   * @param debug Adds extra debugging info in the XML if true
   * @param buildFields True if fields have to be built, false otherwise
   * @param depth Passed for debugging and logging purposes
   * @param xPathInst Allows to evaluate xpath expressions
   */
  private static void buildNodesAndFields(final FieldsAndNodes fieldsAndNodes, final Document doc,
      final ConceptNode conceptElem, final Element xmlNodeElem, final boolean debug,
      final boolean buildFields, final int depth, final XPath xPathInst,
      final boolean onlyIfPriority) {
    final List<ConceptNode> conceptNodes = conceptElem.getConceptNodes();
    for (final ConceptNode conceptElemChild : conceptNodes) {
      final String nodeId = conceptElemChild.getId();

      // Get the node meta-data from the SDK.
      final JsonNode nodeMeta = fieldsAndNodes.getNodeById(nodeId);
      final String xpathAbs = getTextStrict(nodeMeta, "xpathAbsolute");

      Element previousElem = xmlNodeElem;
      Element partElem = null;

      // xpathRelative can contain many xml elements. We must build the hierarchy.
      // TODO Use ANTLR xpath grammar later??
      // TODO maybe use xpath to locate the tag in the doc ? What xpath finds is where to add the
      // data.

      final String[] partsArr = getXpathPartsArr(xpathAbs);
      final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
      parts.remove(0); // If absolute.
      parts.remove(0); // If absolute.
      System.out.println("  PARTS NODE: " + parts);
      for (final String partXpath : parts) {

        final PhysicalXpath px = handleXpathPart(partXpath);
        final Optional<String> schemeNameOpt = px.getSchemeNameOpt();
        final String xpathExpr = px.getXpathExpr();
        final String tag = px.getTag();
        System.out.println("  tag=" + tag);
        System.out.println("  xmlTag=" + xmlNodeElem.getTagName());

        // TODO tttt if the element is not repeatable, reuse it. Maybe here is the right place to
        // use the counter.

        // Find existing elements in the context of the previous element.
        final NodeList foundElements;

        if (previousElem.getTagName().equals(tag) && xpathExpr.equals(tag)) {
          // Sometimes the xpath absolute part already matches the previous element.
          // If there is no special xpath expression, just skip the part.
          // This avoids nesting of the same .../tag/tag/...
          // TODO tttt this may be fixed by TEDEFO-1466
          continue; // Skip this tag.
        }

        foundElements = evaluateXpath(xPathInst, previousElem, xpathExpr);

        if (foundElements.getLength() > 0) {
          assert foundElements.getLength() == 1;
          final Node xmlNode = foundElements.item(0);
          if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
            // An existing element was found, reuse it.
            partElem = (Element) xmlNode;
          } else {
            throw new RuntimeException(String.format("NodeType=%s not an Element", xmlNode));
          }
        } else {
          // Create an XML element for the node.
          System.out.println("  creating node " + tag);
          logger.debug("Creating nodeId={}, tag={}", nodeId, tag);
          partElem = createElem(doc, tag);
        }

        previousElem.appendChild(partElem);

        if (schemeNameOpt.isPresent()) {
          partElem.setAttribute("schemeName", schemeNameOpt.get());
        }

        previousElem = partElem;

      } // End of for loop on parts.

      // Build child nodes recursively.
      buildPhysicalModelXmlRec(fieldsAndNodes, doc, conceptElemChild, partElem, debug, buildFields,
          depth + 1, onlyIfPriority, xPathInst);

    } // End of for loop on concept nodes.
  }

  /**
   * Builds the fields, some fields have nodes in their xpath, those will also be built. As a
   * side-effect the doc and the passed xml element will be modified.
   */
  private static void buildFields(final FieldsAndNodes fieldsAndNodes,
      final ConceptNode conceptElem, final Document doc, final XPath xPathInst,
      final Element xmlNodeElem, final boolean debug, final boolean onlyIfPriority) {

    // logger.debug("xmlEleme=" + EditorXmlUtils.getNodePath(xmlNodeElem));
    final List<ConceptField> conceptFields = conceptElem.getConceptFields();
    System.out.println("  conceptFields" + conceptFields);
    for (final ConceptField conceptField : conceptFields) {
      final String value = conceptField.getValue();
      final String fieldId = conceptField.getId();

      System.out.println("");
      System.out.println("  fieldId=" + fieldId);

      // Get the field meta-data from the SDK.
      final JsonNode fieldMeta = fieldsAndNodes.getFieldById(fieldId);
      Validate.notNull(fieldMeta, "fieldMeta null for fieldId=%s", fieldId);

      final String xpathRel = getTextStrict(fieldMeta, "xpathRelative");
      // final String xpathAbs = getTextStrict(fieldMeta, "xpathAbsolute");

      final boolean fieldMetaRepeatable = JsonUtils.getBoolStrict(fieldMeta, "repeatable");

      Element previousElem = xmlNodeElem;
      Element partElem = null;

      // TODO Use ANTLR xpath grammar later.
      final String[] partsArr = getXpathPartsArr(xpathRel);
      final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
      System.out.println("  PARTS FIELD: " + parts);
      for (final String partXpath : parts) {

        final PhysicalXpath px = handleXpathPart(partXpath);
        final Optional<String> schemeNameOpt = px.getSchemeNameOpt();
        final String xpathExpr = fieldMetaRepeatable ? "somethingimpossible" : px.getXpathExpr();
        final String tag = px.getTag();

        final NodeList foundElements = evaluateXpath(xPathInst, previousElem, xpathExpr);
        if (foundElements.getLength() > 0) {
          assert foundElements.getLength() == 1;

          final Node xmlNode;
          if (foundElements.getLength() > 1) {
            xmlNode = foundElements.item(0); // Which? 0, 1, ...???
            logger.warn("  FOUND MULTIPLE ELEMENTS for: {}", fieldId);
          } else {
            xmlNode = foundElements.item(0);
          }

          if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
            // An existing element was found, reuse it.
            partElem = (Element) xmlNode;
          } else {
            throw new RuntimeException(String.format("NodeType=%s not an Element", xmlNode));
          }

        } else {
          // Create an XML element for the field.
          logger.debug("Creating tag={}", tag);
          partElem = createElem(doc, tag);
          partElem.setAttribute("temp", "temp");
        }

        previousElem.appendChild(partElem);

        if (schemeNameOpt.isPresent()) {
          partElem.setAttribute("schemeName", schemeNameOpt.get());
        }

        previousElem = partElem;

      } // End of for loop on parts.

      // The last element is a leaf, so it is a field in this case.
      final Element fieldElem = partElem;
      Validate.notNull(fieldElem, "fieldElem is null for %s", fieldId);

      if (onlyIfPriority && StringUtils.isBlank(fieldElem.getAttribute("schemeName"))) {
        // Remove created and appended child elements.
        Element elem = fieldElem;
        while (true) {
          if (elem.hasAttribute("temp")) {
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
        continue; // Skip, it will be added later.
      } else {
        // Remove created and appended children.
        Element elem = fieldElem;
        while (true) {
          if (elem.hasAttribute("temp")) {
            final Node parentNode = elem.getParentNode();
            if (parentNode != null) {
              elem.removeAttribute("temp");
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

      if (debug) {
        fieldElem.setAttribute("editorCounterSelf", Integer.toString(conceptField.getCounter()));
        fieldElem.setAttribute("editorCounterPrnt",
            Integer.toString(conceptField.getParentCounter()));
      }

      final String fieldType = JsonUtils.getTextStrict(fieldMeta, "type");
      if (fieldType == "code") {
        // Convention: in the XML the codelist is set in the listName attribute.
        String listName = JsonUtils.getTextStrict(fieldMeta, "codeListId");
        if ("OPP-105-Business".equals(fieldId)) {
          // TODO tttt sector, temporary fix here, should be provided in the SDK.
          // Maybe via a special key/value.
          listName = "sector";
        }
        fieldElem.setAttribute("listName", listName);
      }

      if (debug) {
        fieldElem.setAttribute("editorFieldId", fieldId);
      }

    } // End of for loop on concept fields.
  }

  static NodeList evaluateXpath(final XPath xPathInst, final Object contextElem,
      final String xpathExpr) {
    Validate.notBlank(xpathExpr);
    try {

      // Potential optimization would be to reuse some of the compiled xpath.
      final NodeList nodeList =
          (NodeList) xPathInst.compile(xpathExpr).evaluate(contextElem, XPathConstants.NODESET);

      // final NodeList nodeList =
      // (NodeList) xPathInst.evaluate(xpathExpr, contextElem, XPathConstants.NODESET);

      return nodeList;
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  private static String[] getXpathPartsArr(final String xpathAbs) {
    // TODO this fixes a few problems with this naive implementation.
    return xpathAbs.replace("/@", REPLACEMENT + "@").split("/");
  }

  private static PhysicalXpath handleXpathPart(final String partParam) {
    String tag = partParam;
    final Optional<String> schemeNameOpt;

    if (tag.contains("[not(@schemeName = 'EU')]")) {
      // TEMPORARY FIX until we have a proper solution inside of the SDK.
      tag = tag.replace("[not(@schemeName = 'EU')]", "[@schemeName = 'national']");
    }

    // if (part.contains("[not(cbc:CompanyID~~~@schemeName = 'EU')]")) {
    // // TEMPORARY FIX until we have a proper solution inside of the SDK.
    // part = part.replace("[not(cbc:CompanyID~~~@schemeName = 'EU')]",
    // "[cbc:CompanyID/@schemeName = 'national']");
    // }

    if (tag.contains("[@schemeName = '")) {
      // Example:
      // "xpathAbsolute" : "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName =
      // 'EU']",
      // Here we want to extract EU text.
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

    // For the xpath expression keep the original.
    final String xpathExpr = partParam.replaceAll(REPLACEMENT, "/");

    return new PhysicalXpath(xpathExpr, tag, schemeNameOpt);
  }

  public static List<String> setupCbcOrder() {
    // TODO tttt later: load this dynamically from the xsd sequence.
    // EFORMS-BusinessRegistrationInformationNotice.xsd
    final List<String> list = new ArrayList<>();
    list.add("ext:UBLExtensions");
    list.add("cbc:UBLVersionID");
    list.add("cbc:CustomizationID");
    list.add("cbc:ProfileID");
    list.add("cbc:ProfileExecutionID");
    list.add("cbc:ID");
    list.add("cbc:UUID");
    list.add("cbc:IssueDate");
    list.add("cbc:IssueTime");
    list.add("cbc:VersionID");
    list.add("cbc:PreviousVersionID");
    list.add("cbc:RequestedPublicationDate");
    list.add("cbc:RegulatoryDomain");
    list.add("cbc:NoticeTypeCode");
    list.add("cbc:NoticeLanguageCode");
    list.add("cbc:Note");
    list.add("cbc:BriefDescription");
    list.add("cac:AdditionalNoticeLanguage");
    list.add("cac:Signature");
    list.add("cac:SenderParty");
    list.add("cac:ReceiverParty");
    list.add("cac:BusinessParty");
    list.add("cac:BrochureDocumentReference");
    list.add("cac:AdditionalDocumentReference");
    list.add("cac:BusinessCapability");
    list.add("efac:BusinessPartyGroup");
    list.add("efac:NoticePurpose");
    list.add("efac:NoticeSubType");
    return list;
  }

}
