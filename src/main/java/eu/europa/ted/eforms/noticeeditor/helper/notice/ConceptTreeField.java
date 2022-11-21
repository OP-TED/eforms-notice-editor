package eu.europa.ted.eforms.noticeeditor.helper.notice;

/**
 * Conceptual field. Leaf in the conceptual tree. This holds non-metadata field information and the
 * field id. This is not an SDK field, this only points to an SDK field to reference metadata.
 */
public class ConceptTreeField extends ConceptTreeItem {
  private final String value;

  public ConceptTreeField(final String idUnique, final String idInSdkFieldsJson, final String value,
      final int counter) {
    super(idUnique, idInSdkFieldsJson, counter);
    this.value = value; // The value may or may not be blank, we cannot validate it here.
  }

  /**
   * For convenience and to make it clear that the ID in the SDK is the field ID in this case.
   */
  public String getFieldId() {
    return idInSdkFieldsJson;
  }

  public String getValue() {
    return value;
  }
}
