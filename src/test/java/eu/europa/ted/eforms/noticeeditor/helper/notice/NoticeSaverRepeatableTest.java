package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putFieldDef;
import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putGroupDef;
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

/**
 * A test with focus on repeatability, including nested repeatable groups.
 */
public class NoticeSaverRepeatableTest extends NoticeSaverTest {

  private static final String NOTICE_DOCUMENT_TYPE = "BRIN";

  private static final String ND_A = "ND_A";
  private static final String ND_B = "ND_B";

  private static final String BT_FIELD_123 = "BT-field-123";

  private static final String VIS_CHILDREN = VisualModel.VIS_CHILDREN;

  private static VisualModel setupVisualModel(final ObjectMapper mapper,
      final String fakeSdkForTest, final String noticeSubTypeForTest) {

    final ObjectNode visRoot = mapper.createObjectNode();
    putGroupDef(visRoot);
    visRoot.put(VIS_CONTENT_ID, "the_visual_root");
    visRoot.put(VIS_NODE_ID, ND_ROOT);
    final ArrayNode visRootChildren = visRoot.putArray(VIS_CHILDREN);

    {
      //
      // DUMMY NOTICE METADATA (as if coming from a web form before we have the XML).
      //
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
    }

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
      putGroupDef(visGroupA1);
      visGroupA1.put(VIS_CONTENT_ID, "GR-A1");
      visGroupA1.put(VIS_NODE_ID, ND_A);
      visRootChildren.add(visGroupA1);
      final ArrayNode a1Children = visGroupA1.putArray(VIS_CHILDREN);

      final ObjectNode visGroupB1 = mapper.createObjectNode();
      putGroupDef(visGroupB1);
      visGroupB1.put(VIS_CONTENT_ID, "GR-A1-B1");
      visGroupB1.put(VIS_NODE_ID, ND_B);
      a1Children.add(visGroupB1);

      final ObjectNode visGroupB2 = mapper.createObjectNode();
      putGroupDef(visGroupB2);
      visGroupB2.put(VIS_CONTENT_ID, "GR-A1-B2");
      visGroupB2.put(VIS_NODE_ID, ND_B);
      visGroupB2.put(VIS_CONTENT_COUNT, "2"); // Override default.
      a1Children.add(visGroupB2);
    }

    // ---------------------------
    // A2.
    // ---------------------------
    {
      final ObjectNode visGroupA2 = mapper.createObjectNode();
      putGroupDef(visGroupA2);
      visGroupA2.put(VIS_CONTENT_ID, "GR-A2");
      visGroupA2.put(VIS_NODE_ID, ND_A);
      visGroupA2.put(VIS_CONTENT_COUNT, "2"); // Override default.
      visRootChildren.add(visGroupA2);
      final ArrayNode a2Children = visGroupA2.putArray(VIS_CHILDREN);

      // Add B1 in A2.
      final ObjectNode visGroupB1 = mapper.createObjectNode();
      putGroupDef(visGroupB1);
      visGroupB1.put(VIS_CONTENT_ID, "GR-A2-B1");
      visGroupB1.put(VIS_NODE_ID, ND_B);
      a2Children.add(visGroupB1);
      final ArrayNode b1Children = visGroupB1.putArray(VIS_CHILDREN);

      // Add C in A2 B1.
      final ObjectNode visGroupC = mapper.createObjectNode();
      putGroupDef(visGroupC);
      visGroupC.put(VIS_CONTENT_ID, "GR-A2-B1-C1");
      b1Children.add(visGroupC);
      final ArrayNode groupcChildren = visGroupC.putArray(VIS_CHILDREN);

      // Add dummy field in C.
      final ObjectNode visGroupcField1 = mapper.createObjectNode();
      putFieldDef(visGroupcField1);
      visGroupcField1.put(VIS_CONTENT_ID, BT_FIELD_123);
      visGroupcField1.put(VIS_VALUE, "value-of-field-c1");
      groupcChildren.add(visGroupcField1);

      final ObjectNode visGroupcField2 = mapper.createObjectNode();
      putFieldDef(visGroupcField2);
      visGroupcField2.put(VIS_CONTENT_ID, BT_FIELD_123);
      visGroupcField2.put(VIS_VALUE, "value-of-field-c2");
      visGroupcField2.put(VIS_CONTENT_COUNT, "2"); // Override default.
      groupcChildren.add(visGroupcField2);
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
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/a");
      node.put(KEY_XPATH_REL, "a");
      NoticeSaverTest.fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_B, node);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/a/b");
      node.put(KEY_XPATH_REL, "b");
      NoticeSaverTest.fieldPutRepeatable(node, false);
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
      fieldById.put(BT_FIELD_123, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT_EXTENSION);
      field.put(KEY_XPATH_ABS, "/*/a/b/c");
      field.put(KEY_XPATH_REL, "c");
      field.put(KEY_TYPE, TYPE_TEXT);
      NoticeSaverTest.fieldPutRepeatable(field, true);
    }
    // Setup more fields here ...?
    return fieldById;
  }


  @Test
  public final void test() throws IOException, ParserConfigurationException {
    final ObjectMapper mapper = new ObjectMapper();
    final String prefixedSdkVersion = "eforms-sdk-" + "1.3.0"; // A dummy 1.3.0, not real 1.3.0
    final String noticeSubType = "X02"; // A dummy X02, not the real X02 of 1.3.0

    //
    // NODES from fields.json
    //
    final Map<String, JsonNode> nodeById = setupFieldsJsonXmlStructureNodes(mapper);

    //
    // FIELDS from fields.json
    //
    final Map<String, JsonNode> fieldById = setupFieldsJsonFields(mapper);

    //
    // OTHER from notice-types.json
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
      info.put("rootElement", "xyz");
      documentInfoByType.put(NOTICE_DOCUMENT_TYPE, info);
    }

    //
    // BUILD VISUAL MODEL.
    //
    final VisualModel visualModel = setupVisualModel(mapper, prefixedSdkVersion, noticeSubType);

    //
    // BUILD CONCEPTUAL MODEL.
    //
    final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldById, nodeById);
    final ConceptualModel conceptModel = visualModel.toConceptualModel(fieldsAndNodes);

    //
    // BUILD PHYSICAL MODEL.
    //
    final boolean debug = true; // Adds field ids in the XML.
    final boolean buildFields = true;
    final SchemaInfo schemaInfo = SchemaToolsTest.getTestSchemaInfo();

    final PhysicalModel pm = NoticeSaver.buildPhysicalModelXml(fieldsAndNodes, noticeInfoBySubtype,
        documentInfoByType, conceptModel, debug, buildFields, schemaInfo);

    final String xml = pm.getXmlAsText(false); // Not indented to avoid line breaks.
    System.out.println(pm.getXmlAsText(true));

    contains(xml, "encoding=\"UTF-8\"");

    // Check fields root node.
    contains(xml, "xmlns=");

    // Check some metadata.
    contains(xml, noticeSubType);
    contains(xml, prefixedSdkVersion);

    count(xml, 1, "<cbc:CustomizationID");
    count(xml, 1, "<cbc:SubTypeCode");

    // Verify repeatable nodes at top level.
    count(xml, 2, "<a");
    count(xml, 2, "editorNodeId=\"ND_A\"");

    // Verify nested repeatable nodes.
    count(xml, 3, "<b");
    count(xml, 3, "editorNodeId=\"ND_B\"");

    // Verify repeatable field.
    count(xml, 1, "editorFieldId=\"BT-field-123\">value-of-field-c1</c>");
    count(xml, 1, "editorFieldId=\"BT-field-123\">value-of-field-c2</c>");
  }

}
