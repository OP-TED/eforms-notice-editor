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

public class NoticeSaverX02DummyTest {

  private static final String X02 = "X02";
  private static final String BRIN = "BRIN";

  private static final String SECOND = "-2";
  private static final String FIRST = "-1";

  private static final String ND_ROOT = "ND-Root";
  private static final String ND_ROOT_EXTENSION = "ND-RootExtension";
  private static final String ND_OPERATION_TYPE = "ND-OperationType";
  private static final String ND_EU_ENTITY = "ND-EuEntity";
  private static final String ND_LOCAL_ENTITY = "ND-LocalEntity";
  private static final String ND_BUSINESS_PARTY = "ND-BusinessParty";

  private static final String OPT_002_NOTICE = "OPT-002-notice";
  private static final String OPP_070_NOTICE = "OPP-070-notice";
  private static final String OPP_100_BUSINESS = "OPP-100-Business";
  private static final String OPP_105_BUSINESS = "OPP-105-Business";
  private static final String OPP_113_BUSINESS_EUROPEAN = "OPP-113-Business-European";

  private static final String BT_500_BUSINESS = "BT-500-Business";
  private static final String BT_501_BUSINESS_NATIONAL = "BT-501-Business-National";
  private static final String BT_501_BUSINESS_EUROPEAN = "BT-501-Business-European";
  private static final String BT_505_BUSINESS = "BT-505-Business";

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
      root.set(OPT_002_NOTICE + FIRST, vis);
      vis.put("contentId", OPT_002_NOTICE);
      vis.put("value", fakeSdkForTest);
    }
    {
      // Notice sub type.
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_070_NOTICE + FIRST, vis);
      vis.put("contentId", OPP_070_NOTICE);
      vis.put("value", noticeSubTypeForTest);
    }

    //
    // NOTICE CONTENT.
    //
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(BT_505_BUSINESS + FIRST, vis);
      vis.put("contentId", BT_505_BUSINESS);
      vis.put("value", "http://www.acme-solution.co.uk");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(BT_501_BUSINESS_EUROPEAN + FIRST, vis);
      vis.put("contentId", BT_501_BUSINESS_EUROPEAN);
      vis.put("value", "The EU registration number");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_113_BUSINESS_EUROPEAN + FIRST, vis);
      vis.put("contentId", OPP_113_BUSINESS_EUROPEAN);
      vis.put("value", "2020-11-14+01:00");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(BT_500_BUSINESS + FIRST, vis);
      vis.put("contentId", BT_500_BUSINESS);
      vis.put("value", "ACME Solution");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(BT_501_BUSINESS_NATIONAL + FIRST, vis);
      vis.put("contentId", BT_501_BUSINESS_NATIONAL);
      vis.put("value", "The national registration number");
    }

    // Repeating node for 'sector'.
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_105_BUSINESS + FIRST, vis);
      vis.put("contentId", OPP_105_BUSINESS);
      vis.put("value", "education");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_105_BUSINESS + SECOND, vis);
      vis.put("contentId", OPP_105_BUSINESS);
      vis.put("value", "health");
      vis.put("contentCount", "2");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_100_BUSINESS + FIRST, vis);
      vis.put("contentId", OPP_100_BUSINESS);
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
      fieldById.put(OPT_002_NOTICE, field);
      field.put("parentNodeId", ND_ROOT);
      field.put("xpathAbsolute", "/*/cbc:CustomizationID");
      field.put("xpathRelative", "cbc:CustomizationID");
      field.put("type", "id");
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_070_NOTICE, field);
      field.put("parentNodeId", ND_ROOT_EXTENSION);
      field.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:NoticeSubType/cbc:SubTypeCode");
      field.put("xpathRelative", "efac:NoticeSubType/cbc:SubTypeCode");
      field.put("type", "code");
      fieldPutRepeatable(field, false);
      field.put("codeListId", "notice-subtype");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_505_BUSINESS, field);
      field.put("parentNodeId", ND_BUSINESS_PARTY);
      field.put("xpathAbsolute", "/*/cac:BusinessParty/cbc:WebsiteURI");
      field.put("xpathRelative", "cbc:WebsiteURI");
      field.put("type", "url");
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_500_BUSINESS, field);
      field.put("parentNodeId", ND_LOCAL_ENTITY);
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]/cbc:RegistrationName");
      field.put("xpathRelative", "cbc:RegistrationName");
      field.put("type", "text");
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_501_BUSINESS_EUROPEAN, field);
      field.put("parentNodeId", ND_EU_ENTITY);
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']/cbc:CompanyID[@schemeName = 'EU']");
      field.put("xpathRelative", "cbc:CompanyID[@schemeName = 'EU']");
      field.put("type", "id");
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_501_BUSINESS_NATIONAL, field);
      field.put("parentNodeId", ND_LOCAL_ENTITY);
      field.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[not(@schemeName = 'EU')]/cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put("xpathRelative", "cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put("type", "id");
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_105_BUSINESS, field);
      field.put("parentNodeId", ND_ROOT);
      field.put("xpathAbsolute", "/*/cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put("xpathRelative", "cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put("type", "code");
      fieldPutRepeatable(field, true);
      field.put("codeListId", "main-activity");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_100_BUSINESS, field);
      field.put("parentNodeId", ND_OPERATION_TYPE);
      field.put("xpathAbsolute", "/*/efac:NoticePurpose/cbc:PurposeCode");
      field.put("xpathRelative", "cbc:PurposeCode");
      field.put("type", "code");
      fieldPutRepeatable(field, false);
      field.put("codeListId", "notice-purpose");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_113_BUSINESS_EUROPEAN, field);
      field.put("parentNodeId", ND_EU_ENTITY);
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
      nodeById.put(ND_ROOT, node);
      node.put("xpathAbsolute", "/*");
      node.put("xpathRelative", "/*");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_ROOT_EXTENSION, node);
      node.put("parentId", ND_ROOT);
      node.put("xpathAbsolute",
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put("xpathRelative",
          "ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_BUSINESS_PARTY, node);
      node.put("parentId", ND_ROOT);
      node.put("xpathAbsolute", "/*/cac:BusinessParty");
      node.put("xpathRelative", "cac:BusinessParty");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_LOCAL_ENTITY, node);
      node.put("parentId", ND_BUSINESS_PARTY);
      node.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      node.put("xpathRelative", "cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_EU_ENTITY, node);
      node.put("parentId", ND_BUSINESS_PARTY);
      node.put("xpathAbsolute",
          "/*/cac:BusinessParty/cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      node.put("xpathRelative", "cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_OPERATION_TYPE, node);
      node.put("parentId", ND_ROOT);
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
    final String noticeSubType = X02;

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
      info.put("documentType", BRIN);
      noticeInfoBySubtype.put(noticeSubType, info);
    }

    final Map<String, JsonNode> documentInfoByType = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("namespace",
          "http://data.europa.eu/p27/eforms-business-registration-information-notice/1");
      info.put("rootElement", "BusinessRegistrationInformationNotice");
      documentInfoByType.put(BRIN, info);
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
    count(xml, 1, String.format("<cac:BusinessParty editorNodeId=\"%s\"", ND_BUSINESS_PARTY));
    count(xml, 1, String.format("<efac:NoticePurpose editorNodeId=\"%s\"", ND_OPERATION_TYPE));

    // It is the same xml tag, but here we can even check the nodeId is originally correct.
    // Without debug true this editor intermediary information would otherwise be lost.
    count(xml, 1, String.format("<cac:PartyLegalEntity editorNodeId=\"%s\"", ND_EU_ENTITY));
    count(xml, 1, String.format("<cac:PartyLegalEntity editorNodeId=\"%s\"", ND_LOCAL_ENTITY));

    // Check fields.
    contains(xml, OPP_070_NOTICE + "\"");

    contains(xml, BT_500_BUSINESS + "\"");
    contains(xml, BT_501_BUSINESS_NATIONAL + "\"");
    contains(xml, BT_501_BUSINESS_NATIONAL + "\" schemeName=\"national\"");

    contains(xml, BT_501_BUSINESS_EUROPEAN + "\"");
    contains(xml, BT_501_BUSINESS_EUROPEAN + "\" schemeName=\"EU\"");
    contains(xml, OPP_113_BUSINESS_EUROPEAN + "\"");

    contains(xml, OPP_100_BUSINESS + "\"");

    // Test repeatable field OPP-105-Business.
    // Ensure the field is indeed repeatable so that the test itself is not broken.
    assert fieldById.get(OPP_105_BUSINESS).get("repeatable").get("value").asBoolean();
    contains(xml, OPP_105_BUSINESS + "\"");
    contains(xml, ">education<");
    contains(xml, ">health<");
    contains(xml, OPP_105_BUSINESS + "\" listName=\"sector\">education<");
    contains(xml, OPP_105_BUSINESS + "\" listName=\"sector\">health<");
  }

  private static final void contains(final String xml, final String text) {
    assertTrue(xml.contains(text), text);
  }

  private static final void count(final String xml, final int expectedCount, final String toMatch) {
    assertEquals(expectedCount, StringUtils.countMatches(xml, toMatch), toMatch);
  }

}
