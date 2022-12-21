package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.sorting.XmlTagSorting;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;

public class XmlTagSortingTest {

  @SuppressWarnings("static-method")
  @Test
  public void test() throws IOException, ParserConfigurationException, SAXException {
    final DocumentBuilder builder = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo();

    // SORT.
    final Document docUnsorted = DummySdk.getDummyX02NoticeNoComments(builder);
    XmlTagSorting.sortXmlTags(builder, docUnsorted.getDocumentElement(), docTypeInfo,
        DummySdk.getDummySdkRoot());

    // COMPARE.
    final Document docSorted = DummySdk.getDummyX02NoticeNoComments(builder);
    final String textSorted = EditorXmlUtils.asText(docSorted, true);
    final String textUnsortedAfterSort = EditorXmlUtils.asText(docUnsorted, true);
    System.out.println(textUnsortedAfterSort);
    assertEquals(textSorted, textUnsortedAfterSort);
  }
}
