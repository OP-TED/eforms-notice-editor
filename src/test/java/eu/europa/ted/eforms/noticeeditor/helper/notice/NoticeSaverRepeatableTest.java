package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class NoticeSaverRepeatableTest extends NoticeSaverTest {
  private static final String NOTICE_SUB_TYPE_VALUE = "X02";

  private static ObjectNode setupVisualModel(final ObjectMapper mapper, final String fakeSdkForTest,
      final String noticeSubTypeForTest) {
    final ObjectNode root = mapper.createObjectNode();

    //
    // DUMMY NOTICE DATA (as if coming from a web form before we have the XML).
    //
    {
      // SDK version.
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(NoticeSaver.FIELD_ID_SDK_VERSION + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, NoticeSaver.FIELD_ID_SDK_VERSION);
      vis.put(VIS_VALUE, fakeSdkForTest);
    }
    {
      // Notice sub type.
      final ObjectNode vis = mapper.createObjectNode();
      putDefault(vis);
      root.set(NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE + VIS_FIRST, vis);
      vis.put(VIS_CONTENT_ID, NoticeSaver.FIELD_ID_NOTICE_SUB_TYPE);
      vis.put(VIS_VALUE, noticeSubTypeForTest);
    }

    //
    // NOTICE CONTENT.
    //
    return root;
  }

  @Test
  public final void test() {
    final ObjectMapper mapper = new ObjectMapper();
    final String prefixedSdkVersion = "eforms-sdk-" + "1.3.0"; // A dummy 1.3.0, not real 1.3.0
    final String noticeSubType = NOTICE_SUB_TYPE_VALUE; // A dummy 10, not the real 10 of 1.3.0
    setupVisualModel(mapper, prefixedSdkVersion, noticeSubType);
  }

}
