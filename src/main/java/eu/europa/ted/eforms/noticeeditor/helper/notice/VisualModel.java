package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

/**
 * Visual model.
 *
 * Wrapper around the representation of the form data.
 */
public class VisualModel {

  private static final Logger logger = LoggerFactory.getLogger(VisualModel.class);

  static final String VIS_CHILDREN = "children";

  static final String VIS_CONTENT_PARENT_COUNT = "contentParentCount";
  static final String VIS_CONTENT_COUNT = "contentCount";
  static final String VIS_CONTENT_ID = "contentId";

  static final String VIS_NODE_ID = "visNodeId";
  static final String VIS_VALUE = "value";

  static final String VIS_TYPE = "type";
  private static final String VIS_TYPE_FIELD = "field";
  private static final String VIS_TYPE_GROUP = "group";

  /**
   * As we use a web UI the data is received as JSON. We work directly on this JSON tree model.
   */
  private final JsonNode visRoot;

  /**
   * @param visRoot The visual model as JSON, usually set from a user interface.
   * @param fieldsAndNodes
   */
  public VisualModel(final JsonNode visRoot) {
    final String rootNodeId = JsonUtils.getTextStrict(visRoot, VIS_NODE_ID);
    final String expected = NoticeSaver.ND_ROOT;
    Validate.isTrue(expected.equals(rootNodeId), "Visual model root must be %s", expected);
    this.visRoot = visRoot;
    this.getNoticeSubType(); // This must not crash.
  }

  public JsonNode getVisRoot() {
    return this.visRoot;
  }

  /**
   * Set default field info.
   */
  static void putFieldDef(final ObjectNode vis) {
    vis.put(VIS_TYPE, VIS_TYPE_FIELD);
    vis.put(VIS_CONTENT_COUNT, "1");
    vis.put(VIS_CONTENT_PARENT_COUNT, "1");
  }

  /**
   * Set default group info.
   */
  static void putGroupDef(final ObjectNode vis) {
    vis.put(VIS_TYPE, VIS_TYPE_GROUP);
    vis.put(VIS_CONTENT_COUNT, "1");
    vis.put(VIS_CONTENT_PARENT_COUNT, "1");
  }

  public static ArrayNode setupVisualRootForTest(final ObjectMapper mapper,
      final String fakeSdkForTest, final String noticeSubTypeForTest, final ObjectNode visRoot) {

    putGroupDef(visRoot);
    visRoot.put(VIS_CONTENT_ID, "the_visual_root");
    visRoot.put(VIS_NODE_ID, NoticeSaver.ND_ROOT);
    final ArrayNode visRootChildren = visRoot.putArray(VIS_CHILDREN);

    //
    // DUMMY NOTICE METADATA (as if coming from a web form before we have the XML).
    //
    {
      // SDK version.
      final ObjectNode visSdkVersion = mapper.createObjectNode();
      visRootChildren.add(visSdkVersion);
      putFieldDef(visSdkVersion);
      visSdkVersion.put(VIS_CONTENT_ID, NoticeSaver.FIELD_ID_SDK_VERSION);
      visSdkVersion.put(VIS_VALUE, fakeSdkForTest);
    }

    // Root xtension.
    {
      final ObjectNode visRootExtension = mapper.createObjectNode();
      visRootChildren.add(visRootExtension);
      putGroupDef(visRootExtension);
      visRootExtension.put(VIS_CONTENT_ID, "the_root_extension");
      visRootExtension.put(VIS_NODE_ID, NoticeSaver.ND_ROOT_EXTENSION);
      final ArrayNode visRootExtChildren = visRootExtension.putArray(VIS_CHILDREN);

      // Notice sub type.
      final ObjectNode visNoticeSubType = mapper.createObjectNode();
      visRootExtChildren.add(visNoticeSubType);
      putFieldDef(visNoticeSubType);
      visNoticeSubType.put(VIS_CONTENT_ID, NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE);
      visNoticeSubType.put(VIS_VALUE, noticeSubTypeForTest);
    }
    return visRootChildren;
  }


  public String getNoticeSubType() {
    final JsonNode rootExt = findByNodeId(visRoot, NoticeSaver.ND_ROOT_EXTENSION);
    final JsonNode noticeSubJson = findByContentId(rootExt, NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE);
    return JsonUtils.getTextStrict(noticeSubJson, VIS_VALUE);
  }

  private static JsonNode findByNodeId(final JsonNode toSearch, final String nodeId) {
    // HARDCODED LOGIC.
    final ArrayNode visChildren = (ArrayNode) toSearch.get(VIS_CHILDREN);
    for (final JsonNode searched : visChildren) {
      final JsonNode nodeIdJsonMaybeNull = searched.get(VIS_NODE_ID);
      if (nodeIdJsonMaybeNull != null) {
        final String nodeIdCurrent = nodeIdJsonMaybeNull.asText(null);
        if (nodeId.equals(nodeIdCurrent)) {
          return searched;
        }
      }
    }
    throw new RuntimeException(String.format("Node id=%s not found in visual model!", nodeId));
  }

  private static JsonNode findByContentId(final JsonNode toSearch, final String contentId) {
    // HARDCODED LOGIC.
    final ArrayNode visChildren = (ArrayNode) toSearch.get(VIS_CHILDREN);
    for (final JsonNode searched : visChildren) {
      final String contentIdCurrent = searched.get(VIS_CONTENT_ID).asText(null);
      if (contentId.equals(contentIdCurrent)) {
        return searched;
      }
    }
    throw new RuntimeException(
        String.format("Content id=%s not found in visual model!", contentId));
  }

  /**
   * Build the conceptual model from the visual model.
   *
   * @param visRoot The root of the visual model
   * @return The conceptual model
   */
  public ConceptualModel toConceptualModel(final FieldsAndNodes fieldsAndNodes) {
    logger.info("Attempting to build conceptual model from visual model.");

    final Optional<ConceptItem> conceptItemOpt = parseVisualModelRec(fieldsAndNodes, visRoot, null);
    if (!conceptItemOpt.isPresent()) {
      throw new RuntimeException("Expecting concept item at root level.");
    }
    final ConceptNode rootNode = (ConceptNode) conceptItemOpt.get();

    // TODO more work is required for the full metadata.

    return new ConceptualModel(rootNode);
  }

  /**
   * Visit the tree of the visual model and build the visual model. Depth-first search
   *
   * @param jsonItem The current visual json item
   * @return An optional concept item, if present it is to be appended outside of the call,
   *         otherwise no action should be taken in the caller
   */
  private static Optional<ConceptItem> parseVisualModelRec(final FieldsAndNodes fieldsAndNodes,
      final JsonNode jsonItem, final ConceptNode closestParentNode) {
    Validate.notNull(jsonItem, "jsonNode is null, jsonNode=%s", jsonItem);

    final JsonNode jsonType = jsonItem.get(VIS_TYPE);
    Validate.notNull(jsonType, "jsonType is null, jsonNode=%s", jsonItem);

    final int counter = jsonItem.get(VIS_CONTENT_COUNT).asInt(-1);
    final int parentCounter = jsonItem.get(VIS_CONTENT_PARENT_COUNT).asInt(-1);

    final String visualType = jsonType.asText(null);

    if (visualType == VIS_TYPE_FIELD) {
      // This is a field (leaf of the tree).
      final String id = JsonUtils.getTextStrict(jsonItem, VIS_CONTENT_ID);
      final ConceptField field = new ConceptField(id, id + "-" + Math.round(Math.random() * 1000),
          jsonItem.get(VIS_VALUE).asText(null), counter, parentCounter);
      return Optional.of(field); // Leaf of tree: just return.
    }

    if (visualType == VIS_TYPE_GROUP) {
      // This is a node.
      final JsonNode nodeIdItem = jsonItem.get(VIS_NODE_ID);
      if (nodeIdItem == null) {

        // This is a group which has no nodeId.
        // In that case we want the children to be moved up to the nearest parent node, flattening
        // the tree.
        final JsonNode maybeNull = jsonItem.get(VIS_CHILDREN);
        // Could be "null if empty" depending on how the JSON is constructed.
        if (maybeNull != null) {
          final ArrayNode visChildren = (ArrayNode) maybeNull;
          for (final JsonNode visChild : visChildren) {
            final Optional<ConceptItem> itemToAppendOpt =
                parseVisualModelRec(fieldsAndNodes, visChild, closestParentNode);
            if (itemToAppendOpt.isPresent()) {
              Validate.notNull(closestParentNode, "closestParentNode is null");
              closestParentNode.addConceptItem(itemToAppendOpt.get());
            }
          }
        }
        // Cannot return anything to append.
        return Optional.empty();
      }

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
              parseVisualModelRec(fieldsAndNodes, visChild, conceptNode);
          if (itemToAppendOpt.isPresent()) {
            // Append field or node.
            conceptNode.addConceptItem(itemToAppendOpt.get());
          }
        }
      }
      return Optional.of(conceptNode);
    }

    throw new RuntimeException(String.format("Unsupported visual type '%s'", visualType));
  }

}
