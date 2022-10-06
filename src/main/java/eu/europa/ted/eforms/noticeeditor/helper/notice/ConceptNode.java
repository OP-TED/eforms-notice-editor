package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public class ConceptNode {
  private final String id;
  private List<ConceptField> conceptFields = new ArrayList<>();
  private List<ConceptNode> conceptNodes = new ArrayList<>();

  public ConceptNode(final String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public final void addConceptField(final ConceptField conceptField) {
    Validate.notNull(conceptField);
    conceptFields.add(conceptField);
  }

  public final void addConceptNode(final ConceptNode conceptNode) {
    Validate.notNull(conceptNode);
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
    return "ConceptNode [id=" + id + ", conceptFields=" + conceptFields + "]";
  }
}
