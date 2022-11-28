package eu.europa.ted.eforms.noticeeditor.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
@RestController
@RequestMapping(value = "/sdk")
public class SdkRestController implements AsyncConfigurer {
  private Path eformsSdkDir;

  @Value("${eforms.sdk.versions}")
  private List<SdkVersion> supportedSdks;

  @Autowired
  private SdkService sdkService;

  public SdkRestController(@Value("${eforms.sdk.path}") final String eformsSdkDir,
      @Value("${eforms.sdk.versions}") final List<String> supportedSdks) {
    Validate.notEmpty(eformsSdkDir, "Undefined eForms SDK directory");
    Validate.notNull(supportedSdks, "Undefined supported SDK versions");

    this.eformsSdkDir = Path.of(eformsSdkDir);
    this.supportedSdks = supportedSdks.stream().map(SdkVersion::new).collect(Collectors.toList());
  }

  /**
   * Get JSON containing basic home info.
   *
   * @throws IOException
   */
  @RequestMapping(value = "/info", method = RequestMethod.GET, produces = SdkService.MIME_TYPE_JSON)
  public Map<String, Object> selectHomeInfo() throws IOException {
    return SdkService.getHomePageInfo(supportedSdks);
  }

  /**
   * Get JSON containing basic home info.
   */
  @RequestMapping(value = "/{sdkVersion}/notice-types", method = RequestMethod.GET,
      produces = SdkService.MIME_TYPE_JSON)
  public Map<String, Object> selectNoticeTypesList(
      @PathVariable(value = "sdkVersion") final String sdkVersion) {
    return SdkService.getNoticeSubTypes(new SdkVersion(sdkVersion), eformsSdkDir);
  }

  /**
   * Get JSON containing data about the specified codelist, with text in the given language.
   */
  @RequestMapping(value = "/{sdkVersion}/codelists/{codelistGc}/lang/{langCode}",
      method = RequestMethod.GET, produces = SdkService.MIME_TYPE_JSON)
  @ResponseBody
  private String serveCodelist(@PathVariable(value = "sdkVersion") final String sdkVersion,
      @PathVariable(value = "codelistGc") final String codelistGc,
      @PathVariable(value = "langCode") final String langCode, final HttpServletResponse response)
      throws IOException {
    return SdkService.serveCodelistAsJson(new SdkVersion(sdkVersion), eformsSdkDir, codelistGc,
        langCode, response);
  }

  /**
   * GET JSON containing data about the SDK fields and the codelists metadata.
   */
  @RequestMapping(value = "/{sdkVersion}/basic-meta-data", method = RequestMethod.GET,
      produces = SdkService.MIME_TYPE_JSON)
  public void serveFieldsJson(final HttpServletResponse response,
      final @PathVariable(value = "sdkVersion") String sdkVersion) {
    sdkService.serveSdkBasicMetadata(response, new SdkVersion(sdkVersion));
  }

  /**
   * Get JSON containing data about the SDK notice types.
   */
  @RequestMapping(value = "/{sdkVersion}/notice-types/{noticeId}", method = RequestMethod.GET,
      produces = SdkService.MIME_TYPE_JSON)
  public void serveNoticeTypeJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") final String sdkVersion,
      @PathVariable(value = "noticeId") final String noticeId) {
    final String filenameForDownload = String.format("%s.json", noticeId);
    sdkService.serveSdkJsonFile(response, new SdkVersion(sdkVersion), SdkResource.NOTICE_TYPES,
        filenameForDownload);
  }

  /**
   * Get JSON containing data about translations for the given language.
   */
  @RequestMapping(value = "/{sdkVersion}/translations/{langCode}.json", method = RequestMethod.GET,
      produces = SdkService.MIME_TYPE_JSON)
  public void serveTranslationsFields(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") final String sdkVersion,
      @PathVariable(value = "langCode") final String langCode)
      throws ParserConfigurationException, SAXException, IOException {
    final Language lang = Language.valueOfFromLocale(langCode);
    final String filenameForDownload = String.format("i18n_%s.xml", lang.getLocale().getLanguage());
    SdkService.serveTranslations(response, new SdkVersion(sdkVersion), eformsSdkDir, langCode,
        filenameForDownload);
  }

  /**
   * Save: Takes notice as JSON and builds notice XML. The SDK version is in the notice metadata.
   *
   * @throws IOException
   */
  @RequestMapping(value = "/notice/save", method = RequestMethod.POST,
      produces = SdkService.MIME_TYPE_XML, consumes = SdkService.MIME_TYPE_JSON)
  public void saveNotice(final HttpServletResponse response, final @RequestBody String noticeJson)
      throws ParserConfigurationException, IOException {
    sdkService.saveNoticeAsXml(Optional.of(response), noticeJson);
  }

}
