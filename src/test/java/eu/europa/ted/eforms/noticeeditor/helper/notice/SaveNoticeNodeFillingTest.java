package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putGroupDef;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.helper.VersionHelper;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * A test with focus on the save notice "filling in" of missing non-repeating intermediary
 * conceptual nodes.
 */
@SpringBootTest
public class SaveNoticeNodeFillingTest extends SaveNoticeTest {

  private static final Logger logger = LoggerFactory.getLogger(SaveNoticeNodeFillingTest.class);

  /**
   * It could be anything for this test dummy, but we will use BRIN.
   */
  private static final String NOTICE_DOCUMENT_TYPE = "BRIN";

  private static final String ND_X = "ND_X";
  private static final String ND_Y = "ND_Y";

  @Autowired
  protected SdkService sdkService;

  /**
   * Setup dummy node metadata.
   *
   * @param nodeById This map will be modified as a SIDE-EFFECT
   */
  @Override
  protected Map<String, JsonNode> setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper) {
    final Map<String, JsonNode> nodeById = super.setupFieldsJsonXmlStructureNodes(mapper);
    {
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_X, node);
      node.put(KEY_NODE_ID, ND_X);
      node.put(KEY_NODE_PARENT_ID, ND_ROOT);
      node.put(KEY_XPATH_ABS, "/*/x");
      node.put(KEY_XPATH_REL, "x");
      SaveNoticeTest.fieldPutRepeatable(node, false); // NON-REPEATABLE!
    }
    {
      // Y has X as a parent.
      final ObjectNode node = mapper.createObjectNode();
      nodeById.put(ND_Y, node);
      node.put(KEY_NODE_ID, ND_Y);
      node.put(KEY_NODE_PARENT_ID, ND_X);
      node.put(KEY_XPATH_ABS, "/*/x/y");
      node.put(KEY_XPATH_REL, "y");
      SaveNoticeTest.fieldPutRepeatable(node, false); // NON-REPEATABLE TOO!
    }
    return Collections.unmodifiableMap(nodeById);
  }

  @Override
  protected VisualModel setupVisualModel(final ObjectMapper mapper, final SdkVersion sdkVersion,
      final String noticeSubTypeForTest) {

    final VisualModel visualModel =
        super.setupVisualModel(mapper, sdkVersion, noticeSubTypeForTest);
    final JsonNode visRoot = visualModel.getVisRoot();
    final ArrayNode visRootChildren = visualModel.getVisRootChildren();

    //
    // NOTICE CONTENT.
    //

    final ObjectNode vis = mapper.createObjectNode();
    visRootChildren.add(vis);
    putGroupDef(vis);
    vis.put(VIS_CONTENT_ID, ND_Y);
    vis.put(VIS_NODE_ID, ND_Y);

    // ND_X (the parent) is never set, on purpose.

    return new VisualModel(visRoot);
  }


  /**
   * Setup dummy field metadata.
   *
   * @param fieldById This map will be modified as a SIDE-EFFECT
   */
  @Override
  protected Map<String, JsonNode> setupFieldsJsonFields(final ObjectMapper mapper) {
    final Map<String, JsonNode> fieldById = super.setupFieldsJsonFields(mapper);
    // Not field is defined on purpose.
    return fieldById;
  }

  @Test
  public final void test() throws ParserConfigurationException, IOException, SAXException {
    final ObjectMapper mapper = new ObjectMapper();

    // A dummy 1.9.0, not real 1.9.0
    final SdkVersion sdkVersion = new SdkVersion("1.9.0");
    final String prefixedSdkVersion =
        VersionHelper.prefixSdkVersionWithoutPatch(sdkVersion).toString();
    final String noticeSubType = "X02"; // A dummy X02, not the real X02 of the SDK.

    final VisualModel visualModel = setupVisualModel(mapper, sdkVersion, noticeSubType);

    final PhysicalModel physicalModel =
        setupPhysicalModel(mapper, noticeSubType, NOTICE_DOCUMENT_TYPE, visualModel,
            sdkService.getSdkRootFolder(), sdkVersion);

    final String xml = physicalModel.toXmlText(false); // Not indented to avoid line breaks.
    logger.info(physicalModel.toXmlText(true));

    // IDEA it would be more maintainable to use xpath to check the XML instead of pure text.
    // physicalModel.evaluateXpathForTests("/", "test2");

    checkCommon(prefixedSdkVersion, noticeSubType, xml);

    // Verify nodes.

    // y
    count(xml, 1, "<y"); // 1 in total
    count(xml, 1, "editorNodeId=\"ND_Y\"");

    // x
    count(xml, 1, "<x"); // 1 in total
    count(xml, 1, "editorNodeId=\"ND_X\"");
  }

}
