package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;

public class NoticeSaverTest {

  private static ObjectNode setupVisualModel(final ObjectMapper mapper, final String fakeSdkForTest,
      final String noticeSubTypeForTest) {
    final ObjectNode root = mapper.createObjectNode();

    // NOTICE METADATA.
    {
      // SDK version.
      final ObjectNode vis = mapper.createObjectNode();
      root.set("OPT-002-notice-1", vis);
      vis.put("contentId", "OPT-002-notice");
      vis.put("type", "field");
      vis.put("value", fakeSdkForTest);
      vis.put("contentCount", "1");
      vis.put("contentParentCount", "1");
    }
    {
      // Notice sub type.
      final ObjectNode vis = mapper.createObjectNode();
      root.set("OPP-070-notice-1", vis);
      vis.put("contentId", "OPP-070-notice");
      vis.put("type", "field");
      vis.put("value", noticeSubTypeForTest);
      vis.put("contentCount", "1");
      vis.put("contentParentCount", "1");
    }

    // NOTICE CONTENT.
    {
      final ObjectNode vis = mapper.createObjectNode();
      root.set("BT-505-Business-1", vis);
      vis.put("contentId", "BT-505-Business");
      vis.put("type", "field");
      vis.put("value", "http://www.acme-solution.co.uk");
      vis.put("contentCount", "1");
      vis.put("contentParentCount", "1");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      root.set("BT-501-Business-European-1", vis);
      vis.put("contentId", "BT-501-Business-European");
      vis.put("type", "field");
      vis.put("value", "The EU registration number");
      vis.put("contentCount", "1");
      vis.put("contentParentCount", "1");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      root.set("OPP-113-Business-European-1", vis);
      vis.put("contentId", "OPP-113-Business-European");
      vis.put("type", "field");
      vis.put("value", "2020-11-14+01:00");
      vis.put("contentCount", "1");
      vis.put("contentParentCount", "1");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      root.set("BT-500-Business-1", vis);
      vis.put("contentId", "BT-500-Business");
      vis.put("type", "field");
      vis.put("value", "ACME Solution");
      vis.put("contentCount", "1");
      vis.put("contentParentCount", "1");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      root.set("BT-501-Business-National-1", vis);
      vis.put("contentId", "BT-501-Business-National");
      vis.put("type", "field");
      vis.put("value", "The national registration number");
      vis.put("contentCount", "1");
      vis.put("contentParentCount", "1");
    }

    // Repeating node for 'sector'.
    {
      final ObjectNode vis = mapper.createObjectNode();
      root.set("OPP-105-Business-1", vis);
      vis.put("contentId", "OPP-105-Business");
      vis.put("type", "field");
      vis.put("value", "education");
      vis.put("contentCount", "1");
      vis.put("contentParentCount", "1");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      root.set("OPP-105-Business-2", vis);
      vis.put("contentId", "OPP-105-Business");
      vis.put("value", "health");
      vis.put("contentCount", "2");
      vis.put("type", "field");
      vis.put("contentParentCount", "1");
    }

    {
      final ObjectNode vis = mapper.createObjectNode();
      root.set("OPP-100-Business-1", vis);
      vis.put("contentId", "OPP-100-Business");
      vis.put("type", "field");
      vis.put("value", "reg");
      vis.put("contentCount", "1");
      vis.put("contentParentCount", "1");
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
      field.put("repeatable", false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("OPP-070-notice", field);
      field.put("parentNodeId", "ND-RootExtension");
      field.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:NoticeSubType/cbc:SubTypeCode");
      field.put("xpathRelative", "efac:NoticeSubType/cbc:SubTypeCode");
      field.put("type", "code");
      field.put("repeatable", false);
      field.put("codeListId", "notice-subtype");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-505-Business", field);
      field.put("parentNodeId", "ND-BusinessParty");
      field.put("xpathAbsolute", "/*/cac:BusinessParty/cbc:WebsiteURI");
      field.put("xpathRelative", "cbc:WebsiteURI");
      field.put("type", "url");
      field.put("repeatable", false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-500-Business", field);
      field.put("parentNodeId", "ND-LocalEntity");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]/cbc:RegistrationName");
      field.put("xpathRelative", "cbc:RegistrationName");
      field.put("type", "text");
      field.put("repeatable", false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-501-Business-European", field);
      field.put("parentNodeId", "ND-EuEntity");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']/cbc:CompanyID[@schemeName = 'EU']");
      field.put("xpathRelative", "cbc:CompanyID[@schemeName = 'EU']");
      field.put("type", "id");
      field.put("repeatable", false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-501-Business-National", field);
      field.put("parentNodeId", "ND-LocalEntity");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[not(@schemeName = 'EU')]/cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put("xpathRelative", "cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put("type", "id");
      field.put("repeatable", false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("OPP-105-Business", field);
      field.put("parentNodeId", "ND-Root");
      field.put("xpathAbsolute", "/*/cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put("xpathRelative", "cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put("type", "code");
      field.put("repeatable", true);
      field.put("codeListId", "main-activity");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("OPP-100-Business", field);
      field.put("parentNodeId", "ND-OperationType");
      field.put("xpathAbsolute", "/*/efac:NoticePurpose/cbc:PurposeCode");
      field.put("xpathRelative", "cbc:PurposeCode");
      field.put("type", "code");
      field.put("repeatable", false);
      field.put("codeListId", "notice-purpose");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("OPP-113-Business-European", field);
      field.put("parentNodeId", "ND-EuEntity");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']/cbc:RegistrationDate");
      field.put("xpathRelative", "cbc:RegistrationDate");
      field.put("type", "date");
      field.put("repeatable", false);
    }
  }

  private static void setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper,
      final Map<String, JsonNode> nodeById) {
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-Root", node);
      node.put("xpathAbsolute", "/*");
      node.put("xpathRelative", "/*");
      node.put("repeatable", false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-RootExtension", node);
      node.put("parentId", "ND-Root");
      node.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put("xpathRelative",
          "ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put("repeatable", false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-BusinessParty", node);
      node.put("parentId", "ND-Root");
      node.put("xpathAbsolute", "/*/cac:BusinessParty");
      node.put("xpathRelative", "cac:BusinessParty");
      node.put("repeatable", false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-LocalEntity", node);
      node.put("parentId", "ND-BusinessParty");
      node.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      node.put("xpathRelative", "cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      node.put("repeatable", false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-OperationType", node);
      node.put("parentId", "ND-Root");
      node.put("xpathAbsolute", "/*/efac:NoticePurpose");
      node.put("xpathRelative", "efac:NoticePurpose");
      node.put("repeatable", false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-EuEntity", node);
      node.put("parentId", "ND-BusinessParty");
      node.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      node.put("xpathRelative", "cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      node.put("repeatable", false);
    }
  }

  @SuppressWarnings("static-method")
  @Test
  public void test() throws ParserConfigurationException, IOException {
    final ObjectMapper mapper = new ObjectMapper();

    final String prefixedSdkVersion = "eforms-sdk-" + "1.1.0";
    final String noticeSubType = "X02";

    final ObjectNode root = setupVisualModel(mapper, prefixedSdkVersion, noticeSubType);

    //
    // NODES from fields.json
    //
    final Map<String, JsonNode> nodeById = new LinkedHashMap<>();
    setupFieldsJsonXmlStructureNodes(mapper, nodeById);

    //
    // FIELDS from fields.json
    //
    final Map<String, JsonNode> fieldById = new LinkedHashMap<>();
    setupFieldsJsonFields(mapper, fieldById);

    //
    // OTHER from notice-types.json
    //
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
    final boolean debug = true; // Very useful for the testing.
    final boolean buildFields = true;
    SchemaInfo schemaInfo = SchemaToolsTest.getTestSchemaInfo();
    final PhysicalModel pm = NoticeSaver.buildPhysicalModelXml(fieldsAndNodes, noticeInfoBySubtype,
        documentInfoByType, conceptualModel, debug, buildFields, schemaInfo);

    System.out.println("XML as text:");

    final String xml = pm.getXmlAsText(false); // Not indented to avoid line breaks.

    System.out.println(pm.getXmlAsText(true));

    // Check fields root node.
    assertTrue(xml.contains("BusinessRegistrationInformationNotice"));
    assertTrue(xml.contains("xmlns="));

    // Check some metadata.
    assertTrue(xml.contains(noticeSubType));
    assertTrue(xml.contains(prefixedSdkVersion));
    assertEquals(1, StringUtils.countMatches(xml, "<cbc:CustomizationID"));
    assertEquals(1, StringUtils.countMatches(xml, "<cbc:SubTypeCode"));

    // Check nodes.
    assertEquals(1, StringUtils.countMatches(xml, "<BusinessRegistrationInformationNotice"));
    assertEquals(1, StringUtils.countMatches(xml, "<ext:UBLExtensions>"));
    assertEquals(1, StringUtils.countMatches(xml, "<cac:BusinessParty>"));
    assertEquals(1, StringUtils.countMatches(xml, "<efac:NoticePurpose>"));

    // Not passing yet:
    // assertEquals(2, StringUtils.countMatches(xmlText, "<cac:PartyLegalEntity>"));

    // Check fields.
    assertTrue(xml.contains("OPP-070-notice\""));
    assertTrue(xml.contains("BT-500-Business\""));

    assertTrue(xml.contains("BT-501-Business-National\""));
    assertTrue(xml.contains("BT-501-Business-National\" schemeName=\"national\""));

    assertTrue(xml.contains("BT-501-Business-European\""));
    assertTrue(xml.contains("BT-501-Business-European\" schemeName=\"EU\""));

    assertTrue(xml.contains("OPP-100-Business\""));

    assertTrue(xml.contains("OPP-105-Business\""));
    assertTrue(xml.contains(">education<"));
    assertTrue(xml.contains(">health<"));

    assertTrue(xml.contains("listName=\"sector\">education<"));
    assertTrue(xml.contains("listName=\"sector\">health<"));
  }

}
