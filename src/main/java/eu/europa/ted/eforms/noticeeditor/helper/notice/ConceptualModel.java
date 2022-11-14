package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.GraphvizDotTool;

public class ConceptualModel {

  private final ConceptNode rootNode;

  public ConceptualModel(final ConceptNode rootNode) {
    Validate.isTrue(NoticeSaver.ND_ROOT.equals(rootNode.getId()));
    this.rootNode = rootNode;
  }

  public ConceptNode getRoot() {
    return rootNode;
  }

  public String getNoticeSubType() {
    // HARDCODED LOGIC.
    final ConceptNode rootExtension = rootNode.getConceptNodes().stream()
        .filter(item -> item.getId().equals(NoticeSaver.ND_ROOT_EXTENSION)).findFirst().get();
    return rootExtension.getConceptFields().stream()
        .filter(item -> item.getId().equals(NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE)).findFirst().get()
        .getValue();
  }

  @Override
  public String toString() {
    return "ConceptualModel [rootNode=" + rootNode + "]";
  }

  public String toDot(final FieldsAndNodes fieldsAndNodes, final boolean includeFields) {

    final StringBuilder sb = new StringBuilder();
    final ConceptNode root = this.getRoot();
    toDotRec(fieldsAndNodes, sb, root, includeFields);

    final StringBuilder sbDot = new StringBuilder();
    final String noticeSubType = this.getNoticeSubType();
    final String title = noticeSubType;
    GraphvizDotTool.appendDiGraph(sb.toString(), sbDot, title,
        "Conceptual model of " + noticeSubType, false, true);

    return sbDot.toString();
  }

  public static void toDotRec(final FieldsAndNodes fieldsAndNodes, final StringBuilder sb,
      final ConceptNode cn, final boolean includeFields) {
    final String cnIdIsNodeId = cn.getNodeId();
    final String cnIdForDebug = cn.getIdForDebug();

    // Include nodes in dot file.
    for (final ConceptNode childNode : cn.getConceptNodes()) {
      final JsonNode nodeMeta = fieldsAndNodes.getNodeById(childNode.getId());
      Validate.notNull(nodeMeta, "null for nodeId=%s", childNode.getId());

      final boolean nodeIsRepeatable = FieldsAndNodes.isNodeRepeatable(nodeMeta);
      final String color =
          nodeIsRepeatable ? GraphvizDotTool.COLOR_GREEN : GraphvizDotTool.COLOR_BLACK;

      GraphvizDotTool.appendEdge(cnIdIsNodeId, color,

          cnIdForDebug, childNode.getIdForDebug(), // node -> node

          sb);
      toDotRec(fieldsAndNodes, sb, childNode, includeFields);
    }

    // Include fields in dot file?
    if (includeFields) {
      // This makes the tree a lot more bushy and can be hard to read.
      for (final ConceptField cf : cn.getConceptFields()) {
        GraphvizDotTool.appendEdge(cnIdIsNodeId, GraphvizDotTool.COLOR_BLUE,

            cnIdForDebug, cf.getIdForDebug() + "=" + cf.getValue(), // node -> field

            sb);
      }
    }
  }

}
