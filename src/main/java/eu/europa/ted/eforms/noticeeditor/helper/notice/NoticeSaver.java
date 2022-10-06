package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.util.JsonUtils.getTextStrict;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

public class NoticeSaver {

  private static final Logger logger = LoggerFactory.getLogger(NoticeSaver.class);

  public static Map<String, ConceptNode> buildConceptualModel(final ObjectNode fields,
      final ObjectNode nodes, final JsonNode visualRoot) {
    Validate.notNull(visualRoot, "visualRoot");

    logger.info("Attempting to build conceptual model.");
    final Map<String, ConceptNode> conceptNodeById = new HashMap<>();

    for (JsonNode visualItem : visualRoot) {
      final String type = getTextStrict(visualItem, "type");
      if ("field".equals(type)) {

        // If the type is field then the contentId is a field id.
        final String contentId = getTextStrict(visualItem, "contentId");
        // final String contentNodeId = visual.get("contentNodeId").asText(null);
        System.out.println(contentId);
        final JsonNode fieldMeta = fields.get(contentId);

        final String value = getTextStrict(visualItem, "value");
        final ConceptField conceptField = new ConceptField(contentId, value);

        final String parentNodeId = getTextStrict(fieldMeta, "parentNodeId");
        final ConceptNode conceptNode = buildAncestryRec(conceptNodeById, parentNodeId, nodes);
        conceptNode.addConceptField(conceptField);
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
      final String nodeId, final JsonNode nodes) {
    final JsonNode nodeMeta = nodes.get(nodeId);
    ConceptNode conceptNode = conceptNodeById.get(nodeId);
    if (conceptNode == null) {
      conceptNode = new ConceptNode(nodeId);
      conceptNodeById.put(nodeId, conceptNode);
      final Optional<String> parentNodeIdOpt = JsonUtils.getTextOpt(nodeMeta, "parentId");
      if (parentNodeIdOpt.isPresent()) {
        final ConceptNode pcn = buildAncestryRec(conceptNodeById, parentNodeIdOpt.get(), nodes);
        pcn.addConceptNode(conceptNode);
      }
    }
    return conceptNode;
  }

  public static Document buildPhysicalModel(final ObjectNode fields, final ObjectNode nodes,
      final ConceptNode conceptRoot) throws ParserConfigurationException {
    logger.info("Attempting to build physical model.");

    final DocumentBuilder safeDocBuilder =
        SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);
    final Document doc = safeDocBuilder.newDocument();
    doc.setXmlStandalone(true);
    // XML Namespaces.
    // doc.createAttributeNS("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
    // doc.createAttributeNS("xmlns:cbc",
    // "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
    // doc.createAttributeNS("xmlns:cac",
    // "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
    // doc.createAttributeNS("xmlns:efext", "http://data.europa.eu/p27/eforms-ubl-extensions/1");
    // doc.createAttributeNS("xmlns:efac",
    // "http://data.europa.eu/p27/eforms-ubl-extension-aggregate-components/1");
    // doc.createAttributeNS("xmlns:efbc",
    // "http://data.europa.eu/p27/eforms-ubl-extension-basic-components/1");
    // doc.createAttributeNS("xmlns:ext",
    // "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");

    // TODO get namespace from notice-types.json
    // final String namespaceUri =
    // "http://data.europa.eu/p27/eforms-business-registration-information-notice/1";
    // doc.createAttributeNS(namespaceUri, "xmlns");

    // TODO get this from notice-types.json
    final String rootElementType = "BusinessRegistrationInformationNotice";
    // "documentTypes" : [ {
    // "id" : "BRIN",
    // "namespace" : "http://data.europa.eu/p27/eforms-business-registration-information-notice/1",
    // "rootElement" : "BusinessRegistrationInformationNotice"
    // }, {

    final Element rootElement = doc.createElement(rootElementType);
    doc.appendChild(rootElement);
    buildPhysicalModelRec(fields, nodes, conceptRoot, doc, rootElement);
    return doc;
  }

  private static void buildPhysicalModelRec(final ObjectNode fields, final ObjectNode nodes,
      final ConceptNode conceptElem, final Document doc, final Element xmlElem) {

    final List<ConceptNode> conceptNodes = conceptElem.getConceptNodes();
    for (final ConceptNode conceptElemChild : conceptNodes) {
      final JsonNode nodeMeta = nodes.get(conceptElemChild.getId());
      final String xpathRelative = getTextStrict(nodeMeta, "xpathRelative");
      // TODO
      // System.out.println(xpathRelative);
      // final Element xmlElemChild = doc.createElement(xpathRelative);
      // buildPhysicalModelRec(fields, nodes, conceptElemChild, doc, xmlElemChild);
    }

    final List<ConceptField> conceptFields = conceptElem.getConceptFields();
    for (final ConceptField conceptField : conceptFields) {
      final String value = conceptField.getValue();
      final JsonNode fieldMeta = fields.get(conceptField.getId());
      final String xpathRelative = getTextStrict(fieldMeta, "xpathRelative");
      System.out.println(xpathRelative);
      final Element elem = doc.createElement(xpathRelative);
      elem.setTextContent(value);
      xmlElem.appendChild(elem);
    }
  }

  public static void main(String[] args) throws ParserConfigurationException {
    final ObjectMapper mapper = new ObjectMapper();

    final ObjectNode root = mapper.createObjectNode();
    {
      final ObjectNode visual = mapper.createObjectNode();
      root.set("OPT-002-notice-1", visual);
      visual.put("domId", "editor-id-OPT-002-notice-0001");
      visual.put("contentId", "OPT-002-notice");
      visual.put("type", "field");
      visual.put("contentCount", "1");
      visual.put("value", "eforms-sdk-1.1.0");
      visual.put("contentParentId", "THE_METADATA-container-elem");
      visual.put("contentParentCount", "1");
      visual.put("contentNodeId", "ND-Root");
    }
    {
      final ObjectNode visual = mapper.createObjectNode();
      root.set("OPP-070-notice-1", visual);
      visual.put("domId", "editor-id-OPP-070-notice-0001");
      visual.put("contentId", "OPP-070-notice");
      visual.put("type", "field");
      visual.put("contentCount", "1");
      visual.put("value", "X02");
      visual.put("contentParentId", "THE_METADATA-container-elem");
      visual.put("contentParentCount", "1");
      visual.put("contentNodeId", "ND-RootExtension");
    }

    final ObjectNode fields = mapper.createObjectNode();
    {
      final ObjectNode field = mapper.createObjectNode();
      fields.set("OPT-002-notice", field);
      field.put("parentNodeId", "ND-Root");
      field.put("btId", "OPT-002");
      field.put("xpathAbsolute", "/*/cbc:CustomizationID");
      field.put("xpathRelative", "cbc:CustomizationID");
      field.put("type", "id");
      // field.put("repeatable", "false");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fields.set("OPP-070-notice", field);
      field.put("parentNodeId", "ND-RootExtension");
      field.put("btId", "OPP-070");
      field.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:NoticeSubType/cbc:SubTypeCode");
      field.put("xpathRelative", "efac:NoticeSubType/cbc:SubTypeCode");
      field.put("type", "code");
      // field.put("repeatable", "false");
    }

    final ObjectNode nodes = mapper.createObjectNode();
    {
      final ObjectNode node = mapper.createObjectNode();
      nodes.set("ND-Root", node);
      node.put("xpathAbsolute", "/*");
      node.put("xpathRelative", "/*");
      node.put("repeatable", "false");
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodes.set("ND-RootExtension", node);
      node.put("parentId", "ND-Root");
      node.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put("xpathRelative",
          "ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put("repeatable", "false");
    }

    final Map<String, ConceptNode> conceptNodeById =
        NoticeSaver.buildConceptualModel(fields, nodes, root);

    final ConceptNode conceptRoot = conceptNodeById.get("ND-Root");
    final Document doc = NoticeSaver.buildPhysicalModel(fields, nodes, conceptRoot);
    System.out.println(EditorXmlUtils.asText(doc));

    // saveNotice(Optional.empty(), root.toString(), fields, nodes);
  }

}
