package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Conceptual node. This holds non-metadata information about a node. This is not an SDK node, this
 * only points to an SDK node to reference metadata. A node can have child items!
 */
public class ConceptTreeNode extends ConceptTreeItem {
  private static final Logger logger = LoggerFactory.getLogger(ConceptTreeNode.class);

  private final List<ConceptTreeField> conceptFields = new ArrayList<>();

  /**
   * A node can have sub nodes.
   */
  private final List<ConceptTreeNode> conceptNodes = new ArrayList<>();

  private final boolean repeatable;

  /**
   * @param idUnique A unique id, at least unique at the level of the siblings
   * @param idInSdkFieldsJson The id of the item in the SDK fields.json
   * @param repeatable True if this kind of node instance can be repeated, false otherwise
   */
  public ConceptTreeNode(final String idUnique, final String idInSdkFieldsJson, final int counter,
      final boolean repeatable) {
    super(idUnique, idInSdkFieldsJson, counter);
    this.repeatable = repeatable;
  }

  /**
   * @param item The item to add.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "ITC_INHERITANCE_TYPE_CHECKING",
      justification = "Spotbugs is confused, the check is done on the passed item, not the class.")
  public final boolean addConceptItem(final ConceptTreeItem item) {
    if (item instanceof ConceptTreeNode) {
      final boolean strict = true;
      return addConceptNode((ConceptTreeNode) item, strict);
    }
    if (item instanceof ConceptTreeField) {
      return addConceptField((ConceptTreeField) item);
    }
    throw new RuntimeException(
        String.format("Unexpected item type for concept item=%s", item.getIdUnique()));
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

  public final boolean addConceptField(final ConceptTreeField conceptField) {
    Validate.notNull(conceptField);
    conceptFields.add(conceptField);
    logger.debug("Added concept uniqueId={} to uniqueId={}", conceptField.getFieldId(),
        this.getIdUnique(),
        conceptField.getIdUnique());
    return true;
  }

  /**
   * @param cn The concept node to add
   * @param strictAdd When true, if the item is already contained it will fail, set to true
   */
  public final boolean addConceptNode(final ConceptTreeNode cn, final boolean strictAdd) {
    Validate.notNull(cn);
    final String nodeIdToAdd = cn.getNodeId();
    final String nodeIdSelf = getNodeId();
    if (nodeIdToAdd.equals(nodeIdSelf)) {
      // Detect cycle, we have a tree, we do not want a graph, cannot self reference!
      throw new RuntimeException(
          String.format("Cannot have child=%s that is same as parent=%s (cycle), self reference.",
              nodeIdToAdd, nodeIdSelf));
    }
    if (cn.isRepeatable()) {
      // It is repeatable, meaning it can exist multiple times, just add it.
      conceptNodes.add(cn);
      return true;
    }

    // It is not repeatable.
    // Is it already contained?
    final boolean cnAlreadyContained = conceptNodes.contains(cn);

    // It should not already be contained.
    if (strictAdd) {
      // Strict add.
      if (cnAlreadyContained) {
        throw new RuntimeException(String.format(
            "Conceptual model: node is not repeatable "
                + "but it would be added twice, id=%s (nodeId=%s), parentId=%s",
            cn.getIdUnique(), cn.getNodeId(), this.getIdUnique()));
      }
      conceptNodes.add(cn);
      return true;
    }

    // Non-strict add.
    // We can add even if it is already contained.
    // if (!cnAlreadyContained) {
    // Add if not contained. Do not complain if already contained.

    // We DO NOT want to add the entire thing blindly.

    // Example: add X

    // conceptNodes -> empty, X is not there just add X (no problem)

    // conceptNodes -> (X -> Y -> 4141)

    // Example: add X but X is already there
    // conceptNodes -> (X -> Y -> Z -> 4242)

    // We want to fuse / fusion of branches:
    // conceptNodes -> X -> Y -> 4141
    // .......................-> Z -> 4242

    // So X and Y are reused (fused), and Z is attached to Y

    if (cnAlreadyContained) {
      final int indexOfCn = conceptNodes.indexOf(cn);
      if (indexOfCn >= 0) {
        // Iterative fusion logic:
        // X could be already contained, but some child item like Y may not be there yet.
        final ConceptTreeNode existingCn = conceptNodes.get(indexOfCn);
        // We know that the branches uni-dimensional (flat), so a simple for loop with return should
        // work. At least for the known case this works.
        for (ConceptTreeNode cn2 : cn.getConceptNodes()) {
          if (existingCn.addConceptNode(cn2, strictAdd)) {
            return true;
          }
        }
      }
    } else {
      // Not contained yet, add it.
      conceptNodes.add(cn);
    }
    return true;
    // }
    // return false;
  }

  public List<ConceptTreeField> getConceptFields() {
    return conceptFields;
  }

  public List<ConceptTreeNode> getConceptNodes() {
    return conceptNodes;
  }

  public boolean isRepeatable() {
    return repeatable;
  }

  @Override
  public String toString() {
    return "ConceptTreeNode id=" + this.idInSdkFieldsJson + " [conceptFields=" + conceptFields
        + ", conceptNodes=" + conceptNodes
        + ", repeatable=" + repeatable + "]";
  }

}
