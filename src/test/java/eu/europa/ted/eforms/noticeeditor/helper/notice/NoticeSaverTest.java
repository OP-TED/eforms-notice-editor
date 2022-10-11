package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;

public class NoticeSaverTest {

  private static ObjectNode setupVisualModel(final ObjectMapper mapper, final String fakeSdkForTest,
      final String noticeSubTypeForTest) {
    final ObjectNode root = mapper.createObjectNode();
    {
      final ObjectNode visual = mapper.createObjectNode();
      root.set("OPT-002-notice-1", visual);
      visual.put("domId", "editor-id-OPT-002-notice-0001");
      visual.put("contentId", "OPT-002-notice");
      visual.put("type", "field");
      visual.put("value", fakeSdkForTest);
      visual.put("contentCount", "1");
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
      visual.put("value", noticeSubTypeForTest);
      visual.put("contentCount", "1");
      visual.put("contentParentId", "THE_METADATA-container-elem");
      visual.put("contentParentCount", "1");
      visual.put("contentNodeId", "ND-RootExtension");
    }
    {
      final ObjectNode visual = mapper.createObjectNode();
      root.set("BT-500-Business-1", visual);
      visual.put("domId", "editor-id-BT-500-Business-0001");
      visual.put("contentId", "BT-500-Business");
      visual.put("type", "field");
      visual.put("value", "The official name XYZ");
      visual.put("contentCount", "1");
      visual.put("contentParentId", "GR-Business-Party-container-elem");
      visual.put("contentParentCount", "1");
      visual.put("contentNodeId", "ND-LocalEntity");
    }
    {
      final ObjectNode visual = mapper.createObjectNode();
      root.set("BT-501-Business-European-1", visual);
      visual.put("domId", "editor-id-BT-501-Business-European-0001");
      visual.put("contentId", "BT-501-Business-European");
      visual.put("type", "field");
      visual.put("value", "The EU registration number");
      visual.put("contentCount", "1");
      visual.put("contentParentId", "GR-Business-Party-container-elem");
      visual.put("contentParentCount", "1");
      visual.put("contentNodeId", "ND-BusinessParty");
    }
    {
      final ObjectNode visual = mapper.createObjectNode();
      root.set("BT-501-Business-National-1", visual);
      visual.put("domId", "editor-id-BT-501-Business-National-0001");
      visual.put("contentId", "BT-501-Business-National");
      visual.put("type", "field");
      visual.put("value", "The national registration number");
      visual.put("contentCount", "1");
      visual.put("contentParentId", "GR-Business-Party-container-elem");
      visual.put("contentParentCount", "1");
      visual.put("contentNodeId", "ND-BusinessParty");
    }

    return root;
  }

  private static void setupFieldsJsonFields(final ObjectMapper mapper,
      final Map<String, JsonNode> fieldById) {
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
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-501-Business-European", field);
      field.put("parentNodeId", "ND-BusinessParty");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']");
      field.put("xpathRelative", "cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']");
      field.put("type", "id");
      // field.put("repeatable", "false");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-501-Business-National", field);
      field.put("parentNodeId", "ND-BusinessParty");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put("xpathRelative", "cac:PartyLegalEntity/cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put("type", "id");
      // field.put("repeatable", "false");
    }
  }

  private static void setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper,
      final Map<String, JsonNode> nodeById) {
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
  }

  @SuppressWarnings("static-method")
  @Test
  public void test() throws ParserConfigurationException {
    final ObjectMapper mapper = new ObjectMapper();

    final String prefixedSdkVersion = "eforms-sdk-1.1.0";
    final String noticeSubType = "X02";

    final ObjectNode root = setupVisualModel(mapper, prefixedSdkVersion, noticeSubType);

    // fields.json NODES.
    final Map<String, JsonNode> nodeById = new HashMap<>();
    setupFieldsJsonXmlStructureNodes(mapper, nodeById);

    // fields.json FIELDS.
    final Map<String, JsonNode> fieldById = new HashMap<>();
    setupFieldsJsonFields(mapper, fieldById);

    // notice-types.json
    // Setup dummy notice-types.json info that we need for the XML generation.
    final Map<String, JsonNode> noticeInfoBySubtype = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("documentType", "BRIN");
      noticeInfoBySubtype.put(noticeSubType, info);
    }
    final Map<String, JsonNode> documentInfoByType = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("namespace",
          "http://data.europa.eu/p27/eforms-business-registration-information-notice/1");
      info.put("rootElement", "BusinessRegistrationInformationNotice");
      documentInfoByType.put("BRIN", info);
    }

    //
    // BUILD CONCEPTUAL MODEL.
    //
    final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldById, nodeById);
    final Map<String, ConceptNode> conceptNodeById =
        NoticeSaver.buildConceptualModel(fieldsAndNodes, root);
    final ConceptNode conceptRoot = conceptNodeById.get(SdkService.ND_ROOT);
    final ConceptualModel conceptualModel = new ConceptualModel(conceptRoot);

    //
    // BUILD PHYSICAL MODEL.
    //
    final Document doc = NoticeSaver.buildPhysicalModelXml(fieldsAndNodes, noticeInfoBySubtype,
        documentInfoByType, conceptualModel);

    System.out.println("XML output");
    final String xmlText = EditorXmlUtils.asText(doc, true);
    System.out.println(xmlText);

    assertTrue(xmlText.contains(noticeSubType));
    assertTrue(xmlText.contains(prefixedSdkVersion));
    assertTrue(xmlText.contains("OPP-070-notice\""));
    assertTrue(xmlText.contains("BT-500-Business\""));
    assertTrue(xmlText.contains("BT-501-Business-National\""));

    // TODO tttt european was overwritten by national due to schemaName issues
    assertTrue(xmlText.contains("BT-501-Business-European\""));

    // saveNotice(Optional.empty(), root.toString(), fields, nodes);
  }
}
