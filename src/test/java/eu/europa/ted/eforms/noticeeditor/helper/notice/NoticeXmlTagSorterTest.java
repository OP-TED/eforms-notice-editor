package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.sorting.NoticeXmlTagSorter;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.XpathUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

public class NoticeXmlTagSorterTest {

  private static final String JAXP_SCHEMA_LANGUAGE =
      "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

  @SuppressWarnings("static-method")
  @Test
  public void test() throws IOException, ParserConfigurationException, SAXException {

    // The configured document builder is reusable.
    final DocumentBuilder builder = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

    // Get document type info from the SDK. It contains information about XML namespaces.
    final SdkVersion sdkVersion = new SdkVersion("1.6.0");
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo(sdkVersion);

    // The xpath instance namespace aware and reusable.
    final XPath xpathInst = XpathUtils.setupXpathInst(docTypeInfo, Optional.empty());

    final Document docUnsorted = DummySdk.getDummyX02NoticeNoComments(builder, sdkVersion);

    // SORT.
    final NoticeXmlTagSorter sorter = new NoticeXmlTagSorter(builder, xpathInst, docTypeInfo,
        DummySdk.buildDummySdkPath(sdkVersion));

    sortAndCompare(sdkVersion, builder, docUnsorted, sorter);

    // Call it one more time to ensure it is stable.
    sortAndCompare(sdkVersion, builder, docUnsorted, sorter);
  }

  private static void sortAndCompare(final SdkVersion sdkVersion, final DocumentBuilder builder,
      final Document docUnsorted, final NoticeXmlTagSorter sorter)
      throws SAXException, IOException, ParserConfigurationException {

    final boolean indentXml = true;

    final Document docPreSorted = DummySdk.getDummyX02NoticeNoComments(builder, sdkVersion);
    final String textPreSorted = EditorXmlUtils.asText(docPreSorted, indentXml);

    // VALIDATE THE REFERENCE.
    final Path mainXsdPath = sorter.getMainXsdPath();
    validateXml(textPreSorted, mainXsdPath);

    final String textBeforeSorting = EditorXmlUtils.asText(docUnsorted, indentXml);
    sorter.sortXml(docUnsorted);

    final String textUnsortedAfterSort = EditorXmlUtils.asText(docUnsorted, indentXml);
    System.out.println(textUnsortedAfterSort);

    // CHECK IT IS SORTED.
    if (!textPreSorted.equals(textUnsortedAfterSort)) {
      // Show diff for debugging convenience.
      System.out.println("");
      System.out.println("DIFFERENCE: ");
      System.out.println(StringUtils.difference(textUnsortedAfterSort, textPreSorted));
      System.out.println(StringUtils.difference(textPreSorted, textUnsortedAfterSort));
    }
    assertEquals(textPreSorted, textUnsortedAfterSort);

    // TODO this should not pass.
    validateXml(textBeforeSorting, mainXsdPath);

    // VALIDATE after sorting.
    validateXml(textUnsortedAfterSort, mainXsdPath);


  }

  private static void validateXml(final String xmlAsText, final Path mainXsdPath)
      throws ParserConfigurationException, SAXException, IOException {
    System.out.println(String.format("Attempting to validate using schema: %s", mainXsdPath));

    // https://docs.oracle.com/javase/tutorial/jaxp/dom/validating.html
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
    dbf.setIgnoringElementContentWhitespace(true);
    dbf.setNamespaceAware(true); // Support XML namespaces.
    dbf.setValidating(true); // Use XSD validation.
    try {
      dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
    } catch (IllegalArgumentException ex) {
      System.err.println(
          "Error: JAXP DocumentBuilderFactory attribute not recognized: " + JAXP_SCHEMA_LANGUAGE);
      System.err.println("Check to see if parser conforms to JAXP spec.");
      throw ex;
    }

    final DocumentBuilder builder = dbf.newDocumentBuilder();
    builder.setErrorHandler(new MyErrorHandler());

    final byte[] xmlAsUtf8Bytes = xmlAsText.getBytes(StandardCharsets.UTF_8);
    try (final InputStream in = new ByteArrayInputStream(xmlAsUtf8Bytes)) {
      // Validate the XML as TEXT.
      // builder.parse(in);
    }

    final SchemaFactory schemaFactory =
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    // schemaFactory.setResourceResolver(new ResourceResolver);
    final Schema schema = schemaFactory.newSchema(mainXsdPath.toFile());
    Validator validator = schema.newValidator();
    validator.validate(new StreamSource(new StringReader(xmlAsText)));

    System.out.println("Validated notice XML.");
  }

  private static final class MyErrorHandler implements ErrorHandler {
    @Override
    public void warning(SAXParseException e) throws SAXException {
      System.err.println(e.getMessage());
      throw e;
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      System.err.println(e.getMessage());
      throw e;
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      System.err.println(e.getMessage());
      throw e;
    }
  }
}
