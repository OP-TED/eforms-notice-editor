package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.GraphvizDotTool;

/**
 * The conceptual model (CM) is an intermediary model that is between the visual and the physical
 * model. It holds a tree made of node and field instances. The tree items must reference SDK nodes
 * or fields so that SDK metadata can be retrieved.
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
   * The conceptual tree root node.
   */
  private final ConceptTreeNode treeRootNode;

  public ConceptualModel(final ConceptTreeNode rootNode) {
    Validate.isTrue(ND_ROOT.equals(rootNode.getNodeId()));
    this.treeRootNode = rootNode;
  }

  public ConceptTreeNode getTreeRootNode() {
    return treeRootNode;
  }

  public String getNoticeSubType() {
    // HARDCODED LOGIC.
    System.out.println(treeRootNode);
    final ConceptTreeNode rootExtension = treeRootNode.getConceptNodes().stream()
        .filter(item -> item.getNodeId().equals(ND_ROOT_EXTENSION)).findFirst().get();
    return rootExtension.getConceptFields().stream()
        .filter(item -> item.getFieldId().equals(FIELD_ID_NOTICE_SUB_TYPE)).findFirst().get()
        .getValue();
  }

  @Override
  public String toString() {
    return "ConceptualModel [rootNode=" + treeRootNode + "]";
  }

  /**
   * This can be used for visualization of the conceptual model tree.
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

  public static void toDotRec(final FieldsAndNodes fieldsAndNodes, final StringBuilder sb,
      final ConceptTreeNode cn, final boolean includeFields) {
    final String cnIdUnique = cn.getIdUnique();
    final String edgeLabel = cn.getNodeId();

    // Include nodes in dot file.
    for (final ConceptTreeNode childNode : cn.getConceptNodes()) {
      final JsonNode nodeMeta = fieldsAndNodes.getNodeById(childNode.getNodeId());
      Validate.notNull(nodeMeta, "null for nodeId=%s", childNode.getNodeId());

      final boolean nodeIsRepeatable = FieldsAndNodes.isNodeRepeatable(nodeMeta);
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
