package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Holds JSON data of the SDK "fields.json" file. Reuse this after construction. As with all SDK
 * data this is associated with an SDK version.
 */
public class FieldsAndNodes {

  public static final String ND_ROOT = "ND-Root";
  public static final String FIELD_OR_NODE_ID_KEY = "id";
  public static final String XPATH_RELATIVE = "xpathRelative";
  public static final String XPATH_ABSOLUTE = "xpathAbsolute";

  public static final String XSD_SEQUENCE_ORDER_KEY = "xsdSequenceOrder"; // Since SDK 1.7

  static final String VALUE = "value";

  static final String CODELIST_ID = "id";
  static final String CODELIST_TYPE = "type";

  public static final String FIELD_PARENT_NODE_ID = "parentNodeId";
  public static final String NODE_PARENT_NODE_ID = "parentId";

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
        nodesMap.put(node.get(FIELD_OR_NODE_ID_KEY).asText(), node);
      }
      this.nodeById = Collections.unmodifiableMap(nodesMap);
    }

    // SDK fields.
    {
      final JsonNode fields = fieldsJsonRoot.get(SdkConstants.FIELDS_JSON_FIELDS_KEY);
      final Map<String, JsonNode> fieldsMap = new LinkedHashMap<>(1028);
      for (final JsonNode field : fields) {
        fieldsMap.put(field.get(FIELD_OR_NODE_ID_KEY).asText(), field);
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

  /**
   * This can be used when working on common values like id, xpathRelative, ...
   */
  public JsonNode getFieldOrNodeById(final String fieldOrNodeId) {
    return fieldOrNodeId.startsWith("ND-") ? getNodeById(fieldOrNodeId)
        : getFieldById(fieldOrNodeId);
  }

  public Map<String, List<JsonNode>> buildMapOfFieldOrNodeByParentNodeId() {

    final Map<String, List<JsonNode>> fieldOrNodeByParentNodeId = new HashMap<>(512);

    // BUILD SPECIAL TREE WHERE THE KEY IS THE PARENT (group by parent).

    // NODE BY ID.
    for (final JsonNode node : nodeById.values()) {
      final Optional<String> parentNodeIdOpt = JsonUtils.getTextOpt(node, NODE_PARENT_NODE_ID);
      if (parentNodeIdOpt.isPresent()) {
        final String parentNodeId = parentNodeIdOpt.get();
        fieldOrNodeByParentNodeId.putIfAbsent(parentNodeId, new ArrayList<>());
        fieldOrNodeByParentNodeId.get(parentNodeId).add(node);
      }
    }

    // FIELD BY ID.
    for (final JsonNode field : fieldById.values()) {
      final Optional<String> parentNodeIdOpt = JsonUtils.getTextOpt(field, FIELD_PARENT_NODE_ID);
      if (parentNodeIdOpt.isPresent()) {
        final String parentNodeId = parentNodeIdOpt.get();
        fieldOrNodeByParentNodeId.putIfAbsent(parentNodeId, new ArrayList<>());
        fieldOrNodeByParentNodeId.get(parentNodeId).add(field);
      }
    }

    return fieldOrNodeByParentNodeId;
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
    return JsonUtils.getBoolStrict(prop, VALUE);
  }

  public static JsonNode getFieldProperty(final JsonNode json, final String propKey) {
    final JsonNode prop = json.get(propKey);
    Validate.notNull(prop, "Property is null for propKey=%s", propKey);
    return prop;
  }

  public static JsonNode getFieldPropertyValue(final JsonNode json, final String propKey) {
    final JsonNode prop = json.get(propKey);
    Validate.notNull(prop, "Property is null for propKey=%s", propKey);
    return prop.get(VALUE);
  }

  public boolean isFieldRepeatable(final String fieldId) {
    return isFieldRepeatableStatic(this.getFieldById(fieldId));
  }

  public boolean isNodeRepeatable(final String nodeId) {
    return isNodeRepeatableStatic(this.getNodeById(nodeId));
  }

  public boolean isNodeRepeatable(final Optional<String> nodeIdOpt) {
    // If there is no node id we say it is non-repeatable (false).
    return nodeIdOpt.isPresent() ? isNodeRepeatable(nodeIdOpt.get()) : false;
  }

  public static boolean isFieldRepeatableStatic(final JsonNode fieldMeta) {
    return getFieldPropertyValueBoolStrict(fieldMeta, FIELD_REPEATABLE);
  }

  public static boolean isNodeRepeatableStatic(final JsonNode nodeMeta) {
    return JsonUtils.getBoolStrict(nodeMeta, NODE_REPEATABLE);
  }

  public static void setFieldFlatCodeList(final ObjectMapper mapper, final ObjectNode field,
      final String codelistId) {
    final ObjectNode codeList = mapper.createObjectNode();
    final ObjectNode codelistValue = mapper.createObjectNode();
    codelistValue.put(FieldsAndNodes.CODELIST_ID, codelistId);
    codelistValue.put(FieldsAndNodes.CODELIST_TYPE, "flat");
    codeList.set(FieldsAndNodes.VALUE, codelistValue);
    field.set(PhysicalModel.FIELD_CODE_LIST, codeList);
  }

  public JsonNode getRootNode() {
    return getNodeById(ND_ROOT);
  }
}
