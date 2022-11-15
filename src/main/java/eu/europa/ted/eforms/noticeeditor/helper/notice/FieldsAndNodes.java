package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;

/**
 * Equivalent to the SDK "fields.json".
 */
public class FieldsAndNodes {

  private static final String FIELD_ID_KEY = "id";
  private static final String NODE_ID_KEY = "id";

  private static final String FIELD_REPEATABLE = "repeatable";
  private static final String NODE_REPEATABLE = "repeatable";

  private final Map<String, JsonNode> fieldById;
  private final Map<String, JsonNode> nodeById;

  /**
   * This constructor is meant to be used to read from fields.json data.
   *
   * @param fieldsJsonRoot The root of the SDK fields.json file
   */
  public FieldsAndNodes(final JsonNode fieldsJsonRoot) {
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
  }

  /**
   * This constructor is meant to be used by unit tests.
   */
  FieldsAndNodes(final Map<String, JsonNode> fieldById, final Map<String, JsonNode> nodeById) {
    this.fieldById = fieldById;
    this.nodeById = nodeById;
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

  public static boolean isFieldRepeatable(final JsonNode json) {
    return getFieldPropertyValueBoolStrict(json, FIELD_REPEATABLE);
  }

  public static boolean isNodeRepeatable(final JsonNode json) {
    return JsonUtils.getBoolStrict(json, NODE_REPEATABLE);
  }
}
