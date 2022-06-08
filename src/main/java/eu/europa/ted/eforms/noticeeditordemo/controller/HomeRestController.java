package eu.europa.ted.eforms.noticeeditordemo.controller;

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
import org.springframework.web.bind.annotation.RestController;
import com.helger.genericode.v10.CodeListDocument;
import eu.europa.ted.eforms.noticeeditordemo.genericode.CustomGenericodeMarshaller;
import eu.europa.ted.eforms.noticeeditordemo.genericode.GenericodeTools;
import eu.europa.ted.eforms.noticeeditordemo.util.IntuitiveStringComparator;
import eu.europa.ted.eforms.noticeeditordemo.util.JavaTools;

// @EnableAsync
@SuppressWarnings("static-method")
@RestController
@RequestMapping(value = "/home")
public class HomeRestController implements AsyncConfigurer {

  private static final Logger logger = LoggerFactory.getLogger(HomeRestController.class);

  private static final String APP_VERSION = "1.0.0";
  private static final String EFORMS_SDKS = "eforms-sdks";

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
  @RequestMapping(value = "/sdk/{sdkVersion}/notice-types", method = RequestMethod.GET,
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

  @RequestMapping(value = "/sdk/{sdkVersion}/codelists/{codeListId}/lang/{langCode}",
      method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  private void serveCodelist(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") final String sdkVersion,
      @PathVariable(value = "codeListId") final String codeListId,
      @PathVariable(value = "langCode") final String langCode) throws IOException {

    final String pathStr = buildPathToSdk(sdkVersion, String.format("codelists/%s.gc", codeListId));

    // use Helger lib to load SDK .gc file.
    final CustomGenericodeMarshaller marshaller = GenericodeTools.getMarshaller();
    try (InputStream is = Files.newInputStream(Path.of(pathStr))) {
      final CodeListDocument codeListDoc = GenericodeTools.parseGenericode(is, marshaller);
      // transform the XML to Java objects.

      // serialize to JSON, what about the labels, only load ????
      // Probably only load language used in the HTML page.
      // en to eng
      // new Locale(langCode).getISO3Language()
      // <Value ColumnRef="eng_label">
      // <SimpleValue>Accessibility criteria for persons with disabilities are not included because
      // the procurement is not intended for use by natural persons</SimpleValue>
      // </Value>

      // TODO tttt it is interesting but should we really demo all of this ????
      // We need a key value flat map
      logger.info(codeListDoc.getIdentification().getShortNameValue());
    }
  }

  @RequestMapping(value = "/sdk/{sdkVersion}/fields", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void serveNoticeTypeJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion) {
    final String filenameForDownload = "fields.json";
    final String sdkRelativePathStr = String.format("fields/%s", filenameForDownload);
    serveSdkFile(response, sdkVersion, sdkRelativePathStr, filenameForDownload);
  }

  @RequestMapping(value = "/sdk/{sdkVersion}/notice-types/{noticeId}", method = RequestMethod.GET,
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
