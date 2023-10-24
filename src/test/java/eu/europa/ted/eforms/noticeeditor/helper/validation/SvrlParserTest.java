package eu.europa.ted.eforms.noticeeditor.helper.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DummySdk;
import eu.europa.ted.eforms.sdk.SdkVersion;

public class SvrlParserTest {

  @SuppressWarnings("static-method")
  @Test
  public void testParse() throws SAXException, IOException, ParserConfigurationException {
    final Path svrlPath = DummySdk.buildDummySdkPath(new SdkVersion("1.8.0"))
        .resolve("examples/reports/INVALID_cn_24_empty.svrl");

    final Document svrlDoc = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(false)
        .parse(Files.newInputStream(svrlPath));
    svrlDoc.normalize();
    final ObjectNode report = CvsApiClient.createJsonReport(svrlDoc, Optional.empty());

    final JsonNode failedAssertsNode = report.get("failedAsserts");
    assertNotNull(failedAssertsNode);

    final ArrayNode failedAsserts = (ArrayNode) failedAssertsNode;
    assertEquals(23, failedAsserts.size());
  }
}
