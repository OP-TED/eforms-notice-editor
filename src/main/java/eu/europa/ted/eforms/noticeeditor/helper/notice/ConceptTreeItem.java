package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * Abstract item holding common conceptual information for a notice. References SDK metadata.
 */
public abstract class ConceptTreeItem {
  /**
   * Unique identifier among children at same level. Counter excluded.
   */
  private final String idUnique;

  /**
   * This id is not unique as some concept items can be repeatead while still have the same metadata
   * (pointing to same field or same node multiple times). A counter helps to differentiate them.
   */
  protected final String idInSdkFieldsJson;

  private final int counter;

  protected ConceptTreeItem(final String idUnique, final String idInSdkFieldsJson,
      final int counter) {
    Validate.notBlank(idUnique, "idUnique of concept node is blank for %s", idInSdkFieldsJson);
    Validate.notBlank(idInSdkFieldsJson, "idInSdkFieldsJson of concept node is blank for %s",
        idUnique);
    Validate.isTrue(counter >= 1, "counter must be >= 1 but found %s for %s", counter, idUnique);
    this.idUnique = idUnique;
    this.idInSdkFieldsJson = idInSdkFieldsJson;
    this.counter = counter;
  }

  /**
   * @return Unique identifier among children at same level. Counter excluded.
   */
  public String getIdUnique() {
    return idUnique;
  }

  public int getCounter() {
    return counter;
  }

  @Override
  public String toString() {
    return "ConceptTreeItem [idUnique=" + idUnique + ", idInSdkFieldsJson=" + idInSdkFieldsJson
        + ", counter=" + counter + "]";
  }

  @Override
  public int hashCode() {
    // Important: the counter is taken into account, this matters for repeatable items.
    return Objects.hash(counter, idInSdkFieldsJson, idUnique);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ConceptTreeItem other = (ConceptTreeItem) obj;
    // Important: the counter is taken into account, this matters for repeatable items.
    return counter == other.counter && Objects.equals(idInSdkFieldsJson, other.idInSdkFieldsJson)
        && Objects.equals(idUnique, other.idUnique);
  }

}
