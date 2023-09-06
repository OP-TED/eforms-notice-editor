package eu.europa.ted.eforms.noticeeditor.helper.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

@SuppressWarnings("static-method")
public class SvrlParserTest {

  private static final SdkVersion SDK_VERSION = new SdkVersion("1.9.0");

  @Test
  public void testParseCn24Empty() throws SAXException, IOException, ParserConfigurationException {
    final SdkVersion sdkVersion = SDK_VERSION;
    final Path svrlPath = DummySdk.buildDummySdkPath(sdkVersion)
        .resolve("examples/reports/INVALID_cn_24_empty.svrl");

    final Document svrlDoc = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(false)
        .parse(Files.newInputStream(svrlPath));
    svrlDoc.normalize();
    final Map<String, String> translationsForLang = new HashMap<>();

    // Put one so that we can test this.
    final String labelId = "rule|text|ND-ProcedureTenderingProcess-16";
    final String dummyTranslation = "A dummy translation";
    translationsForLang.put(labelId, dummyTranslation);
    final ObjectNode report =
        CvsApiClient.createJsonReport(svrlDoc, "en", Optional.empty(), sdkVersion,
            translationsForLang);

    final JsonNode failedAssertsNode = report.get("failedAsserts");
    assertNotNull(failedAssertsNode);

    final ArrayNode failedAsserts = (ArrayNode) failedAssertsNode;
    assertEquals(22, failedAsserts.size());

    // Find one of the known items by node id.
    final List<JsonNode> list = JsonUtils.getList(failedAsserts);
    final String nodeId = "ND-ProcedureTenderingProcess";
    final Optional<JsonNode> first = findFirstItemByNodeId(list, nodeId);
    assertTrue(first.isPresent());
    if (first.isPresent()) {
      final JsonNode reportItem = first.get();
      final String translation = JsonUtils.getTextStrict(reportItem, "translation");
      assertEquals(dummyTranslation, translation);
    }
  }

  @Test
  public void testParseCn24Multiple()
      throws SAXException, IOException, ParserConfigurationException {
    final SdkVersion sdkVersion = SDK_VERSION;
    final Path svrlPath = DummySdk.buildDummySdkPath(sdkVersion)
        .resolve("examples/reports/INVALID_cn_24_multiple.svrl");

    final Document svrlDoc = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(false)
        .parse(Files.newInputStream(svrlPath));
    svrlDoc.normalize();

    final Map<String, String> translationsForLang = new HashMap<>();

    // Put one so that we can test this.
    final ObjectNode report =
        CvsApiClient.createJsonReport(svrlDoc, "en", Optional.empty(), sdkVersion,
            translationsForLang);

    final JsonNode failedAssertsNode = report.get("failedAsserts");
    assertNotNull(failedAssertsNode);

    final ArrayNode failedAsserts = (ArrayNode) failedAssertsNode;
    assertEquals(16, failedAsserts.size());

    // Check the location array.
    final List<JsonNode> list = JsonUtils.getList(failedAsserts);
    final Optional<JsonNode> first = findFirstItemById(list, "BR-BT-00026-0672");
    assertTrue(first.isPresent());
    if (first.isPresent()) {
      final JsonNode reportItem = first.get();
      final List<JsonNode> location = JsonUtils.getList(reportItem.get("location"));
      // location="/cn:ContractNotice/cac:ProcurementProjectLot[1]/cac:ProcurementProject/cac:MainCommodityClassification[2]"
      assertEquals("cn:ContractNotice", JsonUtils.getTextStrict(location.get(0), "xml"));

      assertEquals("cac:ProcurementProjectLot", JsonUtils.getTextStrict(location.get(1), "xml"));
      assertEquals(1, JsonUtils.getIntStrict(location.get(1), "instance"));

      assertEquals("cac:ProcurementProject", JsonUtils.getTextStrict(location.get(2), "xml"));

      assertEquals("cac:MainCommodityClassification",
          JsonUtils.getTextStrict(location.get(3), "xml"));
      assertEquals(2, JsonUtils.getIntStrict(location.get(3), "instance"));
    }
  }

  private static Optional<JsonNode> findFirstItemByNodeId(final List<JsonNode> list,
      final String nodeId) {
    return list.stream().filter(
        item -> {
          final Optional<String> nodeIdOpt = JsonUtils.getTextOpt(item, "nodeId");
          return nodeIdOpt.isEmpty() ? false : nodeIdOpt.get().equals(nodeId);
        }).findFirst();
  }

  private static Optional<JsonNode> findFirstItemById(final List<JsonNode> list,
      final String id) {
    return list.stream().filter(
        item -> {
          final Optional<String> idOpt = JsonUtils.getTextOpt(item, "id");
          return idOpt.isEmpty() ? false : idOpt.get().equals(id);
        }).findFirst();
  }
}
