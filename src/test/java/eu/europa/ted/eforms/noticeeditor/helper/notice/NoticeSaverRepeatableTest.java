package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

public class NoticeSaverRepeatableTest extends NoticeSaverTest {

  private static final String NOTICE_SUB_TYPE_VALUE = "X02";
  private static final String NOTICE_DOCUMENT_TYPE = "BRIN";

  private static final String VIS_ROOT = "visRoot";
  private static final String VIS_CHILDREN = "children";

  private static final String ND_A = "ND_A";
  private static final String ND_B = "ND_B";

  private static final String BT_FIELD_123 = "BT-field-123";

  private static ObjectNode setupVisualModel(final ObjectMapper mapper, final String fakeSdkForTest,
      final String noticeSubTypeForTest) {

    final ObjectNode visRoot = mapper.createObjectNode();
    putGroupDef(visRoot);
    visRoot.put(VIS_CONTENT_ID, VIS_ROOT);
    visRoot.put(VIS_NODE_ID, ND_ROOT);

    final ArrayNode visRootChildren = visRoot.putArray(VIS_CHILDREN);

    {
      //
      // DUMMY NOTICE DATA (as if coming from a web form before we have the XML).
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
        // Notice sub type.
        final ObjectNode vis = mapper.createObjectNode();
        putFieldDef(vis);
        vis.put(VIS_CONTENT_ID, NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE);
        vis.put(VIS_VALUE, noticeSubTypeForTest);
        visRootChildren.add(vis);
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

    // A1.
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
      a1Children.add(visGroupB2);
    }

    // A2.
    {
      final ObjectNode visGroupA2 = mapper.createObjectNode();
      putGroupDef(visGroupA2);
      visGroupA2.put(VIS_CONTENT_ID, "GR-A2");
      visGroupA2.put(VIS_NODE_ID, ND_A);
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
      groupcChildren.add(visGroupcField2);
    }

    return visRoot;
  }

  /**
   * Setup dummy node metadata.
   *
   * @param nodeById This map will be modified as a SIDE-EFFECT
   */
  @Override
  protected void setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper,
      final Map<String, JsonNode> nodeById) {

    super.setupFieldsJsonXmlStructureNodes(mapper, nodeById);

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
  }

  /**
   * Setup dummy field metadata.
   *
   * @param fieldById This map will be modified as a SIDE-EFFECT
   */
  @Override
  protected void setupFieldsJsonFields(final ObjectMapper mapper,
      final Map<String, JsonNode> fieldById) {

    super.setupFieldsJsonFields(mapper, fieldById);

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
  }


  @Test
  public final void test() throws IOException, ParserConfigurationException {
    final ObjectMapper mapper = new ObjectMapper();
    final String prefixedSdkVersion = "eforms-sdk-" + "1.3.0"; // A dummy 1.3.0, not real 1.3.0
    final String noticeSubType = NOTICE_SUB_TYPE_VALUE; // A dummy 10, not the real 10 of 1.3.0

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
    final ObjectNode visRoot = setupVisualModel(mapper, prefixedSdkVersion, noticeSubType);

    //
    // BUILD CONCEPTUAL MODEL.
    //
    final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldById, nodeById);

    // final ConceptualModel conceptualModel = NoticeSaver.buildConceptualModel2(fieldsAndNodes,
    // visRoot);
    final ConceptualModel conceptualModel = buildConceptualModel(visRoot);

    //
    // BUILD PHYSICAL MODEL.
    //
    final boolean debug = true; // Adds field ids in the XML.
    final boolean buildFields = true;
    final SchemaInfo schemaInfo = SchemaToolsTest.getTestSchemaInfo();

    final PhysicalModel pm = NoticeSaver.buildPhysicalModelXml(fieldsAndNodes, noticeInfoBySubtype,
        documentInfoByType, conceptualModel, debug, buildFields, schemaInfo);

    final String xml = pm.getXmlAsText(false); // Not indented to avoid line breaks.
    System.out.println(pm.getXmlAsText(true));

    contains(xml, "encoding=\"UTF-8\"");

    // Check fields root node.
    contains(xml, "xmlns=");

    // Check some metadata.
    contains(xml, noticeSubType);
    contains(xml, prefixedSdkVersion);

    count(xml, 1, "<cbc:CustomizationID");
    // count(xml, 1, "<cbc:SubTypeCode");

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

  /**
   * Build the conceptual model from the visual model.
   *
   * @param visRoot The root of the visual model
   * @return The conceptual model
   */
  private static final ConceptualModel buildConceptualModel(final ObjectNode visRoot) {
    Validate.isTrue(visRoot.get(VIS_NODE_ID).asText(null).equals(ND_ROOT));

    final Optional<ConceptItem> conceptItemOpt = parseVisualModelRec(visRoot, null);
    if (!conceptItemOpt.isPresent()) {
      throw new RuntimeException("Expecting concept item at root level.");
    }
    final ConceptNode rootNode = (ConceptNode) conceptItemOpt.get();
    final String idForDebug = JsonUtils.getTextStrict(visRoot, VIS_CONTENT_ID);

    // HARDCODED: add notice sub type.
    final ConceptNode rootExtension =
        new ConceptNode(NoticeSaver.ND_ROOT_EXTENSION, idForDebug, 1, 1);
    rootNode.addConceptNode(rootExtension);
    final String fieldId = NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE;
    rootExtension.addConceptField(new ConceptField(fieldId, fieldId, NOTICE_SUB_TYPE_VALUE, 1, 1));

    // TODO more work is required for the full metadata.

    return new ConceptualModel(rootNode);
  }

  /**
   * Visit the tree of the visual model and build the visual model.
   *
   * @param jsonItem The current visual json item
   * @return An optional concept item, if present it is to be appended outside of the call,
   *         otherwise no action should be taken in the caller
   */
  private static Optional<ConceptItem> parseVisualModelRec(final JsonNode jsonItem,
      final ConceptNode closestParentNode) {
    Validate.notNull(jsonItem, "jsonNode is null, jsonNode=%s", jsonItem);

    final JsonNode jsonType = jsonItem.get(NoticeSaver.VIS_TYPE);
    Validate.notNull(jsonType, "jsonType is null, jsonNode=%s", jsonItem);

    final int counter = jsonItem.get(VIS_CONTENT_COUNT).asInt(-1);
    final int parentCounter = jsonItem.get(VIS_CONTENT_PARENT_COUNT).asInt(-1);

    final String visualType = jsonType.asText(null);

    if (visualType == NoticeSaver.VIS_TYPE_FIELD) {
      // This is a field (leaf of the tree).
      final String id = JsonUtils.getTextStrict(jsonItem, VIS_CONTENT_ID);
      final ConceptField field = new ConceptField(id, id + "-" + Math.round(Math.random() * 1000),
          jsonItem.get(VIS_VALUE).asText(null), counter, parentCounter);
      return Optional.of(field);
    }

    if (visualType == NoticeSaver.VIS_TYPE_GROUP) {
      // This is a node.
      final JsonNode nodeIdItem = jsonItem.get(VIS_NODE_ID);
      if (nodeIdItem == null) {

        // This is a group which has no nodeId.
        // In that case we want the children to be moved up to the nearest node, flattening the
        // tree.
        final JsonNode maybeNull = jsonItem.get(VIS_CHILDREN);
        if (maybeNull != null) {
          final ArrayNode visChildren = (ArrayNode) maybeNull;
          for (final JsonNode visChild : visChildren) {
            final Optional<ConceptItem> itemToAppendOpt =
                parseVisualModelRec(visChild, closestParentNode);
            if (itemToAppendOpt.isPresent()) {
              Validate.notNull(closestParentNode, "closestParentNode is null");
              closestParentNode.addConceptItem(itemToAppendOpt.get());
            }
          }
        }
        return Optional.empty(); // Cannot return anything to append.

      } else {
        // This is a group which references a node.
        final String nodeId = nodeIdItem.asText(null);
        final ConceptNode conceptNode = new ConceptNode(nodeId,
            jsonItem.get(VIS_CONTENT_ID).asText(null), jsonItem.get(VIS_CONTENT_COUNT).asInt(-1),
            jsonItem.get(VIS_CONTENT_PARENT_COUNT).asInt(-1));

        // Not a leaf of the tree: recursion on children:
        final JsonNode maybeNull = jsonItem.get(VIS_CHILDREN);
        if (maybeNull != null) {
          final ArrayNode visChildren = (ArrayNode) maybeNull;
          for (final JsonNode visChild : visChildren) {
            final Optional<ConceptItem> itemToAppendOpt =
                parseVisualModelRec(visChild, conceptNode);
            if (itemToAppendOpt.isPresent()) {
              // Append field or node.
              conceptNode.addConceptItem(itemToAppendOpt.get());
            }
          }
        }
        return Optional.of(conceptNode);
      }
    }

    throw new RuntimeException(String.format("Unsupported visual type %s", visualType));
  }
}
