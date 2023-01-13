package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Abstract, common code for testing the save notice to XML feature.
 */
public abstract class SaveNoticeTest {
  // IDEA: reuse some common constants even with notice saver.

  //
  // UI FORM RELATED.
  //
  static final String VIS_CHILDREN = VisualModel.VIS_CHILDREN;
  static final String VIS_CONTENT_COUNT = VisualModel.VIS_CONTENT_COUNT;
  static final String VIS_CONTENT_ID = VisualModel.VIS_CONTENT_ID;
  static final String VIS_VALUE = VisualModel.VIS_VALUE;
  static final String VIS_NODE_ID = VisualModel.VIS_NODE_ID;

  //
  // FIELDS JSON RELATED.
  //
  private static final String CODELIST_NOTICE_SUBTYPE = "notice-subtype";

  /**
   * Root node. Root of all other nodes and starting point of the recursion.
   */
  static final String ND_ROOT = ConceptualModel.ND_ROOT;

  /**
   * Root extension node, holds some important metadata and more.
   */
  static final String ND_ROOT_EXTENSION = ConceptualModel.ND_ROOT_EXTENSION;

  static final String KEY_TYPE = "type";

  static final String KEY_PARENT_NODE_ID = "parentNodeId";
  static final String KEY_NODE_PARENT_ID = "parentId";

  static final String KEY_XPATH_REL = "xpathRelative";
  static final String KEY_XPATH_ABS = "xpathAbsolute";

  static final String KEY_FIELD_REPEATABLE = "repeatable";
  static final String KEY_NODE_REPEATABLE = "repeatable";
  static final String KEY_VALUE = FieldsAndNodes.VALUE;

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
      SaveNoticeTest.fieldPutRepeatable(node, false);
    }
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_ROOT_EXTENSION, node);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS,
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      node.put(KEY_XPATH_REL,
          "ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension");
      SaveNoticeTest.fieldPutRepeatable(node, false);
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
    // This only sets most commonly required fields.
    final Map<String, JsonNode> fieldById = new LinkedHashMap<>(1024);
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(ConceptualModel.FIELD_ID_SDK_VERSION, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT);
      field.put(KEY_XPATH_ABS, "/*/cbc:CustomizationID");
      field.put(KEY_XPATH_REL, "cbc:CustomizationID");
      field.put(KEY_TYPE, TYPE_ID);
      SaveNoticeTest.fieldPutRepeatable(field, false);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(ConceptualModel.FIELD_ID_NOTICE_SUB_TYPE, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT_EXTENSION);
      field.put(KEY_XPATH_ABS,
          "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:NoticeSubType/cbc:SubTypeCode");
      field.put(KEY_XPATH_REL, "efac:NoticeSubType/cbc:SubTypeCode");
      field.put(KEY_TYPE, TYPE_CODE);
      SaveNoticeTest.fieldPutRepeatable(field, false);
      FieldsAndNodes.setFieldFlatCodeList(mapper, field, CODELIST_NOTICE_SUBTYPE);
    }
    {
      final ObjectNode field = mapper.createObjectNode();
      fieldById.put(ConceptualModel.FIELD_NOTICE_ID, field);
      field.put(KEY_PARENT_NODE_ID, ND_ROOT);
      field.put(KEY_XPATH_ABS, "/*/cbc:ID[@schemeName='notice-id']");
      field.put(KEY_XPATH_REL, "cbc:ID[@schemeName='notice-id']");
      field.put(KEY_TYPE, TYPE_ID);
      SaveNoticeTest.fieldPutRepeatable(field, false);
    }
    return fieldById;
  }

  @SuppressWarnings("static-method")
  protected VisualModel setupVisualModel(final ObjectMapper mapper, final SdkVersion sdkVersion,
      final String noticeSubTypeForTest) {
    final String prefixedSdkVersion = FieldsAndNodes.EFORMS_SDK_PREFIX + sdkVersion.toString();

    // Setup root of the visual model.
    final ObjectNode visRoot = mapper.createObjectNode();
    VisualModel.setupVisualRootForTest(mapper, prefixedSdkVersion, noticeSubTypeForTest, visRoot);
    return new VisualModel(visRoot);
  }

  protected PhysicalModel setupPhysicalModel(final ObjectMapper mapper, final String noticeSubType,
      final String documentType, final VisualModel visModel, final SdkVersion sdkVersion)
      throws ParserConfigurationException, IOException, SAXException {

    //
    // NODES like in fields.json
    //
    final Map<String, JsonNode> nodeById = setupFieldsJsonXmlStructureNodes(mapper);

    //
    // FIELDS like in fields.json
    //
    final Map<String, JsonNode> fieldById = setupFieldsJsonFields(mapper);

    //
    // OTHER like in notice-types.json
    //
    // Setup dummy notice-types.json info that we need for the XML generation.
    final Map<String, JsonNode> noticeInfoBySubtype = new HashMap<>();
    {
      final ObjectNode info = mapper.createObjectNode();
      info.put("documentType", documentType);
      noticeInfoBySubtype.put(noticeSubType, info);
    }

    // final Map<String, JsonNode> documentInfoByType = new HashMap<>();
    // {
    // final ObjectNode info = mapper.createObjectNode();
    // info.put("namespace", namespace);
    // info.put("rootElement", rootElementName);
    // info.put("schemaLocation", "xyz");
    // documentInfoByType.put(documentType, info);
    // }

    final Map<String, JsonNode> documentInfoByType = DummySdk.buildDocInfoByType(sdkVersion);

    //
    // BUILD CONCEPTUAL MODEL.
    //
    final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldById, nodeById, sdkVersion);
    final ConceptualModel conceptualModel = visModel.toConceptualModel(fieldsAndNodes);

    //
    // BUILD PHYSICAL MODEL.
    //
    final boolean debug = true; // Adds field ids in the XML, making it easier to test the output.
    final boolean buildFields = true;

    final Path sdkRootFolder = SdkConstants.DEFAULT_SDK_ROOT;
    final PhysicalModel physicalModel = PhysicalModel.buildPhysicalModel(conceptualModel,
        fieldsAndNodes, noticeInfoBySubtype, documentInfoByType, debug, buildFields, sdkRootFolder);

    return physicalModel;
  }

  static void checkCommon(final String prefixedSdkVersion, final String noticeSubType,
      final String xml) {

    count(xml, 1, "encoding=\"UTF-8\"");
    contains(xml, " xmlns=");

    // Notice sub type.
    count(xml, 1, "<cbc:SubTypeCode");
    count(xml, 1, " editorFieldId=\"" + ConceptualModel.FIELD_ID_NOTICE_SUB_TYPE + "\""
        + " listName=\"notice-subtype\">" + noticeSubType + "<");

    // SDK version.
    count(xml, 1, "<cbc:CustomizationID");
    count(xml, 1, " editorFieldId=\"" + ConceptualModel.FIELD_ID_SDK_VERSION + "\">"
        + prefixedSdkVersion + "<");

  }

}
