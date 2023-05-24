package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import org.apache.commons.lang3.Validate;
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

  /**
   * This is a large example that covers many cases. It is best to run tests against large files
   * after first solving problems found on smaller examples.
   */
  public static Document getDummyCan24MaximalReference(final DocumentBuilder builder,
      final SdkVersion sdkVersion) throws SAXException, IOException {
    return parseDoc(builder, sdkVersion, "examples/notices/can_24_maximal.xml");
  }

  /**
   * This is a small example and covers things that most of the other examples do not cover. Tests
   * are usually first performed on smaller examples to first solve basic problems.
   */
  public static Document getDummyX02NoticeReference(final DocumentBuilder builder,
      final SdkVersion sdkVersion) throws SAXException, IOException {
    return parseDoc(builder, sdkVersion, "examples/notices/X02_registration-reference.xml");
  }

  /**
   * This document does not respect the element sort order on purpose and is used to test sorting
   * against a reference.
   */
  public static Document getDummyX02NoticeUnsorted(final DocumentBuilder builder,
      final SdkVersion sdkVersion) throws SAXException, IOException {
    return parseDoc(builder, sdkVersion, "examples/notices/X02_registration-UNSORTED.xml");
  }

  private static File resolveToFile(final SdkVersion sdkVersion, final String path) {
    return buildDummySdkPath(sdkVersion).resolve(path).toFile();
  }

  private static Document parseDoc(final DocumentBuilder builder, final SdkVersion sdkVersion,
      String path) throws SAXException, IOException {
    final File file = resolveToFile(sdkVersion, path);
    Validate.isTrue(file.exists());
    Validate.isTrue(file.isFile());
    return builder.parse(file);
  }
}
