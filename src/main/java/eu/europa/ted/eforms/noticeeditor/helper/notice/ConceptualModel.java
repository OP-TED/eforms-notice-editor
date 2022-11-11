package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.GraphvizDotTool;

public class ConceptualModel {

  private final ConceptNode rootNode;

  public ConceptualModel(final ConceptNode rootNode) {
    this.rootNode = rootNode;
  }

  public ConceptNode getRoot() {
    return rootNode;
  }

  public String getNoticeSubType() {
    // HARDCODED.
    final ConceptNode rootExtension = rootNode.getConceptNodes().stream()
        .filter(item -> item.getId().equals(NoticeSaver.ND_ROOT_EXTENSION)).findFirst().get();
    return rootExtension.getConceptFields().stream()
        .filter(item -> item.getId().equals(NoticeSaver.OPP_070_NOTICE)).findFirst().get()
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
    final String cnId = cn.getId();

    // Include nodes in dot file.
    for (final ConceptNode c : cn.getConceptNodes()) {
      final JsonNode nodeMeta = fieldsAndNodes.getNodeById(c.getId());
      Validate.notNull(nodeMeta, "null for nodeId=%s", c.getId());

      final boolean nodeIsRepeatable = FieldsAndNodes.isNodeRepeatable(nodeMeta);
      final String color =
          nodeIsRepeatable ? GraphvizDotTool.COLOR_GREEN : GraphvizDotTool.COLOR_BLACK;

      GraphvizDotTool.appendEdge("", color, cnId, c.getId(), sb);
      toDotRec(fieldsAndNodes, sb, c, includeFields);
    }

    // Include fields in dot file.
    if (includeFields) {
      // This makes the tree a lot more bushy and can be hard to read.
      for (final ConceptField c : cn.getConceptFields()) {
        GraphvizDotTool.appendEdge("", GraphvizDotTool.COLOR_BLACK, cnId, c.getId(), sb);
      }
    }
  }

}
