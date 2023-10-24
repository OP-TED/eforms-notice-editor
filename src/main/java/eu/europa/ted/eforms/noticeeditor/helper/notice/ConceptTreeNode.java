package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Conceptual node. This holds non-metadata information about a node. This is not an SDK node, this
 * only points to an SDK node to reference metadata. A node can have child items!
 */
@JsonPropertyOrder({"idUnique", "counter", "nodeId", "repeatable", "conceptFields", "conceptNodes"})
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
   * @param sb A string builder passed for debugging purposes
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "ITC_INHERITANCE_TYPE_CHECKING",
      justification = "Spotbugs is confused, the check is done on the passed item, not the class.")
  public final Optional<ConceptTreeNode> addConceptItem(final ConceptTreeItem item,
      final StringBuilder sb) {
    if (item instanceof ConceptTreeNode) {
      final boolean strict = true;
      return addConceptNode((ConceptTreeNode) item, strict, sb, "addConceptItem");
    }
    if (item instanceof ConceptTreeField) {
      addConceptField((ConceptTreeField) item, sb);
      return Optional.of(this); // Always added on self.
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

  public final void addConceptField(final ConceptTreeField conceptField,
      final StringBuilder sb) {
    Validate.notNull(conceptField);
    conceptFields.add(conceptField);
    logger.debug("Added concept field uniqueId={} to fieldId={}", conceptField.getFieldId(),
        this.getIdUnique(),
        conceptField.getIdUnique());
    sb.append("Added concept field: ")
        .append(conceptField.getIdUnique())
        .append(" to ").append(this.getIdUnique())
        .append('\n');
  }

  /**
   * @param cn The concept node to add
   * @param strictAdd When true, if the item is already contained it will fail
   * @param sb A string builder passed for debugging purposes
   *
   * @return The conceptual node to which the passed element was added, this must be used on the
   *         outside of the call
   */
  public final Optional<ConceptTreeNode> addConceptNode(final ConceptTreeNode cn,
      final boolean strictAdd,
      final StringBuilder sb, final String originalOfCall) {
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
      addConceptNodePrivate(cn, "repeatable");
      sb.append("Added concept node (repeatable): ").append(cn.getIdUnique()).append(" to ")
          .append(this.getIdUnique()).append('\n');
      logger.debug("Added concept node uniqueId={} to nodeId={}", cn.getIdUnique(),
          this.getNodeId());
      return Optional.of(cn);
    }

    // It is not repeatable.
    // Is it already contained?
    final boolean cnAlreadyContained = containsNodeOrNodeId(cn);

    if (strictAdd) {
      // Strict add.
      if (cnAlreadyContained) {
        if (containsNodeEquals(cn)) {
          // It should not already be contained as an exact copy, this is a problem!
          throw new RuntimeException(String.format(
              "Conceptual model: node is not repeatable, it would be added twice, "
                  + "id=%s (nodeId=%s), parentId=%s, originOfCall=%s",
              cn.getIdUnique(), cn.getNodeId(), this.getIdUnique(), originalOfCall));
        } else {
          // It is already contained but is not the exact same object, there is another concept node
          // with the same node id, so it that cannot be repeated.
          // In this case we have to fuse the concept nodes.
          // Fuse with existing node.
          final Optional<ConceptTreeNode> existingCnOpt = findFirstByConceptNodeId(nodeIdSelf);
          if (existingCnOpt.isPresent()) {
            final ConceptTreeNode cnExisting = existingCnOpt.get();
            // Fuse fields.
            for (final ConceptTreeField cnField : cn.getConceptFields()) {
              cnExisting.addConceptField(cnField, sb);
            }
            // Fuse nodes.
            for (final ConceptTreeNode cnNode : cn.getConceptNodes()) {
              cnExisting.addConceptNode(cnNode, strictAdd, sb, "fusion2");
            }
            return Optional.of(cnExisting);
          }
          throw new RuntimeException(String.format(
              "Conceptual model: node not found by nodeId=%s, parentId=%s, originOfCall=%s",
              cn.getNodeId(), this.getIdUnique(), originalOfCall));
        }
      }
      // Not repeatable and not already contained, add it.
      addConceptNodePrivate(cn, "not repeatable (strictAdd)");
      sb.append("Added concept node (not repeatable): ").append(cn.getIdUnique()).append(" to ")
          .append(this.getIdUnique()).append('\n');
      logger.debug("Added concept node uniqueId={} to nodeId={}", cn.getIdUnique(),
          this.getNodeId());
      return Optional.of(cn);
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
        // We know that the branches are uni-dimensional (flat), so a simple for loop with return
        // should work. At least for the known cases this works.
        for (final ConceptTreeNode cn2 : cn.getConceptNodes()) {
          final Optional<ConceptTreeNode> cnToWhichItWasAdded =
              existingCn.addConceptNode(cn2, strictAdd, sb, "fusion1");
          if (cnToWhichItWasAdded.isPresent()) {
            logger.debug("Fusion for uniqueId={}", cn2.getIdUnique());
            sb.append("Fusion for uniqueId=").append(cn2.getIdUnique()).append('\n');
            return cnToWhichItWasAdded;
          }
        }
        return Optional.empty();
      }
    } else {
      // Not contained yet, add it.
      addConceptNodePrivate(cn, "not repeatable (bis)");
      sb.append("Added concept node (not repeatable)(bis): ").append(cn.getIdUnique())
          .append(" to ")
          .append(this.getIdUnique()).append('\n');
      logger.debug("Added concept node uniqueId={} to nodeId={}", cn.getIdUnique(),
          this.getNodeId());
      return Optional.of(cn); // It was add on this item.
    }
    return Optional.empty();
  }

  /**
   * @return true if it contains the exact same concept node (at the level of the children, not
   *         recursive)
   */
  private boolean containsNodeEquals(final ConceptTreeNode cn) {
    return conceptNodes.contains(cn);
  }

  /**
   * @return true if it contains the exact same concept node or a concept node with the same node id
   *         (at the level of the children, not recursive)
   */
  private boolean containsNodeOrNodeId(final ConceptTreeNode cn) {
    return conceptNodes.contains(cn) || conceptNodes.stream()
        .filter(item -> item.getNodeId().equals(cn.getNodeId())).count() > 0;
  }

  /**
   * Every add on this collection must go through this to ensure coherence. NOT RECURSIVE!
   *
   * @param cn The concept node to add
   * @param originOfAdd The origin of the add call, put in the error messages or logs For internal
   *        usage only, used to centralise add check adds.
   */
  private boolean addConceptNodePrivate(final ConceptTreeNode cn, final String originOfAdd) {
    if (!cn.isRepeatable()) {
      // This catches unexpected behaviour, probably an algorithms or bad data is to blame.
      // The part of the code which lead here is added to the exception.
      if (containsNodeOrNodeId(cn)) {
        // Throw an exception as otherwise the conceptual model would be broken from here on.
        throw new RuntimeException(
            String.format(
                "Attempting to add already contained non-repeatable node: %s, originOfAdd=%s",
                cn.getNodeId(), originOfAdd));
      }
    }
    return conceptNodes.add(cn); // The only place we use .add on this list in this class!
  }

  public List<ConceptTreeField> getConceptFields() {
    return Collections.unmodifiableList(conceptFields); // Unmodifiable to avoid side-effects!
  }

  public List<ConceptTreeNode> getConceptNodes() {
    return Collections.unmodifiableList(conceptNodes); // Unmodifiable to avoid side-effects!
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
