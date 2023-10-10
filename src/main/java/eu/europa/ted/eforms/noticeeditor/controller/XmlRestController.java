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
  @RequestMapping(value = "/notice/save/validation/none", method = RequestMethod.POST,
      produces = SdkService.MIME_TYPE_XML, consumes = SdkService.MIME_TYPE_JSON)
  public void saveNotice(final HttpServletResponse response, final @RequestBody String noticeJson)
      throws Exception {

    // Booleans that control the XML generation can be set here.

    // debug = true, Enriches the XML for human readability but it becomes invalid.
    // Also adds .dot files in target.
    final boolean debug = true;

    // Skips items if there is no value set.
    // This can help reduce the noise (empty fields ...) in the generated items.
    // This is especially useful with an empty form when setting only some values of interest.
    // This simplifies the XML files and the conceptual .dot file (assuming debug = true).
    final boolean skipIfNoValue = false;

    // Default should be true, use false for development or debugging purposes only.
    // Not sorting will obviously create problems with XSD validation as it expects a given order.
    final boolean sortXml = true;

    xmlService.saveNoticeAsXml(Optional.of(response), noticeJson, debug, skipIfNoValue, sortXml);
  }

  /**
   * Save: Takes notice as JSON and builds notice XML. The SDK version is in the notice metadata.
   * The notice XML is validated against the appropriate SDK XSDs.
   */
  @RequestMapping(value = "/notice/save/validation/xsd", method = RequestMethod.POST,
      produces = SdkService.MIME_TYPE_XML, consumes = SdkService.MIME_TYPE_JSON)
  public void saveNoticeAndXsdValidate(final HttpServletResponse response,
      final @RequestBody String noticeJson) throws Exception {
    final boolean debug = false;
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
