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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
      }
      // else
      // TODO node?
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

  public static Document buildPhysicalModel(final FieldsAndNodes fieldsAndNodes,
      final Map<String, JsonNode> noticeInfoBySubtype,
      final Map<String, JsonNode> documentInfoByType, final ConceptualModel concept)
      throws ParserConfigurationException {
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

    // TODO get this from notice-types.json

    final String noticeSubType = concept.getNoticeSubType();
    final JsonNode noticeInfo = noticeInfoBySubtype.get(noticeSubType);
    final String documentType = JsonUtils.getTextStrict(noticeInfo, "documentType");
    final JsonNode documentTypeInfo = documentInfoByType.get(documentType);
    final String namespaceUri = JsonUtils.getTextStrict(documentTypeInfo, "namespace");
    // doc.createAttributeNS(namespaceUri, "xmlns");

    final String rootElementType = JsonUtils.getTextStrict(documentTypeInfo, "rootElement");
    final Element rootElement = doc.createElement(rootElementType);
    doc.appendChild(rootElement);

    buildPhysicalModelRec(fieldsAndNodes, concept.getRoot(), doc, rootElement);

    return doc;
  }

  private static void buildPhysicalModelRec(final FieldsAndNodes fieldsAndNodes,
      final ConceptNode conceptElem, final Document doc, final Element xmlElem) {

    // NODES.
    final List<ConceptNode> conceptNodes = conceptElem.getConceptNodes();
    for (final ConceptNode conceptElemChild : conceptNodes) {
      final JsonNode nodeMeta = fieldsAndNodes.getNodeById(conceptElemChild.getId());
      final String xpath = getTextStrict(nodeMeta, "xpathAbsolute");

      Element firstElem = null;
      Element previousElem = null;
      Element partElem = null;

      // xpathRelative can contain many xml elements. We must build the hierarchy.
      // TODO Use ANTLR xpath grammar later.
      // TODO maybe use xpath to locate the tag in the doc ? What xpath finds is where to add the
      // data.
      // doc.find using xpath

      final String[] partsArr = xpath.split("/");
      final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
      parts.remove(0);
      parts.remove(0);
      System.out.println("PARTS NODE: " + parts);
      int i = 0;
      for (String part : parts) {
        System.out.println(part);
        partElem = doc.createElement(part);
        if (previousElem != null) {
          previousElem.appendChild(partElem);
        }
        if (i == 0) {
          firstElem = partElem;
        }
        previousElem = partElem;
        i++;
      }
      Validate.notNull(firstElem);
      Validate.notNull(partElem);
      xmlElem.appendChild(firstElem);

      buildPhysicalModelRec(fieldsAndNodes, conceptElemChild, doc, partElem);
    }

    //
    // FIELDS.
    //
    final List<ConceptField> conceptFields = conceptElem.getConceptFields();
    for (final ConceptField conceptField : conceptFields) {
      final String value = conceptField.getValue();
      final JsonNode fieldMeta = fieldsAndNodes.getFieldById(conceptField.getId());
      final String xpath = getTextStrict(fieldMeta, "xpathRelative");

      Element previousElem = null;
      Element partElem = null;
      // TODO Use ANTLR xpath grammar later.
      final String[] partsArr = xpath.split("/");
      final List<String> parts = new ArrayList<>(Arrays.asList(partsArr));
      System.out.println("PARTS FIELD: " + parts);
      for (String part : parts) {
        partElem = doc.createElement(part);
        if (previousElem != null) {
          previousElem.appendChild(partElem);
        }
        previousElem = partElem;
      }
      Validate.notNull(partElem);
      partElem.setTextContent(value);
      xmlElem.appendChild(partElem);
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

    // {
    // final ObjectNode visual = mapper.createObjectNode();
    // root.set("BT-500-Business-1", visual);
    // visual.put("domId", "editor-id-BT-500-Business-0001");
    // visual.put("contentId", "BT-500-Business");
    // visual.put("type", "field");
    // visual.put("contentCount", "1");
    // visual.put("value", "The official name XYZ");
    // visual.put("contentParentId", "GR-Business-Party-container-elem");
    // visual.put("contentParentCount", "1");
    // visual.put("contentNodeId", "ND-LocalEntity");
    // }

    // NODES.
    final Map<String, JsonNode> nodeById = new HashMap<>();
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-Root", node);
      node.put("xpathAbsolute", "/*");
      node.put("xpathRelative", "/*");
      node.put("repeatable", "false");
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-RootExtension", node);
      node.put("parentId", "ND-Root");
      node.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put("xpathRelative",
          "ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put("repeatable", "false");
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-BusinessParty", node);
      node.put("parentId", "ND-Root");
      node.put("xpathAbsolute", "/*/cac:BusinessParty");
      node.put("xpathRelative", "cac:BusinessParty");
      node.put("repeatable", "false");
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-LocalEntity", node);
      node.put("parentId", "ND-BusinessParty");
      node.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      node.put("xpathRelative", "cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      node.put("repeatable", "false");
    }

    // FIELDS.
    final Map<String, JsonNode> fieldById = new HashMap<>();
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("OPT-002-notice", field);
      field.put("parentNodeId", "ND-Root");
      field.put("xpathAbsolute", "/*/cbc:CustomizationID");
      field.put("xpathRelative", "cbc:CustomizationID");
      field.put("type", "id");
      // field.put("repeatable", "false");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("OPP-070-notice", field);
      field.put("parentNodeId", "ND-RootExtension");
      field.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:NoticeSubType/cbc:SubTypeCode");
      field.put("xpathRelative", "efac:NoticeSubType/cbc:SubTypeCode");
      field.put("type", "code");
      // field.put("repeatable", "false");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-500-Business", field);
      field.put("parentNodeId", "ND-LocalEntity");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]/cbc:RegistrationName");
      field.put("xpathRelative", "cbc:RegistrationName");
      field.put("type", "text");
      // field.put("repeatable", "false");
    }

    final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldById, nodeById);
    final Map<String, ConceptNode> conceptNodeById =
        NoticeSaver.buildConceptualModel(fieldsAndNodes, root);

    final ConceptNode conceptRoot = conceptNodeById.get("ND-Root");
    final ConceptualModel conceptualModel = new ConceptualModel(conceptRoot);

    final Map<String, JsonNode> noticeInfoBySubtype = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("documentType", "BRIN");
      noticeInfoBySubtype.put("X02", info);
    }

    final Map<String, JsonNode> documentInfoByType = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("namespace",
          "http://data.europa.eu/p27/eforms-business-registration-information-notice/1");
      info.put("rootElement", "BusinessRegistrationInformationNotice");
      documentInfoByType.put("BRIN", info);
    }

    final Document doc = NoticeSaver.buildPhysicalModel(fieldsAndNodes, noticeInfoBySubtype,
        documentInfoByType, conceptualModel);
    System.out.println(EditorXmlUtils.asText(doc));

    // saveNotice(Optional.empty(), root.toString(), fields, nodes);
  }

}
