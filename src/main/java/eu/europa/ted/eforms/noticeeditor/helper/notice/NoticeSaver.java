package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.util.JsonUtils.getTextStrict;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
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

  private static final Logger logger = LoggerFactory.getLogger(NoticeSaver.class);

  public static Map<String, ConceptNode> buildConceptualModel(final FieldsAndNodes fieldsAndNodes,
      final JsonNode visualRoot) {
    Validate.notNull(visualRoot, "visualRoot");

    logger.info("Attempting to build conceptual model.");
    final Map<String, ConceptNode> conceptNodeById = new HashMap<>();

    for (JsonNode visualItem : visualRoot) {
      final String type = getTextStrict(visualItem, "type");
      if ("field".equals(type)) {

        // If the type is field then the contentId is a field id.
        final String contentId = getTextStrict(visualItem, "contentId");
        final JsonNode fieldMeta = fieldsAndNodes.getFieldById(contentId);

        final String value = getTextStrict(visualItem, "value");
        final ConceptField conceptField = new ConceptField(contentId, value);

        final String parentNodeId = getTextStrict(fieldMeta, "parentNodeId");
        final ConceptNode conceptNode =
            buildAncestryRec(conceptNodeById, parentNodeId, fieldsAndNodes);
        conceptNode.addConceptField(conceptField);

      } else {
        // TODO node??
        System.out.println("TODO type" + type);
      }
    }
    return conceptNodeById;
  }

  /**
   * Builds the node and the parents until the root is reached.
   *
   * @param conceptNodeById is populated as a side effect.
   *
   * @return The concept node.
   */
  private static ConceptNode buildAncestryRec(final Map<String, ConceptNode> conceptNodeById,
      final String nodeId, final FieldsAndNodes fieldsAndNodes) {
    final JsonNode nodeMeta = fieldsAndNodes.getNodeById(nodeId);
    ConceptNode conceptNode = conceptNodeById.get(nodeId);
    if (conceptNode == null) {
      conceptNode = new ConceptNode(nodeId);
      conceptNodeById.put(nodeId, conceptNode);
      final Optional<String> parentNodeIdOpt = JsonUtils.getTextOpt(nodeMeta, "parentId");
      if (parentNodeIdOpt.isPresent()) {
        final ConceptNode pcn =
            buildAncestryRec(conceptNodeById, parentNodeIdOpt.get(), fieldsAndNodes);
        pcn.addConceptNode(conceptNode);
      }
    }
    return conceptNode;
  }

  public static Document buildPhysicalModelXml(final FieldsAndNodes fieldsAndNodes,
      final Map<String, JsonNode> noticeInfoBySubtype,
      final Map<String, JsonNode> documentInfoByType, final ConceptualModel concept)
      throws ParserConfigurationException {
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
    final Element rootElement = doc.createElement(rootElementType);
    doc.appendChild(rootElement);

    setXmlNamespaces(namespaceUri, rootElement);
    buildPhysicalModelXmlRec(fieldsAndNodes, concept.getRoot(), doc, rootElement);

    return doc;
  }

  /**
   * Set XML namespaces.
   *
   * @param namespaceUri This depends on the notice sub type
   * @param rootElement The root element of the XML
   */
  private static void setXmlNamespaces(final String namespaceUri, final Element rootElement) {
    rootElement.setAttribute("xmlns", namespaceUri);

    final String xmlnsUri = "http://www.w3.org/2000/xmlns/";
    rootElement.setAttributeNS(xmlnsUri, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

    rootElement.setAttributeNS(xmlnsUri, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

    rootElement.setAttributeNS(xmlnsUri, "xmlns:cbc",
        "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");

    rootElement.setAttributeNS(xmlnsUri, "xmlns:cac",
        "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");

    rootElement.setAttributeNS(xmlnsUri, "xmlns:efext",
        "http://data.europa.eu/p27/eforms-ubl-extensions/1");

    rootElement.setAttributeNS(xmlnsUri, "xmlns:efac",
        "http://data.europa.eu/p27/eforms-ubl-extension-aggregate-components/1");

    rootElement.setAttributeNS(xmlnsUri, "xmlns:efbc",
        "http://data.europa.eu/p27/eforms-ubl-extension-basic-components/1");

    rootElement.setAttributeNS(xmlnsUri, "xmlns:ext",
        "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
  }

  private static void buildPhysicalModelXmlRec(final FieldsAndNodes fieldsAndNodes,
      final ConceptNode conceptElem, final Document doc, final Element xmlNodeElem) {
    Validate.notNull(xmlNodeElem, "xmlElem is null");

    // NODES.
    final List<ConceptNode> conceptNodes = conceptElem.getConceptNodes();
    for (final ConceptNode conceptElemChild : conceptNodes) {

      final String nodeId = conceptElemChild.getId();

      // Get the node meta-data from the SDK.
      final JsonNode nodeMeta = fieldsAndNodes.getNodeById(nodeId);
      String xpath = getTextStrict(nodeMeta, "xpathAbsolute");

      Element firstElem = null;
      Element previousElem = null;
      Element partElem = null;

      // xpathRelative can contain many xml elements. We must build the hierarchy.
      // TODO Use ANTLR xpath grammar later.
      // TODO maybe use xpath to locate the tag in the doc ? What xpath finds is where to add the
      // data.
      // doc.find using xpath

      final PhysicalXpath xpathInfo = handleXpath(xpath);
      xpath = xpathInfo.getXpath();
      final Optional<String> schemeNameOpt = xpathInfo.getSchemeNameOpt();
      final String[] partsArr = xpath.split("/");
      final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
      parts.remove(0);
      parts.remove(0);
      System.out.println("PARTS NODE: " + parts);
      for (final String part : parts) {
        System.out.println(part);

        // TODO tttt if the element is not repeatable, reuse it.
        // Find existing elements in the context of the previous element (or doc).
        final NodeList elementsByTagName =
            previousElem != null ? previousElem.getElementsByTagName(part)
                : doc.getElementsByTagName(part);

        if (elementsByTagName.getLength() == 1) {
          final Node xmlNode = elementsByTagName.item(0);
          if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
            // An existing element was found, reuse it.
            partElem = (Element) xmlNode;
          } else {
            throw new RuntimeException(String.format("NodeType=%s not an Element", xmlNode));
          }
        } else {
          partElem = doc.createElement(part);
        }

        if (previousElem != null) {
          previousElem.appendChild(partElem);
        }

        if (firstElem == null && elementsByTagName.getLength() == 0) {
          // This is the newest parent closest to the root.
          firstElem = partElem;
        }

        previousElem = partElem;
      }

      if (firstElem != null) {
        Validate.notNull(partElem);
        xmlNodeElem.appendChild(firstElem);
      }

      buildPhysicalModelXmlRec(fieldsAndNodes, conceptElemChild, doc, partElem);
    }

    //
    // FIELDS.
    //
    System.out.println("---");
    final List<ConceptField> conceptFields = conceptElem.getConceptFields();
    for (final ConceptField conceptField : conceptFields) {
      final String value = conceptField.getValue();
      final String fieldId = conceptField.getId();

      System.out.println("");
      System.out.println("fieldId=" + fieldId);

      // Get the field meta-data from the SDK.
      final JsonNode fieldMeta = fieldsAndNodes.getFieldById(fieldId);
      String xpathRel = getTextStrict(fieldMeta, "xpathRelative");

      final String xpathAbs = getTextStrict(fieldMeta, "xpathAbsolute");
      // TODO tttt compare hierarchy.
      System.out.println("xpathRel=" + xpathRel);
      System.out.println("xpathAsb=" + xpathAbs);
      System.out.println("xmlEleme=" + EditorXmlUtils.getNodePath(xmlNodeElem));

      // xpathAb=/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']
      // xmlElem=/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']

      final PhysicalXpath xpathInfo = handleXpath(xpathRel);
      xpathRel = xpathInfo.getXpath();
      final Optional<String> schemeNameOpt = xpathInfo.getSchemeNameOpt();

      Element firstElem = null;
      Element previousElem = null;
      Element partElem = null;

      // TODO Use ANTLR xpath grammar later.
      final String[] partsArr = xpathRel.split("/");
      final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
      System.out.println("PARTS FIELD: " + parts);
      for (final String part : parts) {

        final NodeList elementsByTagName =
            previousElem != null ? previousElem.getElementsByTagName(part)
                : xmlNodeElem.getElementsByTagName(part);
        System.out.println("length=" + elementsByTagName.getLength() + " for part=" + part);

        if (elementsByTagName.getLength() > 0) {
          final Node xmlNode;
          if (elementsByTagName.getLength() > 1) {
            xmlNode = elementsByTagName.item(0); // Which? 0, 1, ... todo use xpath selector?
          } else {
            xmlNode = elementsByTagName.item(0);
          }
          if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
            // An existing element was found, reuse it.
            partElem = (Element) xmlNode;
          } else {
            throw new RuntimeException(String.format("NodeType=%s not an Element", xmlNode));
          }
        } else {
          partElem = doc.createElement(part);
        }

        if (previousElem != null) {
          previousElem.appendChild(partElem);
        }

        if (firstElem == null) {
          // This is the newest parent closest to the xmlElem.
          firstElem = partElem;
        }

        previousElem = partElem;
      }


      // The last element is a leaf, so it is a field in this case.F
      Validate.notNull(partElem, "partElem is null for %s", fieldId);
      partElem.setTextContent(value);
      if (schemeNameOpt.isPresent()) {
        partElem.setAttribute("schemeName", schemeNameOpt.get());
      }
      if (schemeNameOpt.isEmpty() && xpathAbs.contains("cbc:CompanyID[not(@schemeName = 'EU')")) {
        // TODO tttt How to get schemeName 'national'?
        // Temporarily hardcode it here so that other things be figured out.
        partElem.setAttribute("schemeName", "national");
      }
      partElem.setAttribute("debugId", fieldId);

      if (firstElem != null) {
        xmlNodeElem.appendChild(firstElem);
      } else {
        xmlNodeElem.appendChild(firstElem);
      }
    }
  }

  private static PhysicalXpath handleXpath(final String xpathParam) {
    String xpath = xpathParam;
    if (xpath.contains("[not(")) {
      // TEMPORARY FIX.
      // Ignore predicate with negation as it is not useful for XML generation.
      // Example:
      // "xpathAbsolute" :
      // "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName =
      // 'EU')]/cbc:RegistrationName",
      xpath = xpath.substring(0, xpath.indexOf('['));
    }
    final Optional<String> schemeNameOpt;
    if (xpath.contains("[@schemeName = '")) {
      // Example:
      // "xpathAbsolute" : "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName =
      // 'EU']",
      // Here we want to extract EU text.
      final int indexOfSchemeName = xpath.indexOf("[@schemeName = '");
      String schemeName = xpath.substring(indexOfSchemeName + "[@schemeName = '".length());
      // Remove the ']
      schemeName = schemeName.substring(0, schemeName.length() - "']".length());
      Validate.notBlank(schemeName);
      xpath = xpath.substring(0, indexOfSchemeName);
      schemeNameOpt = Optional.of(schemeName);
    } else {
      schemeNameOpt = Optional.empty();
    }
    return new PhysicalXpath(xpath, schemeNameOpt);
  }

}
