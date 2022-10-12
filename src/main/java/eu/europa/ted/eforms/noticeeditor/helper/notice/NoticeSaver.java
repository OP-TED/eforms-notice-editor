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

public class NoticeSaver {

  private static final String REPLACEMENT = "~~~";
  private static final Logger logger = LoggerFactory.getLogger(NoticeSaver.class);

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
    final Document doc = safeDocBuilder.newDocument();
    doc.setXmlStandalone(true);

    // Get the namespace and the document type from the SDK data.
    final String noticeSubType = concept.getNoticeSubType();
    final JsonNode noticeInfo = noticeInfoBySubtype.get(noticeSubType);
    final String documentType = JsonUtils.getTextStrict(noticeInfo, "documentType");
    final JsonNode documentTypeInfo = documentInfoByType.get(documentType);
    final String namespaceUri = JsonUtils.getTextStrict(documentTypeInfo, "namespace");

    final String rootElementType = JsonUtils.getTextStrict(documentTypeInfo, "rootElement");
    final Element rootElem = doc.createElement(rootElementType);
    doc.appendChild(rootElem);

    final XPath xPathInst = XPathFactory.newInstance().newXPath();
    setXmlNamespaces(namespaceUri, rootElem, xPathInst);
    buildPhysicalModelXmlRec(fieldsAndNodes, doc, concept.getRoot(), rootElem, debug, buildFields,
        0, false, xPathInst); // tttt

    // Sort order.
    // TODO tttt use proper order depending on the notice sub type ...
    final List<String> setupCbcOrder = setupCbcOrder();
    for (final String tag : setupCbcOrder) {

      // TODO tttt
      // xpathInst should be prefix and namespace URI aware but it is not ...
      final String tagNoNamespace = tag.substring(tag.indexOf(":") + 1);

      final NodeList elementsFound = evaluateXpath(xPathInst, rootElem, tagNoNamespace);
      for (int i = 0; i < elementsFound.getLength(); i++) {
        final Node elem = elementsFound.item(i);
        rootElem.removeChild(elem);
        rootElem.appendChild(elem);
      }
    }

    return new PhysicalModel(doc);
  }

  /**
   * Set XML namespaces.
   *
   * @param namespaceUriRoot This depends on the notice sub type
   * @param rootElement The root element of the XML
   */
  private static void setXmlNamespaces(final String namespaceUriRoot, final Element rootElement,
      final XPath xPathInst) {

    final Map<String, String> map = new LinkedHashMap<>();
    map.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    map.put("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
    map.put("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
    map.put("efext", "http://data.europa.eu/p27/eforms-ubl-extensions/1");
    map.put("efac", "http://data.europa.eu/p27/eforms-ubl-extension-aggregate-components/1");
    map.put("efbc", "http://data.europa.eu/p27/eforms-ubl-extension-basic-components/1");
    map.put("ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");

    // On the XML document root.
    final String xmlns = "xmlns";
    rootElement.setAttribute(xmlns, namespaceUriRoot);
    final String xmlnsUri = "http://www.w3.org/2000/xmlns/";
    final Set<Entry<String, String>> entrySet = map.entrySet();
    for (Entry<String, String> entry : entrySet) {
      rootElement.setAttributeNS(xmlnsUri, xmlns + ":" + entry.getKey(), entry.getValue());
    }

    // FOR XPATH.
    final NamespaceContext namespaceCtx = new NamespaceContext() {
      @Override
      public String getNamespaceURI(String prefix) {
        final String namespaceUri = map.get(prefix);
        Validate.notBlank(namespaceUri, "namespace is blank for prefix=%s", prefix);
        return namespaceUri;
      }

      @Override
      public String getPrefix(String uri) {
        return null;
      }

      @Override
      public Iterator<String> getPrefixes(String namespaceURI) {
        return null;
      }
    };
    xPathInst.setNamespaceContext(namespaceCtx);
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

        // TODO tttt if the element is not repeatable, reuse it.

        // Find existing elements in the context of the previous element.
        final NodeList foundElements;

        if (previousElem.getTagName().equals(tag) && xpathExpr.equals(tag)) {
          // Sometimes the xpath absolute part already matches the previous element.
          // If there is no special xpath expression, just skip the part.
          // This avoids nesting of the same .../tag/tag/...
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
          partElem = doc.createElement(tag);
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

      Element previousElem = xmlNodeElem;
      Element partElem = null;

      // TODO Use ANTLR xpath grammar later.
      final String[] partsArr = getXpathPartsArr(xpathRel);
      final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
      System.out.println("  PARTS FIELD: " + parts);
      for (final String partXpath : parts) {

        final PhysicalXpath px = handleXpathPart(partXpath);
        final Optional<String> schemeNameOpt = px.getSchemeNameOpt();
        final String xpathExpr = px.getXpathExpr();
        final String tag = px.getTag();

        final NodeList foundElements = evaluateXpath(xPathInst, previousElem, xpathExpr);
        System.out.println("previousElem=" + EditorXmlUtils.getNodePath(previousElem));
        System.out.println("xpathExpr=" + xpathExpr);
        System.out.println("foundElements=" + foundElements.getLength());

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
          partElem = doc.createElement(tag);
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

      fieldElem.setTextContent(value);

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
        fieldElem.setAttribute("debugId", fieldId);
      }

    } // End of for loop on concept fields.
  }

  private static NodeList evaluateXpath(final XPath xPathInst, final Object contextElem,
      final String xpathExpr) {
    Validate.notBlank(xpathExpr);
    try {

      // Potential optimization would be to reuse some of the compiled xpath.
      // final NodeList nodeList =
      // (NodeList) xPathInst.compile(xpathExpr).evaluate(contextElem, XPathConstants.NODESET);

      final NodeList nodeList =
          (NodeList) xPathInst.evaluate(xpathExpr, contextElem, XPathConstants.NODESET);

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
