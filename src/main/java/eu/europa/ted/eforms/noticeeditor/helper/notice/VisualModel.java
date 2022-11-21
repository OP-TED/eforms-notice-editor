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
 * Visual model (VM).
 *
 * <p>
 * Wrapper around the JSON representation of the form data. Vis is be used as a shorthand for
 * visual.
 * </p>
 * <p>
 * NOTE: the form data hierarchy is supposed to follow the SDK notice-types definitions (.json)
 * hierarchy for the given SDK and notice sub type.
 * </p>
 * <p>
 * NOTE: the Jackson xyzNode objects are not related to the SDK node concept, it is just that the
 * term "node" is commonly used for items of a tree (tree nodes) and that JSON data is hierarchical.
 * </p>
 */
public class VisualModel {

  private static final Logger logger = LoggerFactory.getLogger(VisualModel.class);

  static final String VIS_CHILDREN = "children";

  static final String VIS_CONTENT_PARENT_COUNT = "contentParentCount";
  static final String VIS_CONTENT_COUNT = "contentCount";

  static final String VIS_CONTENT_ID = "contentId";
  static final String VIS_FIELD_ID = "visFieldId";
  static final String VIS_NODE_ID = "visNodeId";

  static final String VIS_VALUE = "value";
  static final String VIS_TYPE = "type";

  private static final String VIS_TYPE_FIELD = "field";
  private static final String VIS_TYPE_GROUP = "group";

  static final String VIS_FIRST = "-1";
  static final String VIS_SECOND = "-2";

  static final String NODE_PARENT_ID = "parentId";

  /**
   * As we use a web UI the data is received as JSON. We work directly on this JSON tree model.
   */
  private final JsonNode visRoot;

  /**
   * @param visRoot The visual model as JSON, usually set from a user interface.
   */
  public VisualModel(final JsonNode visRoot) {
    final String rootNodeId = JsonUtils.getTextStrict(visRoot, VIS_NODE_ID);
    final String expected = ConceptualModel.ND_ROOT;
    Validate.isTrue(expected.equals(rootNodeId), "Visual model root must be %s", expected);
    this.visRoot = visRoot;
    this.getNoticeSubTypeStrict(); // This must not crash.
  }

  /**
   * Set default field info.
   */
  static void putFieldDef(final ObjectNode vis, final String fieldId) {
    putFieldDef(vis, fieldId, 1);
  }

  /**
   * Set default field info.
   */
  static void putFieldDef(final ObjectNode vis, final String fieldId, final int count) {
    vis.put(VIS_TYPE, VIS_TYPE_FIELD);
    vis.put(VIS_CONTENT_ID, fieldId + "-" + count);
    vis.put(VIS_FIELD_ID, fieldId);
    vis.put(VIS_CONTENT_COUNT, count);
    vis.put(VIS_CONTENT_PARENT_COUNT, count); // TODO fix this or remove it?
  }

  /**
   * Set default group info.
   */
  static void putGroupDef(final ObjectNode vis) {
    vis.put(VIS_TYPE, VIS_TYPE_GROUP);
    vis.put(VIS_CONTENT_COUNT, 1);
    vis.put(VIS_CONTENT_PARENT_COUNT, 1); // TODO fix this or remove it?
  }

  /**
   * Common code which can be used in unit tests. It is placed here as it is about the visual model.
   */
  public static ArrayNode setupVisualRootForTest(final ObjectMapper mapper,
      final String fakeSdkForTest, final String noticeSubTypeForTest, final ObjectNode visRoot) {

    putGroupDef(visRoot);
    visRoot.put(VIS_CONTENT_ID, "the_visual_root");
    visRoot.put(VIS_NODE_ID, ConceptualModel.ND_ROOT);
    final ArrayNode visRootChildren = visRoot.putArray(VIS_CHILDREN);

    //
    // DUMMY NOTICE METADATA (as if coming from a web form before we have the XML).
    //
    {
      // SDK version.
      final ObjectNode visSdkVersion = mapper.createObjectNode();
      visRootChildren.add(visSdkVersion);
      putFieldDef(visSdkVersion, ConceptualModel.FIELD_ID_SDK_VERSION);
      visSdkVersion.put(VIS_VALUE, fakeSdkForTest);
    }

    // Root extension.
    {
      final ObjectNode visRootExtension = mapper.createObjectNode();
      visRootChildren.add(visRootExtension);
      putGroupDef(visRootExtension);
      visRootExtension.put(VIS_CONTENT_ID, "the_root_extension");
      visRootExtension.put(VIS_NODE_ID, ConceptualModel.ND_ROOT_EXTENSION);
      final ArrayNode visRootExtChildren = visRootExtension.putArray(VIS_CHILDREN);

      // Notice sub type.
      final ObjectNode visNoticeSubType = mapper.createObjectNode();
      visRootExtChildren.add(visNoticeSubType);
      putFieldDef(visNoticeSubType, ConceptualModel.FIELD_ID_NOTICE_SUB_TYPE);
      visNoticeSubType.put(VIS_VALUE, noticeSubTypeForTest);
    }
    return visRootChildren;
  }

  /**
   * @return The notice sub type, otherwise an exception is thrown
   */
  public String getNoticeSubTypeStrict() {
    final JsonNode rootExt = findByNodeIdStrict(visRoot, ConceptualModel.ND_ROOT_EXTENSION);
    final JsonNode noticeSubJson =
        findByFieldIdStrict(rootExt, ConceptualModel.FIELD_ID_NOTICE_SUB_TYPE);
    return JsonUtils.getTextStrict(noticeSubJson, VIS_VALUE);
  }

  /**
   * @param toSearch The json item to be searched into
   * @param nodeId The node id to find
   *
   * @return the json of the node, an exception is thrown if not found
   */
  private static JsonNode findByNodeIdStrict(final JsonNode toSearch, final String nodeId) {
    final ArrayNode visChildren = (ArrayNode) toSearch.get(VIS_CHILDREN);
    for (final JsonNode item : visChildren) {
      final Optional<String> nodeIdOpt = JsonUtils.getTextOpt(item, VIS_NODE_ID);
      if (nodeIdOpt.isPresent() && nodeIdOpt.get().equals(nodeId)) {
        return item; // Found the item.
      }
    }
    throw new RuntimeException(String.format("Node %s not found in visual model!", nodeId));
  }

  /**
   * @param toSearch The json item to be searched into
   * @param fieldId The field id to find
   *
   * @return the json of the field, an exception is thrown if not found
   */
  private static JsonNode findByFieldIdStrict(final JsonNode toSearch, final String fieldId) {
    final ArrayNode visChildren = (ArrayNode) toSearch.get(VIS_CHILDREN);
    for (final JsonNode item : visChildren) {
      final Optional<String> fieldIdOpt = JsonUtils.getTextOpt(item, VIS_FIELD_ID);
      if (fieldIdOpt.isPresent() && fieldIdOpt.get().equals(fieldId)) {
        return item; // Found the item.
      }
    }
    throw new RuntimeException(String.format("Field %s not found in visual model!", fieldId));
  }

  /**
   * Build the conceptual model from the visual model.
   *
   * @param fieldsAndNodes Field and node metadata
   * @return The conceptual model for this visual model
   */
  public ConceptualModel toConceptualModel(final FieldsAndNodes fieldsAndNodes) {
    logger.info("Attempting to build conceptual model from visual model.");

    final int childCounter = 1;

    // This is located in this class as most of the code is about reading the visual model.
    final Optional<ConceptTreeItem> conceptItemOpt =
        parseVisualModelRec(fieldsAndNodes, visRoot, null, childCounter);

    if (!conceptItemOpt.isPresent()) {
      throw new RuntimeException("Expecting concept item at root level.");
    }
    final ConceptTreeNode rootNode = (ConceptTreeNode) conceptItemOpt.get();

    // TODO more work is required for the full metadata (see with realistic X02) !?

    return new ConceptualModel(rootNode);
  }

  /**
   * Fills in the gaps by adding non-repeatable nodes to the concept model. Filling. See unit test
   * about filling to fully understand this.
   *
   * @param fieldsAndNodes SDK meta info
   * @param closestParentNode This is the closest parent node we have in the model
   * @param cn The current conceptual node
   */
  private static void addIntermediaryNonRepeatingNodesRec(final FieldsAndNodes fieldsAndNodes,
      final ConceptTreeNode closestParentNode, final ConceptTreeNode cn) {
    if (closestParentNode.getNodeId().equals(cn.getNodeId())) {
      // cn is the closest parent, stop.
      return;
    }
    final JsonNode nodeMeta = fieldsAndNodes.getNodeById(cn.getNodeId());
    final String nodeParentId = JsonUtils.getTextStrict(nodeMeta, NODE_PARENT_ID);
    if (nodeParentId.equals(closestParentNode.getNodeId())) {
      // The closestParent is the parent, just attach it and stop.
      // -> closestParent -> cn
      closestParentNode.addConceptNode(cn);
      return;
    }

    final JsonNode nodeParentMeta = fieldsAndNodes.getNodeById(nodeParentId);
    if (FieldsAndNodes.isNodeRepeatable(nodeParentMeta)) {
      // The SDK says the desired parentNodeId is repeatable and is missing in the
      // visual model, thus we have a serious problem!
      throw new RuntimeException(
          String.format("Problem in visual node hierarchy, unexpected missing repeatable nodeId=%s",
              nodeParentId));
    }
    // The parent is not the closest parent we know about and it is not repeatable.
    // Try to create an intermediary node in the conceptual model.
    // -> closestParent -> cnNew -> cn
    final ConceptTreeNode cnNew =
        new ConceptTreeNode(nodeParentId + VIS_FIRST + "-generated", nodeParentId, 1, 1);
    cnNew.addConceptNode(cn);

    // There may be more to add, recursion:
    addIntermediaryNonRepeatingNodesRec(fieldsAndNodes, closestParentNode, cnNew);
  }

  /**
   * Visit the tree of the visual model and build the visual model. Depth-first order, recursive.
   *
   * @param jsonItem The current visual json item
   * @param childCounter The position of the current item
   * @return An optional concept item, if present it is to be appended outside of the call,
   *         otherwise no action should be taken in the caller
   */
  private static Optional<ConceptTreeItem> parseVisualModelRec(final FieldsAndNodes fieldsAndNodes,
      final JsonNode jsonItem, final ConceptTreeNode closestParentNode, final int childCounter) {
    Validate.notNull(jsonItem, "jsonNode is null, jsonNode=%s", jsonItem);

    final int counter = jsonItem.get(VIS_CONTENT_COUNT).asInt(-1);
    final int parentCounter = jsonItem.get(VIS_CONTENT_PARENT_COUNT).asInt(-1);

    final String visualType = JsonUtils.getTextStrict(jsonItem, VIS_TYPE);
    if (visualType == VIS_TYPE_FIELD) {
      //
      // FIELD.
      //
      // This is a field (leaf of the tree).
      // Every field points to an SDK field for the SDK metadata.
      final String sdkFieldId = JsonUtils.getTextStrict(jsonItem, VIS_FIELD_ID);
      // final String idUnique = idInSdkFieldsJson + "-" + childCounter;
      final String idUnique = jsonItem.get(VIS_CONTENT_ID).asText(null);
      final ConceptTreeField conceptField = new ConceptTreeField(idUnique, sdkFieldId,
          jsonItem.get(VIS_VALUE).asText(null), counter, parentCounter);

      final JsonNode sdkFieldMeta = fieldsAndNodes.getFieldById(sdkFieldId);

      // We found a field.
      // But is the current concept hierarchy matching the hierarchy found in the SDK fields.json?
      final String sdkParentNodeId =
          JsonUtils.getTextStrict(sdkFieldMeta, FieldsAndNodes.FIELD_PARENT_NODE_ID);

      if (!closestParentNode.getNodeId().equals(sdkParentNodeId)) {
        // The parents do not match.

        final JsonNode sdkParentNode = fieldsAndNodes.getNodeById(sdkParentNodeId);
        if (FieldsAndNodes.isNodeRepeatable(sdkParentNode)) {
          // The SDK says the desired parentNodeId is repeatable and is missing in the visual model,
          // thus we have a serious problem!
          throw new RuntimeException(String.format(
              "Problem in visual node hierarchy, fieldId=%s is not included in the correct parent. Expecting %s but found %s",
              sdkFieldId, sdkParentNodeId, closestParentNode.getNodeId()));
        }

        // The SDK says the desired parent node is not repeatable or "non-repeatable". We can
        // tolerate that the visual model does not point to it (or not yet) and generate it as this
        // is not problematic in the visual model in this case. Ideally we want the full SDK node
        // chain to be present in the correct order in the conceptual model.
        // ND-Root -> ... -> ... -> otherConceptualNode -> The field (leaf)
        final Optional<ConceptTreeNode> cnOpt =
            closestParentNode.findFirstByConceptNodeId(sdkParentNodeId);
        final ConceptTreeNode cn;
        if (cnOpt.isPresent()) {
          // Reuse existing conceptual node.
          cn = cnOpt.get();
        } else {
          // Create and add the missing conceptual node.
          // Generate missing conceptual node.
          // IDEA what if more than one intermediary nodes are missing? For now we will assume that
          // this is not the case.
          // ND-Root -> ... -> closestParentNode -> newConceptNode -> ... -> field
          // By convention we will add "-generated" to these generated concept nodes.
          cn = new ConceptTreeNode(sdkParentNodeId + VIS_FIRST + "-generated", sdkParentNodeId, 1,
              1);

          // See unit test about filling to fully understand this.
          // closestParentNode.addConceptNode(cn); // NO: there may be more items to fill in.
          addIntermediaryNonRepeatingNodesRec(fieldsAndNodes, closestParentNode, cn);
        }

        // Always add the current field.
        cn.addConceptField(conceptField);

        return Optional.empty();
      }
      return Optional.of(conceptField); // Leaf of tree: just return.
    }

    if (visualType == VIS_TYPE_GROUP) {
      // This is a group (with or without nodeId).

      final Optional<String> nodeIdOpt = JsonUtils.getTextOpt(jsonItem, VIS_NODE_ID);
      if (nodeIdOpt.isEmpty()) {
        //
        // GROUP WITHOUT nodeId.
        //
        // This group is used purely to group fields visually in the UI (visual model).
        // We could call this a purely visual group.
        // The conceptual model must ignore this group but keep the content.
        // In that case we want the children to be moved up to the nearest parent node, flattening
        // the tree.
        final JsonNode maybeNull = jsonItem.get(VIS_CHILDREN);
        // Could be "null if empty" depending on how the JSON is constructed.
        if (maybeNull != null) {
          final ArrayNode visChildren = (ArrayNode) maybeNull;
          int childCounterB = 1;
          for (final JsonNode visChild : visChildren) {
            final Optional<ConceptTreeItem> itemToAppendOpt =
                parseVisualModelRec(fieldsAndNodes, visChild, closestParentNode, childCounterB++);
            if (itemToAppendOpt.isPresent()) {
              Validate.notNull(closestParentNode, "closestParentNode is null");
              closestParentNode.addConceptItem(itemToAppendOpt.get());
            }
          }
        }
        // Cannot return anything to append.
        return Optional.empty();
      }

      //
      // GROUP WITH nodeId.
      //
      // This is a group which references a node.
      final String sdkNodeId = nodeIdOpt.get();
      fieldsAndNodes.getNodeById(sdkNodeId); // Just for the checks.

      // final String idUnique = idInSdkFieldsJson + "-" + childCounter;
      final String idUnique = jsonItem.get(VIS_CONTENT_ID).asText(null);
      final ConceptTreeNode conceptNode =
          new ConceptTreeNode(idUnique, sdkNodeId, jsonItem.get(VIS_CONTENT_COUNT).asInt(-1),
              jsonItem.get(VIS_CONTENT_PARENT_COUNT).asInt(-1));

      // Not a leaf of the tree: recursion on children:
      final JsonNode maybeNull = jsonItem.get(VIS_CHILDREN);
      // Could be null depending on how the JSON is serialized (not present, thus null if the array
      // is empty).
      if (maybeNull != null) {
        final ArrayNode visChildren = (ArrayNode) maybeNull;
        for (final JsonNode visChild : visChildren) {
          int childCounterC = 1;
          final Optional<ConceptTreeItem> itemToAppendOpt =
              parseVisualModelRec(fieldsAndNodes, visChild, conceptNode, childCounterC++);
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
