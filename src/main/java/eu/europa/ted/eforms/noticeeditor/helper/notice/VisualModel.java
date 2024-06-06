package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.util.GraphvizDotTool;
import eu.europa.ted.eforms.noticeeditor.util.JavaTools;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

/**
 * <h1>The Visual Model (VM)</h1>
 *
 * <p>
 * Wrapper around the JSON representation of the form data. Vis is be used as a shorthand for
 * visual.
 * </p>
 *
 * <p>
 * NOTE: the form data hierarchy is supposed to follow the SDK notice-types definitions (.json)
 * hierarchy for the given SDK and notice sub type.
 * </p>
 *
 * <p>
 * NOTE: the Jackson xyzNode objects are not related to the SDK node concept, it is just that the
 * term "node" is commonly used for items of a tree (tree nodes) and that JSON data is hierarchical.
 * </p>
 */
public class VisualModel {

  private static final Logger logger = LoggerFactory.getLogger(VisualModel.class);

  /**
   * This is used in the front-end, where the visual model is created.
   */
  private static final String NOTICE_METADATA = "notice-metadata";

  public static final String VIS_SDK_VERSION = "sdkVersion";
  public static final String VIS_NOTICE_UUID = "noticeUuid";
  private static final String VIS_NOTICE_SUB_TYPE = "noticeSubType";

  static final String VIS_CHILDREN = "children";
  static final String VIS_CONTENT_COUNT = "contentCount";
  static final String VIS_CONTENT_ID = "contentId";
  static final String VIS_NODE_ID = "visNodeId";

  static final String VIS_VALUE = "value";
  static final String VIS_TYPE = "visType"; // Called visType to avoid confusion with HTML type attr

  private static final String VIS_TYPE_FIELD = "field";
  private static final String VIS_TYPE_NON_FIELD = "non-field";

  private static final String SUFFIX_GENERATED = "-generated";

  /**
   * As we use a web UI the data is received as JSON. We work directly on this JSON tree model.
   */
  private final JsonNode visRoot;

  /**
   * This can be used for debugging, setting values only on items of interest
   */
  private final boolean skipIfNoValue;

  /**
   * @param visRoot The visual model as JSON, usually set from a user interface.
   */
  public VisualModel(final JsonNode visRoot, final boolean skipIfNoValue) {
    final String rootNodeId = JsonUtils.getTextStrict(visRoot, VIS_NODE_ID);
    final String expected = ConceptualModel.ND_ROOT;
    Validate.isTrue(expected.equals(rootNodeId), "Visual model root must be %s", expected);
    this.visRoot = visRoot;
    this.skipIfNoValue = skipIfNoValue;
    getNoticeSubType(); // This must not crash.
  }

  public VisualModel(final JsonNode visRoot) {
    this(visRoot, false);
  }

  @Override
  public String toString() {
    try {
      return JsonUtils.getStandardJacksonObjectMapper().writerWithDefaultPrettyPrinter()
          .writeValueAsString(visRoot);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

  private String getNoticeSubType() {
    return JsonUtils.getTextStrict(visRoot, VIS_NOTICE_SUB_TYPE);
  }

  public JsonNode getVisRoot() {
    return this.visRoot;
  }

  public ArrayNode getVisRootChildren() {
    return getChildren(this.visRoot);
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
    vis.put(VIS_CONTENT_ID, fieldId);
    vis.put(VIS_CONTENT_COUNT, count);
  }

  /**
   * Set default group info.
   */
  static void putGroupDef(final ObjectNode vis) {
    vis.put(VIS_TYPE, VIS_TYPE_NON_FIELD);
    vis.put(VIS_CONTENT_COUNT, 1);
  }

  /**
   * Common code which can be used in unit tests. It is placed here as it is about the visual model.
   */
  public static ArrayNode setupVisualRootForTest(final ObjectMapper mapper,
      final String fakeSdkForTest, final String noticeSubTypeForTest, final ObjectNode visRoot) {

    putGroupDef(visRoot);
    visRoot.put(VIS_CONTENT_ID, "the_visual_root");

    // TODO remove this ND ROOT from here.
    visRoot.put(VIS_NODE_ID, ConceptualModel.ND_ROOT);

    // Put some primary info at the top level.
    visRoot.put(VIS_SDK_VERSION, fakeSdkForTest);
    visRoot.put(VIS_NOTICE_SUB_TYPE, noticeSubTypeForTest);

    final String noticeUuid = UUID.randomUUID().toString();
    visRoot.put(VIS_NOTICE_UUID, noticeUuid);

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

      final ObjectNode metadata = mapper.createObjectNode();
      visRootChildren.add(metadata);
      putGroupDef(metadata);
      metadata.put(VIS_CONTENT_ID, NOTICE_METADATA);
      final ArrayNode metadataChildren = metadata.putArray(VIS_CHILDREN);

      // Notice sub type.
      final ObjectNode visNoticeSubType = mapper.createObjectNode();
      metadataChildren.add(visNoticeSubType);
      putFieldDef(visNoticeSubType, ConceptualModel.FIELD_NOTICE_ID);
      visNoticeSubType.put(VIS_VALUE, noticeUuid);
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
   * Build the conceptual model from the visual model.
   *
   * @param fieldsAndNodes Field and node metadata
   *
   * @return The conceptual model for this visual model
   */
  public ConceptualModel toConceptualModel(final FieldsAndNodes fieldsAndNodes,
      final boolean debug) {
    logger.info("Attempting to build the conceptual model from the visual model.");

    // This fake top level node is used to simplify the algorithm, as we always want to have a
    // parent to attach to but for the root we have none.
    final ConceptTreeNode fakeConceptRoot =
        new ConceptTreeNode("fake_root", "ND-Fake-Root",
            1, false);

    final StringBuilder sb = new StringBuilder(512);

    // This is located in this class as most of the code is about reading the visual model.
    parseVisualModelRec(fieldsAndNodes, visRoot, fakeConceptRoot, skipIfNoValue, sb);

    if (debug) {
      final Path path = Path.of("target", "debug");
      try {
        Files.createDirectories(path);
        JavaTools.writeTextFile(path.resolve("visual-model.json"), this.toString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (debug) {
      final Path path = Path.of("target", "debug");
      try {
        Files.createDirectories(path);
        JavaTools.writeTextFile(path.resolve("conceptual-model.txt"), sb.toString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    Validate.isTrue(1 == fakeConceptRoot.getConceptNodes().size(), "Expecting one element!");
    final ConceptTreeNode conceptRoot = fakeConceptRoot.getConceptNodes().get(0);

    Validate.notEmpty(conceptRoot.getConceptNodes(), "Concept nodes list is empty");
    Validate.notEmpty(conceptRoot.getConceptFields(), "Concept fields list is empty");

    return new ConceptualModel(conceptRoot, fieldsAndNodes.getSdkVersion());
  }

  /**
   * Fills in the gaps by adding non-repeatable nodes to the concept model. Filling. See unit test
   * about filling to fully understand this.
   *
   * @param fieldsAndNodes SDK meta info
   * @param closestParentNode This is the closest parent node we have in the model
   * @param cn The current conceptual node
   * @param fieldOrNodeId The id of the content for which this was started
   * @return the passed conceptual node (cn), or a parent cn that was added around it
   */
  private static ConceptTreeNode addIntermediaryNonRepeatingNodesRec(
      final FieldsAndNodes fieldsAndNodes,
      final ConceptTreeNode closestParentNode, final ConceptTreeNode cn,
      final String fieldOrNodeId, final StringBuilder sb) {

    final String currentNodeId = cn.getNodeId();
    if (closestParentNode.getNodeId().equals(currentNodeId)) {
      // cn is the closest parent, stop.
      return cn;
    }

    if (ConceptualModel.ND_ROOT.equals(currentNodeId)) {
      // Went as far as possible.
      return cn;
    }

    final JsonNode nodeMeta = fieldsAndNodes.getNodeById(currentNodeId);
    final String cnParentIdInSdk =
        JsonUtils.getTextStrict(nodeMeta, FieldsAndNodes.NODE_PARENT_NODE_ID);
    if (cnParentIdInSdk.equals(closestParentNode.getNodeId())) {
      // The closestParent is the parent of cn (desired), just attach it and stop.
      // -> closestParent -> cn
      final boolean strict = false;
      final boolean added = closestParentNode.addConceptNode(cn, strict, sb);
      if (added) {
        logger.debug(
            "Case A: true Added intermediary concept tree node, conceptNodeId={}, sdkId={}",
            cn.getIdUnique(), fieldOrNodeId);
      } else {
        logger.debug(
            "Case A: false Added intermediary concept tree node, conceptNodeId={}, sdkId={}",
            cn.getIdUnique(), fieldOrNodeId);
      }
      return cn;
    }

    final boolean isRepeatable = fieldsAndNodes.isNodeRepeatable(cnParentIdInSdk);
    if (isRepeatable) {
      // The SDK says the desired parentNodeId is repeatable and is missing in the
      // visual model, thus we have a serious problem!
      final String msg =
          String.format(
              "Problem in visual node hierarchy, unexpected missing repeatable nodeId=%s, "
                  + "sdkId=%s",
              cnParentIdInSdk, fieldOrNodeId);
      System.err.println(msg);
      // throw new RuntimeException(msg);
    }

    final String uniqueId = cnParentIdInSdk + SUFFIX_GENERATED;
    sb.append("Adding intermediary node=")
        .append(uniqueId)
        .append(" for ").append(currentNodeId)
        .append('\n');

    // The parent is not the closest parent we know about and it is not repeatable.
    // Try to create an intermediary node in the conceptual model.
    // -> closestParent -> cnNew -> cn
    final ConceptTreeNode cnNew =
        new ConceptTreeNode(uniqueId, cnParentIdInSdk, 1, isRepeatable);
    final boolean strict = false;
    final boolean added = cnNew.addConceptNode(cn, strict, sb);
    if (added) {
      logger.debug(
          "Case B: true Added intermediary concept tree node, conceptNodeId={}, sdkId={}",
          cn.getIdUnique(), fieldOrNodeId);
    } else {
      logger.debug(
          "Case B: false Added intermediary concept tree node, conceptNodeId={}, sdkId={}",
          cn.getIdUnique(), fieldOrNodeId);
    }

    // There may be more to add, recursion:
    return addIntermediaryNonRepeatingNodesRec(fieldsAndNodes, closestParentNode, cnNew,
        fieldOrNodeId, sb);
  }

  /**
   * Visit the tree of the visual model and build the visual model. Depth-first order, recursive.
   *
   * @param jsonItem The current visual json item
   * @param closestParentNode It must be attached to the root directly or indirectly.
   * @param sb A string builder used for debugging
   * @return An optional concept item, if present it is to be appended outside of the call,
   *         otherwise no action should be taken in the caller
   */
  private static void parseVisualModelRec(final FieldsAndNodes fieldsAndNodes,
      final JsonNode jsonItem, final ConceptTreeNode closestParentNode,
      final boolean skipIfNoValue, final StringBuilder sb) {
    Validate.notNull(closestParentNode, "closestParentNode is null, jsonItem=%s", jsonItem);
    Validate.notNull(jsonItem, "jsonNode is null, closestParentNode=%s", closestParentNode);

    final String visContentId = getContentId(jsonItem);
    final String visualType = getContentType(jsonItem);

    //
    // VISUAL FIELD.
    // Regular form field.
    //
    if (isField(visualType)) {
      sb.append('\n');
      sb.append("Field: ").append(visContentId).append('\n');
      handleVisualField(fieldsAndNodes, jsonItem, closestParentNode, skipIfNoValue, visContentId,
          sb);
      return;
    }

    //
    // VISUAL NON-FIELD.
    // For example a group.
    //
    if (isNonField(visualType)) {
      sb.append('\n');
      sb.append("Group: ").append(visContentId).append('\n');
      handleVisualGroup(fieldsAndNodes, jsonItem, closestParentNode, visContentId,
          skipIfNoValue, sb);
      return;
    }

    throw new RuntimeException(String.format("Unsupported visual type '%s'", visualType));
  }

  private static void handleVisualField(final FieldsAndNodes fieldsAndNodes,
      final JsonNode jsonItem, final ConceptTreeNode closestParentNode, final boolean skipIfNoValue,
      final String visContentId, final StringBuilder sb) {
    // What we call a field is some kind of form field which has a value. The value came from an
    // input, textarea, combobox, ...
    final JsonNode counterJson = jsonItem.get(VIS_CONTENT_COUNT);
    Validate.notNull(counterJson, "visual count is null for %s", visContentId);
    final int counter = jsonItem.get(VIS_CONTENT_COUNT).asInt(-1);

    final Optional<ConceptTreeField> conceptFieldOpt = buildVisualField(jsonItem, visContentId,
        counter, skipIfNoValue);

    if (conceptFieldOpt.isPresent()) {
      final ConceptTreeField conceptItem = conceptFieldOpt.get();

      final String sdkId = conceptItem.getIdInSdkFieldsJson();
      final JsonNode sdkFieldMeta = fieldsAndNodes.getFieldById(sdkId);

      // We found a field.
      // But is the current concept hierarchy matching the hierarchy found in the SDK fields.json?
      final String sdkParentNodeId =
          JsonUtils.getTextStrict(sdkFieldMeta, FieldsAndNodes.FIELD_PARENT_NODE_ID);

      addConceptualItem(fieldsAndNodes, closestParentNode, conceptItem, sdkId, sdkParentNodeId, sb);
    }
  }

  private static void addConceptualItem(final FieldsAndNodes fieldsAndNodes,
      final ConceptTreeNode closestParentNode, final ConceptTreeItem conceptItem,
      final String sdkId, final String sdkParentNodeId, final StringBuilder sb) {
    if (sdkParentNodeId == null) {
      // Special case for the root.
      sb.append("CASE OF ROOT").append('\n');
      closestParentNode.addConceptItem(conceptItem, sb);
      return;
    }
    if (!closestParentNode.getNodeId().equals(sdkParentNodeId)) {
      // The parents do not match.
      final boolean isRepeatable = fieldsAndNodes.isNodeRepeatable(sdkParentNodeId);
      if (isRepeatable) {
        // The SDK says the desired parentNodeId is repeatable and is missing in the visual
        // model, thus we have a serious problem!
        final String msg = String.format(
            "Problem in visual node hierarchy, fieldId=%s is not included"
                + " in the correct parent. Expecting nodeId=%s but found nodeId=%s",
            sdkId, sdkParentNodeId, closestParentNode.getNodeId());
        System.err.println(msg);
        // throw new RuntimeException(msg);
      }

      // The SDK says the desired parent node is not repeatable or "non-repeatable". We can
      // tolerate that the visual model does not point to it (or not yet) and generate it as
      // this
      // is not problematic in the visual model in this case. Ideally we want the full SDK node
      // chain to be present in the correct order in the conceptual model.
      // ND-Root -> ... -> ... -> otherConceptualNode -> The field (leaf)
      final Optional<ConceptTreeNode> cnOpt =
          closestParentNode.findFirstByConceptNodeId(sdkParentNodeId);
      ConceptTreeNode cn;
      if (cnOpt.isPresent()) {
        // Reuse existing conceptual node.
        cn = cnOpt.get();
      } else {
        sb.append("Concept node sdkParentNodeId=")
            .append(sdkParentNodeId).append(" not present in closestParentNode=")
            .append(closestParentNode)
            .append('\n');
        // Create and add the missing conceptual node.
        // Generate missing conceptual node.
        // IDEA what if more than one intermediary nodes are missing? For now we will assume
        // that
        // this is not the case.
        // ND-Root -> ... -> closestParentNode -> newConceptNode -> ... -> field
        // By convention we will add a suffix to these generated concept nodes.
        final String idUnique = sdkParentNodeId + SUFFIX_GENERATED;
        cn = new ConceptTreeNode(idUnique, sdkParentNodeId, 1,
            isRepeatable);

        // See unit test about filling to fully understand this.
        // closestParentNode.addConceptNode(cn); // NO: there may be more items to fill in.
        // cn = addIntermediaryNonRepeatingNodesRec( // NO !
        addIntermediaryNonRepeatingNodesRec(fieldsAndNodes, closestParentNode, cn, sdkId,
            sb);
      }

      // Always add the current item.
      cn.addConceptItem(conceptItem, sb);
    } else {
      closestParentNode.addConceptItem(conceptItem, sb);
    }
  }

  private static boolean isNonField(final String visualType) {
    return VIS_TYPE_NON_FIELD.equals(visualType);
  }

  private static boolean isField(final String visualType) {
    return VIS_TYPE_FIELD.equals(visualType);
  }

  private static String getContentType(final JsonNode jsonItem) {
    return JsonUtils.getTextStrict(jsonItem, VIS_TYPE);
  }

  private static String getContentId(final JsonNode jsonItem) {
    return JsonUtils.getTextStrict(jsonItem, VIS_CONTENT_ID);
  }

  private static String getContentCount(final JsonNode jsonItem) {
    return JsonUtils.getTextStrict(jsonItem, VIS_CONTENT_COUNT);
  }

  private static ArrayNode getChildren(final JsonNode item) {
    return (ArrayNode) item.get(VIS_CHILDREN);
  }

  /**
   *
   * @param closestParentNode May be modified as a side effect.
   */
  private static void handleVisualGroup(final FieldsAndNodes fieldsAndNodes,
      final JsonNode visualItem, final ConceptTreeNode closestParentNode, final String contentId,
      final boolean skipIfNoValue, final StringBuilder sb) {

    // This is a visual group (with or without nodeId).
    final Optional<String> visualNodeIdOpt = getNodeIdOpt(visualItem);

    //
    // GROUP WITH NO nodeId.
    //
    // This group is used purely to group fields visually in the UI (visual model).
    // We could call this a purely visual group.
    //
    if (visualNodeIdOpt.isEmpty()) {
      sb.append("Visual node id: none").append('\n');
      handleGroupWithoutNodeId(fieldsAndNodes, visualItem, closestParentNode, skipIfNoValue, sb);
    } else {
      sb.append("Visual node id: ").append(visualNodeIdOpt.get()).append('\n');
      //
      // GROUP WITH nodeId.
      //
      // This is a group which references a node.
      // This group must be kept in the conceptual model.
      //
      handleGroupWithNodeId(fieldsAndNodes, visualItem, closestParentNode, contentId,
          visualNodeIdOpt.get(), skipIfNoValue, sb);
    }
  }

  private static Optional<String> getNodeIdOpt(final JsonNode jsonItem) {
    return JsonUtils.getTextOpt(jsonItem, VIS_NODE_ID);
  }

  private static void handleGroupWithNodeId(final FieldsAndNodes fieldsAndNodes,
      final JsonNode visualItem, final ConceptTreeNode closestParentNode, final String visContentId,
      final String sdkNodeId, final boolean skipIfNoValue, final StringBuilder sb) {
    Validate.notBlank(sdkNodeId, "sdkNodeId is blank for visContentId=%s", visContentId);

    final boolean isRepeatable = fieldsAndNodes.isNodeRepeatable(sdkNodeId);
    final ConceptTreeNode conceptNodeNew = new ConceptTreeNode(visContentId, sdkNodeId,
        visualItem.get(VIS_CONTENT_COUNT).asInt(-1), isRepeatable);

    final JsonNode sdkNodeMeta = fieldsAndNodes.getNodeById(sdkNodeId);

    // We found a field.
    // But is the current concept hierarchy matching the hierarchy found in the SDK
    // fields.json?
    final String sdkParentNodeId = FieldsAndNodes.ND_ROOT.equals(sdkNodeId) ? null
        : JsonUtils.getTextStrict(sdkNodeMeta, FieldsAndNodes.NODE_PARENT_NODE_ID);

    // This concept node is not yet attached to anything as it was just created!
    // Attach it.
    addConceptualItem(fieldsAndNodes, closestParentNode, conceptNodeNew, sdkNodeId,
        sdkParentNodeId, sb);

    //
    // Not a leaf of the tree: recursion on children:
    //
    // The children array could be null depending on how the JSON is serialized (not present in the
    // JSON at all means null, or empty []).
    final JsonNode childrenMaybeNull = VisualModel.getChildren(visualItem);
    if (childrenMaybeNull == null) {
      sb.append("Found no visual child items for ").append(visContentId).append('\n');
      return; // Cannot return anything to append to.
    }
    final ArrayNode visChildren = (ArrayNode) childrenMaybeNull;
    for (final JsonNode visChild : visChildren) {
      parseVisualModelRec(fieldsAndNodes, visChild, conceptNodeNew, skipIfNoValue, sb);
    }
  }

  private static void handleGroupWithoutNodeId(final FieldsAndNodes fieldsAndNodes,
      final JsonNode visualItem, final ConceptTreeNode closestParentNode,
      final boolean skipIfNoValue, final StringBuilder sb) {

    // The conceptual model must ignore this group but keep the contained content.
    // In that case we want the children to be moved up to the nearest conceptual parent node.
    // This is flattening/simplifying the tree.
    // In other words the visual tree has extra items that the conceptual model does not need.

    // Could be "null if empty" depending on how the JSON is constructed.
    // No children in JSON could be a value like [] or just no key value pair.
    // Both possibilities are tolerated.
    final JsonNode maybeNull = VisualModel.getChildren(visualItem);
    if (maybeNull == null) {
      sb.append("Visual item has no child items, cannot return anything to append to").append('\n');
      return; // Cannot return anything to append to.
    }

    final ArrayNode visChildren = (ArrayNode) maybeNull;
    for (final JsonNode visChild : visChildren) {
      parseVisualModelRec(fieldsAndNodes, visChild, closestParentNode, skipIfNoValue, sb);
    }
  }

  /**
   * @param jsonItem The current visual json item
   * @param sdkFieldId The SDK field id for the current visual json item
   * @param skipIfNoValue Skip if there is no value
   */
  private static Optional<ConceptTreeField> buildVisualField(
      final JsonNode jsonItem, final String sdkFieldId, final int counter,
      final boolean skipIfNoValue) {

    // This is a visual field (leaf of the tree).
    // Every visual field points to an SDK field for the SDK metadata.
    final String value = jsonItem.get(VIS_VALUE).asText(null);
    if (skipIfNoValue && StringUtils.isBlank(value)) {
      // This helps when debugging, in case only one value has been set, ti
      return Optional.empty();
    }
    final ConceptTreeField conceptField =
        new ConceptTreeField(sdkFieldId, sdkFieldId, value, counter);

    return Optional.of(conceptField); // Leaf of tree: just return.
  }

  // private static String getUniqueIdForDotSimple(
  // final String uniqueIdOfParentForDot, final JsonNode item, final Optional<String> nodeIdOpt) {
  // return getContentId(item) + (nodeIdOpt.isPresent() ? "_" + nodeIdOpt.get() : "");
  // }

  private static String getUniqueIdForDot(
      final String uniqueIdOfParentForDot, final JsonNode item, final Optional<String> nodeIdOpt) {
    return

    // Added so to make it unique in the dot ... (but hard to read)
    (StringUtils.isNotBlank(uniqueIdOfParentForDot) ? "_" + uniqueIdOfParentForDot : "")

        + getContentId(item)

        + (nodeIdOpt.isPresent() ? "_" + nodeIdOpt.get() : "")

        // We need the count to avoid merging of repeating nodes and fields.
        + "_" + getContentCount(item);
  }

  /**
   * Recursively create DOT format text and append it to the string builder (sb).
   *
   * @param fieldsAndNodes SDK field and node metadata
   * @param includeFields If true include fields in the graph, otherwise do not
   */
  private static void toDotRec(final String uniqueIdOfParentForDot,
      final FieldsAndNodes fieldsAndNodes,
      final StringBuilder sb, final JsonNode item, final boolean includeFields) {
    final Optional<String> nodeIdOpt = getNodeIdOpt(item);
    final String idUniqueForDot = getUniqueIdForDot(uniqueIdOfParentForDot, item, nodeIdOpt);

    final String edgeLabel = "";
    // final String edgeLabel = nodeIdOpt.isPresent() ? nodeIdOpt.get() : idUnique;

    // Include children in dot file.
    final String visualType = VisualModel.getContentType(item);
    if (VisualModel.isNonField(visualType)) {
      for (final JsonNode child : VisualModel.getChildren(item)) {

        final Optional<String> childNodeIdOpt = getNodeIdOpt(child);
        final boolean nodeIsRepeatable = fieldsAndNodes.isNodeRepeatable(childNodeIdOpt);
        final String color =
            nodeIsRepeatable ? GraphvizDotTool.COLOR_GREEN : GraphvizDotTool.COLOR_BLACK;

        final String idUniqueChildForDot = getUniqueIdForDot(idUniqueForDot, child, childNodeIdOpt);

        GraphvizDotTool.appendEdge(edgeLabel, color,
            idUniqueForDot, idUniqueChildForDot, // concept node -> child concept node
            sb);

        toDotRec(idUniqueChildForDot, fieldsAndNodes, sb, child, includeFields);
      }
    }
  }

  /**
   * This can be used for visualization of the visual model tree (as a graph). The graphviz dot text
   * itself is interesting but it makes even more sense when seen inside a tool.
   */
  private String toDot(final FieldsAndNodes fieldsAndNodes, final boolean includeFields) {

    final StringBuilder sb = new StringBuilder(1024);
    final JsonNode root = this.visRoot;
    toDotRec("", fieldsAndNodes, sb, root, includeFields);

    final StringBuilder sbDot = new StringBuilder();
    final String noticeSubType = this.getNoticeSubType();
    final String title = "visual_" + noticeSubType; // - is not supported.
    GraphvizDotTool.appendDiGraph(sb.toString(), sbDot, title, "Visual model of " + noticeSubType,
        false, true);

    return sbDot.toString();
  }

  /**
   * Write dot graph file for debugging purposes.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS", justification = "Ok here")
  public void writeDotFile(final FieldsAndNodes fieldsAndNodes) {
    try {
      // Generate dot file for the conceptual model.
      // Visualizing it can help understand how it works or find problems.
      final boolean includeFields = true;
      final String dotText = this.toDot(fieldsAndNodes, includeFields);
      final Path pathToFolder = Path.of("target", "dot");
      Files.createDirectories(pathToFolder);
      final Path pathToFile = pathToFolder.resolve(this.getNoticeSubType() + "-visual.dot");
      JavaTools.writeTextFile(pathToFile, dotText);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
