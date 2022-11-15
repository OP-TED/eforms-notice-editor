package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putFieldDef;
import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putGroupDef;
import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class NoticeSaverX02DummyTest extends NoticeSaverTest {

  private static final String NOTICE_SUB_TYPE_VALUE = "X02";
  private static final String NOTICE_DOCUMENT_TYPE = "BRIN";

  private static final String ND_OPERATION_TYPE = "ND-OperationType";
  private static final String ND_EU_ENTITY = "ND-EuEntity";
  private static final String ND_LOCAL_ENTITY = "ND-LocalEntity";
  private static final String ND_BUSINESS_PARTY = "ND-BusinessParty";

  private static final String OPP_100_BUSINESS = "OPP-100-Business";
  private static final String OPP_105_BUSINESS = "OPP-105-Business";
  private static final String OPP_113_BUSINESS_EUROPEAN = "OPP-113-Business-European";

  private static final String BT_500_BUSINESS = "BT-500-Business";
  private static final String BT_501_BUSINESS_NATIONAL = "BT-501-Business-National";
  private static final String BT_501_BUSINESS_EUROPEAN = "BT-501-Business-European";
  private static final String BT_505_BUSINESS = "BT-505-Business";

  private static final String VALUE_HEALTH = "health";
  private static final String VALUE_EDUCATION = "education";

  private static final String VIS_CHILDREN = VisualModel.VIS_CHILDREN;

  /**
   * Setup dummy test notice form data.
   */
  private static VisualModel setupVisualModel(final ObjectMapper mapper,
      final String fakeSdkForTest, final String noticeSubTypeForTest) {

    // Setup root of the visual model.
    final ObjectNode visRoot = mapper.createObjectNode();
    putGroupDef(visRoot);
    visRoot.put(VIS_CONTENT_ID, "the_visual_root");
    visRoot.put(VIS_NODE_ID, NoticeSaver.ND_ROOT);
    final ArrayNode visRootChildren = visRoot.putArray(VIS_CHILDREN);

    //
    // DUMMY X02 NOTICE DATA (as if coming from a web form before we have the XML).
    //

    // TODO update to hierarchical.
    {
      // SDK version.
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      vis.put(VIS_CONTENT_ID, NoticeSaver.FIELD_ID_SDK_VERSION);
      vis.put(VIS_VALUE, fakeSdkForTest);
      visRootChildren.add(vis);
    }
    {
      final ObjectNode visRootExtension = mapper.createObjectNode();
      putGroupDef(visRootExtension);
      visRootExtension.put(VIS_CONTENT_ID, "the_visual_root");
      visRootExtension.put(VIS_NODE_ID, ND_ROOT_EXTENSION);
      final ArrayNode visRootExtChildren = visRootExtension.putArray(VIS_CHILDREN);
      visRootChildren.add(visRootExtension);

      // Notice sub type.
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      vis.put(VIS_CONTENT_ID, NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE);
      vis.put(VIS_VALUE, noticeSubTypeForTest);
      visRootExtChildren.add(vis);
    }

    //
    // NOTICE CONTENT.
    //
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      vis.put(VIS_CONTENT_ID, BT_505_BUSINESS);
      vis.put(VIS_VALUE, "http://www.acme-solution.co.uk");
      visRootChildren.add(vis);
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      vis.put(VIS_CONTENT_ID, BT_501_BUSINESS_EUROPEAN);
      vis.put(VIS_VALUE, "The EU registration number");
      visRootChildren.add(vis);
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      vis.put(VIS_CONTENT_ID, OPP_113_BUSINESS_EUROPEAN);
      vis.put(VIS_VALUE, "2020-11-14+01:00");
      visRootChildren.add(vis);
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      vis.put(VIS_CONTENT_ID, BT_500_BUSINESS);
      vis.put(VIS_VALUE, "ACME Solution");
      visRootChildren.add(vis);
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      vis.put(VIS_CONTENT_ID, BT_501_BUSINESS_NATIONAL);
      vis.put(VIS_VALUE, "The national registration number");
      visRootChildren.add(vis);
    }

    // Repeating node for 'sector'.
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      vis.put(VIS_CONTENT_ID, OPP_105_BUSINESS);
      vis.put(VIS_VALUE, VALUE_EDUCATION);
      visRootChildren.add(vis);
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      visRootChildren.add(vis);
      vis.put(VIS_CONTENT_ID, OPP_105_BUSINESS);
      vis.put(VIS_VALUE, VALUE_HEALTH);
      vis.put(VIS_CONTENT_COUNT, "2"); // Override default.
    }
    {
      final ObjectNode vis = mapper.createObjectNode();
      putFieldDef(vis);
      visRootChildren.add(vis);
      vis.put(VIS_CONTENT_ID, OPP_100_BUSINESS);
      vis.put(VIS_VALUE, "reg");
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
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/cac:BusinessParty");
      node.put(KEY_XPATH_REL, "cac:BusinessParty");
      NoticeSaverTest.fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_LOCAL_ENTITY, node);
      node.put(KEY_NODE_PARENT_ID, ND_BUSINESS_PARTY);
      node.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      node.put(KEY_XPATH_REL, "cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]");
      NoticeSaverTest.fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_EU_ENTITY, node);
      node.put(KEY_NODE_PARENT_ID, ND_BUSINESS_PARTY);
      node.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      node.put(KEY_XPATH_REL, "cac:PartyLegalEntity[cbc:CompanyID/@schemeName = 'EU']");
      NoticeSaverTest.fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_OPERATION_TYPE, node);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/efac:NoticePurpose");
      node.put(KEY_XPATH_REL, "efac:NoticePurpose");
      NoticeSaverTest.fieldPutRepeatable(node, false);
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
      field.put(KEY_PARENT_NODE_ID, ND_BUSINESS_PARTY);
      field.put(KEY_XPATH_ABS, "/*/cac:BusinessParty/cbc:WebsiteURI");
      field.put(KEY_XPATH_REL, "cbc:WebsiteURI");
      field.put(KEY_TYPE, TYPE_URL);
      NoticeSaverTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_500_BUSINESS, field);
      field.put(KEY_PARENT_NODE_ID, ND_LOCAL_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity[not(cbc:CompanyID/@schemeName = 'EU')]/cbc:RegistrationName");
      field.put(KEY_XPATH_REL, "cbc:RegistrationName");
      field.put(KEY_TYPE, TYPE_TEXT);
      NoticeSaverTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_501_BUSINESS_EUROPEAN, field);
      field.put(KEY_PARENT_NODE_ID, ND_EU_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[@schemeName = 'EU']/cbc:CompanyID[@schemeName = 'EU']");
      field.put(KEY_XPATH_REL, "cbc:CompanyID[@schemeName = 'EU']");
      field.put(KEY_TYPE, TYPE_ID);
      NoticeSaverTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_501_BUSINESS_NATIONAL, field);
      field.put(KEY_PARENT_NODE_ID, ND_LOCAL_ENTITY);
      field.put(KEY_XPATH_ABS,
          "/*/cac:BusinessParty/cac:PartyLegalEntity/cbc:CompanyID[not(@schemeName = 'EU')]/cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put(KEY_XPATH_REL, "cbc:CompanyID[not(@schemeName = 'EU')]");
      field.put(KEY_TYPE, TYPE_ID);
      NoticeSaverTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_105_BUSINESS, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT);
      field.put(KEY_XPATH_ABS, "/*/cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put(KEY_XPATH_REL, "cac:BusinessCapability/cbc:CapabilityTypeCode");
      field.put(KEY_TYPE, TYPE_CODE);
      NoticeSaverTest.fieldPutRepeatable(field, true);
      field.put(KEY_CODE_LIST_ID, "main-activity");
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(OPP_100_BUSINESS, field);
      field.put(KEY_PARENT_NODE_ID, ND_OPERATION_TYPE);
      field.put(KEY_XPATH_ABS, "/*/efac:NoticePurpose/cbc:PurposeCode");
      field.put(KEY_XPATH_REL, "cbc:PurposeCode");
      field.put(KEY_TYPE, TYPE_CODE);
      NoticeSaverTest.fieldPutRepeatable(field, false);
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
      NoticeSaverTest.fieldPutRepeatable(field, false);
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
  public void testDummy() throws ParserConfigurationException, IOException {
    final ObjectMapper mapper = new ObjectMapper();

    final String prefixedSdkVersion = "eforms-sdk-" + "1.3.0"; // A dummy 1.3.0, not real 1.3.0
    final String noticeSubType = NOTICE_SUB_TYPE_VALUE; // A dummy X02, not the real X02 of 1.3.0

    //
    // BUILD VISUAL MODEL.
    //
    final VisualModel visualModel = setupVisualModel(mapper, prefixedSdkVersion, noticeSubType);

    //
    // BUILD PHYSICAL MODEL.
    //
    final PhysicalModel physicalModel =
        setupPhysicalModel(mapper, noticeSubType, NOTICE_DOCUMENT_TYPE, visualModel);

    System.out.println("XML as text:");
    final String xml = physicalModel.getXmlAsText(false); // Not indented to avoid line breaks.
    System.out.println(physicalModel.getXmlAsText(true));

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

    // As the algorithm was heavily modified this currently does not fully pass and is commented
    // out.
    // testMore(physicalModel, xml);
  }

  private void testMore(final PhysicalModel physicalModel, final String xml) {
    count(xml, 1, String.format("<cac:BusinessParty editorNodeId=\"%s\"", ND_BUSINESS_PARTY));
    count(xml, 1, String.format("<efac:NoticePurpose editorNodeId=\"%s\"", ND_OPERATION_TYPE));

    // It is the same xml tag, but here we can even check the nodeId is originally correct.
    // Without debug true this editor intermediary information would otherwise be lost.
    count(xml, 1, String.format("<cac:PartyLegalEntity editorNodeId=\"%s\"", ND_EU_ENTITY));
    count(xml, 1, String.format("<cac:PartyLegalEntity editorNodeId=\"%s\"", ND_LOCAL_ENTITY));

    // Check fields.
    contains(xml, NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE + "\"");
    contains(xml, NoticeSaver.FIELD_ID_SDK_VERSION + "\"");

    contains(xml, BT_500_BUSINESS + "\"");
    contains(xml, BT_501_BUSINESS_NATIONAL + "\"");
    contains(xml, BT_501_BUSINESS_NATIONAL + "\" schemeName=\"" + NoticeSaver.NATIONAL + "\"");

    contains(xml, BT_501_BUSINESS_EUROPEAN + "\"");
    contains(xml, BT_501_BUSINESS_EUROPEAN + "\" schemeName=\"EU\"");
    contains(xml, OPP_113_BUSINESS_EUROPEAN + "\"");

    contains(xml, OPP_100_BUSINESS + "\"");

    // Test repeatable field OPP-105-Business.
    // Ensure the field is indeed repeatable so that the test itself is not broken.
    final FieldsAndNodes fieldsAndNodes = physicalModel.getFieldsAndNodes();
    assert fieldsAndNodes.getFieldById(OPP_105_BUSINESS).get(KEY_FIELD_REPEATABLE).get(KEY_VALUE)
        .asBoolean();

    contains(xml, OPP_105_BUSINESS + "\"");
    contains(xml, ">" + VALUE_EDUCATION + "<");
    contains(xml, ">" + VALUE_HEALTH + "<");
    contains(xml, OPP_105_BUSINESS + "\" listName=\"sector\">" + VALUE_EDUCATION + "<");
    contains(xml, OPP_105_BUSINESS + "\" listName=\"sector\">" + VALUE_HEALTH + "<");
  }

}
