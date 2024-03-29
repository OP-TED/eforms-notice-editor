package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Abstraction around the JSON data of the SDK document type information.
 *
 * <p>
 * This is specific to a given SDK version, which is contained.
 * </p>
 */
public class DocumentTypeInfo {
  private final String namespaceUri;
  private final String rootElementType;
  private final Optional<String> xsdPathInSdkOpt;
  private final List<DocumentTypeNamespace> additionalNamespaces;
  private final SdkVersion sdkVersion;

  public DocumentTypeInfo(final JsonNode jsonParam, final SdkVersion sdkVersion) {
    Validate.notNull(jsonParam, "jsonParam of document type is null");

    this.sdkVersion = sdkVersion;
    final JsonNode jsonItem = jsonParam;

    this.namespaceUri =
        JsonUtils.getTextStrict(jsonItem, SdkConstants.NOTICE_TYPES_JSON_NAMESPACE_KEY);

    this.rootElementType =
        JsonUtils.getTextStrict(jsonItem, SdkConstants.NOTICE_TYPES_JSON_ROOT_ELEMENT_KEY);

    this.xsdPathInSdkOpt = Optional.of(JsonUtils.getTextStrict(jsonItem, "schemaLocation"));
    this.additionalNamespaces = new ArrayList<>(parseAdditionalNamespaces(jsonItem));
  }

  static List<DocumentTypeNamespace> parseAdditionalNamespaces(final JsonNode json) {
    final List<DocumentTypeNamespace> namespaces;
    final JsonNode temp = json.get("additionalNamespaces");
    // It should not be null but tolerate it for now, null could mean the array is empty.
    // This would allow it to be null in the future without breaking this application.
    if (temp != null) {
      final ArrayNode additNamespaces = (ArrayNode) temp;
      namespaces = new ArrayList<>(additNamespaces.size());
      for (final JsonNode namespace : additNamespaces) {
        final String prefix = JsonUtils.getTextStrict(namespace, "prefix");
        final String uri = JsonUtils.getTextStrict(namespace, "uri");
        final String schemaLocation = JsonUtils.getTextStrict(namespace, "schemaLocation");
        namespaces.add(new DocumentTypeNamespace(prefix, uri, schemaLocation));
      }
    } else {
      namespaces = Collections.emptyList();
    }
    return namespaces;
  }

  public Optional<String> getSdkXsdPathOpt() {
    return xsdPathInSdkOpt;
  }

  public String getNamespaceUri() {
    return namespaceUri;
  }

  public String getRootElementTagName() {
    return rootElementType;
  }

  public List<DocumentTypeNamespace> getAdditionalNamespaces() {
    return additionalNamespaces;
  }

  public SdkVersion getSdkVersion() {
    return sdkVersion;
  }

  /**
   * Provided for convenience. Relies on the additional namespaces.
   */
  public Map<String, DocumentTypeNamespace> buildAdditionalNamespacesByPrefix() {
    final Map<String, DocumentTypeNamespace> dtnByPrefix =
        new LinkedHashMap<>(additionalNamespaces.size());
    for (final DocumentTypeNamespace dtn : additionalNamespaces) {
      dtnByPrefix.put(dtn.getPrefix(), dtn);
    }
    return dtnByPrefix;
  }

  /**
   * Provided for convenience. Relies on the additional namespaces.
   */
  public Map<String, String> buildAdditionalNamespaceUriByPrefix() {
    final Map<String, String> dtnByPrefix = new LinkedHashMap<>(additionalNamespaces.size());
    for (final DocumentTypeNamespace dtn : additionalNamespaces) {
      dtnByPrefix.put(dtn.getPrefix(), dtn.getUri());
    }
    return dtnByPrefix;
  }

  @Override
  public String toString() {
    return "DocumentTypeInfo [namespaceUri=" + namespaceUri + ", rootElementType=" + rootElementType
        + ", xsdPathInSdk=" + xsdPathInSdkOpt + "]";
  }
}
