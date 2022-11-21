package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;

/**
 * Abstract item holding common information. References SDK metadata.
 */
public abstract class ConceptTreeItem {
  /**
   * Unique identifier among children at same level.
   */
  private final String idUnique;

  /**
   * This id is not unique as some concept items can be repeatead while still have the same metadata
   * (pointing to same field or same node multiple times).
   */
  protected final String idInSdkFieldsJson;

  private final int counter;

  protected ConceptTreeItem(final String idUnique, final String idInSdkFieldsJson,
      final int counter) {
    Validate.notBlank(idUnique, "idUnique of concept node is blank");
    Validate.notBlank(idInSdkFieldsJson, "idInSdkFieldsJson of concept node is blank");
    Validate.isTrue(counter >= 1);
    this.idUnique = idUnique;
    this.idInSdkFieldsJson = idInSdkFieldsJson;
    this.counter = counter;
  }

  /**
   * @return Unique identifier among children at same level.
   */
  public String getIdUnique() {
    return idUnique;
  }

  public int getCounter() {
    return counter;
  }
}
