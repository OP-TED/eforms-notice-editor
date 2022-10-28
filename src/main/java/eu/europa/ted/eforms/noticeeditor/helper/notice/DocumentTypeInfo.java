package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;

/**
 * Abstraction around the json data of the document type information.
 */
public class DocumentTypeInfo {
  private final JsonNode jsonItem;
  private final String namespaceUri;
  private final String rootElementType;
  private final String xsdFile;

  public DocumentTypeInfo(final JsonNode jsonParam) {
    Validate.notNull(jsonParam, "jsonParam of document type is null");
    this.jsonItem = jsonParam;

    this.namespaceUri =
        JsonUtils.getTextStrict(this.jsonItem, SdkConstants.NOTICE_TYPES_JSON_NAMESPACE_KEY);

    this.rootElementType =
        JsonUtils.getTextStrict(this.jsonItem, SdkConstants.NOTICE_TYPES_JSON_ROOT_ELEMENT_KEY);

    /// We do not have this information in the SDK yet. We want to add a mapping later on.
    // this.xsd = JsonUtils.getTextStrict(this.json, "xsd");
    this.xsdFile = "EFORMS-BusinessRegistrationInformationNotice.xsd";
  }

  public String getXsdFile() {
    return xsdFile;
  }

  public String getNamespaceUri() {
    return namespaceUri;
  }

  public String getRootElementTagName() {
    return rootElementType;
  }
}
