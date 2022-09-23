package eu.europa.ted.eforms.noticeeditor.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.helger.genericode.v10.CodeListDocument;
import com.helger.genericode.v10.Column;
import com.helger.genericode.v10.Row;
import com.helger.genericode.v10.Value;
import eu.europa.ted.eforms.noticeeditor.EformsNoticeEditorApp;
import eu.europa.ted.eforms.noticeeditor.domain.Language;
import eu.europa.ted.eforms.noticeeditor.genericode.CustomGenericodeMarshaller;
import eu.europa.ted.eforms.noticeeditor.genericode.GenericodeTools;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.util.IntuitiveStringComparator;
import eu.europa.ted.eforms.noticeeditor.util.JavaTools;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;


/**
 * Reads and serves data from the SDK.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS", justification = "Checked to Runtime OK here")
public class SdkService {

  private static final Logger logger = LoggerFactory.getLogger(SdkService.class);

  private static final String EFORMS_SDKS_DIR = "eforms-sdks";

  /**
   * The number of seconds in one hour.
   */
  private static final int SECONDS_IN_ONE_HOUR = 3600;

  /**
   * This depends on the needs of your application and how often you update the SDK.
   */
  public static final int CACHE_MAX_AGE_SECONDS = SECONDS_IN_ONE_HOUR * 2;

  /**
   * SECURITY: this regex is security related, so be careful if you decide to change it. This is
   * security related as the sdkVersion is used in file system folder related operations.
   */
  private static final Pattern REGEX_SDK_VERSION =
      Pattern.compile("\\p{Digit}{1,2}\\.\\p{Digit}{1,2}\\.\\p{Digit}{1,2}");

  public static String buildJsonFromCodelistGc(final String codeListId, final String pathStr,
      final String langCode) throws IOException {
    // Use GC Helger lib to load SDK .gc file.
    final CustomGenericodeMarshaller marshaller = GenericodeTools.getMarshaller();
    try (InputStream is = Files.newInputStream(Path.of(pathStr))) {

      // Transform the XML to Java objects.
      final CodeListDocument gcDoc = GenericodeTools.parseGenericode(is, marshaller);

      final String shortName = gcDoc.getIdentification().getShortNameValue();
      final String longName = gcDoc.getIdentification().getLongNameAtIndex(0).getValue();

      final ObjectMapper jsonMapper = JsonUtils.getStandardJacksonObjectMapper();
      final ObjectNode jsonCodelist = jsonMapper.createObjectNode();

      // By convention of the SDK the longname is the codelist identifier.
      jsonCodelist.put("id", longName);

      // This could be used in the UI for display purposes.
      jsonCodelist.put("longName", longName);
      jsonCodelist.put("shortName", shortName);

      final ArrayNode jsonRows = jsonCodelist.putArray("codes");

      // Example: "en "to "eng_label"
      final Language desiredLang = Language.valueOfFromLocale(langCode);
      final String genericodeLang = desiredLang.getGenericodeLanguage();

      final List<Row> gcRows = gcDoc.getSimpleCodeList().getRow();
      for (final Row gcRow : gcRows) {

        final List<Value> gcRowValues = gcRow.getValue();
        final Optional<Value> technicalCodeValOpt = gcFindFirstColumnRef(gcRowValues, "code");

        final String technicalCode;
        if (technicalCodeValOpt.isPresent()) {
          technicalCode = technicalCodeValOpt.get().getSimpleValueValue();
          Validate.notBlank("technicalCode is blank for codeListId=%s", codeListId);

          // Get desired language first, fallback to eng.
          final Optional<Value> desiredLabelOpt = gcFindFirstColumnRef(gcRowValues, genericodeLang);
          if (desiredLabelOpt.isPresent()) {
            putCodeValueAndLangCode(langCode, jsonMapper, jsonRows, technicalCode, desiredLabelOpt);
          } else {
            final Optional<Value> englishLabelOpt = gcFindFirstColumnRef(gcRowValues, "eng_label");
            if (englishLabelOpt.isPresent()) {
              putCodeValueAndLangCode(langCode, jsonMapper, jsonRows, technicalCode,
                  englishLabelOpt);
            } else {
              // Just take the Name and assume it is in english.
              final Optional<Value> nameLabelOpt = gcFindFirstColumnRef(gcRowValues, "Name");
              if (nameLabelOpt.isPresent()) {
                putCodeValueAndLangCode(langCode, jsonMapper, jsonRows, technicalCode,
                    nameLabelOpt);
              }
            }
          }
        }
      }

      // Generate JSON.
      return JsonUtils.marshall(jsonCodelist);
    }
  }

  /**
   * @param sdkDir The directory where all SDKs reside
   * @param sdkVersion The desired SDK version, this may come from the front end and will be
   *        validated (security)
   * @param sdkRelativePathStr A path relative to the sdkDir
   * @return The path to the file
   */
  public static String buildPathToSdk(final String sdkDir, final String sdkVersion,
      final String sdkRelativePathStr) {
    securityValidateSdkVersionFormatThrows(sdkVersion);
    return String.format("%s/%s/%s", sdkDir, sdkVersion, sdkRelativePathStr);
  }

  private static Optional<Value> gcFindFirstColumnRef(final Collection<Value> gcRowValues,
      final String columnRefText) {
    return gcRowValues.stream()//
        .filter(v -> ((Column) v.getColumnRef()).getId().equals(columnRefText))//
        .findFirst();
  }

  private static void putCodeValueAndLangCode(final String langCode, final ObjectMapper jsonMapper,
      final ArrayNode jsonRows, final String technicalCode, final Optional<Value> englishLabelOpt) {
    final String englishText = englishLabelOpt.get().getSimpleValueValue();
    final ObjectNode jsonRow = jsonMapper.createObjectNode();
    jsonRows.add(jsonRow);
    jsonRow.put("codeValue", technicalCode.strip());
    jsonRow.put(langCode, englishText.strip());
  }

  /**
   * Validates the format of the sdk version string because the sdk version may also be a folder
   * name.
   *
   * @return true if valid, else false
   */
  static boolean securityValidateSdkVersionFormat(final String sdkVersion) {
    if (StringUtils.isBlank(sdkVersion)) {
      return false;
    }
    return REGEX_SDK_VERSION.matcher(sdkVersion).matches();
  }

  /**
   * Throws a runtime exception if the sdkVersion does not match the expected format.
   */
  public static void securityValidateSdkVersionFormatThrows(final String sdkVersion) {
    if (!securityValidateSdkVersionFormat(sdkVersion)) {
      throw new RuntimeException(String.format("Invalid SDK version format: %s", sdkVersion));
    }
  }

  public static Map<String, Object> getHomePageInfo() {
    final Map<String, Object> map = new LinkedHashMap<>();
    final Instant now = Instant.now();
    final String instantNowIso8601Str = now.toString();
    logger.info("Fetching home info: {}", instantNowIso8601Str);

    // This will be used to display the version of the editor application so that users can include
    // this version when reporting a bug.
    map.put("appVersion", EformsNoticeEditorApp.APP_VERSION);

    try {
      // Look at existing SDK folders in order to list them in the UI.
      // If a version should be excluded, this can be done here.
      final List<String> availableSdkVersions = JavaTools.listFolders(EFORMS_SDKS_DIR);
      availableSdkVersions.sort(new IntuitiveStringComparator<String>());
      Collections.reverse(availableSdkVersions);
      map.put("sdkVersions", availableSdkVersions);

    } catch (final IOException e) {
      logger.error(e.toString(), e);
      throw new RuntimeException(e);
    }

    logger.info("Fetching home info: DONE");
    return map;
  }

  /**
   * Dynamically get the available notice sub types from the given SDK.
   */
  public static Map<String, Object> getNoticeSubTypes(final String sdkVersion) {
    securityValidateSdkVersionFormatThrows(sdkVersion);

    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("sdkVersion", sdkVersion);
    try {
      final List<String> availableNoticeTypes =
          JavaTools.listFiles(EFORMS_SDKS_DIR + "/" + sdkVersion + "/notice-types/");

      final List<String> noticeTypes = availableNoticeTypes.stream()//
          // Remove some files.
          .filter(filename -> filename.endsWith(".json") && !"notice-types.json".equals(filename))//
          // Remove extension.
          .map(filename -> filename.substring(0, filename.lastIndexOf('.')))//
          .collect(Collectors.toList());

      noticeTypes.sort(new IntuitiveStringComparator<String>());
      map.put("noticeTypes", noticeTypes);

    } catch (final IOException e) {
      logger.error(e.toString(), e);
    }
    logger.info("Fetching main info: DONE");
    return map;
  }

  /**
   * Serve an SDK codelist information as JSON.
   */
  public static String serveCodelistAsJson(final String sdkVersion, final String codeListId,
      final String langCode, final HttpServletResponse response) throws IOException {
    final String pathStr = SdkService.buildPathToSdk(EFORMS_SDKS_DIR, sdkVersion,
        String.format("codelists/%s.gc", codeListId));

    // As the SDK and other details are inside the url this data can be cached for a while.
    SdkService.setResponseCacheControl(response, SdkService.CACHE_MAX_AGE_SECONDS);

    return SdkService.buildJsonFromCodelistGc(codeListId, pathStr, langCode);
  }

  /**
   * Serves the specified JSON file as download.
   *
   * @param response The HTTP response to serve the download to
   * @param pathStr Path of the folder which contains the file
   * @param filenameForDownload The filename to set in the headers
   * @param isAsDownload Serve as attachement or not
   * @throws IOException If a problem occurs during flush buffer
   */
  private static void serveJsonFile(final HttpServletResponse response, final String pathStr,
      final String filenameForDownload, final boolean isAsDownload) throws IOException {
    Validate.notBlank(filenameForDownload, "filenameForDownload is blank");
    // ---------------------------------
    // THE FILE IS SMALL, JUST COPY IT.
    // ---------------------------------
    try (InputStream is = Files.newInputStream(Path.of(pathStr))) {
      if (is == null) {
        throw new RuntimeException(String.format("InputStream is null for %s", pathStr));
      }
      // Indicate the content type and encoding BEFORE writing to output.
      response.setContentType("application/json");
      response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
      // setGzipResponse(response);

      if (isAsDownload) {
        response.setHeader("Content-Disposition",
            String.format("attachment; filename=\"%s\"", filenameForDownload));
      }

      // Write response content.
      org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
      response.flushBuffer();

    } catch (IOException ex) {
      logger.info("Error responding with file '{}' for download.", pathStr, ex);
      throw new RuntimeException("IOException writing file to output stream.", ex);
    }
  }

  /**
   * Serves the specified json string as download.
   *
   * @param response The HTTP response to serve the download to
   * @param jsonString The JSON string to serve
   * @param filenameForDownload The filename to set in the headers
   * @param isAsDownload Serve as attachement or not
   * @throws IOException If a problem occurs during flush buffer
   */
  private static void serveJsonString(final HttpServletResponse response, final String jsonString,
      final String filenameForDownload, final boolean isAsDownload) throws IOException {
    Validate.notBlank(jsonString, "jsonString is blank");
    Validate.notBlank(filenameForDownload, "filenameForDownload is blank");
    // ---------------------------------
    // THE FILE IS SMALL, JUST COPY IT.
    // ---------------------------------
    final Charset utf8 = StandardCharsets.UTF_8;
    try (InputStream is = IOUtils.toInputStream(jsonString, utf8)) {
      if (is == null) {
        throw new RuntimeException(
            String.format("InputStream is null for %s", filenameForDownload));
      }
      // Indicate the content type and encoding BEFORE writing to output.
      response.setContentType("application/json");
      response.setCharacterEncoding(utf8.toString());
      // setGzipResponse(response);

      if (isAsDownload) {
        response.setHeader("Content-Disposition",
            String.format("attachment; filename=\"%s\"", filenameForDownload));
      }

      // Write response content.
      org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
      response.flushBuffer();

    } catch (IOException ex) {
      logger.info("Error responding with file '{}' for download.", filenameForDownload, ex);
      throw new RuntimeException("IOException writing json string to output stream.", ex);
    }
  }

  /**
   * Common SDK folder logic for reading files.
   */
  public static void serveSdkJsonFile(final HttpServletResponse response, final String sdkVersion,
      final String sdkRelativePathStr, final String filenameForDownload) {
    Validate.notBlank(sdkVersion, "sdkVersion is blank");
    try {
      final String pathStr = buildPathToSdk(EFORMS_SDKS_DIR, sdkVersion, sdkRelativePathStr);

      // As the sdkVersion and other details are in the url this can be cached for a while.
      setResponseCacheControl(response, CACHE_MAX_AGE_SECONDS);
      serveJsonFile(response, pathStr, filenameForDownload, false);

    } catch (Exception ex) {
      logger.error(ex.toString(), ex);
      throw new RuntimeException(
          String.format("Exception serving JSON file %s", filenameForDownload), ex);
    }
  }

  /**
   * Common SDK JSON string logic.
   */
  public static void serveSdkJsonString(final HttpServletResponse response, final String jsonStr,
      final String filenameForDownload) {
    Validate.notBlank(jsonStr, "jsonStr is blank");
    try {

      // As the sdkVersion and other details are in the url this can be cached for a while.
      setResponseCacheControl(response, CACHE_MAX_AGE_SECONDS);
      serveJsonString(response, jsonStr, filenameForDownload, false);

    } catch (Exception ex) {
      logger.error(ex.toString(), ex);
      throw new RuntimeException(
          String.format("Exception serving JSON file %s", filenameForDownload), ex);
    }
  }

  /**
   * Reads an SDK translation file for given SDK and language. The files are in XML format but they
   * will be converted to JSON.
   *
   * @return Map of the labels by id
   */
  public static Map<String, String> getTranslations(final String sdkVersion,
      final String labelAssetType, final String langCode)
      throws ParserConfigurationException, SAXException, IOException {
    // SECURITY: Do not inject the passed language directly into a string that goes to the file
    // system. We use our internal enum as a whitelist.
    final Language lang = Language.valueOfFromLocale(langCode);
    final String filenameForDownload =
        String.format("%s_%s.xml", labelAssetType, lang.getLocale().getLanguage());

    final String sdkRelativePathStr = String.format("translations/%s", filenameForDownload);
    final String pathStr = buildPathToSdk(EFORMS_SDKS_DIR, sdkVersion, sdkRelativePathStr);
    final Path path = Path.of(pathStr);

    // Example:
    // <?xml version="1.0" encoding="UTF-8"?>
    // <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
    // <properties>
    // <entry key="field|name|BT-01(c)-Procedure">Procedure Legal Basis (ELI - celex)</entry>
    // ...

    // Parse the XML, build a map of text by id.
    final DocumentBuilder db = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype();
    final File file = path.toFile();

    // NOTE: the file may not exist if there are no translations yet.
    if (lang != Language.EN && !file.exists()) {
      // The best fallback is to respond as if the file was there but with empty values.
      // The keys (labelIds) are the same in english, we are only missing the values.
      // Take the english file to get the keys and leave the the translations empty.
      logger.warn("File does not exist: " + file.getName());
      final Map<String, String> fallbackMap = getTranslations(sdkVersion, labelAssetType, "en");
      for (final Entry<String, String> entry : fallbackMap.entrySet()) {
        entry.setValue("");
      }
      return fallbackMap;
    }

    final Document doc = db.parse(file);
    doc.getDocumentElement().normalize();

    // Get all entries and populate a map with the key and values.
    final NodeList entries = doc.getElementsByTagName("entry");
    final Map<String, String> labelById = new LinkedHashMap<>();
    for (int i = 0; i < entries.getLength(); i++) {
      final Node entry = entries.item(i);
      if (entry.getNodeType() == Node.ELEMENT_NODE) {
        final NamedNodeMap attributes = entry.getAttributes();
        final String id = attributes.getNamedItem("key").getTextContent().strip();
        final String labelText = entry.getTextContent().strip();
        labelById.put(id, labelText);
      }
    }
    return labelById;
  }

  public static void serveTranslations(final HttpServletResponse response, final String sdkVersion,
      final String langCode, String filenameForDownload)
      throws ParserConfigurationException, SAXException, IOException, JsonProcessingException {
    final Map<String, String> labelById = new LinkedHashMap<>();

    // Security: set the asset type on the server side!
    final String labelAssetTypeField = "field";
    labelById.putAll(SdkService.getTranslations(sdkVersion, labelAssetTypeField, langCode));

    // Security: set the asset type on the server side!
    final String labelAssetTypeGroup = "group";
    labelById.putAll(SdkService.getTranslations(sdkVersion, labelAssetTypeGroup, langCode));

    // Convert to JSON string and respond.
    final String jsonStr = new ObjectMapper().writeValueAsString(labelById);
    serveSdkJsonString(response, jsonStr, filenameForDownload);
  }


  /**
   * HTTP header, cache related.
   */
  public static void setResponseCacheControl(final HttpServletResponse response,
      final int maxAgeSeconds) {
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
    response.setHeader("Cache-Control",
        String.format("public, max-age=%s, immutable", maxAgeSeconds));
  }

  /**
   * Just an idea, gzipping could reduce the size of the JSON data on the network.
   */
  @SuppressWarnings("unused")
  private static void setResponseGzip(final HttpServletResponse response) {
    // In a non demo environment, you may want to setup gzip or put a proxy in front to do it.
    // This will work well with large repetitive text files like JSON files, XML files, ...
    // https://stackoverflow.com/questions/21410317/using-gzip-compression-with-spring-boot-mvc-javaconfig-with-restful
    response.setHeader("Content-Encoding", "gzip");
  }
}
