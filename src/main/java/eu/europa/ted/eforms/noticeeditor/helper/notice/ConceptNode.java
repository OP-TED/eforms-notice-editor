package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public class ConceptNode extends ConceptItem {
  private final List<ConceptField> conceptFields = new ArrayList<>();
  private final List<ConceptNode> conceptNodes = new ArrayList<>();

  public ConceptNode(final String id, final String idForDebug, final int counter,
      final int parentCounter) {
    super(id, idForDebug, counter, parentCounter);
  }

  public final void addConceptItem(final ConceptItem item) {
    if (item instanceof ConceptNode) {
      addConceptNode((ConceptNode) item);
    } else if (item instanceof ConceptField) {
      addConceptField((ConceptField) item);
    } else {
      throw new RuntimeException("Unexpected item type");
    }
  }

  /**
   * For convenience and to make it clear that the id is the node id in this case.
   */
  public String getNodeId() {
    return getId();
  }

  public final void addConceptField(final ConceptField conceptField) {
    Validate.notNull(conceptField);
    conceptFields.add(conceptField);
  }

  public final void addConceptNode(final ConceptNode conceptNode) {
    Validate.notNull(conceptNode);
    if (conceptNode.getId().equals(this.getId())) {
      // Detect cycle, we have a tree, we do not want a graph, cannot self reference!
      throw new RuntimeException(
          String.format("Cannot have child=%s that is same as parent=%s (cycle), self reference.",
              conceptNode.getId(), this.getId()));
    }
    conceptNodes.add(conceptNode);
  }

  public List<ConceptField> getConceptFields() {
    return conceptFields;
  }

  public List<ConceptNode> getConceptNodes() {
    return conceptNodes;
  }

  @Override
  public String toString() {
    return "ConceptNode [id=" + this.getId() + ", conceptFields=" + conceptFields
        + ", conceptNodes=" + conceptNodes + "]";
  }
}
