package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;

public class FieldsAndNodes {
  private Map<String, JsonNode> fieldById;
  private Map<String, JsonNode> nodeById;

  public FieldsAndNodes(final JsonNode fieldsJsonRoot) {
    {
      final JsonNode fields = fieldsJsonRoot.get("fields");
      final Map<String, JsonNode> fieldsMap = new HashMap<>();
      for (final JsonNode field : fields) {
        fieldsMap.put(field.get("id").asText(), field);
      }
      this.fieldById = fieldsMap;
    }
    final JsonNode nodes = fieldsJsonRoot.get("xmlStructure");
    final Map<String, JsonNode> nodesMap = new HashMap<>();
    for (final JsonNode node : nodes) {
      nodesMap.put(node.get("id").asText(), node);
    }
    this.nodeById = nodesMap;
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
