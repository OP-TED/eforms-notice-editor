package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putFieldDef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * A test with focus on the filling in of non-repeating intermediary conceptual nodes.
 */
public class SaveNoticeFillingTest extends SaveNoticeTest {

  /**
   * It could be anything for this test dummy, but we will use BRIN.
   */
  private static final String NOTICE_DOCUMENT_TYPE = "BRIN";

  private static final String ND_X = "ND_X";
  private static final String ND_Y = "ND_Y";

  private static final String BT_FIELD_DUMMY_Z = "BT-field-z";

  private static VisualModel setupVisualModel(final ObjectMapper mapper,
      final String fakeSdkForTest, final String noticeSubTypeForTest) {

    // Setup root of the visual model.
    final ObjectNode visRoot = mapper.createObjectNode();
    final ArrayNode visRootChildren =
        VisualModel.setupVisualRootForTest(mapper, fakeSdkForTest, noticeSubTypeForTest, visRoot);

    //
    // NOTICE CONTENT.
    //

    // For the test data we want nested repeatability (repeating groups inside a repeating group).
    // On the left is the content
    // On the right is the node id (without the GR- prefix to simplify):
    // X -> Y "means X has child Y and Y is child of X"

    // ---------------------------
    // X Y Z
    // ---------------------------
    // X (non-repeating group) -> Y (non-repeating group) -> Z (field)
    {
      // IMPORTANT: even if the visual model is missing non-repeating nodes they will be added to
      // the conceptual model.
      // In practice this happens with a lot of fields because the visual model does not have to
      // strictly follow the SDK node hierarchy, except for repeatability.
      // When non-repeatable nodes are involved the only thing to be respected is their order (from
      // root to field).
      // In this dummy test example we deliberately omit two consecutive non-repeatable nodes.
      // This means the field Z is directly attached to the root even if there could be X -> Y (or
      // just X or just Y, but not Y -> X).
      // The code building the conceptual model will automatically insert them to fill-in the gaps.

      // final ObjectNode visGroupX = mapper.createObjectNode();
      // visRootChildren.add(visGroupX);
      // VisualModel.putGroupDef(visGroupX);
      // visGroupX.put(VIS_CONTENT_ID, "GR-X");
      // visGroupX.put(VIS_NODE_ID, ND_X);
      // visGroupX.put(VIS_CONTENT_COUNT, "1"); // Override default.
      // final ArrayNode xChildren = visGroupX.putArray(VIS_CHILDREN);
      //
      // // Add Y in X.
      // final ObjectNode visGroupY = mapper.createObjectNode();
      // xChildren.add(visGroupY);
      // VisualModel.putGroupDef(visGroupY);
      // visGroupY.put(VIS_CONTENT_ID, "GR-X-Y");
      // visGroupY.put(VIS_NODE_ID, ND_Y);
      // final ArrayNode yChildren = visGroupY.putArray(VIS_CHILDREN);

      // Add dummy field Z in Y.
      final ObjectNode vis = mapper.createObjectNode();
      visRootChildren.add(vis);
      // yChildren.add(vis);
      putFieldDef(vis, BT_FIELD_DUMMY_Z);
      vis.put(VIS_VALUE, "value-of-field-z");
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
      nodeById.put(ND_X, node);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/x");
      node.put(KEY_XPATH_REL, "x");
      SaveNoticeTest.fieldPutRepeatable(node, false); // NON-REPEATABLE!
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_Y, node);
      node.put(KEY_NODE_PARENT_ID, ND_X);
      node.put(KEY_XPATH_ABS, "/*/x/y");
      node.put(KEY_XPATH_REL, "y");
      SaveNoticeTest.fieldPutRepeatable(node, false); // NON-REPEATABLE TOO!
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
    {
      // Add a repeatable field to also cover field repeatability.
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(BT_FIELD_DUMMY_Z, field);
      field.put(KEY_PARENT_NODE_ID, ND_Y);
      field.put(KEY_XPATH_ABS, "/*/x/y/z");
      field.put(KEY_XPATH_REL, "z");
      field.put(KEY_TYPE, TYPE_TEXT);
      SaveNoticeTest.fieldPutRepeatable(field, true);
    }
    return fieldById;
  }

  @Test
  public final void test() throws IOException, ParserConfigurationException {
    final ObjectMapper mapper = new ObjectMapper();
    // A dummy 1.3.0, not real 1.3.0
    final SdkVersion sdkVersion = new SdkVersion("1.3.0");
    final String prefixedSdkVersion = FieldsAndNodes.EFORMS_SDK_PREFIX + sdkVersion.toString();
    final String noticeSubType = "X02"; // A dummy X02, not the real X02 of 1.3.0

    //
    // NODES like in fields.json
    //
    final Map<String, JsonNode> nodeById = setupFieldsJsonXmlStructureNodes(mapper);

    //
    // FIELDS like in fields.json
    //
    final Map<String, JsonNode> fieldById = setupFieldsJsonFields(mapper);

    //
    // Some data like notice-types.json
    //
    // Setup dummy notice-types.json info that we need for the XML generation.
    final Map<String, JsonNode> noticeInfoBySubtype = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("documentType", NOTICE_DOCUMENT_TYPE);
      noticeInfoBySubtype.put(noticeSubType, info);
    }

    final Map<String, JsonNode> documentInfoByType = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("namespace",
          "http://data.europa.eu/p27/eforms-business-registration-information-notice/1");
      info.put("rootElement", "bla");
      documentInfoByType.put(NOTICE_DOCUMENT_TYPE, info);
    }

    //
    // BUILD VISUAL MODEL.
    //
    final VisualModel visualModel = setupVisualModel(mapper, prefixedSdkVersion, noticeSubType);

    //
    // BUILD CONCEPTUAL MODEL.
    //
    final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldById, nodeById, sdkVersion);
    final ConceptualModel conceptModel = visualModel.toConceptualModel(fieldsAndNodes);

    assertEquals(noticeSubType, conceptModel.getNoticeSubType());
    assertEquals(ConceptualModel.ND_ROOT, conceptModel.getTreeRootNode().getNodeId());

    //
    // BUILD PHYSICAL MODEL.
    //
    final boolean debug = true; // Adds field ids in the XML.
    final boolean buildFields = true;
    final XmlSchemaInfo schemaInfo = SchemaToolsTest.getTestSchemaInfo();

    final PhysicalModel physicalModel = PhysicalModel.buildPhysicalModel(conceptModel,
        fieldsAndNodes, noticeInfoBySubtype, documentInfoByType, debug, buildFields, schemaInfo);

    final String xml = physicalModel.toXmlText(false); // Not indented to avoid line breaks.
    System.out.println(physicalModel.toXmlText(true));

    // IDEA it would be more maintainable to use xpath to check the XML instead of pure text.
    // physicalModel.evaluateXpathForTests("/", "test2");

    count(xml, 1, "encoding=\"UTF-8\"");

    // Check fields root node.
    contains(xml, "xmlns=");

    // Check some metadata.
    count(xml, 1, noticeSubType);
    count(xml, 1, prefixedSdkVersion);

    count(xml, 1, "<cbc:CustomizationID");
    count(xml, 1, "<cbc:SubTypeCode");

    // Verify nodes.
    // x
    count(xml, 1, "<x"); // 1 in total
    count(xml, 1, "editorNodeId=\"ND_X\"");

    // y
    count(xml, 1, "<y"); // 1 in total
    count(xml, 1, "editorNodeId=\"ND_Y\"");

    // Verify field z.
    count(xml, 1, "editorFieldId=\"BT-field-z\">value-of-field-z</z>");
    count(xml, 1, "editorCounterSelf=\"1\" editorFieldId=\"BT-field-z\">value-of-field-z</z>");
  }

}