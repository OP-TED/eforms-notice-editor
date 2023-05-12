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

/**
 * Provides easy access to dummy data for tests.
 */
public class DummySdk {

  public static Map<String, JsonNode> buildDocInfoByType(final SdkVersion sdkVersion)
      throws IOException {
    final ObjectMapper objectMapper = JsonUtils.getStandardJacksonObjectMapper();
    final JsonNode noticeTypesJson =
        objectMapper.readTree(resolveToFile(sdkVersion, "notice-types/dummy-notice-types.json"));
    final Map<String, JsonNode> docTypeById = XmlWriteService.parseDocumentTypes(noticeTypesJson);
    return docTypeById;
  }

  public static Path buildDummySdkPath(final SdkVersion sdkVersion) {
    return Path.of("src/test/resources/dummy-sdk/", sdkVersion.toNormalisedString(true));
  }

  public static DocumentTypeInfo getDummyBrinDocTypeInfo(final SdkVersion sdkVersion)
      throws IOException {
    final Map<String, JsonNode> docTypeById = buildDocInfoByType(sdkVersion);
    final DocumentTypeInfo docTypeInfo = new DocumentTypeInfo(docTypeById.get("BRIN"), sdkVersion);
    return docTypeInfo;
  }

  public static Document getDummyX02NoticeReference(final DocumentBuilder builder,
      final SdkVersion sdkVersion) throws SAXException, IOException {
    final File file = resolveToFileX02Reference(sdkVersion);
    return builder.parse(file);
  }

  /**
   * This document does not respect the element sort order on purpose.
   */
  public static Document getDummyX02NoticeUnsorted(final DocumentBuilder builder,
      final SdkVersion sdkVersion) throws SAXException, IOException {
    final File file = getDummyX02Unsorted(sdkVersion);
    return builder.parse(file);
  }

  private static File getDummyX02Unsorted(final SdkVersion sdkVersion) {
    return resolveToFile(sdkVersion, "examples/notices/X02_registration-UNSORTED.xml");
  }

  private static File resolveToFile(final SdkVersion sdkVersion, final String path) {
    return buildDummySdkPath(sdkVersion).resolve(path).toFile();
  }

  private static File resolveToFileX02Reference(final SdkVersion sdkVersion) {
    return resolveToFile(sdkVersion, "examples/notices/X02_registration-reference.xml");
  }
}
