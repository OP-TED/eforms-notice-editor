package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
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

    // How the test works:

    // 1. There is a reference notice XML document.
    // 2. A copy of this reference was created and tag order was purposefully modified.
    // 3. The test sorts the purposefully modified version and outputs it as indented xml text.
    // 4. The reference is parsed and rewritten as indented xml (same indentation as other xml
    // text).
    // 5. The sorted text is compared to the reference text.
    // 6. The test ensures the unsorted XML is invalid for coherence of the test.
    // 7. Then it validates the sorted XML.

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
    final Path mainXsdPath = sorter.getMainXsdPath();
    validateXml(textReference, mainXsdPath);

    final String textBeforeSorting = EditorXmlUtils.asText(docUnsorted, indentXml);
    try {
      // Ensure it would fail if it was not sorted.
      validateXml(textBeforeSorting, mainXsdPath);
      fail();
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
    validateXml(textUnsortedAfterSort, mainXsdPath);
  }

  private static void validateXml(final String xmlAsText, final Path mainXsdPath)
      throws SAXException, IOException {
    logger.info(String.format("Attempting to validate using schema: %s", mainXsdPath));

    final MyXsdErrorHandler xsdErrorHandler = new MyXsdErrorHandler();

    // validateUsingDom(xmlAsText, xsdErrorHandler);
    validateUsingSchema(xmlAsText, mainXsdPath, xsdErrorHandler);

    // Show exceptions.
    final List<SAXParseException> exceptions = xsdErrorHandler.getExceptions();
    exceptions.forEach(ex -> logger.error(ex.getMessage()));

    assertTrue(exceptions.isEmpty());
    logger.info("Validated notice XML.");
  }

  private static void validateUsingSchema(final String xmlAsText, final Path mainXsdPath,
      final MyXsdErrorHandler xsdErrorHandler)
      throws SAXNotRecognizedException, SAXNotSupportedException, SAXException, IOException {

    final SchemaFactory schemaFactory =
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    // schemaFactory.setResourceResolver(new ResourceResolver);
    schemaFactory.setErrorHandler(xsdErrorHandler);
    schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
    schemaFactory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);

    final Schema schema = schemaFactory.newSchema(mainXsdPath.toFile());
    final Validator validator = schema.newValidator();
    validator.validate(new StreamSource(new StringReader(xmlAsText)));
  }

  private static final class MyXsdErrorHandler implements ErrorHandler {
    private final List<SAXParseException> exceptions = new ArrayList<>();

    public List<SAXParseException> getExceptions() {
      return exceptions;
    }

    @Override
    public void warning(SAXParseException exception) {
      exceptions.add(exception);
    }

    @Override
    public void error(SAXParseException exception) {
      exceptions.add(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) {
      exceptions.add(exception);
    }
  }
}
