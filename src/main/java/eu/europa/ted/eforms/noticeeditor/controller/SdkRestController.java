package eu.europa.ted.eforms.noticeeditor.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
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
@SuppressWarnings("static-method")
@RestController
@RequestMapping(value = "/sdk")
public class SdkRestController implements AsyncConfigurer {

  /**
   * Get JSON containing basic home info.
   *
   * @throws IOException
   */
  @RequestMapping(value = "/info", method = RequestMethod.GET, produces = SdkService.MIME_TYPE_JSON)
  public Map<String, Object> selectHomeInfo() throws IOException {
    return SdkService.getHomePageInfo();
  }

  /**
   * Get JSON containing basic home info.
   */
  @RequestMapping(value = "/{sdkVersion}/notice-types", method = RequestMethod.GET,
      produces = SdkService.MIME_TYPE_JSON)
  public Map<String, Object> selectNoticeTypesList(
      @PathVariable(value = "sdkVersion") String sdkVersion) {
    return SdkService.getNoticeSubTypes(new SdkVersion(sdkVersion));
  }

  /**
   * Get JSON containing data about the specified codelist, with text in the given language.
   */
  @RequestMapping(value = "/{sdkVersion}/codelists/{codeListId}/lang/{langCode}",
      method = RequestMethod.GET, produces = SdkService.MIME_TYPE_JSON)
  @ResponseBody
  private String serveCodelist(@PathVariable(value = "sdkVersion") final String sdkVersion,
      @PathVariable(value = "codeListId") final String codeListId,
      @PathVariable(value = "langCode") final String langCode, final HttpServletResponse response)
      throws IOException {
    return SdkService.serveCodelistAsJson(new SdkVersion(sdkVersion), codeListId, langCode,
        response);
  }

  /**
   * GET JSON containing data about the SDK fields.
   */
  @RequestMapping(value = "/{sdkVersion}/fields", method = RequestMethod.GET,
      produces = SdkService.MIME_TYPE_JSON)
  public void serveFieldsJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion) {
    SdkService.serveSdkJsonFile(response, new SdkVersion(sdkVersion), SdkResource.FIELDS,
        SdkService.SDK_FIELDS_JSON);
  }

  /**
   * Get JSON containing data about the SDK notice types.
   */
  @RequestMapping(value = "/{sdkVersion}/notice-types/{noticeId}", method = RequestMethod.GET,
      produces = SdkService.MIME_TYPE_JSON)
  public void serveNoticeTypeJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion,
      @PathVariable(value = "noticeId") String noticeId) {
    final String filenameForDownload = String.format("%s.json", noticeId);
    SdkService.serveSdkJsonFile(response, new SdkVersion(sdkVersion), SdkResource.NOTICE_TYPES,
        filenameForDownload);
  }

  /**
   * Get JSON containing data about translations for the given language.
   */
  @RequestMapping(value = "/{sdkVersion}/translations/{langCode}.json", method = RequestMethod.GET,
      produces = SdkService.MIME_TYPE_JSON)
  public void serveTranslationsFields(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion,
      @PathVariable(value = "langCode") String langCode)
      throws ParserConfigurationException, SAXException, IOException {
    final Language lang = Language.valueOfFromLocale(langCode);
    final String filenameForDownload = String.format("i18n_%s.xml", lang.getLocale().getLanguage());
    SdkService.serveTranslations(response, new SdkVersion(sdkVersion), langCode,
        filenameForDownload);
  }

  /**
   * Save: Takes notice as JSON and builds notice XML. The SDK version is in the notice metadata.
   *
   * @throws IOException
   */
  @RequestMapping(value = "/notice/save", method = RequestMethod.POST,
      produces = SdkService.MIME_TYPE_XML, consumes = SdkService.MIME_TYPE_JSON)
  public void saveNotice(final HttpServletResponse response, @RequestBody String notice)
      throws ParserConfigurationException, IOException {
    SdkService.saveNoticeAsXml(Optional.of(response), notice);
  }

}
