package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;

public abstract class ConceptItem {
  private final String id;
  private final String idForDebug;
  private final int counter;
  private final int parentCounter;

  public ConceptItem(final String id, final String idForDebug, final int counter,
      final int parentCounter) {
    Validate.notBlank(id, "id of concept node is blank");
    Validate.isTrue(counter >= 1);
    Validate.isTrue(parentCounter >= 1);
    this.id = id;
    this.idForDebug = idForDebug;
    this.counter = counter;
    this.parentCounter = parentCounter;
  }

  public String getIdForDebug() {
    return idForDebug;
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
