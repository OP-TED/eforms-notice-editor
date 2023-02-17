package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.Objects;
import org.apache.commons.lang3.Validate;

public class DocumentTypeNamespace {
  private final String prefix;
  private final String uri;
  private final String schemaLocation;

  public DocumentTypeNamespace(final String prefix, final String uri, final String schemaLocation) {
    Validate.notBlank("prefix is blank");
    Validate.notBlank("uri is blank");
    Validate.notBlank("schemaLocation is blank");
    this.prefix = prefix;
    this.uri = uri;
    this.schemaLocation = schemaLocation;
  }

  /**
   * @return The prefix of the xsd namespace
   */
  public String getPrefix() {
    return prefix;
  }

  /**
   * @return The uri of the xsd namespace
   */
  public String getUri() {
    return uri;
  }

  /**
   * @return The location of the schema relative to the root of the SDK
   */
  public String getSchemaLocation() {
    return schemaLocation;
  }

  @Override
  public String toString() {
    return "DocumentTypeNamespace [prefix=" + prefix + ", uri=" + uri + ", schemaLocation="
        + schemaLocation + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(prefix, schemaLocation, uri);
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
    DocumentTypeNamespace other = (DocumentTypeNamespace) obj;
    return Objects.equals(prefix, other.prefix)
        && Objects.equals(schemaLocation, other.schemaLocation) && Objects.equals(uri, other.uri);
  }
}
