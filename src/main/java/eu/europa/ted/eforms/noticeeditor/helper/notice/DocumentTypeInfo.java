package eu.europa.ted.eforms.noticeeditor.helper.notice;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

/**
 * Abstraction around the json data of the document type information.
 */
public class DocumentTypeInfo {
  private final JsonNode json;
  private final String namespaceUri;
  private final String rootElementType;
  private final String xsd;

  public DocumentTypeInfo(final JsonNode jsonParam) {
    this.json = jsonParam;
    this.namespaceUri = JsonUtils.getTextStrict(this.json, "namespace");
    this.rootElementType = JsonUtils.getTextStrict(this.json, "rootElement");
    // this.xsd = JsonUtils.getTextStrict(this.json, "xsd");
    this.xsd = "todo"; // TODO
  }

  public String getXsdPath() {
    return xsd;
  }

  public String getNamespaceUri() {
    return namespaceUri;
  }

  public String getRootElementTagName() {
    return rootElementType;
  }
}
