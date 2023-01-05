package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.GraphvizDotTool;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * The conceptual model (CM) is an intermediary model that is between the visual and the physical
 * model. It holds a tree made of conceptual node and conceptual field instances.
 * <p>
 * In this model the tree items must reference SDK nodes or fields so that SDK metadata can be
 * retrieved!
 * </p>
 * <p>
 * There are no tree items which do not point to SDK field or node, so if a visual group is not
 * pointing to a node all the children must be moved to the closes parent which points to a node in
 * the conceptual hierarchy. In other words this is one step closer to the physical representation.
 * </p>
 */
public class ConceptualModel {

  static final String ND_ROOT = "ND-Root";
  static final String ND_ROOT_EXTENSION = "ND-RootExtension";

  /**
   * Notice field id having the eformsSdkVersion as a value.
   */
  public static final String FIELD_ID_SDK_VERSION = "OPT-002-notice";

  /**
   * Notice field id having the notice sub type as a value.
   */
  public static final String FIELD_ID_NOTICE_SUB_TYPE = "OPP-070-notice";

  /**
   * Sector of activity.
   */
  public static final String OPP_105_BUSINESS = "OPP-105-Business";

  /**
   * The root node of the conceptual model.
   */
  private final ConceptTreeNode treeRootNode;

  private final SdkVersion sdkVersion;

  public ConceptualModel(final ConceptTreeNode rootNode, final SdkVersion sdkVersion) {
    Validate.isTrue(ND_ROOT.equals(rootNode.getNodeId()));
    this.treeRootNode = rootNode;
    this.sdkVersion = sdkVersion;
  }

  public SdkVersion getSdkVersion() {
    return sdkVersion;
  }

  public ConceptTreeNode getTreeRootNode() {
    return treeRootNode;
  }

  public String getNoticeSubType() {
    // HARDCODED LOGIC.
    final ConceptTreeNode rootExtension = treeRootNode.getConceptNodes().stream()
        .filter(item -> item.getNodeId() == ND_ROOT_EXTENSION).findFirst().get();
    return rootExtension.getConceptFields().stream()
        .filter(item -> item.getFieldId() == FIELD_ID_NOTICE_SUB_TYPE).findFirst().get().getValue();
  }

  @Override
  public String toString() {
    return "ConceptualModel [rootNode=" + treeRootNode + "]";
  }

  /**
   * This can be used for visualization of the conceptual model tree (as a graph). The graphviz dot
   * text itself is interesting but it makes even more sense when seen inside a tool.
   */
  public String toDot(final FieldsAndNodes fieldsAndNodes, final boolean includeFields) {

    final StringBuilder sb = new StringBuilder();
    final ConceptTreeNode root = this.treeRootNode;
    toDotRec(fieldsAndNodes, sb, root, includeFields);

    final StringBuilder sbDot = new StringBuilder();
    final String noticeSubType = this.getNoticeSubType();
    final String title = noticeSubType;
    GraphvizDotTool.appendDiGraph(sb.toString(), sbDot, title,
        "Conceptual model of " + noticeSubType, false, true);

    return sbDot.toString();
  }

  /**
   * Recursively create DOT format text and append it to the string builder (sb).
   *
   * @param fieldsAndNodes SDK field and node metadata
   * @param includeFields If true include fields in the graph, otherwise do not
   */
  private static void toDotRec(final FieldsAndNodes fieldsAndNodes, final StringBuilder sb,
      final ConceptTreeNode cn, final boolean includeFields) {
    final String cnIdUnique = cn.getIdUnique();
    final String edgeLabel = cn.getNodeId();

    // Include nodes in dot file.
    for (final ConceptTreeNode childNode : cn.getConceptNodes()) {
      final JsonNode nodeMeta = fieldsAndNodes.getNodeById(childNode.getNodeId());
      Validate.notNull(nodeMeta, "null for nodeId=%s", childNode.getNodeId());

      final boolean nodeIsRepeatable = FieldsAndNodes.isNodeRepeatableStatic(nodeMeta);
      final String color =
          nodeIsRepeatable ? GraphvizDotTool.COLOR_GREEN : GraphvizDotTool.COLOR_BLACK;

      GraphvizDotTool.appendEdge(edgeLabel, color,

          cnIdUnique, childNode.getIdUnique(), // concept node -> concept node

          sb);

      toDotRec(fieldsAndNodes, sb, childNode, includeFields);
    }

    // Include fields in dot file?
    if (includeFields) {
      // This makes the tree a lot more bushy and can be hard to read.
      for (final ConceptTreeField cf : cn.getConceptFields()) {
        GraphvizDotTool.appendEdge(edgeLabel, GraphvizDotTool.COLOR_BLUE,

            cnIdUnique, cf.getIdUnique() + "=" + cf.getValue(), // node -> field

            sb);
      }
    }
  }

}
