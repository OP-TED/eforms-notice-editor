package eu.europa.ted.eforms.noticeeditor.helper.notice;

/**
 * Node identifier field id information.
 */
public class NodeIdentifierFieldId {
  private final String id; // Example: ORG-0001
  private final String scheme; // Example: ORG
  private final int counter; // Example: 1

  public NodeIdentifierFieldId(final String id, final String scheme, final int counter) {
    this.id = id;
    this.counter = counter;
    this.scheme = scheme;
  }

  public String getIdWithCount() {
    return id;
  }

  public int getCounter() {
    return counter;
  }

  public String getScheme() {
    return scheme;
  }
}
