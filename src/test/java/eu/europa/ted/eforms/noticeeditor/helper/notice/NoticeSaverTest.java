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
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

public class NoticeSaverTest {

  private static ObjectNode setupVisualModel(final ObjectMapper mapper, final String fakeSdkForTest,
      final String noticeSubTypeForTest) {
    final ObjectNode root = mapper.createObjectNode();

    //
    // DUMMY X02 NOTICE DATA (as if coming from a web form before we have the XML).
    //

    {
      // SDK version.
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("OPT-002-notice-1", vis);
      vis.put("contentId", "OPT-002-notice");
      vis.put("value", fakeSdkForTest);
    }
    {
      // Notice sub type.
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("OPP-070-notice-1", vis);
      vis.put("contentId", "OPP-070-notice");
      vis.put("value", noticeSubTypeForTest);
    }

    //
    // NOTICE CONTENT.
    //
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("BT-505-Business-1", vis);
      vis.put("contentId", "BT-505-Business");
      vis.put("value", "http://www.acme-solution.co.uk");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("BT-501-Business-European-1", vis);
      vis.put("contentId", "BT-501-Business-European");
      vis.put("value", "The EU registration number");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("OPP-113-Business-European-1", vis);
      vis.put("contentId", "OPP-113-Business-European");
      vis.put("value", "2020-11-14+01:00");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("BT-500-Business-1", vis);
      vis.put("contentId", "BT-500-Business");
      vis.put("value", "ACME Solution");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("BT-501-Business-National-1", vis);
      vis.put("contentId", "BT-501-Business-National");
      vis.put("value", "The national registration number");
    }

    // Repeating node for 'sector'.
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("OPP-105-Business-1", vis);
      vis.put("contentId", "OPP-105-Business");
      vis.put("value", "education");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("OPP-105-Business-2", vis);
      vis.put("contentId", "OPP-105-Business");
      vis.put("value", "health");
      vis.put("contentCount", "2");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set("OPP-100-Business-1", vis);
      vis.put("contentId", "OPP-100-Business");
      vis.put("value", "reg");
    }

    return root;
  }

  private static void putDefault(final ObjectNode vis) {
    vis.put("type", "field");
    vis.put("contentCount", "1");
    vis.put("contentParentCount", "1");
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
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("OPP-070-notice", field);
      field.put("parentNodeId", "ND-RootExtension");
      field.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:NoticeSubType/cbc:SubTypeCode");
      field.put("xpathRelative", "efac:NoticeSubType/cbc:SubTypeCode");
      field.put("type", "code");
      fieldPutRepeatable(field, false);
      field.put("codeListId", "notice-subtype");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-505-Business", field);
      field.put("parentNodeId", "ND-BusinessParty");
      field.put("xpathAbsolute", "/*/cac:BusinessParty/cbc:WebsiteURI");
      field.put("xpathRelative", "cbc:WebsiteURI");
      field.put("type", "url");
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-500-Business", field);
      field.put("parentNodeId", "ND-LocalEntity");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]/cbc:RegistrationName");
      field.put("xpathRelative", "cbc:RegistrationName");
      field.put("type", "text");
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-501-Business-European", field);
      field.put("parentNodeId", "ND-EuEntity");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']/cbc:CompanyID[@schemeName = 'EU']");
      field.put("xpathRelative", "cbc:CompanyID[@schemeName = 'EU']");
      field.put("type", "id");
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("BT-501-Business-National", field);
      field.put("parentNodeId", "ND-LocalEntity");
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[not(@schemeName = 'EU')]/cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put("xpathRelative", "cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put("type", "id");
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("OPP-105-Business", field);
      field.put("parentNodeId", "ND-Root");
      field.put("xpathAbsolute", "/*/cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put("xpathRelative", "cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put("type", "code");
      fieldPutRepeatable(field, true);
      field.put("codeListId", "main-activity");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put("OPP-100-Business", field);
      field.put("parentNodeId", "ND-OperationType");
      field.put("xpathAbsolute", "/*/efac:NoticePurpose/cbc:PurposeCode");
      field.put("xpathRelative", "cbc:PurposeCode");
      field.put("type", "code");
      fieldPutRepeatable(field, false);
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
      fieldPutRepeatable(field, false);
    }
  }

  private static void fieldPutRepeatable(final ObjectNode field, final boolean repeatable) {
    final ObjectNode prop = JsonUtils.createObjectNode();
    prop.put("value", repeatable);
    field.set("repeatable", prop);
  }

  private static void setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper,
      final Map<String, JsonNode> nodeById) {
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-Root", node);
      node.put("xpathAbsolute", "/*");
      node.put("xpathRelative", "/*");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-RootExtension", node);
      node.put("parentId", "ND-Root");
      node.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put("xpathRelative",
          "ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-BusinessParty", node);
      node.put("parentId", "ND-Root");
      node.put("xpathAbsolute", "/*/cac:BusinessParty");
      node.put("xpathRelative", "cac:BusinessParty");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-LocalEntity", node);
      node.put("parentId", "ND-BusinessParty");
      node.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      node.put("xpathRelative", "cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-EuEntity", node);
      node.put("parentId", "ND-BusinessParty");
      node.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      node.put("xpathRelative", "cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put("ND-OperationType", node);
      node.put("parentId", "ND-Root");
      node.put("xpathAbsolute", "/*/efac:NoticePurpose");
      node.put("xpathRelative", "efac:NoticePurpose");
      fieldPutRepeatable(node, false);
    }

  }

  /**
   * Based on X02 meta data. This unit test relies on test metadata and only tests parts of X02 but
   * enough to cover the basics. It setups dummy fields and nodes metadata, notice-types metadata,
   * and some notice data. It will be hard to maintain but is fully self-contained, so it can also
   * be used to quickly debug a problem by modification of the dummy data or metadata.
   */
  @SuppressWarnings("static-method")
  @Test
  public void testDummy() throws ParserConfigurationException, IOException {
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
    final boolean debug = true; // Adds field ids in the XML.
    final boolean buildFields = true;
    final SchemaInfo schemaInfo = SchemaToolsTest.getTestSchemaInfo();
    final PhysicalModel pm = NoticeSaver.buildPhysicalModelXml(fieldsAndNodes, noticeInfoBySubtype,
        documentInfoByType, conceptualModel, debug, buildFields, schemaInfo);

    System.out.println("XML as text:");

    final String xml = pm.getXmlAsText(false); // Not indented to avoid line breaks.
    System.out.println(pm.getXmlAsText(true));

    contains(xml, "encoding=\"UTF-8\"");

    // Check fields root node.
    contains(xml, "BusinessRegistrationInformationNotice");
    contains(xml, "xmlns=");

    // TODO it would be more maintainable to use xpath to check the XML instead of pure text.

    // Check some metadata.
    contains(xml, noticeSubType);
    contains(xml, prefixedSdkVersion);
    count(xml, 1, "<cbc:CustomizationID");
    count(xml, 1, "<cbc:SubTypeCode");

    // Check nodes.
    count(xml, 1, "<BusinessRegistrationInformationNotice");
    count(xml, 1, "<ext:UBLExtensions>");
    count(xml, 1, "<cac:BusinessParty editorNodeId=\"ND-BusinessParty\"");
    count(xml, 1, "<efac:NoticePurpose editorNodeId=\"ND-OperationType\"");

    // It is the same xml tag, but here we can even check the nodeId is originally correct.
    // Without debug true this editor intermediary information would otherwise be lost.
    count(xml, 1, "<cac:PartyLegalEntity editorNodeId=\"ND-EuEntity\"");
    count(xml, 1, "<cac:PartyLegalEntity editorNodeId=\"ND-LocalEntity\"");

    // Check fields.
    contains(xml, "OPP-070-notice\"");

    contains(xml, "BT-500-Business\"");
    contains(xml, "BT-501-Business-National\"");
    contains(xml, "BT-501-Business-National\" schemeName=\"national\"");

    contains(xml, "BT-501-Business-European\"");
    contains(xml, "BT-501-Business-European\" schemeName=\"EU\"");
    contains(xml, "OPP-113-Business-European\"");

    contains(xml, "OPP-100-Business\"");

    // Test repeatable field OPP-105-Business.
    // Ensure the field is indeed repeatable so that the test itself is not broken.
    assert fieldById.get("OPP-105-Business").get("repeatable").get("value").asBoolean();
    contains(xml, "OPP-105-Business\"");
    contains(xml, ">education<");
    contains(xml, ">health<");
    contains(xml, "OPP-105-Business\" listName=\"sector\">education<");
    contains(xml, "OPP-105-Business\" listName=\"sector\">health<");
  }

  private static final void contains(final String xml, final String text) {
    assertTrue(xml.contains(text), text);
  }

  private static final void count(final String xml, final int expectedCount, final String toMatch) {
    assertEquals(expectedCount, StringUtils.countMatches(xml, toMatch), toMatch);
  }

}
