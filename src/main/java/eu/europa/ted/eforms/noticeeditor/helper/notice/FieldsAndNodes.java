package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.SdkConstants;

/**
 * Equivalent to SDK "fields.json".
 */
public class FieldsAndNodes {

  private static final String JSONK_FIELDS_ID_KEY = "id";
  private static final String JSONK_XML_STRUCTURE_ID_KEY = "id";

  private final Map<String, JsonNode> fieldById;
  private final Map<String, JsonNode> nodeById;

  public FieldsAndNodes(final JsonNode fieldsJsonRoot) {
    {
      final JsonNode nodes = fieldsJsonRoot.get(SdkConstants.FIELDS_JSON_XML_STRUCTURE_KEY);
      final Map<String, JsonNode> nodesMap = new LinkedHashMap<>();
      for (final JsonNode node : nodes) {
        nodesMap.put(node.get(JSONK_XML_STRUCTURE_ID_KEY).asText(), node);
      }
      this.nodeById = nodesMap;
    }
    {
      final JsonNode fields = fieldsJsonRoot.get(SdkConstants.FIELDS_JSON_FIELDS_KEY);
      final Map<String, JsonNode> fieldsMap = new LinkedHashMap<>();
      for (final JsonNode field : fields) {
        fieldsMap.put(field.get(JSONK_FIELDS_ID_KEY).asText(), field);
      }
      this.fieldById = fieldsMap;
    }
  }

  public FieldsAndNodes(Map<String, JsonNode> fields, Map<String, JsonNode> nodes) {
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
}
