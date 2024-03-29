package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
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

  @Override
  public String toString() {
    try {
      return JsonUtils.getStandardJacksonObjectMapper().writeValueAsString(visRoot);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * @param visRoot The visual model as JSON, usually set from a user interface.
   */
  public VisualModel(final JsonNode visRoot) {
    final String rootNodeId = JsonUtils.getTextStrict(visRoot, VIS_NODE_ID);
    final String expected = ConceptualModel.ND_ROOT;
    Validate.isTrue(expected.equals(rootNodeId), "Visual model root must be %s", expected);
    this.visRoot = visRoot;
    getNoticeSubType(); // This must not crash.
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
      metadata.put(VIS_CONTENT_ID, "notice-metadata");
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
   * @return The conceptual model for this visual model
   */
  public ConceptualModel toConceptualModel(final FieldsAndNodes fieldsAndNodes) {
    logger.info("Attempting to build the conceptual model from the visual model.");

    // This is located in this class as most of the code is about reading the visual model.
    final Optional<ConceptTreeItem> conceptItemOpt =
        parseVisualModelRec(fieldsAndNodes, visRoot, null);
    if (!conceptItemOpt.isPresent()) {
      throw new RuntimeException("Expecting concept item at root level.");
    }
    final ConceptTreeNode rootNode = (ConceptTreeNode) conceptItemOpt.get();
    return new ConceptualModel(rootNode, fieldsAndNodes.getSdkVersion());
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

    if (ConceptualModel.ND_ROOT.equals(cn.getNodeId())) {
      return;
    }

    final JsonNode nodeMeta = fieldsAndNodes.getNodeById(cn.getNodeId());
    final String nodeParentId =
        JsonUtils.getTextStrict(nodeMeta, FieldsAndNodes.NODE_PARENT_NODE_ID);
    if (nodeParentId.equals(closestParentNode.getNodeId())) {
      // The closestParent is the parent, just attach it and stop.
      // -> closestParent -> cn
      closestParentNode.addConceptNode(cn, false);
      return;
    }

    final boolean isRepeatable = fieldsAndNodes.isNodeRepeatable(nodeParentId);
    if (isRepeatable) {
      // The SDK says the desired parentNodeId is repeatable and is missing in the
      // visual model, thus we have a serious problem!
      final String msg =
          String.format("Problem in visual node hierarchy, unexpected missing repeatable nodeId=%s",
              nodeParentId);
      System.err.println(msg);
      // throw new RuntimeException(msg);
    }

    // The parent is not the closest parent we know about and it is not repeatable.
    // Try to create an intermediary node in the conceptual model.
    // -> closestParent -> cnNew -> cn
    final ConceptTreeNode cnNew =
        new ConceptTreeNode(nodeParentId + SUFFIX_GENERATED, nodeParentId, 1, isRepeatable);
    cnNew.addConceptNode(cn, false);

    // There may be more to add, recursion:
    addIntermediaryNonRepeatingNodesRec(fieldsAndNodes, closestParentNode, cnNew);
  }

  /**
   * Visit the tree of the visual model and build the visual model. Depth-first order, recursive.
   *
   * @param jsonItem The current visual json item
   * @return An optional concept item, if present it is to be appended outside of the call,
   *         otherwise no action should be taken in the caller
   */
  private static Optional<ConceptTreeItem> parseVisualModelRec(final FieldsAndNodes fieldsAndNodes,
      final JsonNode jsonItem, final ConceptTreeNode closestParentNode) {
    Validate.notNull(jsonItem, "jsonNode is null, jsonNode=%s", jsonItem);

    final String visContentId = getContentId(jsonItem);
    final String visualType = getContentType(jsonItem);

    //
    // VISUAL FIELD.
    //
    if (isField(visualType)) {
      // What we call a field is some kind of form field which has a value.
      final JsonNode counterJson = jsonItem.get(VIS_CONTENT_COUNT);
      Validate.notNull(counterJson, "visual count is null for %s", visContentId);
      final int counter = jsonItem.get(VIS_CONTENT_COUNT).asInt(-1);
      return handleVisualField(fieldsAndNodes, jsonItem, closestParentNode, visContentId, counter);
    }

    //
    // VISUAL NON-FIELD (group, ...)
    //
    if (isNonField(visualType)) {
      return handleVisualGroup(fieldsAndNodes, jsonItem, closestParentNode, visContentId);
    }

    throw new RuntimeException(String.format("Unsupported visual type '%s'", visualType));
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

  private static ArrayNode getChildren(final JsonNode item) {
    return (ArrayNode) item.get(VIS_CHILDREN);
  }

  private static Optional<ConceptTreeItem> handleVisualGroup(final FieldsAndNodes fieldsAndNodes,
      final JsonNode jsonItem, final ConceptTreeNode closestParentNode, final String contentId) {

    // This is a group (with or without nodeId).
    final Optional<String> nodeIdOpt = getNodeIdOpt(jsonItem);

    //
    // GROUP WITH NO nodeId.
    //
    // This group is used purely to group fields visually in the UI (visual model).
    // We could call this a purely visual group.
    //
    if (nodeIdOpt.isEmpty()) {
      handleGroupWithNodeId(fieldsAndNodes, jsonItem, closestParentNode);
      return Optional.empty(); // Cannot return anything to append to as it was removed.
    }

    //
    // GROUP WITH nodeId.
    //
    // This is a group which references a node.
    // This group must be kept in the conceptual model.
    //
    final ConceptTreeNode conceptNode =
        handleGroupWithoutNodeId(fieldsAndNodes, jsonItem, contentId, nodeIdOpt);
    return Optional.of(conceptNode);
  }

  private static Optional<String> getNodeIdOpt(final JsonNode jsonItem) {
    return JsonUtils.getTextOpt(jsonItem, VIS_NODE_ID);
  }

  private static ConceptTreeNode handleGroupWithoutNodeId(final FieldsAndNodes fieldsAndNodes,
      final JsonNode jsonItem, final String contentId, final Optional<String> nodeIdOpt) {

    final String sdkNodeId = nodeIdOpt.get();
    fieldsAndNodes.getNodeById(sdkNodeId); // Just for the checks.

    final boolean isRepeatable = fieldsAndNodes.isNodeRepeatable(nodeIdOpt.get());
    final ConceptTreeNode conceptNode = new ConceptTreeNode(contentId, sdkNodeId,
        jsonItem.get(VIS_CONTENT_COUNT).asInt(-1), isRepeatable);

    // Not a leaf of the tree: recursion on children:
    final JsonNode maybeNull = VisualModel.getChildren(jsonItem);

    // The children array could be null depending on how the JSON is serialized (not present in the
    // JSON at all means null, or empty []).
    if (maybeNull != null) {
      final ArrayNode visChildren = (ArrayNode) maybeNull;
      for (final JsonNode visChild : visChildren) {
        final Optional<ConceptTreeItem> itemToAppendOpt =
            parseVisualModelRec(fieldsAndNodes, visChild, conceptNode);
        if (itemToAppendOpt.isPresent()) {
          // Append field or node.
          conceptNode.addConceptItem(itemToAppendOpt.get());
        }
      }
    }
    return conceptNode;
  }

  private static void handleGroupWithNodeId(final FieldsAndNodes fieldsAndNodes,
      final JsonNode jsonItem, final ConceptTreeNode closestParentNode) {

    // The conceptual model must ignore this group but keep the contained content.
    // In that case we want the children to be moved up to the nearest conceptual parent node.
    // This is flattening/simplifying the tree.
    // In other words the visual tree has extra items that the conceptual model does not need.

    final JsonNode maybeNull = jsonItem.get(VIS_CHILDREN);

    // Could be "null if empty" depending on how the JSON is constructed.
    // No children in JSON could be a value like [] or just no key value pair.
    // Both possibilities are tolerated.
    if (maybeNull == null) {
      return; // Cannot return anything to append to.
    }

    final ArrayNode visChildren = (ArrayNode) maybeNull;
    for (final JsonNode visChild : visChildren) {
      final Optional<ConceptTreeItem> itemToAppendOpt =
          parseVisualModelRec(fieldsAndNodes, visChild, closestParentNode);
      if (itemToAppendOpt.isPresent()) {
        final ConceptTreeItem item = itemToAppendOpt.get();
        Validate.notNull(closestParentNode, "closestParentNode is null for %s", item.getIdUnique());
        closestParentNode.addConceptItem(item);
      }
    }
  }

  private static Optional<ConceptTreeItem> handleVisualField(final FieldsAndNodes fieldsAndNodes,
      final JsonNode jsonItem, final ConceptTreeNode closestParentNode, final String contentId,
      final int counter) {

    // This is a visual field (leaf of the tree).
    // Every field points to an SDK field for the SDK metadata.
    final String sdkFieldId = contentId;
    final ConceptTreeField conceptField =
        new ConceptTreeField(contentId, sdkFieldId, jsonItem.get(VIS_VALUE).asText(null), counter);

    final JsonNode sdkFieldMeta = fieldsAndNodes.getFieldById(sdkFieldId);

    // We found a field.
    // But is the current concept hierarchy matching the hierarchy found in the SDK fields.json?
    final String sdkParentNodeId =
        JsonUtils.getTextStrict(sdkFieldMeta, FieldsAndNodes.FIELD_PARENT_NODE_ID);

    if (!closestParentNode.getNodeId().equals(sdkParentNodeId)) {
      // The parents do not match.

      final boolean isRepeatable = fieldsAndNodes.isNodeRepeatable(sdkParentNodeId);
      if (isRepeatable) {
        // The SDK says the desired parentNodeId is repeatable and is missing in the visual model,
        // thus we have a serious problem!
        final String msg = String.format(
            "Problem in visual node hierarchy, fieldId=%s is not included"
                + " in the correct parent. Expecting %s but found %s",
            sdkFieldId, sdkParentNodeId, closestParentNode.getNodeId());
        System.err.println(msg);
        // throw new RuntimeException(msg);
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
        // By convention we will add a suffix to these generated concept nodes.
        cn = new ConceptTreeNode(sdkParentNodeId + SUFFIX_GENERATED, sdkParentNodeId, 1,
            isRepeatable);

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


  /**
   * This can be used for visualization of the visual model tree (as a graph). The graphviz dot text
   * itself is interesting but it makes even more sense when seen inside a tool.
   */
  private String toDot(final FieldsAndNodes fieldsAndNodes, final boolean includeFields) {

    final StringBuilder sb = new StringBuilder(1024);
    final JsonNode root = this.visRoot;
    toDotRec(fieldsAndNodes, sb, root, includeFields);

    final StringBuilder sbDot = new StringBuilder();
    final String noticeSubType = this.getNoticeSubType();
    final String title = "visual_" + noticeSubType; // - is not supported.
    GraphvizDotTool.appendDiGraph(sb.toString(), sbDot, title, "Visual model of " + noticeSubType,
        false, true);

    return sbDot.toString();
  }


  /**
   * Recursively create DOT format text and append it to the string builder (sb).
   *
   * @param fieldsAndNodes SDK field and node metadata
   * @param includeFields If true include fields in the graph, otherwise do not
   */
  private static void toDotRec(final FieldsAndNodes fieldsAndNodes, final StringBuilder sb,
      final JsonNode item, final boolean includeFields) {
    final Optional<String> nodeIdOpt = getNodeIdOpt(item);
    final String idUnique =
        getContentId(item) + (nodeIdOpt.isPresent() ? "_" + nodeIdOpt.get() : "");

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

        final String idUniqueChild =
            getContentId(child) + (childNodeIdOpt.isPresent() ? "_" + childNodeIdOpt.get() : "");
        GraphvizDotTool.appendEdge(edgeLabel, color,

            idUnique, idUniqueChild, // concept node -> child concept node

            sb);

        toDotRec(fieldsAndNodes, sb, child, includeFields);
      }
    }
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
      final Path pathToFolder = Path.of("target/dot/");
      Files.createDirectories(pathToFolder);
      final Path pathToFile = pathToFolder.resolve(this.getNoticeSubType() + "-visual.dot");
      JavaTools.writeTextFile(pathToFile, dotText);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
