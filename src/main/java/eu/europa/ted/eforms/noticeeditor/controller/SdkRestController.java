package eu.europa.ted.eforms.noticeeditor.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;
import eu.europa.ted.eforms.noticeeditor.domain.Language;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;
import eu.europa.ted.eforms.sdk.SdkConstants.SdkResource;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * REST API implementation for download of SDK related resources.
 */
@SuppressWarnings("static-method")
@RestController
@RequestMapping(value = "/sdk")
public class SdkRestController implements AsyncConfigurer {
  private Path eformsSdkDir;

  @Value("${eforms.sdk.versions}")
  private List<SdkVersion> supportedSdks;

  public SdkRestController(@Value("${eforms.sdk.path}") String eformsSdkDir,
      @Value("${eforms.sdk.versions}") List<String> supportedSdks) {
    Validate.notEmpty(eformsSdkDir, "Undefined eForms SDK directory");
    Validate.notNull(supportedSdks, "Undefined supported SDK versions");

    this.eformsSdkDir = Path.of(eformsSdkDir);
    this.supportedSdks = supportedSdks.stream().map(SdkVersion::new).collect(Collectors.toList());
  }

  /**
   * Get JSON containing basic home info.
   * @throws IOException 
   */
  @RequestMapping(value = "/info", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> selectHomeInfo() throws IOException {
    return SdkService.getHomePageInfo(supportedSdks);
  }

  /**
   * Get JSON containing basic home info.
   */
  @RequestMapping(value = "/{sdkVersion}/notice-types", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> selectNoticeTypesList(
      @PathVariable(value = "sdkVersion") String sdkVersion) {
    return SdkService.getNoticeSubTypes(new SdkVersion(sdkVersion), eformsSdkDir);
  }

  /**
   * Get JSON containing data about the specified codelist, with text in the given language.
   */
  @RequestMapping(value = "/{sdkVersion}/codelists/{codeListId}/lang/{langCode}",
      method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  private String serveCodelist(@PathVariable(value = "sdkVersion") final String sdkVersion,
      @PathVariable(value = "codeListId") final String codeListId,
      @PathVariable(value = "langCode") final String langCode, final HttpServletResponse response)
      throws IOException {
    return SdkService.serveCodelistAsJson(new SdkVersion(sdkVersion), eformsSdkDir, codeListId,
        langCode,
        response);
  }

  /**
   * GET JSON containing data about the SDK fields.
   */
  @RequestMapping(value = "/{sdkVersion}/fields", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void serveFieldsJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion) {
    final String filenameForDownload = "fields.json";
    SdkService.serveSdkJsonFile(response, new SdkVersion(sdkVersion), eformsSdkDir,
        SdkResource.FIELDS, filenameForDownload);
  }

  /**
   * Get JSON containing data about the SDK notice types.
   */
  @RequestMapping(value = "/{sdkVersion}/notice-types/{noticeId}", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void serveNoticeTypeJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion,
      @PathVariable(value = "noticeId") String noticeId) {
    final String filenameForDownload = String.format("%s.json", noticeId);
    SdkService.serveSdkJsonFile(response, new SdkVersion(sdkVersion), eformsSdkDir,
        SdkResource.NOTICE_TYPES, filenameForDownload);
  }

  /**
   * Get JSON containing data about translations for the given language.
   */
  @RequestMapping(value = "/{sdkVersion}/translations/{langCode}.json", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void serveTranslationsFields(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion,
      @PathVariable(value = "langCode") String langCode)
      throws ParserConfigurationException, SAXException, IOException {
    final Language lang = Language.valueOfFromLocale(langCode);
    final String filenameForDownload = String.format("i18n_%s.xml", lang.getLocale().getLanguage());
    SdkService.serveTranslations(response, new SdkVersion(sdkVersion), eformsSdkDir, langCode,
        filenameForDownload);
  }

}
