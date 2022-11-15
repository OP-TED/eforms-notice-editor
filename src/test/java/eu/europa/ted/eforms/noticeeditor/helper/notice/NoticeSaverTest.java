package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

/**
 * Abstract, common code for testing the notice saver.
 */
public abstract class NoticeSaverTest {
  // IDEA: reuse some common constants even with notice saver.

  private static final String CODELIST_NOTICE_SUBTYPE = "notice-subtype";
  //
  // UI FORM RELATED.
  //
  static final String VIS_CONTENT_PARENT_COUNT = VisualModel.VIS_CONTENT_PARENT_COUNT;
  static final String VIS_CONTENT_COUNT = VisualModel.VIS_CONTENT_COUNT;
  static final String VIS_CONTENT_ID = VisualModel.VIS_CONTENT_ID;
  static final String VIS_VALUE = VisualModel.VIS_VALUE;
  static final String VIS_NODE_ID = "visNodeId";
  static final String VIS_FIRST = "-1";
  static final String VIS_SECOND = "-2";

  //
  // FIELDS JSON RELATED.
  //
  static final String ND_ROOT = NoticeSaver.ND_ROOT;
  static final String ND_ROOT_EXTENSION = NoticeSaver.ND_ROOT_EXTENSION;

  static final String KEY_CODE_LIST_ID = "codeListId";
  static final String KEY_TYPE = "type";
  static final String KEY_PARENT_NODE_ID = "parentNodeId";
  static final String KEY_NODE_PARENT_ID = "parentId";
  static final String KEY_XPATH_REL = "xpathRelative";
  static final String KEY_XPATH_ABS = "xpathAbsolute";
  static final String KEY_FIELD_REPEATABLE = "repeatable";
  static final String KEY_NODE_REPEATABLE = "repeatable";
  static final String KEY_VALUE = "value";

  static final String TYPE_DATE = "date";
  static final String TYPE_TEXT = "text";
  static final String TYPE_URL = "url";
  static final String TYPE_CODE = "code";
  static final String TYPE_ID = "id";

  static final void contains(final String xml, final String text) {
    assertTrue(xml.contains(text), text);
  }

  static final void count(final String xml, final int expectedCount, final String toMatch) {
    assertEquals(expectedCount, StringUtils.countMatches(xml, toMatch), toMatch);
  }

  static void fieldPutRepeatable(final ObjectNode field, final boolean repeatable) {
    // It has to be done in two steps for the fields.
    final ObjectNode prop = JsonUtils.createObjectNode();
    prop.put(KEY_VALUE, repeatable);
    field.set(KEY_FIELD_REPEATABLE, prop);
  }

  static void nodePutRepeatable(final ObjectNode node, final boolean repeatable) {
    // Very direct in this case.
    node.put(KEY_NODE_REPEATABLE, repeatable);
  }

  /**
   * Setup dummy node metadata.
   *
   * @param fieldById This map will be modified as a SIDE-EFFECT
   */
  @SuppressWarnings("static-method")
  protected Map<String, JsonNode> setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper) {
    final Map<String, JsonNode> nodeById = new LinkedHashMap<>(512);
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_ROOT, node);
      node.put(KEY_XPATH_ABS, "/*");
      node.put(KEY_XPATH_REL, "/*");
      NoticeSaverTest.fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_ROOT_EXTENSION, node);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS,
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put(KEY_XPATH_REL,
          "ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      NoticeSaverTest.fieldPutRepeatable(node, false);
    }
    return nodeById;
  }

  /**
   * Setup dummy field metadata.
   *
   * @param fieldById This map will be modified as a SIDE-EFFECT
   */
  @SuppressWarnings("static-method")
  protected Map<String, JsonNode> setupFieldsJsonFields(final ObjectMapper mapper) {
    final Map<String, JsonNode> fieldById = new LinkedHashMap<>(1024);
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(NoticeSaver.FIELD_ID_SDK_VERSION, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT);
      field.put(KEY_XPATH_ABS, "/*/cbc:CustomizationID");
      field.put(KEY_XPATH_REL, "cbc:CustomizationID");
      field.put(KEY_TYPE, TYPE_ID);
      NoticeSaverTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT_EXTENSION);
      field.put(KEY_XPATH_ABS,
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:NoticeSubType/cbc:SubTypeCode");
      field.put(KEY_XPATH_REL, "efac:NoticeSubType/cbc:SubTypeCode");
      field.put(KEY_TYPE, TYPE_CODE);
      NoticeSaverTest.fieldPutRepeatable(field, false);
      field.put(KEY_CODE_LIST_ID, CODELIST_NOTICE_SUBTYPE);
    }
    return fieldById;
  }

  protected PhysicalModel setupPhysicalModel(final ObjectMapper mapper, final String noticeSubType,
      final String documentType, final VisualModel visModel)
      throws IOException, ParserConfigurationException {
    //
    // NODES from fields.json
    //
    final Map<String, JsonNode> nodeById = setupFieldsJsonXmlStructureNodes(mapper);

    //
    // FIELDS from fields.json
    //
    final Map<String, JsonNode> fieldById = setupFieldsJsonFields(mapper);

    //
    // OTHER from notice-types.json
    //
    // Setup dummy notice-types.json info that we need for the XML generation.
    final Map<String, JsonNode> noticeInfoBySubtype = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("documentType", documentType);
      noticeInfoBySubtype.put(noticeSubType, info);
    }

    final Map<String, JsonNode> documentInfoByType = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("namespace",
          "http://data.europa.eu/p27/eforms-business-registration-information-notice/1");
      info.put("rootElement", "BusinessRegistrationInformationNotice");
      documentInfoByType.put(documentType, info);
    }

    //
    // BUILD CONCEPTUAL MODEL.
    //
    final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldById, nodeById);
    final ConceptualModel conceptualModel = visModel.toConceptualModel(fieldsAndNodes);

    //
    // BUILD PHYSICAL MODEL.
    //
    final boolean debug = true; // Adds field ids in the XML.
    final boolean buildFields = true;
    final SchemaInfo schemaInfo = SchemaToolsTest.getTestSchemaInfo();
    final PhysicalModel pm = NoticeSaver.buildPhysicalModelXml(fieldsAndNodes, noticeInfoBySubtype,
        documentInfoByType, conceptualModel, debug, buildFields, schemaInfo);

    return pm;
  }
}