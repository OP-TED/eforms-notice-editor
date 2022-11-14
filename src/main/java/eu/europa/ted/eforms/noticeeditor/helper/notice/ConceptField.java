package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;

/**
 * Conceptual field.
 */
public class ConceptField extends ConceptItem {
  private final String value;

  public ConceptField(final String id, final String value, final int counter,
      final int parentCounter) {
    super(id, counter, parentCounter);
    Validate.notBlank(id, "id of concept field is blank");
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
