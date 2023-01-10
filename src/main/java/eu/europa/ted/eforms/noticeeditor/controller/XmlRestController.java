package eu.europa.ted.eforms.noticeeditor.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;
import eu.europa.ted.eforms.noticeeditor.service.XmlWriteService;

/**
 * REST API implementation for download of SDK related resources.
 */
@RestController
@RequestMapping(value = "/xml")
public class XmlRestController implements AsyncConfigurer {

  @Autowired
  private XmlWriteService xmlService;

  /**
   * Save: Takes notice as JSON and builds notice XML. The SDK version is in the notice metadata.
   */
  @RequestMapping(value = "/notice/save", method = RequestMethod.POST,
      produces = SdkService.MIME_TYPE_XML, consumes = SdkService.MIME_TYPE_JSON)
  public void saveNotice(final HttpServletResponse response, final @RequestBody String noticeJson)
      throws Exception {
    final boolean debug = false;
    xmlService.saveNoticeAsXml(Optional.of(response), noticeJson, debug);
  }
}
