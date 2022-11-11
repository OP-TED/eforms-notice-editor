package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

public class NoticeSaverX02DummyTest extends NoticeSaveTest {

  private static final String X02 = "X02";
  private static final String BRIN = "BRIN";

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

  private static final String VALUE_HEALTH = "health";
  private static final String VALUE_EDUCATION = "education";

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
      root.set(OPT_002_NOTICE + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, OPT_002_NOTICE);
      vis.put(VIS_VALUE, fakeSdkForTest);
    }
    {
      // Notice sub type.
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_070_NOTICE + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, OPP_070_NOTICE);
      vis.put(VIS_VALUE, noticeSubTypeForTest);
    }

    //
    // NOTICE CONTENT.
    //
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(BT_505_BUSINESS + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, BT_505_BUSINESS);
      vis.put(VIS_VALUE, "http://www.acme-solution.co.uk");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(BT_501_BUSINESS_EUROPEAN + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, BT_501_BUSINESS_EUROPEAN);
      vis.put(VIS_VALUE, "The EU registration number");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_113_BUSINESS_EUROPEAN + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, OPP_113_BUSINESS_EUROPEAN);
      vis.put(VIS_VALUE, "2020-11-14+01:00");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(BT_500_BUSINESS + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, BT_500_BUSINESS);
      vis.put(VIS_VALUE, "ACME Solution");
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(BT_501_BUSINESS_NATIONAL + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, BT_501_BUSINESS_NATIONAL);
      vis.put(VIS_VALUE, "The national registration number");
    }

    // Repeating node for 'sector'.
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_105_BUSINESS + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, OPP_105_BUSINESS);
      vis.put(VIS_VALUE, VALUE_EDUCATION);
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_105_BUSINESS + VIS_SECOND, vis);
      vis.put(VIS_CONTENT_ID, OPP_105_BUSINESS);
      vis.put(VIS_VALUE, VALUE_HEALTH);
      vis.put(VIS_CONTENT_COUNT, "2"); // Override default.
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(OPP_100_BUSINESS + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, OPP_100_BUSINESS);
      vis.put(VIS_VALUE, "reg");
    }

    return root;
  }

  private static void setupFieldsJsonFields(final ObjectMapper mapper,
      final Map<String, JsonNode> fieldById) {
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPT_002_NOTICE, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT);
      field.put(KEY_XPATH_ABS, "/*/cbc:CustomizationID");
      field.put(KEY_XPATH_REL, "cbc:CustomizationID");
      field.put(KEY_TYPE, TYPE_ID);
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_070_NOTICE, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT_EXTENSION);
      field.put(KEY_XPATH_ABS,
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:NoticeSubType/cbc:SubTypeCode");
      field.put(KEY_XPATH_REL, "efac:NoticeSubType/cbc:SubTypeCode");
      field.put(KEY_TYPE, TYPE_CODE);
      fieldPutRepeatable(field, false);
      field.put(KEY_CODE_LIST_ID, "notice-subtype");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_505_BUSINESS, field);
      field.put(KEY_PARENT_NODE_ID, ND_BUSINESS_PARTY);
      field.put(KEY_XPATH_ABS, "/*/cac:BusinessParty/cbc:WebsiteURI");
      field.put(KEY_XPATH_REL, "cbc:WebsiteURI");
      field.put(KEY_TYPE, TYPE_URL);
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_500_BUSINESS, field);
      field.put(KEY_PARENT_NODE_ID, ND_LOCAL_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]/cbc:RegistrationName");
      field.put(KEY_XPATH_REL, "cbc:RegistrationName");
      field.put(KEY_TYPE, TYPE_TEXT);
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_501_BUSINESS_EUROPEAN, field);
      field.put(KEY_PARENT_NODE_ID, ND_EU_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']/cbc:CompanyID[@schemeName = 'EU']");
      field.put(KEY_XPATH_REL, "cbc:CompanyID[@schemeName = 'EU']");
      field.put(KEY_TYPE, TYPE_ID);
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_501_BUSINESS_NATIONAL, field);
      field.put(KEY_PARENT_NODE_ID, ND_LOCAL_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[not(@schemeName = 'EU')]/cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put(KEY_XPATH_REL, "cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put(KEY_TYPE, TYPE_ID);
      fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_105_BUSINESS, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT);
      field.put(KEY_XPATH_ABS, "/*/cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put(KEY_XPATH_REL, "cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put(KEY_TYPE, TYPE_CODE);
      fieldPutRepeatable(field, true);
      field.put(KEY_CODE_LIST_ID, "main-activity");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_100_BUSINESS, field);
      field.put(KEY_PARENT_NODE_ID, ND_OPERATION_TYPE);
      field.put(KEY_XPATH_ABS, "/*/efac:NoticePurpose/cbc:PurposeCode");
      field.put(KEY_XPATH_REL, "cbc:PurposeCode");
      field.put(KEY_TYPE, TYPE_CODE);
      fieldPutRepeatable(field, false);
      field.put(KEY_CODE_LIST_ID, "notice-purpose");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_113_BUSINESS_EUROPEAN, field);
      field.put(KEY_PARENT_NODE_ID, ND_EU_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']/cbc:RegistrationDate");
      field.put(KEY_XPATH_REL, "cbc:RegistrationDate");
      field.put(KEY_TYPE, TYPE_DATE);
      fieldPutRepeatable(field, false);
    }
  }

  private static void fieldPutRepeatable(final ObjectNode field, final boolean repeatable) {
    final ObjectNode prop = JsonUtils.createObjectNode();
    prop.put(KEY_VALUE, repeatable);
    field.set(KEY_REPEATABLE, prop);
  }

  private static void setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper,
      final Map<String, JsonNode> nodeById) {
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_ROOT, node);
      node.put(KEY_XPATH_ABS, "/*");
      node.put(KEY_XPATH_REL, "/*");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_ROOT_EXTENSION, node);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS,
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put(KEY_XPATH_REL,
          "ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_BUSINESS_PARTY, node);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/cac:BusinessParty");
      node.put(KEY_XPATH_REL, "cac:BusinessParty");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_LOCAL_ENTITY, node);
      node.put(KEY_NODE_PARENT_ID, ND_BUSINESS_PARTY);
      node.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      node.put(KEY_XPATH_REL, "cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_EU_ENTITY, node);
      node.put(KEY_NODE_PARENT_ID, ND_BUSINESS_PARTY);
      node.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      node.put(KEY_XPATH_REL, "cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_OPERATION_TYPE, node);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/efac:NoticePurpose");
      node.put(KEY_XPATH_REL, "efac:NoticePurpose");
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

    final String prefixedSdkVersion = "eforms-sdk-" + "1.1.0"; // A dummy 1.1.0, not real 1.1.0
    final String noticeSubType = X02; // A dummy X02, not the real X02 of 1.1.0

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
    contains(xml, BT_501_BUSINESS_NATIONAL + "\" schemeName=\"" + NoticeSaver.NATIONAL + "\"");

    contains(xml, BT_501_BUSINESS_EUROPEAN + "\"");
    contains(xml, BT_501_BUSINESS_EUROPEAN + "\" schemeName=\"EU\"");
    contains(xml, OPP_113_BUSINESS_EUROPEAN + "\"");

    contains(xml, OPP_100_BUSINESS + "\"");

    // Test repeatable field OPP-105-Business.
    // Ensure the field is indeed repeatable so that the test itself is not broken.
    assert fieldById.get(OPP_105_BUSINESS).get(KEY_REPEATABLE).get(KEY_VALUE).asBoolean();
    contains(xml, OPP_105_BUSINESS + "\"");
    contains(xml, ">" + VALUE_EDUCATION + "<");
    contains(xml, ">" + VALUE_HEALTH + "<");
    contains(xml, OPP_105_BUSINESS + "\" listName=\"sector\">" + VALUE_EDUCATION + "<");
    contains(xml, OPP_105_BUSINESS + "\" listName=\"sector\">" + VALUE_HEALTH + "<");
  }

}
