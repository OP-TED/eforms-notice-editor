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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.helper.validation.XsdValidator;
import eu.europa.ted.eforms.noticeeditor.service.XmlWriteService;
import eu.europa.ted.eforms.noticeeditor.sorting.NoticeXmlTagSorter;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.JavaTools;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.XpathUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;


@SpringBootTest
public class NoticeXmlTagSorterTest {

  private static final Logger logger = LoggerFactory.getLogger(NoticeXmlTagSorterTest.class);

  /**
   * NOTE: just changing this is not enough, the dummy examples must also be adapted for some tests.
   */
  private static final SdkVersion SDK_VERSION = new SdkVersion("1.8.0");

  @Autowired
  private XmlWriteService xmlWriteService;

  @Test
  public void testXmlSortingAndValidateSmallX02Xml()
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

    // NOTE: There are some issues with formatting / identation after sorting compared to the
    // reference. For the moment the test reference data is modified to avoid problems related to
    // formatting or indentation as the focus is on the order of the elements.

    // The configured document builder is reusable.
    final DocumentBuilder builder = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

    // Get document type info from the SDK. It contains information about XML namespaces.
    final SdkVersion sdkVersion = SDK_VERSION;
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo(sdkVersion);

    // The xpath instance namespace aware and reusable.
    final XPath xpathInst = XpathUtils.setupXpathInst(docTypeInfo, Optional.empty());

    final Document docReference = DummySdk.getDummyX02NoticeReference(builder, sdkVersion);
    final Document docUnsorted1 = DummySdk.getDummyX02NoticeUnsorted(builder, sdkVersion);

    sortAndCompare(sdkVersion, builder, xpathInst, docTypeInfo, docUnsorted1, docReference, true);
  }

  @Test
  public void testXmlSortingAndValidateT01PrtXml()
      throws IOException, ParserConfigurationException, SAXException {

    // The configured document builder is reusable.
    final DocumentBuilder builder = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

    // Get document type info from the SDK. It contains information about XML namespaces.
    final SdkVersion sdkVersion = SDK_VERSION;
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo(sdkVersion);

    // The xpath instance namespace aware and reusable.
    final XPath xpathInst = XpathUtils.setupXpathInst(docTypeInfo, Optional.empty());

    final Document docReference = DummySdk.getDummyT01PrtReference(builder, sdkVersion);
    XmlUtils.removeXmlComments(xpathInst, docReference);

    // Get it again, we will sort it and compare to the reference later.
    final Document docUnsorted1 = DummySdk.getDummyT01PrtReference(builder, sdkVersion);
    XmlUtils.removeXmlComments(xpathInst, docUnsorted1);

    sortAndCompare(sdkVersion, builder, xpathInst, docTypeInfo, docUnsorted1, docReference, false);
  }

  @Test
  public void testXmlSortingAndValidateContractModificationXml()
      throws IOException, ParserConfigurationException, SAXException {

    // The configured document builder is reusable.
    final DocumentBuilder builder = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

    // Get document type info from the SDK. It contains information about XML namespaces.
    final SdkVersion sdkVersion = SDK_VERSION;
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo(sdkVersion);

    // The xpath instance namespace aware and reusable.
    final XPath xpathInst = XpathUtils.setupXpathInst(docTypeInfo, Optional.empty());

    final Document docReference = DummySdk.getDummyCanModif24Reference(builder, sdkVersion);
    XmlUtils.removeXmlComments(xpathInst, docReference);

    // Get it again, we will sort it and compare to the reference later.
    final Document docUnsorted1 = DummySdk.getDummyCanModif24Reference(builder, sdkVersion);
    XmlUtils.removeXmlComments(xpathInst, docUnsorted1);

    sortAndCompare(sdkVersion, builder, xpathInst, docTypeInfo, docUnsorted1, docReference, false);
  }

  @Test
  public void testXmlSortingAndValidateLargeXml()
      throws IOException, ParserConfigurationException, SAXException {

    // The configured document builder is reusable.
    final DocumentBuilder builder = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

    // Get document type info from the SDK. It contains information about XML namespaces.
    final SdkVersion sdkVersion = SDK_VERSION;
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo(sdkVersion);

    // The xpath instance namespace aware and reusable.
    final XPath xpathInst = XpathUtils.setupXpathInst(docTypeInfo, Optional.empty());

    final Document docReference = DummySdk.getDummyCan24MaximalReference(builder, sdkVersion);
    XmlUtils.removeXmlComments(xpathInst, docReference);

    // Get it again, we will sort it and compare to the reference later.
    final Document docUnsorted1 = DummySdk.getDummyCan24MaximalReference(builder, sdkVersion);
    XmlUtils.removeXmlComments(xpathInst, docUnsorted1);

    sortAndCompare(sdkVersion, builder, xpathInst, docTypeInfo, docUnsorted1, docReference, false);
  }

  private void sortAndCompare(final SdkVersion sdkVersion, final DocumentBuilder builder,
      final XPath xpathInst,
      final DocumentTypeInfo docTypeInfo,
      final Document docUnsorted,
      final Document docReference,
      final boolean validate) throws SAXException, IOException {

    final FieldsAndNodes fieldsAndNodes = xmlWriteService.readFieldsAndNodes(sdkVersion);
    final Path pathToSpecificSdk = DummySdk.buildDummySdkPath(sdkVersion);
    final NoticeXmlTagSorter sorter = new NoticeXmlTagSorter(builder, xpathInst, docTypeInfo,
        pathToSpecificSdk, fieldsAndNodes);

    // Indentation is not required technically speaking but it is much nicer in case of problems.
    final boolean indentXml = true;

    final String textReference = EditorXmlUtils.asText(docReference, indentXml);

    // VALIDATE THE REFERENCE.
    final Optional<Path> mainXsdPathOpt = sorter.getMainXsdPathOpt();
    Validate.isTrue(mainXsdPathOpt.isPresent(), "Expected for SDK 1.8");
    final Path mainXsdPath = mainXsdPathOpt.get();

    validateXml(validate, mainXsdPath, textReference);

    final String textBeforeSorting = EditorXmlUtils.asText(docUnsorted, indentXml);
    validateXmlIsInvalid(validate, mainXsdPath, textBeforeSorting);

    // Sort it.
    sorter.sortXml(docUnsorted);

    // Sort it again to ensure it is stable.
    sorter.sortXml(docUnsorted);

    final String textUnsortedAfterSort = EditorXmlUtils.asText(docUnsorted, indentXml);

    // Ensure it is sorted by comparing to the reference example.
    if (!textReference.equals(textUnsortedAfterSort)) {
      logger.info("Reference length: {}", textReference.length());
      logger.info("Output    length: {}", textUnsortedAfterSort.length());
      // Show diff for debugging convenience.
      logger.info(textUnsortedAfterSort);

      logger.info("");
      logger.info("DIFFERENCE: ");
      // logger.info(StringUtils.difference(textUnsortedAfterSort, textPreSorted));
      logger.info(StringUtils.difference(textReference, textUnsortedAfterSort));

      // Write both files to target as this allows to quickly diff them (in the IDE for example).
      JavaTools.writeTextFile(Path.of("target", "dummy-1-reference.xml"), textReference);
      JavaTools.writeTextFile(Path.of("target", "dummy-2-after-sort.xml"), textUnsortedAfterSort);
    }
    assertEquals(textReference, textUnsortedAfterSort);

    // VALIDATE after sorting.
    validateXml(validate, mainXsdPath, textUnsortedAfterSort);
  }

  private static void validateXml(final boolean validate, final Path mainXsdPath,
      final String xmlText) throws SAXException, IOException, SAXParseException {
    if (!validate) {
      return;
    }
    try {
      final List<SAXParseException> exceptions =
          XsdValidator.validateXml(xmlText, mainXsdPath);
      assertTrue(exceptions.isEmpty());
      logger.info("Validated notice XML using XSD.");
    } catch (final SAXParseException ex) {
      logger.error(ex.toString(), ex);
      logger.error(String.format("xmlText=%s", xmlText));
      throw ex;
    }
  }

  private static void validateXmlIsInvalid(final boolean validate, final Path mainXsdPath,
      final String xmlText) throws SAXException, IOException, SAXParseException {
    if (!validate) {
      return;
    }
    try {
      final List<SAXParseException> exceptions =
          XsdValidator.validateXml(xmlText, mainXsdPath);
      assertTrue(!exceptions.isEmpty());
    } catch (@SuppressWarnings("unused") final SAXParseException ex) {
      //
    }
  }

}
