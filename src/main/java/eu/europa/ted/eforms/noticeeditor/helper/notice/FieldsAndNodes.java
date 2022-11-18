package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Holds JSON data of the SDK "fields.json" file. Reuse this after construction. As with all SDK
 * data this is associated with an SDK version.
 */
public class FieldsAndNodes {

  private static final String FIELD_ID_KEY = "id";
  private static final String NODE_ID_KEY = "id";

  static final String FIELD_PARENT_NODE_ID = "parentNodeId";

  private static final String FIELD_REPEATABLE = "repeatable";
  private static final String NODE_REPEATABLE = "repeatable";

  public static final String EFORMS_SDK_PREFIX = "eforms-sdk-";

  private final Map<String, JsonNode> fieldById;
  private final Map<String, JsonNode> nodeById;

  /**
   * The SDK version this data is associated to.
   */
  private final SdkVersion sdkVersion;

  /**
   * This constructor is meant to be used to read from fields.json data.
   *
   * @param fieldsJsonRoot The root of the SDK fields.json file
   */
  public FieldsAndNodes(final JsonNode fieldsJsonRoot, final SdkVersion expectedSdkVersion) {

    final SdkVersion sdkVersionFound = parseSdkVersion(fieldsJsonRoot);
    if (!expectedSdkVersion.equals(sdkVersionFound)) {
      // Sanity check.
      throw new RuntimeException(
          String.format("The SDK version does not match, expected %s but found %s",
              expectedSdkVersion, sdkVersionFound));
    }
    this.sdkVersion = sdkVersionFound;

    // SDK nodes.
    {
      final JsonNode nodes = fieldsJsonRoot.get(SdkConstants.FIELDS_JSON_XML_STRUCTURE_KEY);
      final Map<String, JsonNode> nodesMap = new LinkedHashMap<>(256);
      for (final JsonNode node : nodes) {
        nodesMap.put(node.get(NODE_ID_KEY).asText(), node);
      }
      this.nodeById = Collections.unmodifiableMap(nodesMap);
    }

    // SDK fields.
    {
      final JsonNode fields = fieldsJsonRoot.get(SdkConstants.FIELDS_JSON_FIELDS_KEY);
      final Map<String, JsonNode> fieldsMap = new LinkedHashMap<>(1028);
      for (final JsonNode field : fields) {
        fieldsMap.put(field.get(FIELD_ID_KEY).asText(), field);
      }
      this.fieldById = Collections.unmodifiableMap(fieldsMap);
    }

    Validate.notEmpty(fieldById);
    Validate.notEmpty(nodeById);
  }

  /**
   * This constructor is meant to be used by unit tests.
   */
  FieldsAndNodes(final Map<String, JsonNode> fieldById, final Map<String, JsonNode> nodeById,
      final SdkVersion sdkVersion) {
    Validate.notNull(sdkVersion);
    Validate.notEmpty(fieldById);
    Validate.notEmpty(nodeById);
    this.fieldById = fieldById;
    this.nodeById = nodeById;
    this.sdkVersion = sdkVersion;
  }

  public JsonNode getFieldById(final String fieldId) {
    final JsonNode jsonNode = fieldById.get(fieldId);
    Validate.notNull(jsonNode, "Field not found for id=%s", fieldId);
    return jsonNode;
  }

  public JsonNode getNodeById(final String nodeId) {
    final JsonNode jsonNode = nodeById.get(nodeId);
    Validate.notNull(jsonNode, "Node not found for id=%s", nodeId);
    return jsonNode;
  }

  public SdkVersion getSdkVersion() {
    return sdkVersion;
  }

  private static SdkVersion parseSdkVersion(final JsonNode fieldsJsonRoot) {
    // Example: "sdkVersion" : "eforms-sdk-1.3.2",
    final String text = fieldsJsonRoot.get("sdkVersion").asText(null);
    return new SdkVersion(text.substring(EFORMS_SDK_PREFIX.length()));
  }

  public static boolean getFieldPropertyValueBoolStrict(final JsonNode json, final String propKey) {
    // As you can see this is not a flat property, you have to do "get" two times:
    // Example:
    // "repeatable" : {
    // "value" : true, WE WANT THE BOOLEAN VALUE AND FAIL IF WE CANNOT HAVE IT
    // "severity" : "ERROR"
    // },
    final JsonNode prop = getFieldProperty(json, propKey);
    return JsonUtils.getBoolStrict(prop, "value");
  }

  public static JsonNode getFieldProperty(final JsonNode json, final String propKey) {
    final JsonNode prop = json.get(propKey);
    Validate.notNull(prop, "Property is null for propKey=%s", propKey);
    return prop;
  }

  public boolean isFieldRepeatable(final String fieldId) {
    return isFieldRepeatable(this.getFieldById(fieldId));
  }

  public boolean isNodeRepeatable(final String nodeId) {
    return isNodeRepeatable(this.getNodeById(nodeId));
  }

  public static boolean isFieldRepeatable(final JsonNode fieldMeta) {
    return getFieldPropertyValueBoolStrict(fieldMeta, FIELD_REPEATABLE);
  }

  public static boolean isNodeRepeatable(final JsonNode nodeMeta) {
    return JsonUtils.getBoolStrict(nodeMeta, NODE_REPEATABLE);
  }
}
