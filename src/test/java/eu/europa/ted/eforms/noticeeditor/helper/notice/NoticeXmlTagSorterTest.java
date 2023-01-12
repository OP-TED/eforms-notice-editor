package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.helper.validation.XsdValidator;
import eu.europa.ted.eforms.noticeeditor.sorting.NoticeXmlTagSorter;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.XpathUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

public class NoticeXmlTagSorterTest {

  private static final Logger logger = LoggerFactory.getLogger(NoticeXmlTagSorterTest.class);

  @SuppressWarnings("static-method")
  @Test
  public void testXmlSortingAndValidate()
      throws IOException, ParserConfigurationException, SAXException {

    //
    // How this test works:
    // 1. There is a reference notice XML document.
    // 2. A copy of this reference was created and tag order was purposefully modified.
    // 3. The test sorts the purposefully modified version and outputs it as indented xml text.
    // 4. The reference is parsed and rewritten as indented xml (same indentation as other xml
    // text).
    // 5. The sorted text is compared to the reference text.
    // 6. The test ensures the unsorted XML is invalid for coherence of the test.
    // 7. Then it validates the sorted XML against the appropriate SDK XSD.
    //

    // The configured document builder is reusable.
    final DocumentBuilder builder = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

    // Get document type info from the SDK. It contains information about XML namespaces.
    final SdkVersion sdkVersion = new SdkVersion("1.6.0");
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo(sdkVersion);

    // The xpath instance namespace aware and reusable.
    final XPath xpathInst = XpathUtils.setupXpathInst(docTypeInfo, Optional.empty());

    final Document docReference = DummySdk.getDummyX02NoticeReference(builder, sdkVersion);
    final Document docUnsorted1 = DummySdk.getDummyX02NoticeUnsorted(builder, sdkVersion);

    // SORT.
    final NoticeXmlTagSorter sorter = new NoticeXmlTagSorter(builder, xpathInst, docTypeInfo,
        DummySdk.buildDummySdkPath(sdkVersion));

    sortAndCompare(docUnsorted1, docReference, sorter);
  }

  private static void sortAndCompare(final Document docUnsorted, final Document docReference,
      final NoticeXmlTagSorter sorter) throws SAXException, IOException {

    // Indentation is not required technically speaking but it is much nicer in case of problems.
    final boolean indentXml = true;

    final String textReference = EditorXmlUtils.asText(docReference, indentXml);

    // VALIDATE THE REFERENCE.
    final Optional<Path> mainXsdPathOpt = sorter.getMainXsdPathOpt();
    Validate.isTrue(mainXsdPathOpt.isPresent(), "Expected for SDK 1.6");
    final Path mainXsdPath = mainXsdPathOpt.get();
    final List<SAXParseException> exceptions = XsdValidator.validateXml(textReference, mainXsdPath);
    assertTrue(exceptions.isEmpty());

    final String textBeforeSorting = EditorXmlUtils.asText(docUnsorted, indentXml);
    try {
      // Ensure it would fail if it was not sorted.
      final List<SAXParseException> exceptions2 =
          XsdValidator.validateXml(textBeforeSorting, mainXsdPath);
      assertTrue(!exceptions2.isEmpty());
    } catch (@SuppressWarnings("unused") Exception ex) {
      //
    }

    // Sort it.
    sorter.sortXml(docUnsorted);

    // Sort it again to ensure it is stable.
    sorter.sortXml(docUnsorted);

    final String textUnsortedAfterSort = EditorXmlUtils.asText(docUnsorted, indentXml);

    // Ensure it is sorted by comparing to the reference example.
    if (!textReference.equals(textUnsortedAfterSort)) {
      // Show diff for debugging convenience.
      logger.info(textUnsortedAfterSort);
      logger.info("");
      logger.info("DIFFERENCE: ");
      // logger.info(StringUtils.difference(textUnsortedAfterSort, textPreSorted));
      logger.info(StringUtils.difference(textReference, textUnsortedAfterSort));
    }
    assertEquals(textReference, textUnsortedAfterSort);

    // VALIDATE after sorting.
    final List<SAXParseException> exceptions3 =
        XsdValidator.validateXml(textUnsortedAfterSort, mainXsdPath);
    assertTrue(exceptions3.isEmpty());
    logger.info("Validated notice XML using XSD.");
  }

}
