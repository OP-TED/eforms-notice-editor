package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

public class XmlAndXpathTest {
  private static final Logger logger = LoggerFactory.getLogger(XmlAndXpathTest.class);

  @SuppressWarnings("static-method")
  @Test
  public void xpathWithNamespaceTest() throws ParserConfigurationException {

    final DocumentBuilder docBuilder =
        SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);
    final Document doc = docBuilder.newDocument();

    // Create a root element.
    final Element rootElement = doc.createElement("BusinessRegistrationInformationNotice");
    doc.appendChild(rootElement);

    // Add some test data.
    final String namespaceUriRoot = "test";
    final XPath xPathInst = NoticeSaver.setXmlNamespaces(namespaceUriRoot, rootElement);
    rootElement.appendChild(doc.createElement("ext:UBLExtensions"));
    rootElement.appendChild(doc.createElement("cbc:CustomizationID"));

    logger.debug(EditorXmlUtils.asText(doc, true));

    // This only worked with Saxon, the "JDK only" test failed.
    assertEquals(1, evalLength(xPathInst, rootElement, "ext:UBLExtensions")); // With prefix.
    assertEquals(0, evalLength(xPathInst, rootElement, "UBLExtensions")); // Without prefix.

    assertEquals(1, evalLength(xPathInst, rootElement, "cbc:CustomizationID"));
  }

  private static int evalLength(final XPath xPathInst, final Element elem, final String xpathExpr) {
    return NoticeSaver.evaluateXpath(xPathInst, elem, xpathExpr, elem.getTagName()).getLength();
  }
}
