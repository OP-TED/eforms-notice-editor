package eu.europa.ted.eforms.noticeeditor.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.helger.genericode.v10.CodeListDocument;
import com.helger.genericode.v10.Column;
import com.helger.genericode.v10.Row;
import com.helger.genericode.v10.Value;
import eu.europa.ted.eforms.noticeeditor.genericode.CustomGenericodeMarshaller;
import eu.europa.ted.eforms.noticeeditor.genericode.GenericodeTools;
import eu.europa.ted.eforms.noticeeditor.util.IntuitiveStringComparator;
import eu.europa.ted.eforms.noticeeditor.util.JavaTools;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

/**
 *
 */
@SuppressWarnings("static-method")
@RestController
@RequestMapping(value = "/sdk")
// @EnableAsync
public class SdkRestController implements AsyncConfigurer {

  private static final Logger logger = LoggerFactory.getLogger(SdkRestController.class);

  private static final String APP_VERSION = "1.0.0";
  private static final String EFORMS_SDKS = "eforms-sdks";

  private static final int SECONDS_IN_ONE_HOUR = 3600;
  private static final int CACHE_MAX_AGE_SECONDS = SECONDS_IN_ONE_HOUR * 24;

  /**
   * SECURITY: this regex is security related, so be careful if you decide to change it. This is
   * security related as the sdkVersion is used in file system folder related operations.
   */
  private static final Pattern REGEX_SDK_VERSION =
      Pattern.compile("\\p{Digit}{1,2}\\.\\p{Digit}{1,2}\\.\\p{Digit}{1,2}");

  /**
   * @return JSON with basic home info.
   */
  @RequestMapping(value = "/info", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> selectHomeInfo() {
    final Instant now = Instant.now();
    final String instantNowIso8601Str = now.toString();
    logger.info("Fetching main info: {}", instantNowIso8601Str);
    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("appVersion", APP_VERSION);
    try {
      final List<String> availableSdkVersions = JavaTools.listFolders(EFORMS_SDKS);
      availableSdkVersions.sort(new IntuitiveStringComparator<String>());
      Collections.reverse(availableSdkVersions);
      map.put("sdkVersions", availableSdkVersions);
    } catch (final IOException e) {
      logger.error(e.toString(), e);
    }

    logger.info("Fetching main info: DONE");
    return map;
  }

  /**
   * @return JSON with basic home info.
   */
  @RequestMapping(value = "/{sdkVersion}/notice-types", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> selectNoticeTypesList(
      @PathVariable(value = "sdkVersion") String sdkVersion) {

    securityValidateSdkVersionFormatThrows(sdkVersion);

    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("sdkVersion", sdkVersion);
    try {
      final List<String> availableNoticeTypes =
          JavaTools.listFiles(EFORMS_SDKS + "/" + sdkVersion + "/notice-types/");

      final List<String> noticeTypes = availableNoticeTypes.stream()//
          // Remove some files.
          .filter(filename -> filename.endsWith(".json") && !filename.equals("notice-types.json"))//
          // Remove extension.
          .map(filename -> filename.substring(0, filename.lastIndexOf(".")))//
          .collect(Collectors.toList());

      noticeTypes.sort(new IntuitiveStringComparator<String>());
      map.put("noticeTypes", noticeTypes);
    } catch (final IOException e) {
      logger.error(e.toString(), e);
    }
    logger.info("Fetching main info: DONE");
    return map;
  }

  @RequestMapping(value = "/{sdkVersion}/codelists/{codeListId}/lang/{langCode}",
      method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  private String serveCodelist(@PathVariable(value = "sdkVersion") final String sdkVersion,
      @PathVariable(value = "codeListId") final String codeListId,
      @PathVariable(value = "langCode") final String langCode, final HttpServletResponse response)
      throws IOException {
    final String pathStr = buildPathToSdk(sdkVersion, String.format("codelists/%s.gc", codeListId));

    // As the SDK and other details are inside the url this data can be cached for a while.
    setResponseCacheControl(response, CACHE_MAX_AGE_SECONDS);

    return buildJsonFromCodelistGc(codeListId, pathStr);
  }

  private void setResponseCacheControl(final HttpServletResponse response,
      final int maxAgeSeconds) {
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
    response.setHeader("Cache-Control",
        String.format("public, max-age=%s, immutable", maxAgeSeconds));
  }

  private String buildJsonFromCodelistGc(final String codeListId, final String pathStr)
      throws JsonProcessingException, IOException {
    // Use GC Helger lib to load SDK .gc file.
    final CustomGenericodeMarshaller marshaller = GenericodeTools.getMarshaller();
    try (InputStream is = Files.newInputStream(Path.of(pathStr))) {
      final CodeListDocument gcDoc = GenericodeTools.parseGenericode(is, marshaller);
      // transform the XML to Java objects.

      final String shortName = gcDoc.getIdentification().getShortNameValue();
      final String longName = gcDoc.getIdentification().getLongNameAtIndex(0).getValue();

      final ObjectMapper jsonm = JsonUtils.getStandardJacksonObjectMapper();
      final ObjectNode jsonCodelist = jsonm.createObjectNode();

      // TODO tttt we should document this somewhere ????
      jsonCodelist.put("id", longName); // By convention of the SDK the longname is the identifier.

      // This could be used in the UI for display purposes.
      jsonCodelist.put("longName", longName);
      jsonCodelist.put("shortName", shortName);

      final ArrayNode jsonRows = jsonCodelist.putArray("codes");

      final List<Row> gcRows = gcDoc.getSimpleCodeList().getRow();
      for (final Row gcRow : gcRows) {

        final List<Value> gcRowValues = gcRow.getValue();
        final Optional<Value> technicalCodeValOpt = gcFindFirstColumnRef(gcRowValues, "code");
        final String technicalCode;
        if (technicalCodeValOpt.isPresent()) {
          technicalCode = technicalCodeValOpt.get().getSimpleValueValue();
          Validate.notBlank("technicalCode is blank for codeListId=%s", codeListId);

          // TODO get desired language first, fallback to eng.
          final Optional<Value> englishLabelOpt = gcFindFirstColumnRef(gcRowValues, "eng_label");
          if (englishLabelOpt.isPresent()) {
            final String englishText = englishLabelOpt.get().getSimpleValueValue();
            final ObjectNode jsonRow = jsonm.createObjectNode();
            jsonRows.add(jsonRow);
            jsonRow.put("codeValue", technicalCode.strip());
            jsonRow.put("en", englishText.strip());
            // jsonRow.put("fr", otherText); // TODO tttt
          }
        }
      }

      // serialize to JSON, what about the labels, only load ????
      // Probably only load language used in the HTML page.
      // en to eng
      // new Locale(langCode).getISO3Language()
      // <Value ColumnRef="eng_label">
      // <SimpleValue>Accessibility criteria for persons with disabilities are not included because
      // the procurement is not intended for use by natural persons</SimpleValue>
      // </Value>

      // Generate JSON.
      return JsonUtils.marshall(jsonCodelist);
    }
  }

  private Optional<Value> gcFindFirstColumnRef(final List<Value> gcRowValues,
      final String columnRefText) {
    return gcRowValues.stream()//
        .filter(v -> ((Column) v.getColumnRef()).getId().toString().equals(columnRefText))//
        .findFirst();
  }

  @RequestMapping(value = "/{sdkVersion}/fields", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void serveFieldsJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion) {
    final String filenameForDownload = "fields.json";
    final String sdkRelativePathStr = String.format("fields/%s", filenameForDownload);
    serveSdkFile(response, sdkVersion, sdkRelativePathStr, filenameForDownload);
  }

  @RequestMapping(value = "/{sdkVersion}/notice-types/{noticeId}", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void serveNoticeTypeJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion,
      @PathVariable(value = "noticeId") String noticeId) {
    final String filenameForDownload = String.format("%s.json", noticeId);
    final String sdkRelativePathStr = String.format("notice-types/%s", filenameForDownload);
    serveSdkFile(response, sdkVersion, sdkRelativePathStr, filenameForDownload);
  }

  private static void serveJsonFile(final HttpServletResponse response, final String pathStr,
      final String filenameForDownload, final boolean isAsDownload) throws IOException {
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
      if (isAsDownload) {
        response.setHeader("Content-Disposition",
            String.format("attachment; filename=\"%s\"", filenameForDownload));
      }

      // Write response content.
      org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());

    } catch (IOException ex) {
      logger.info("Error reponding with file '{}' for download.", pathStr, ex);
      throw new RuntimeException("IOException writing file to output stream.");
    }
    response.flushBuffer();
  }

  private String buildPathToSdk(final String sdkVersion, final String sdkRelativePathStr) {
    securityValidateSdkVersionFormatThrows(sdkVersion);
    // TODO tttt the resources path could be in the config, a root
    // folder for all sdks could be specified
    return String.format("%s/%s/%s", EFORMS_SDKS, sdkVersion, sdkRelativePathStr);
  }

  /**
   * Common SDK folder logic.
   */
  private void serveSdkFile(final HttpServletResponse response, final String sdkVersion,
      final String sdkRelativePathStr, final String filenameForDownload) {
    Validate.notBlank(sdkVersion, "sdkVersion is blank");
    try {
      final String pathStr = buildPathToSdk(sdkVersion, sdkRelativePathStr);

      // As the sdkVersion and other details are in the url this can be cached for a while.
      setResponseCacheControl(response, CACHE_MAX_AGE_SECONDS);

      serveJsonFile(response, pathStr, filenameForDownload, false);
    } catch (Exception ex) {
      logger.error(ex.toString(), ex);
      throw new RuntimeException("Exception serving file.");
    }
  }

  private boolean securityValidateSdkVersionFormat(final String sdkVersion) {
    return REGEX_SDK_VERSION.matcher(sdkVersion).matches();
  }

  /**
   * Throws a runtime exception if the sdkVersion does not match the expected format.
   */
  private void securityValidateSdkVersionFormatThrows(final String sdkVersion) {
    if (!securityValidateSdkVersionFormat(sdkVersion)) {
      throw new RuntimeException(String.format("Invalid SDK version format: %s", sdkVersion));
    }
  }

}
