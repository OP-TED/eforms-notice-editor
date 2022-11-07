package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.GraphvizDotTool;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

public class ConceptualModel {

  private static final String ND_ROOT_EXTENSION = "ND-RootExtension";
  private static final String OPP_070_NOTICE = "OPP-070-notice";

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
        .filter(item -> item.getId().equals(ND_ROOT_EXTENSION)).findFirst().get();
    return rootExtension.getConceptFields().stream()
        .filter(item -> item.getId().equals(OPP_070_NOTICE)).findFirst().get().getValue();
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
    for (final ConceptNode c : cn.getConceptNodes()) {
      final JsonNode jsonNode = fieldsAndNodes.getNodeById(c.getId());
      Validate.notNull(jsonNode, "null for nodeId=%s", c.getId());
      final boolean nodeIsRepeatable =
          JsonUtils.getBoolStrict(jsonNode, NoticeSaver.FIELD_REPEATABLE);
      final String color = nodeIsRepeatable ? "green" : "black";
      GraphvizDotTool.appendEdge("", color, cnId, c.getId(), sb);
      toDotRec(fieldsAndNodes, sb, c, includeFields);
    }
    if (includeFields) {
      for (final ConceptField c : cn.getConceptFields()) {
        GraphvizDotTool.appendEdge("", "black", cnId, c.getId(), sb);
      }
    }
  }

}
