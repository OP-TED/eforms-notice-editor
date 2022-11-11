package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class NoticeSaverRepeatableTest extends NoticeSaverTest {
  private static final String VIS_CHILDREN = "children";
  private static final String NOTICE_SUB_TYPE_VALUE = "X02";

  private static ObjectNode setupVisualModel(final ObjectMapper mapper, final String fakeSdkForTest,
      final String noticeSubTypeForTest) {

    final ObjectNode visRoot = mapper.createObjectNode();
    visRoot.put(VIS_CONTENT_ID, "visRoot");

    {
      //
      // DUMMY NOTICE DATA (as if coming from a web form before we have the XML).
      //
      final ObjectNode visMeta = mapper.createObjectNode();
      putGroupDef(visMeta);
      visMeta.put(VIS_CONTENT_ID, "THE_METADATA");
      visRoot.set("THE_METADATA", visMeta);
      final ArrayNode metaChildren = visMeta.putArray(VIS_CHILDREN);
      {
        // SDK version.
        final ObjectNode vis = mapper.createObjectNode();
        putFieldDef(vis);
        vis.put(VIS_CONTENT_ID, NoticeSaver.FIELD_ID_SDK_VERSION);
        vis.put(VIS_VALUE, fakeSdkForTest);
        metaChildren.add(vis);
      }
      {
        // Notice sub type.
        final ObjectNode vis = mapper.createObjectNode();
        putFieldDef(vis);
        vis.put(VIS_CONTENT_ID, NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE);
        vis.put(VIS_VALUE, noticeSubTypeForTest);
        metaChildren.add(vis);
      }
    }

    //
    // NOTICE CONTENT.
    //

    // For the test data we want nested repeatability (repeating groups inside a repeating group).
    // On the left is the content
    // On the right is the node id (without the GR- prefix to simplify):
    // X -> Y "means X has child Y and Y is child of X"

    // THE_CONTENT -> A1
    // A1 -> A1-B1
    // A1 -> A1-B2

    // THE_CONTENT -> A2
    // A2 -> A2-B1

    {
      final ObjectNode visContent = mapper.createObjectNode();
      putGroupDef(visContent);
      visContent.put(VIS_CONTENT_ID, "THE_CONTENT");
      visRoot.set("THE_CONTENT", visContent);
      final ArrayNode contentChildren = visContent.putArray(VIS_CHILDREN);
      {
        final ObjectNode visGroupA1 = mapper.createObjectNode();
        putGroupDef(visGroupA1);
        visGroupA1.put(VIS_CONTENT_ID, "GR-A1");
        contentChildren.add(visGroupA1);
        final ArrayNode a1Children = visGroupA1.putArray(VIS_CHILDREN);

        final ObjectNode visGroupB1 = mapper.createObjectNode();
        putGroupDef(visGroupB1);
        visGroupB1.put(VIS_CONTENT_ID, "GR-A1-B1");
        a1Children.add(visGroupB1);

        final ObjectNode visGroupB2 = mapper.createObjectNode();
        putGroupDef(visGroupB2);
        visGroupB2.put(VIS_CONTENT_ID, "GR-A1-B2");
        a1Children.add(visGroupB2);
      }
      {
        final ObjectNode visGroupA2 = mapper.createObjectNode();
        putGroupDef(visGroupA2);
        visGroupA2.put(VIS_CONTENT_ID, "GR-A2");
        contentChildren.add(visGroupA2);
        final ArrayNode a2Children = visGroupA2.putArray(VIS_CHILDREN);

        final ObjectNode visGroupB1 = mapper.createObjectNode();
        putGroupDef(visGroupB1);
        visGroupB1.put(VIS_CONTENT_ID, "GR-A2-B1");
        a2Children.add(visGroupB1);
      }
    }

    // "BT-01-notice-1": {
    // "domId": "editor-id-BT-01-notice-0001",
    // "contentId": "BT-01-notice",
    // "type": "field",
    // "contentCount": "1",
    // "value": "",
    // "contentParentId": "THE_METADATA-container-elem",
    // "contentParentCount": "1",
    // "contentNodeId": "ND-Root"
    // },

    // TODO not just a flat list of fields!

    return visRoot;
  }

  @SuppressWarnings("static-method")
  @Test
  public final void test() {
    final ObjectMapper mapper = new ObjectMapper();
    final String prefixedSdkVersion = "eforms-sdk-" + "1.3.0"; // A dummy 1.3.0, not real 1.3.0
    final String noticeSubType = NOTICE_SUB_TYPE_VALUE; // A dummy 10, not the real 10 of 1.3.0
    final ObjectNode root = setupVisualModel(mapper, prefixedSdkVersion, noticeSubType);

    //
    // NODES from fields.json
    //
    final Map<String, JsonNode> nodeById = new LinkedHashMap<>();
    setupFieldsJsonXmlStructureNodes(mapper, nodeById);

    //
    // FIELDS from fields.json
    //
    final Map<String, JsonNode> fieldById = new LinkedHashMap<>();
    setupFieldsJsonFields(mapper, fieldById);

  }

}
