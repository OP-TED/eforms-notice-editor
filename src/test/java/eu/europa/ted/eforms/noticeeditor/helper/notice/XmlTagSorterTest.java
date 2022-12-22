package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.sorting.XmlTagSorter;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

public class XmlTagSorterTest {

  @SuppressWarnings("static-method")
  @Test
  public void test() throws IOException, ParserConfigurationException, SAXException {
    final SdkVersion sdkVersion = new SdkVersion("1.6.0");

    final DocumentBuilder builder = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo(sdkVersion);

    // SORT.
    final Document docUnsorted = DummySdk.getDummyX02NoticeNoComments(builder, sdkVersion);
    final Element noticeRootElement = docUnsorted.getDocumentElement();
    XmlTagSorter.sortXmlTagsTopLevel(builder, noticeRootElement, docTypeInfo,
        DummySdk.buildDummySdkPath(sdkVersion), sdkVersion);

    // COMPARE.
    final Document docPreSorted = DummySdk.getDummyX02NoticeNoComments(builder, sdkVersion);
    final String textPreSorted = EditorXmlUtils.asText(docPreSorted, true);
    final String textUnsortedAfterSort = EditorXmlUtils.asText(docUnsorted, true);

    System.out.println(textUnsortedAfterSort);
    // System.out.println("");
    // System.out.println("DIFFERENCE");
    // System.out.println(StringUtils.difference(textUnsortedAfterSort, textPreSorted));
    // System.out.println(StringUtils.difference(textPreSorted, textUnsortedAfterSort));
    // assertEquals(textPreSorted, textUnsortedAfterSort);
  }
}
