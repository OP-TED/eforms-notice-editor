package eu.europa.ted.eforms.noticeeditor.helper.notice;

/**
 * Conceptual field.
 */
public class ConceptField {
  private final String id;
  private final String value;
  private final int counter;
  private final int parentCounter;

  public ConceptField(final String id, final String value, final int counter,
      final int parentCounter) {
    this.id = id;
    this.value = value;
    this.counter = counter;
    this.parentCounter = parentCounter;
  }

  public int getParentCounter() {
    return parentCounter;
  }

  public String getId() {
    return id;
  }

  public int getCounter() {
    return counter;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return id;
  }

  // @Override
  // public String toString() {
  // return "ConceptField [id=" + id + ", value=" + value + ", counter=" + counter
  // + ", parentCounter=" + parentCounter + "]";
  // }
}
