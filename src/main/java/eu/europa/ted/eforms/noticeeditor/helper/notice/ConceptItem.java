package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;

public abstract class ConceptItem {
  private final String id;
  private final int counter;
  private final int parentCounter;

  public ConceptItem(String id, int counter, int parentCounter) {
    Validate.notBlank(id, "id of concept node is blank");
    Validate.isTrue(counter >= 1);
    Validate.isTrue(parentCounter >= 1);
    this.id = id;
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
}
