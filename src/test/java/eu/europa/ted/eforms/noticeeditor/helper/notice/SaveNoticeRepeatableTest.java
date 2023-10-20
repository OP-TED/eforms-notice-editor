package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putFieldDef;
import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putGroupDef;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
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
import eu.europa.ted.eforms.noticeeditor.helper.VersionHelper;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * A test with focus on save to xml "repeatability", including nested repeatable groups.
 */
@SpringBootTest
public class SaveNoticeRepeatableTest extends SaveNoticeTest {

  private static final Logger logger = LoggerFactory.getLogger(SaveNoticeRepeatableTest.class);

  /**
   * It could be anything for this test dummy, but we will use BRIN.
   */
  private static final String NOTICE_DOCUMENT_TYPE = "BRIN";

  private static final String ND_A = "ND_A";
  private static final String ND_B = "ND_B";

  private static final String BT_FIELD_DUMMY_C_REP = "BT-field-c";

  @Autowired
  protected SdkService sdkService;

  @Override
  protected VisualModel setupVisualModel(final ObjectMapper mapper, final SdkVersion sdkVersion,
      final String noticeSubTypeForTest) {

    final VisualModel visualModel =
        super.setupVisualModel(mapper, sdkVersion, noticeSubTypeForTest);
    final JsonNode visRoot = visualModel.getVisRoot();
    final ArrayNode visRootChildren = visualModel.getVisRootChildren();

    //
    // NOTICE CONTENT.
    //

    // For the test data we want nested repeatability (repeating groups inside a repeating group).
    // On the left is the content
    // On the right is the node id (without the GR- prefix to simplify):
    // X -> Y "means X has child Y and Y is child of X"

    // THE_CONTENT -> A1
    // A1 -> A1-B1
    // A1 -> A1-B2

    // THE_CONTENT -> A2
    // A2 -> A2-B1

    // ---------------------------
    // A1.
    // ---------------------------
    {
      final ObjectNode visGroupA1 = mapper.createObjectNode();
      visRootChildren.add(visGroupA1);
      putGroupDef(visGroupA1);
      visGroupA1.put(VIS_CONTENT_ID, "GR-A1");
      visGroupA1.put(VIS_NODE_ID, ND_A);
      final ArrayNode a1Children = visGroupA1.putArray(VIS_CHILDREN);

      final ObjectNode visGroupB1 = mapper.createObjectNode();
      a1Children.add(visGroupB1);
      putGroupDef(visGroupB1);
      visGroupB1.put(VIS_CONTENT_ID, "GR-A1-B1");
      visGroupB1.put(VIS_NODE_ID, ND_B);

      final ObjectNode visGroupB2 = mapper.createObjectNode();
      a1Children.add(visGroupB2);
      putGroupDef(visGroupB2);
      visGroupB2.put(VIS_CONTENT_ID, "GR-A1-B2");
      visGroupB2.put(VIS_NODE_ID, ND_B);
      visGroupB2.put(VIS_CONTENT_COUNT, "2"); // Override default.
    }

    // ---------------------------
    // A2.
    // ---------------------------
    {
      final ObjectNode visGroupA2 = mapper.createObjectNode();
      visRootChildren.add(visGroupA2);
      putGroupDef(visGroupA2);
      visGroupA2.put(VIS_CONTENT_ID, "GR-A2");
      visGroupA2.put(VIS_NODE_ID, ND_A);
      visGroupA2.put(VIS_CONTENT_COUNT, "2"); // Override default.
      final ArrayNode a2Children = visGroupA2.putArray(VIS_CHILDREN);

      // Add B1 in A2.
      final ObjectNode visGroupB1 = mapper.createObjectNode();
      a2Children.add(visGroupB1);
      putGroupDef(visGroupB1);
      visGroupB1.put(VIS_CONTENT_ID, "GR-A2-B1");
      visGroupB1.put(VIS_NODE_ID, ND_B);
      final ArrayNode b1Children = visGroupB1.putArray(VIS_CHILDREN);

      // Add C in A2 B1.
      final ObjectNode visGroupC = mapper.createObjectNode();
      b1Children.add(visGroupC);
      putGroupDef(visGroupC);
      visGroupC.put(VIS_CONTENT_ID, "GR-A2-B1-C1");
      final ArrayNode groupcChildren = visGroupC.putArray(VIS_CHILDREN);

      // Add dummy field in C.
      final ObjectNode visFieldC1 = mapper.createObjectNode();
      groupcChildren.add(visFieldC1);
      putFieldDef(visFieldC1, BT_FIELD_DUMMY_C_REP);
      visFieldC1.put(VIS_VALUE, "value-of-field-c1");

      // Add another dummy field in C.
      final ObjectNode visFieldC2 = mapper.createObjectNode();
      groupcChildren.add(visFieldC2);
      putFieldDef(visFieldC2, BT_FIELD_DUMMY_C_REP, 2);
      visFieldC2.put(VIS_VALUE, "value-of-field-c2");
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
      nodeById.put(ND_A, node);
      node.put(KEY_NODE_ID, ND_A);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/a");
      node.put(KEY_XPATH_REL, "a");
      SaveNoticeTest.nodePutRepeatable(node, true);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_B, node);
      node.put(KEY_NODE_ID, ND_B);
      node.put(KEY_NODE_PARENT_ID, ND_A);
      node.put(KEY_XPATH_ABS, "/*/a/b");
      node.put(KEY_XPATH_REL, "b");
      SaveNoticeTest.nodePutRepeatable(node, true);
    }
    return Collections.unmodifiableMap(nodeById);
  }

  /**
   * Setup dummy field metadata.
   *
   * @param fieldById This map will be modified as a SIDE-EFFECT
   */
  @Override
  protected Map<String, JsonNode> setupFieldsJsonFields(final ObjectMapper mapper) {
    final Map<String, JsonNode> fieldById = super.setupFieldsJsonFields(mapper);

    // Add a repeatable field to also cover field repeatability.
    final ObjectNode field = mapper.createObjectNode();
    fieldById.put(BT_FIELD_DUMMY_C_REP, field);
    field.put(KEY_FIELD_ID, BT_FIELD_DUMMY_C_REP);
    field.put(KEY_FIELD_PARENT_NODE_ID, ND_B);
    field.put(KEY_XPATH_ABS, "/*/a/b/c");
    field.put(KEY_XPATH_REL, "c");
    field.put(KEY_TYPE, TYPE_TEXT);
    SaveNoticeTest.fieldPutRepeatable(field, true);

    return fieldById;
  }

  @Test
  public final void test() throws ParserConfigurationException, IOException, SAXException {
    final ObjectMapper mapper = new ObjectMapper();
    // A dummy 1.9.0, not real 1.9.0
    final SdkVersion sdkVersion = new SdkVersion("1.9.0");
    final String prefixedSdkVersion = VersionHelper.prefixSdkVersionWithoutPatch(sdkVersion);
    final String noticeSubType = "X02"; // A dummy X02, not the real X02 of the SDK

    final VisualModel visualModel = setupVisualModel(mapper, sdkVersion, noticeSubType);

    final PhysicalModel physicalModel =
        setupPhysicalModel(mapper, noticeSubType, NOTICE_DOCUMENT_TYPE, visualModel,
            sdkService.getSdkRootFolder(), sdkVersion);

    final String xml = physicalModel.toXmlText(false); // Not indented to avoid line breaks.
    logger.info(physicalModel.toXmlText(true));

    checkCommon(prefixedSdkVersion, noticeSubType, xml, physicalModel);

    // Verify repeatable nodes at top level.
    count(xml, 2, "<a");
    count(xml, 2, "editorNodeId=\"ND_A\"");
    assertCount(physicalModel, 2, "//*[local-name()='a']");

    // Verify nested repeatable nodes.
    count(xml, 3, "<b"); // 3 in total
    count(xml, 3, "editorNodeId=\"ND_B\""); // 3 in total
    assertCount(physicalModel, 3, "//*[local-name()='b']");
    assertCount(physicalModel, 3, "//*[local-name()='a']/*[local-name()='b']"); // Checks nesting.

    count(xml, 2, "editorCounterSelf=\"1\" editorNodeId=\"ND_B\"");
    count(xml, 1, "editorCounterSelf=\"2\" editorNodeId=\"ND_B\"");
    count(xml, 1, "<b editorCounterSelf=\"1\" editorNodeId=\"ND_B\">");

    // Verify repeatable field.

    // Ensure the field is indeed repeatable so that the test itself is not broken.
    assert physicalModel.getFieldsAndNodes()
        .isFieldRepeatable(BT_FIELD_DUMMY_C_REP) : BT_FIELD_DUMMY_C_REP + " should be repeatable";

    // c1
    count(xml, 1, "editorFieldId=\"BT-field-c\">value-of-field-c1</c>");
    count(xml, 1, "editorCounterSelf=\"1\" editorFieldId=\"BT-field-c\">value-of-field-c1</c>");

    // c2
    count(xml, 1, "editorFieldId=\"BT-field-c\">value-of-field-c2</c>");
    count(xml, 1, "editorCounterSelf=\"2\" editorFieldId=\"BT-field-c\">value-of-field-c2</c>");
  }

}
