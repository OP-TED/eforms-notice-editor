package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ted.eforms.noticeeditor.service.XmlWriteService;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

public class DummySdk {

  private static final String DUMMY_SDK_ROOT = "src/test/resources/dummy-sdk/";

  public static DocumentTypeInfo getDummyBrinDocTypeInfo() throws IOException {
    final Map<String, JsonNode> docTypeById = buildDocInfoByType();
    final DocumentTypeInfo docTypeInfo = new DocumentTypeInfo(docTypeById.get("BRIN"));
    return docTypeInfo;
  }

  public static Map<String, JsonNode> buildDocInfoByType() throws IOException {
    final ObjectMapper objectMapper = JsonUtils.getStandardJacksonObjectMapper();
    final JsonNode noticeTypesJson = objectMapper
        .readTree(Path.of(DUMMY_SDK_ROOT, "notice-types/dummy-notice-types.json").toFile());
    Map<String, JsonNode> docTypeById = XmlWriteService.parseDocumentTypes(noticeTypesJson);
    return docTypeById;
  }

  public static Document getDummyX02NoticeNoComments(final DocumentBuilder builder)
      throws SAXException, IOException {
    final Document doc = builder.parse(
        Path.of(DUMMY_SDK_ROOT, "examples/notices/X02_registration-NO-COMMENTS.xml").toFile());
    return doc;
  }

  public static Document getDummyX02NoticeUnsorted(final DocumentBuilder builder)
      throws SAXException, IOException {
    final Document doc = builder
        .parse(Path.of(DUMMY_SDK_ROOT, "examples/notices/X02_registration-UNSORTED.xml").toFile());
    return doc;
  }

  public static Document getSdkXml(final DocumentBuilder builder,
      final String locationRelativeToDummySdk) throws SAXException, IOException {
    final Document doc =
        builder.parse(Path.of(DUMMY_SDK_ROOT, locationRelativeToDummySdk).toFile());
    return doc;
  }

  public static Path getDummySdkRoot() {
    return Path.of(DUMMY_SDK_ROOT);
  }
}
