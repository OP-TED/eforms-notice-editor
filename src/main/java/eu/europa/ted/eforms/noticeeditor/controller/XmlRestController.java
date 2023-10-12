package eu.europa.ted.eforms.noticeeditor.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
   * Enriches the XML for human readability but it becomes invalid. Also adds .dot files and other
   * debug files in target.
   */
  @Value("${xml.generation.debug}")
  private boolean debug;

  @Value("${xml.generation.skipIfEmpty}")
  private boolean skipIfNoValue;

  @Value("${xml.generation.sortXmlElements}")
  private boolean sortXmlElements;

  /**
   * Save: Takes notice as JSON and builds notice XML. The SDK version is in the notice metadata.
   */
  @RequestMapping(value = "/notice/save/validation/none", method = RequestMethod.POST,
      produces = SdkService.MIME_TYPE_XML, consumes = SdkService.MIME_TYPE_JSON)
  public void saveNotice(final HttpServletResponse response,
      final @RequestBody String noticeJson)
      throws Exception {
    // For the XML generation config booleans, see application.yaml
    xmlService.saveNoticeAsXml(Optional.of(response), noticeJson,
        debug,
        skipIfNoValue,
        sortXmlElements);
  }

  /**
   * Save: Takes notice as JSON and builds notice XML. The SDK version is in the notice metadata.
   * The notice XML is validated against the appropriate SDK XSDs.
   */
  @RequestMapping(value = "/notice/save/validation/xsd", method = RequestMethod.POST,
      produces = SdkService.MIME_TYPE_XML, consumes = SdkService.MIME_TYPE_JSON)
  public void saveNoticeAndXsdValidate(final HttpServletResponse response,
      final @RequestBody String noticeJson) throws Exception {
    xmlService.validateUsingXsd(Optional.of(response), noticeJson, debug);
  }

  /**
   * Save: Takes notice as JSON and builds notice XML. The SDK version is in the notice metadata.
   * The notice XML is validated using the remote CVS service (through the API). You must configure
   * this in the application.yaml file.
   */
  @RequestMapping(value = "/notice/save/validation/cvs", method = RequestMethod.POST,
      produces = SdkService.MIME_TYPE_XML, consumes = SdkService.MIME_TYPE_JSON)
  public void saveNoticeAndCvsValidate(final HttpServletResponse response,
      final @RequestBody String noticeJson) throws Exception {
    final boolean debug = false;
    xmlService.validateUsingCvs(Optional.of(response), noticeJson, debug);
  }
}
