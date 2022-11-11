package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
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

  //
  // UI FORM RELATED.
  //
  static final String VIS_CONTENT_PARENT_COUNT = NoticeSaver.VIS_CONTENT_PARENT_COUNT;
  static final String VIS_CONTENT_COUNT = NoticeSaver.VIS_CONTENT_COUNT;
  static final String VIS_CONTENT_ID = NoticeSaver.VIS_CONTENT_ID;
  static final String VIS_VALUE = NoticeSaver.VIS_VALUE;
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
  static final String KEY_REPEATABLE = "repeatable";
  static final String KEY_VALUE = "value";

  static final String TYPE_DATE = "date";
  static final String TYPE_TEXT = "text";
  static final String TYPE_URL = "url";
  static final String TYPE_CODE = "code";
  static final String TYPE_ID = "id";

  /**
   * Set default field info.
   */
  static void putFieldDef(final ObjectNode vis) {
    vis.put(NoticeSaver.VIS_TYPE, NoticeSaver.VIS_TYPE_FIELD);
    vis.put(VIS_CONTENT_COUNT, "1");
    vis.put(VIS_CONTENT_PARENT_COUNT, "1");
  }

  /**
   * Set default group info.
   */
  static void putGroupDef(final ObjectNode vis) {
    vis.put(NoticeSaver.VIS_TYPE, NoticeSaver.VIS_TYPE_GROUP);
    vis.put(VIS_CONTENT_COUNT, "1");
    vis.put(VIS_CONTENT_PARENT_COUNT, "1");
  }

  static final void contains(final String xml, final String text) {
    assertTrue(xml.contains(text), text);
  }

  static final void count(final String xml, final int expectedCount, final String toMatch) {
    assertEquals(expectedCount, StringUtils.countMatches(xml, toMatch), toMatch);
  }

  static void fieldPutRepeatable(final ObjectNode field, final boolean repeatable) {
    final ObjectNode prop = JsonUtils.createObjectNode();
    prop.put(KEY_VALUE, repeatable);
    field.set(KEY_REPEATABLE, prop);
  }

  protected void setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper,
      final Map<String, JsonNode> nodeById) {
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
  }

  /**
   * Setup dummy field metadata.
   *
   * @param fieldById This map will be modified as a SIDE-EFFECT
   */
  protected void setupFieldsJsonFields(final ObjectMapper mapper,
      final Map<String, JsonNode> fieldById) {
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
      field.put(KEY_CODE_LIST_ID, "notice-subtype");
    }
  }
}
