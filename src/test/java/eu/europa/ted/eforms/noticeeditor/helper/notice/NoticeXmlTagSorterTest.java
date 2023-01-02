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
import eu.europa.ted.eforms.noticeeditor.sorting.NoticeXmlTagSorter;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

public class NoticeXmlTagSorterTest {

  @SuppressWarnings("static-method")
  @Test
  public void test() throws IOException, ParserConfigurationException, SAXException {

    final DocumentBuilder builder = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);
    final SdkVersion sdkVersion = new SdkVersion("1.6.0");
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo(sdkVersion);

    final Document docUnsorted = DummySdk.getDummyX02NoticeNoComments(builder, sdkVersion);
    final Element noticeRootElement = docUnsorted.getDocumentElement();

    // SORT.
    NoticeXmlTagSorter.sortXmlTags(builder, docTypeInfo, DummySdk.buildDummySdkPath(sdkVersion),
        noticeRootElement);

    final boolean indentXml = true;
    final Document docPreSorted = DummySdk.getDummyX02NoticeNoComments(builder, sdkVersion);
    final String textPreSorted = EditorXmlUtils.asText(docPreSorted, indentXml);

    final String textUnsortedAfterSort = EditorXmlUtils.asText(docUnsorted, indentXml);
    System.out.println(textUnsortedAfterSort);

    //
    // cac:AdditionalDocumentReference is of type DocumentReferenceType
    // TODO there is a problem with the order inside of "cac:AdditionalDocumentReference"
    // <xsd:complexType name="DocumentReferenceType">
    // <xsd:sequence>
    // <xsd:element ref="ext:UBLExtensions" minOccurs="0" maxOccurs="1"/>
    // <xsd:element ref="cbc:ID" minOccurs="1" maxOccurs="1"/>
    // <xsd:element ref="cbc:CopyIndicator" minOccurs="0" maxOccurs="1"/>
    // <xsd:element ref="cbc:UUID" minOccurs="0" maxOccurs="1"/>
    //

    // <cac:AdditionalDocumentReference>
    // <cbc:ReferencedDocumentInternalAddress>Registration of the "ACME Solution" European
    // Company.</cbc:ReferencedDocumentInternalAddress>
    // <cbc:DocumentDescription>The Hornfrestone Gazette</cbc:DocumentDescription>
    // <cac:Attachment>
    // <cac:ExternalReference>
    // <cbc:URI>http://www.hornfrestone-gazette.co.uk/20201116/acme-solution.pdf</cbc:URI>
    // </cac:ExternalReference>
    // </cac:Attachment>
    // <cbc:ID>DUMMY</cbc:ID>
    // <cbc:IssueDate>2020-11-16+01:00</cbc:IssueDate>
    // </cac:AdditionalDocumentReference>

    // COMPARE, diff notice xmls.
    System.out.println("");
    // System.out.println("DIFFERENCE");
    // System.out.println(StringUtils.difference(textUnsortedAfterSort, textPreSorted));
    // System.out.println(StringUtils.difference(textPreSorted, textUnsortedAfterSort));
    // assertEquals(textPreSorted, textUnsortedAfterSort);
  }
}
