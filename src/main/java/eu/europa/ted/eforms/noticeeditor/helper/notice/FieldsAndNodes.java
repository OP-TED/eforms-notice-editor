package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;

/**
 * Equivalent to SDK "fields.json".
 */
public class FieldsAndNodes {

  private static final String FIELD_ID_KEY = "id";
  private static final String NODE_ID_KEY = "id";

  private static final String FIELD_REPEATABLE = "repeatable";
  private static final String NODE_REPEATABLE = "repeatable";

  private final Map<String, JsonNode> fieldById;
  private final Map<String, JsonNode> nodeById;

  public FieldsAndNodes(final JsonNode fieldsJsonRoot) {
    {
      final JsonNode nodes = fieldsJsonRoot.get(SdkConstants.FIELDS_JSON_XML_STRUCTURE_KEY);
      final Map<String, JsonNode> nodesMap = new LinkedHashMap<>();
      for (final JsonNode node : nodes) {
        nodesMap.put(node.get(NODE_ID_KEY).asText(), node);
      }
      this.nodeById = nodesMap;
    }
    {
      final JsonNode fields = fieldsJsonRoot.get(SdkConstants.FIELDS_JSON_FIELDS_KEY);
      final Map<String, JsonNode> fieldsMap = new LinkedHashMap<>();
      for (final JsonNode field : fields) {
        fieldsMap.put(field.get(FIELD_ID_KEY).asText(), field);
      }
      this.fieldById = fieldsMap;
    }
  }

  public FieldsAndNodes(final Map<String, JsonNode> fields, final Map<String, JsonNode> nodes) {
    this.fieldById = fields;
    this.nodeById = nodes;
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
    // Example:
    // "repeatable" : {
    // "value" : true, WE WANT THE BOOLEAN VALUE AND FAIL IF WE CANNOT HAVE IT
    // "severity" : "ERROR"
    // },
    // As you can see this is not a flat property, you have to do two gets.
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
