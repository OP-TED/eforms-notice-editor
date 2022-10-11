package eu.europa.ted.eforms.noticeeditor.helper.notice;

public class ConceptField {
  private final String id;
  private final String value;

  public ConceptField(final String id, final String value) {
    this.id = id;
    this.value = value;
  }

  public String getId() {
    return id;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "ConceptField [id=" + id + ", value=" + value + "]";
  }
}
