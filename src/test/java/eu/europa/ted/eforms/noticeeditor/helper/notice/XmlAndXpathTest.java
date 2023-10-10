package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Testing the evaluation of XPATH in combination with namespaces.
 */
public class XmlAndXpathTest {
  private static final Logger logger = LoggerFactory.getLogger(XmlAndXpathTest.class);

  @SuppressWarnings("static-method")
  @Test
  public void xpathWithNamespaceTest() throws ParserConfigurationException, IOException {
    testXpathWithNamespaces(new SdkVersion("1.9.0")); // SDK for dummy test files.
  }

  private static void testXpathWithNamespaces(final SdkVersion sdkVersion)
      throws IOException, ParserConfigurationException {
    final DocumentTypeInfo docTypeInfo = DummySdk.getDummyBrinDocTypeInfo(sdkVersion);

    final DocumentBuilder docBuilder =
        SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);
    final Document doc = docBuilder.newDocument();

    // Create a root element.
    final Element rootElement = doc.createElement("BusinessRegistrationInformationNotice");
    doc.appendChild(rootElement);

    // Add some test data.
    final XPath xPathInst = PhysicalModel.setXmlNamespaces(docTypeInfo, rootElement);
    rootElement.appendChild(doc.createElement("ext:UBLExtensions"));
    rootElement.appendChild(doc.createElement("cbc:CustomizationID"));
    logger.debug(EditorXmlUtils.asText(doc, true));

    // This only worked with Saxon, the "JDK only" test failed.
    assertEquals(1, evalLength(xPathInst, rootElement, "ext:UBLExtensions")); // With prefix.
    assertEquals(0, evalLength(xPathInst, rootElement, "UBLExtensions")); // Without prefix.

    assertEquals(1, evalLength(xPathInst, rootElement, "cbc:CustomizationID"));
  }

  private static int evalLength(final XPath xPathInst, final Element elem, final String xpathExpr) {
    return XmlUtils.evaluateXpathAsNodeList(xPathInst, elem, xpathExpr, elem.getTagName())
        .getLength();
  }

  @SuppressWarnings("static-method")
  @Test
  public void testXpathParts() {
    final List<String> list = PhysicalModel.getXpathParts("a/b[x/y]/c");
    assertEquals("a", list.get(0));
    assertEquals("b[x/y]", list.get(1));
    assertEquals("c", list.get(2));
  }

  @SuppressWarnings("static-method")
  @Test
  public void testXpathPartsNoPredicates() {
    final List<String> list = PhysicalModel.getXpathPartsWithoutPredicates("a/b[x/y]/c");
    assertEquals("a", list.get(0));
    assertEquals("b", list.get(1)); // The predicate was removed.
    assertEquals("c", list.get(2));
  }
}
