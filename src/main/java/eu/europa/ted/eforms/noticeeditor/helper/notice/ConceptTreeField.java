package eu.europa.ted.eforms.noticeeditor.helper.notice;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Conceptual field. Leaf in the conceptual tree. This holds non-metadata field information like the
 * value and the associated SDK field id. This is not an SDK field, this only points to an SDK field
 * to reference the metadata. A field item cannot have child items!
 */
@JsonPropertyOrder({"idUnique", "counter", "fieldId", "value"})
public class ConceptTreeField extends ConceptTreeItem {
  private final String value;

  public ConceptTreeField(final String idUnique, final String idInSdkFieldsJson, final String value,
      final int counter) {
    super(idUnique, idInSdkFieldsJson, counter);
    this.value = value; // The value may or may not be blank, we cannot validate it here.
  }

  /**
   * For convenience and to make it clear that the ID in the SDK is the field ID in this case. It
   * can be used to get general information about the field (data from fields.json). This does not
   * include the counter.
   */
  public String getFieldId() {
    return idInSdkFieldsJson;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return super.toString() + " [value=" + value + "]";
  }

}
