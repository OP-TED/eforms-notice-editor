package eu.europa.ted.eforms.noticeeditor.controller;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;

/**
 * REST API implementation for download of SDK related resources.
 */
@SuppressWarnings("static-method")
@RestController
@RequestMapping(value = "/sdk")
public class SdkRestController implements AsyncConfigurer {

  /**
   * @return JSON containing basic home info
   */
  @RequestMapping(value = "/info", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> selectHomeInfo() {
    return SdkService.selectHomePageInfo();
  }

  /**
   * @return JSON containing basic home info
   */
  @RequestMapping(value = "/{sdkVersion}/notice-types", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> selectNoticeTypesList(
      @PathVariable(value = "sdkVersion") String sdkVersion) {
    return SdkService.selectNoticeTypes(sdkVersion);
  }

  /**
   * @return JSON containing data about the specified codelist, with text in the given language
   */
  @RequestMapping(value = "/{sdkVersion}/codelists/{codeListId}/lang/{langCode}",
      method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  private String serveCodelist(@PathVariable(value = "sdkVersion") final String sdkVersion,
      @PathVariable(value = "codeListId") final String codeListId,
      @PathVariable(value = "langCode") final String langCode, final HttpServletResponse response)
      throws IOException {
    return SdkService.serveCodelistAsJson(sdkVersion, codeListId, langCode, response);
  }

  /**
   * @return JSON containing data about the SDK fields
   */
  @RequestMapping(value = "/{sdkVersion}/fields", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void serveFieldsJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion) {
    final String filenameForDownload = "fields.json";
    final String sdkRelativePathStr = String.format("fields/%s", filenameForDownload);
    SdkService.serveSdkJsonFile(response, sdkVersion, sdkRelativePathStr, filenameForDownload);
  }

  /**
   * @return JSON containing data about the SDK notice types
   */
  @RequestMapping(value = "/{sdkVersion}/notice-types/{noticeId}", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void serveNoticeTypeJson(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion,
      @PathVariable(value = "noticeId") String noticeId) {
    final String filenameForDownload = String.format("%s.json", noticeId);
    final String sdkRelativePathStr = String.format("notice-types/%s", filenameForDownload);
    SdkService.serveSdkJsonFile(response, sdkVersion, sdkRelativePathStr, filenameForDownload);
  }

  /**
   * @return JSON containing data about translations for the given language
   */
  @RequestMapping(value = "/{sdkVersion}/translations/fields/{langCode}.json",
      method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public void serveTranslationsFields(final HttpServletResponse response,
      @PathVariable(value = "sdkVersion") String sdkVersion,
      @PathVariable(value = "langCode") String langCode)
      throws ParserConfigurationException, SAXException, IOException {
    SdkService.serveTranslationFields(response, sdkVersion, langCode);
  }

}
