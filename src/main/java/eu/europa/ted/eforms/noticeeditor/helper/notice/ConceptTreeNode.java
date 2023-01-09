package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;

/**
 * Conceptual node. This holds non-metadata information about a node. This is not an SDK node, this
 * only points to an SDK node to reference metadata.
 */
public class ConceptTreeNode extends ConceptTreeItem {
  private final List<ConceptTreeField> conceptFields = new ArrayList<>();

  /**
   * A node can have sub nodes.
   */
  private final List<ConceptTreeNode> conceptNodes = new ArrayList<>();

  private final boolean isRepeatable;

  /**
   * @param idUnique A unique id, at least unique at the level of the siblings
   * @param idInSdkFieldsJson The id of the item in the SDK fields.json
   * @param isRepeatable
   */
  public ConceptTreeNode(final String idUnique, final String idInSdkFieldsJson, final int counter,
      final boolean isRepeatable) {
    super(idUnique, idInSdkFieldsJson, counter);
    this.isRepeatable = isRepeatable;
  }

  /**
   * @param item The item to add.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "ITC_INHERITANCE_TYPE_CHECKING",
      justification = "Spotbugs is confused, the check is done on the passed item, not the class.")
  public final void addConceptItem(final ConceptTreeItem item) {
    if (item instanceof ConceptTreeNode) {
      addConceptNode((ConceptTreeNode) item, true);
    } else if (item instanceof ConceptTreeField) {
      addConceptField((ConceptTreeField) item);
    } else {
      throw new RuntimeException(
          String.format("Unexpected item type for concept item=%s", item.getIdUnique()));
    }
  }

  public Optional<ConceptTreeNode> findFirstByConceptNodeId(final String nodeId) {
    return findFirstByConceptNodeIdRec(this, nodeId);
  }

  private static final Optional<ConceptTreeNode> findFirstByConceptNodeIdRec(
      final ConceptTreeNode cn, final String nodeId) {
    if (cn.getNodeId().equals(nodeId)) {
      return Optional.of(cn);
    }
    for (final ConceptTreeNode cnChild : cn.conceptNodes) {
      final Optional<ConceptTreeNode> cnOpt = findFirstByConceptNodeIdRec(cnChild, nodeId);
      if (cnOpt.isPresent()) {
        return cnOpt;
      }
    }
    return Optional.empty();
  }

  /**
   * For convenience and to make it clear that the ID in the SDK is the node ID in this case.
   */
  public String getNodeId() {
    return idInSdkFieldsJson;
  }

  public final void addConceptField(final ConceptTreeField conceptField) {
    Validate.notNull(conceptField);
    conceptFields.add(conceptField);
  }

  public final void addConceptNode(final ConceptTreeNode conceptNode, final boolean strict) {
    Validate.notNull(conceptNode);
    final String otherNodeId = conceptNode.getNodeId();
    final String thisNodeId = getNodeId();
    if (otherNodeId.equals(thisNodeId)) {
      // Detect cycle, we have a tree, we do not want a graph, cannot self reference!
      throw new RuntimeException(
          String.format("Cannot have child=%s that is same as parent=%s (cycle), self reference.",
              otherNodeId, thisNodeId));
    }
    if (conceptNode.isRepeatable()) {
      // It is repeatable, meaning it can exist multiple times, just add it.
      conceptNodes.add(conceptNode);
      return;
    }

    // It is not repeatable. Is it already contained?
    final boolean contained = conceptNodes.contains(conceptNode);

    // It should not already be contained.
    if (strict) {
      if (contained) {
        throw new RuntimeException(String.format(
            "Conceptual model: node is not repeatable but it is added twice, id=%s (nodeId=%s), parentId=%s",
            conceptNode.getIdUnique(), conceptNode.getNodeId(), this.getIdUnique()));
      }
      conceptNodes.add(conceptNode);
    } else if (!contained) {
      // Non-strict.
      // Add if not contained. Do not complain if already contained.
      conceptNodes.add(conceptNode);
    }
  }

  public List<ConceptTreeField> getConceptFields() {
    return conceptFields;
  }

  public List<ConceptTreeNode> getConceptNodes() {
    return conceptNodes;
  }

  public boolean isRepeatable() {
    return isRepeatable;
  }

  @Override
  public String toString() {
    return "ConceptTreeNode [conceptFields=" + conceptFields + ", conceptNodes=" + conceptNodes
        + ", isRepeatable=" + isRepeatable + "]";
  }

}
