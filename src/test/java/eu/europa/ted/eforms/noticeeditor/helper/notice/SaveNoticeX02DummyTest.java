package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putFieldDef;
import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putGroupDef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Save notice to XML, based on a dummy X02. This test parts of X02 as this notice sub type is
 * somewhat simple. This tests field repeatability. This DOES NOT test group repeatability.
 */
@SpringBootTest
public class SaveNoticeX02DummyTest extends SaveNoticeTest {

  private static final Logger logger = LoggerFactory.getLogger(SaveNoticeX02DummyTest.class);

  private static final String NOTICE_DOCUMENT_TYPE = "BRIN";

  private static final String ND_OPERATION_TYPE = "ND-OperationType";
  private static final String ND_EU_ENTITY = "ND-EuEntity";
  private static final String ND_LOCAL_ENTITY = "ND-LocalEntity";
  private static final String ND_BUSINESS_PARTY = "ND-BusinessParty";

  private static final String OPP_100_BUSINESS = "OPP-100-Business";
  private static final String OPP_105_BUSINESS = ConceptualModel.FIELD_SECTOR_OF_ACTIVITY;
  private static final String OPP_113_BUSINESS_EUROPEAN = "OPP-113-Business-European";

  private static final String BT_500_BUSINESS = "BT-500-Business";
  private static final String BT_501_BUSINESS_NATIONAL = "BT-501-Business-National";
  private static final String BT_501_BUSINESS_EUROPEAN = "BT-501-Business-European";
  private static final String BT_505_BUSINESS = "BT-505-Business";

  private static final String VALUE_HEALTH = "health";
  private static final String VALUE_EDUCATION = "education";

  @Autowired
  protected SdkService sdkService;

  /**
   * Setup dummy test notice form data.
   */
  @Override
  protected VisualModel setupVisualModel(final ObjectMapper mapper, final SdkVersion sdkVersion,
      final String noticeSubTypeForTest) {

    final VisualModel visualModel =
        super.setupVisualModel(mapper, sdkVersion, noticeSubTypeForTest);
    final JsonNode visRoot = visualModel.getVisRoot();
    final ArrayNode visRootChildren = visualModel.getVisRootChildren();

    final ObjectNode visBusinessParty = mapper.createObjectNode();
    visRootChildren.add(visBusinessParty);
    putGroupDef(visBusinessParty);
    visBusinessParty.put(VIS_CONTENT_ID, "the_business_party");
    visBusinessParty.put(VIS_NODE_ID, ND_BUSINESS_PARTY);
    final ArrayNode visBusinessPartyChildren = visBusinessParty.putArray(VIS_CHILDREN);

    final ObjectNode visEuEntity = mapper.createObjectNode();
    visBusinessPartyChildren.add(visEuEntity);
    putGroupDef(visEuEntity);
    visEuEntity.put(VIS_CONTENT_ID, "the_eu_entity");
    visEuEntity.put(VIS_NODE_ID, ND_EU_ENTITY);
    final ArrayNode visEuEntityChildren = visEuEntity.putArray(VIS_CHILDREN);

    final ObjectNode visLocalEntity = mapper.createObjectNode();
    visBusinessPartyChildren.add(visLocalEntity);
    putGroupDef(visLocalEntity);
    visLocalEntity.put(VIS_CONTENT_ID, "the_local_entity");
    visLocalEntity.put(VIS_NODE_ID, ND_LOCAL_ENTITY);
    final ArrayNode visLocalEntityChildren = visLocalEntity.putArray(VIS_CHILDREN);

    final ObjectNode visOperationType = mapper.createObjectNode();
    visRootChildren.add(visOperationType);
    putGroupDef(visOperationType);
    visOperationType.put(VIS_CONTENT_ID, "the_operation_type");
    // visOperationType.put(VIS_NODE_ID, ND_OPERATION_TYPE);
    final ArrayNode visOperationTypeChildren = visOperationType.putArray(VIS_CHILDREN);

    //
    // NOTICE CONTENT.
    //

    // Business party.
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis, BT_505_BUSINESS);
      vis.put(VIS_VALUE, "http://www.acme-solution.co.uk");
      visBusinessPartyChildren.add(vis);
    }

    // EU.
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis, BT_501_BUSINESS_EUROPEAN);
      vis.put(VIS_VALUE, "The EU registration number");
      visEuEntityChildren.add(vis);
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis, OPP_113_BUSINESS_EUROPEAN);
      vis.put(VIS_VALUE, "2020-11-14+01:00");
      visEuEntityChildren.add(vis);
    }

    // Local.
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis, BT_500_BUSINESS);
      vis.put(VIS_VALUE, "ACME Solution");
      visLocalEntityChildren.add(vis);
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis, BT_501_BUSINESS_NATIONAL);
      vis.put(VIS_VALUE, "The national registration number");
      visLocalEntityChildren.add(vis);
    }

    // Repeating node for 'sector'.
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis, OPP_105_BUSINESS);
      vis.put(VIS_VALUE, VALUE_EDUCATION);
      visRootChildren.add(vis);
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis, OPP_105_BUSINESS, 2);
      vis.put(VIS_VALUE, VALUE_HEALTH);
      visRootChildren.add(vis);
    }

    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis, OPP_100_BUSINESS);
      vis.put(VIS_VALUE, "reg");
      visOperationTypeChildren.add(vis);
    }

    return new VisualModel(visRoot);
  }

  /**
   * Setup dummy node metadata.
   *
   * @param nodeById This map will be modified as a SIDE-EFFECT
   */
  @Override
  protected Map<String, JsonNode> setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper) {
    final Map<String, JsonNode> nodeById = super.setupFieldsJsonXmlStructureNodes(mapper);
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_BUSINESS_PARTY, node);
      node.put(KEY_NODE_ID, ND_BUSINESS_PARTY);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/cac:BusinessParty");
      node.put(KEY_XPATH_REL, "cac:BusinessParty");
      SaveNoticeTest.fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_LOCAL_ENTITY, node);
      node.put(KEY_NODE_ID, ND_LOCAL_ENTITY);
      node.put(KEY_NODE_PARENT_ID, ND_BUSINESS_PARTY);
      node.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      node.put(KEY_XPATH_REL, "cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      SaveNoticeTest.fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_EU_ENTITY, node);
      node.put(KEY_NODE_ID, ND_EU_ENTITY);
      node.put(KEY_NODE_PARENT_ID, ND_BUSINESS_PARTY);
      node.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      node.put(KEY_XPATH_REL, "cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      SaveNoticeTest.fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_OPERATION_TYPE, node);
      node.put(KEY_NODE_ID, ND_OPERATION_TYPE);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/efac:NoticePurpose");
      node.put(KEY_XPATH_REL, "efac:NoticePurpose");
      SaveNoticeTest.fieldPutRepeatable(node, false);
    }
    return nodeById;
  }

  /**
   * Setup dummy field metadata.
   *
   * @param fieldById This map will be modified as a SIDE-EFFECT
   */
  @Override
  protected Map<String, JsonNode> setupFieldsJsonFields(final ObjectMapper mapper) {
    final Map<String, JsonNode> fieldById = super.setupFieldsJsonFields(mapper);
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_505_BUSINESS, field);
      field.put(KEY_FIELD_ID, BT_505_BUSINESS);
      field.put(KEY_PARENT_NODE_ID, ND_BUSINESS_PARTY);
      field.put(KEY_XPATH_ABS, "/*/cac:BusinessParty/cbc:WebsiteURI");
      field.put(KEY_XPATH_REL, "cbc:WebsiteURI");
      field.put(KEY_TYPE, TYPE_URL);
      SaveNoticeTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_500_BUSINESS, field);
      field.put(KEY_FIELD_ID, BT_500_BUSINESS);
      field.put(KEY_PARENT_NODE_ID, ND_LOCAL_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]/cbc:RegistrationName");
      field.put(KEY_XPATH_REL, "cbc:RegistrationName");
      field.put(KEY_TYPE, TYPE_TEXT);
      SaveNoticeTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_501_BUSINESS_EUROPEAN, field);
      field.put(KEY_FIELD_ID, BT_501_BUSINESS_EUROPEAN);
      field.put(KEY_PARENT_NODE_ID, ND_EU_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']/cbc:CompanyID[@schemeName = 'EU']");
      field.put(KEY_XPATH_REL, "cbc:CompanyID[@schemeName = 'EU']");
      field.put(KEY_TYPE, TYPE_ID);
      SaveNoticeTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_501_BUSINESS_NATIONAL, field);
      field.put(KEY_FIELD_ID, BT_501_BUSINESS_NATIONAL);
      field.put(KEY_PARENT_NODE_ID, ND_LOCAL_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[not(@schemeName = 'EU')]/cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put(KEY_XPATH_REL, "cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put(KEY_TYPE, TYPE_ID);
      SaveNoticeTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_105_BUSINESS, field);
      field.put(KEY_FIELD_ID, OPP_105_BUSINESS);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT);
      field.put(KEY_XPATH_ABS, "/*/cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put(KEY_XPATH_REL, "cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put(KEY_TYPE, TYPE_CODE);
      SaveNoticeTest.fieldPutRepeatable(field, true);
      FieldsAndNodes.setFieldFlatCodeList(mapper, field, "main-activity");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_100_BUSINESS, field);
      field.put(KEY_FIELD_ID, OPP_100_BUSINESS);
      field.put(KEY_PARENT_NODE_ID, ND_OPERATION_TYPE);
      field.put(KEY_XPATH_ABS, "/*/efac:NoticePurpose/cbc:PurposeCode");
      field.put(KEY_XPATH_REL, "cbc:PurposeCode");
      field.put(KEY_TYPE, TYPE_CODE);
      SaveNoticeTest.fieldPutRepeatable(field, false);
      FieldsAndNodes.setFieldFlatCodeList(mapper, field, "notice-purpose");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_113_BUSINESS_EUROPEAN, field);
      field.put(KEY_FIELD_ID, OPP_113_BUSINESS_EUROPEAN);
      field.put(KEY_PARENT_NODE_ID, ND_EU_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']/cbc:RegistrationDate");
      field.put(KEY_XPATH_REL, "cbc:RegistrationDate");
      field.put(KEY_TYPE, TYPE_DATE);
      SaveNoticeTest.fieldPutRepeatable(field, false);
    }
    return fieldById;
  }

  /**
   * Based on X02 meta data. This unit test relies on test metadata and only tests parts of X02 but
   * enough to cover the basics. It setups dummy fields and nodes metadata, notice-types metadata,
   * and some notice data. It will be hard to maintain but is fully self-contained, so it can also
   * be used to quickly debug a problem by modification of the dummy data or metadata.
   */
  @Test
  public void testWithDummyNoticeAndSdk()
      throws ParserConfigurationException, IOException, SAXException {

    final ObjectMapper mapper = new ObjectMapper();

    // This test was initially used to test the "to XML" feature.
    // Here a small dummy visual model is created inside of the test.
    // A dummy minimal fields.json equivalent and other needed data for the test is also created.
    // The entire thing is tested in isolation to exclude SDK download or path (no download) and
    // also to demonstrate how the entire thing works but based on something much smaller and more
    // controllable.

    // A dummy 1.6.0, not real 1.6.0
    final SdkVersion sdkVersion = new SdkVersion("1.6.0");
    final String prefixedSdkVersion = FieldsAndNodes.EFORMS_SDK_PREFIX + sdkVersion.toString();
    final String noticeSubType = "X02"; // A dummy X02, not the real X02 of 1.6.0

    final VisualModel visualModel = setupVisualModel(mapper, sdkVersion, noticeSubType);

    final PhysicalModel physicalModel =
        setupPhysicalModel(mapper, noticeSubType, NOTICE_DOCUMENT_TYPE, visualModel,
            sdkService.getSdkRootFolder(),
            sdkVersion);

    // As this dummy test example has some metadata, ensure those getters work:
    assertEquals(sdkVersion, physicalModel.getSdkVersion());
    assertTrue(StringUtils.isNotBlank(physicalModel.getMainXsdPathOpt().toString()));
    assertTrue(StringUtils.isNotBlank(physicalModel.getNoticeId().toString()));

    logger.info("XML as text:");
    final String xml = physicalModel.toXmlText(false); // Not indented to avoid line breaks.
    logger.info(physicalModel.toXmlText(true));

    // IDEA it would be more maintainable to use xpath to check the XML instead of pure text.
    // physicalModel.evaluateXpathForTests("/", "test1");

    // Check some metadata.
    checkCommon(prefixedSdkVersion, noticeSubType, xml);

    count(xml, 2, "BusinessRegistrationInformationNotice");

    // Check nodes.
    count(xml, 1, "<BusinessRegistrationInformationNotice");
    count(xml, 1, "<ext:UBLExtensions>");

    count(xml, 1, String.format("<cac:BusinessParty editorCounterSelf=\"1\" editorNodeId=\"%s\"",
        ND_BUSINESS_PARTY));

    count(xml, 1, String.format("<efac:NoticePurpose editorCounterSelf=\"1\"", ND_OPERATION_TYPE));

    // count(xml, 1, String.format(
    // "<efac:NoticePurpose editorCounterSelf=\"1\"",
    // ND_OPERATION_TYPE));

    // It is the same xml tag, but here we can even check the nodeId is originally correct.
    // Without debug true this editor intermediary information would otherwise be lost.
    count(xml, 1, String.format("<cac:PartyLegalEntity editorCounterSelf=\"1\" editorNodeId=\"%s\"",
        ND_EU_ENTITY));

    count(xml, 1, String.format("<cac:PartyLegalEntity editorCounterSelf=\"1\" editorNodeId=\"%s\"",
        ND_LOCAL_ENTITY));

    // Check fields.
    contains(xml, ConceptualModel.FIELD_ID_NOTICE_SUB_TYPE + "\"");
    contains(xml, ConceptualModel.FIELD_ID_SDK_VERSION + "\"");

    contains(xml, BT_500_BUSINESS + "\"");
    contains(xml, BT_501_BUSINESS_NATIONAL + "\"");
    contains(xml, BT_501_BUSINESS_NATIONAL + "\" schemeName=\"" + PhysicalModel.NATIONAL + "\"");

    contains(xml, BT_501_BUSINESS_EUROPEAN + "\"");
    contains(xml, BT_501_BUSINESS_EUROPEAN + "\" schemeName=\"EU\"");
    contains(xml, OPP_113_BUSINESS_EUROPEAN + "\"");

    contains(xml, OPP_100_BUSINESS + "\"");

    // Test repeatable field OPP-105-Business.

    // Ensure the field is indeed repeatable so that the test itself is not broken.
    final FieldsAndNodes fieldsAndNodes = physicalModel.getFieldsAndNodes();
    assert fieldsAndNodes.isFieldRepeatable(OPP_105_BUSINESS) : OPP_105_BUSINESS
        + " should be repeatable";

    contains(xml, OPP_105_BUSINESS + "\"");
    contains(xml, ">" + VALUE_EDUCATION + "<");
    contains(xml, ">" + VALUE_HEALTH + "<");
    contains(xml, OPP_105_BUSINESS + "\" listName=\"sector\">" + VALUE_EDUCATION + "<");
    contains(xml, OPP_105_BUSINESS + "\" listName=\"sector\">" + VALUE_HEALTH + "<");
  }

}
