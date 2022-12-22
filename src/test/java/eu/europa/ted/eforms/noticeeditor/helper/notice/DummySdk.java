package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.File;
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
import eu.europa.ted.eforms.sdk.SdkVersion;

public class DummySdk {

  public static DocumentTypeInfo getDummyBrinDocTypeInfo(final SdkVersion sdkVersion)
      throws IOException {
    final Map<String, JsonNode> docTypeById = buildDocInfoByType(sdkVersion);
    final DocumentTypeInfo docTypeInfo = new DocumentTypeInfo(docTypeById.get("BRIN"), sdkVersion);
    return docTypeInfo;
  }

  public static Map<String, JsonNode> buildDocInfoByType(final SdkVersion sdkVersion)
      throws IOException {
    final ObjectMapper objectMapper = JsonUtils.getStandardJacksonObjectMapper();
    final JsonNode noticeTypesJson =
        objectMapper.readTree(resolveToFile(sdkVersion, "notice-types/dummy-notice-types.json"));
    final Map<String, JsonNode> docTypeById = XmlWriteService.parseDocumentTypes(noticeTypesJson);
    return docTypeById;
  }

  private static File resolveToFile(final SdkVersion sdkVersion, final String path) {
    return buildDummySdkPath(sdkVersion).resolve(path).toFile();
  }

  public static Document getDummyX02NoticeNoComments(final DocumentBuilder builder,
      final SdkVersion sdkVersion) throws SAXException, IOException {
    final Document doc = builder
        .parse(resolveToFile(sdkVersion, "examples/notices/X02_registration-NO-COMMENTS.xml"));
    return doc;
  }

  public static Document getDummyX02NoticeUnsorted(final DocumentBuilder builder,
      final SdkVersion sdkVersion) throws SAXException, IOException {
    final Document doc =
        builder.parse(resolveToFile(sdkVersion, "examples/notices/X02_registration-UNSORTED.xml"));
    return doc;
  }

  public static Path buildDummySdkPath(final SdkVersion sdkVersion) {
    return Path.of("src/test/resources/dummy-sdk/", sdkVersion.toNormalisedString(true));
  }
}
