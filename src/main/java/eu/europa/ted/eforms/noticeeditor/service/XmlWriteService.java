package eu.europa.ted.eforms.noticeeditor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.helper.VersionHelper;
import eu.europa.ted.eforms.noticeeditor.helper.notice.ConceptualModel;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeInfo;
import eu.europa.ted.eforms.noticeeditor.helper.notice.FieldsAndNodes;
import eu.europa.ted.eforms.noticeeditor.helper.notice.PhysicalModel;
import eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel;
import eu.europa.ted.eforms.noticeeditor.helper.notice.VisualTreeElement;
import eu.europa.ted.eforms.noticeeditor.helper.validation.CsvValidationMode;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * About writing to XML format. The starting point is the web form seen in the user interface, the
 * data is transformed into a visual model (as JSON). From there it is transformed to an
 * intermediary conceptual model and finally into a physical model (XML).
 */
@Service
public class XmlWriteService {

  private static final Logger logger = LoggerFactory.getLogger(XmlWriteService.class);

  /**
   * Mime type for JSON data.
   */
  public static final String MIME_TYPE_JSON = "application/json";

  @Autowired
  private SdkService sdkService;

  @Autowired
  private NoticeValidationService noticeValidationService;

  /**
   * @param responseOpt used to respond with the XML
   * @param rootElement The notice as serialized JSON as built by the front-end form.
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for
   *        production
   */
  public void saveNoticeAsXml(final Optional<HttpServletResponse> responseOpt, final VisualTreeElement rootElement, final boolean debug) throws Exception {
    final PhysicalModel physicalModel = buildPhysicalModel(rootElement, debug);
    final UUID noticeUuid = physicalModel.getNoticeId();
    final SdkVersion sdkVersion = physicalModel.getSdkVersion();
    try {
      // Transform physical model to XML.
      final String noticeXmlText = physicalModel.toXmlText(true);

      // We perform no validation on the XML.
      // Respond with the XML.
      if (responseOpt.isPresent()) {
        final String filenameForDownload = generateNoticeFilename(noticeUuid, sdkVersion);
        serveSdkXmlStringAsDownload(responseOpt.get(), noticeXmlText, filenameForDownload);
      }
    } catch (final Exception e) {
      // Catch any error, log some useful context and rethrow.
      logger.error("Error for notice uuid={}, sdkVersion={}", noticeUuid,
          sdkVersion.toNormalisedString(true));
      throw e;
    }
  }

  /**
   * Validate the notice using the appropriate SDK XSDs.
   *
   * @param responseOpt used to respond with a report
   * @param rootElement The notice as serialized JSON as built by the front-end form.
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for
   *        production
   */
  public void validateUsingXsd(final Optional<HttpServletResponse> responseOpt, final VisualTreeElement rootElement, final boolean debug) throws Exception {
    final PhysicalModel physicalModel = buildPhysicalModel(rootElement, debug);
    final SdkVersion sdkVersion = physicalModel.getSdkVersion();
    final UUID noticeUuid = physicalModel.getNoticeId();
    try {
      // Transform physical model to XML.
      final String noticeXmlText = physicalModel.toXmlText(true);

      // Validate it using XSD.
      final Optional<Path> mainXsdPathOpt = physicalModel.getMainXsdPathOpt();

      final ObjectNode xsdReport = noticeValidationService.validateNoticeUsingXsd(noticeUuid,
          sdkVersion, noticeXmlText, mainXsdPathOpt);

      final String jsonText = xsdReport.toPrettyString();
      if (responseOpt.isPresent()) {
        final String filenameForDownload =
            String.format("notice-%s-%s-xsd-report.json", sdkVersion, noticeUuid);
        serveJson(responseOpt.get(), filenameForDownload, true, jsonText);
      }

    } catch (final Exception e) {
      // Catch any error, log some useful context and rethrow.
      logger.error("Error for notice uuid={}, sdkVersion={}", noticeUuid,
          sdkVersion.toNormalisedString(true));
      throw e;
    }
  }

  /**
   * Validate the notice using the remote Central Validation Service (CVS). Configuration is
   * required for this to work, see application.yaml
   *
   * @param responseOpt used to respond with a report
   * @param rootElement The notice as serialized JSON as built by the front-end form.
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for
   *        production
   */
  public void validateUsingCvs(final Optional<HttpServletResponse> responseOpt, final VisualTreeElement rootElement, final boolean debug) throws Exception {
    final PhysicalModel physicalModel = buildPhysicalModel(rootElement, debug);
    final UUID noticeUuid = physicalModel.getNoticeId();
    final SdkVersion sdkVersion = physicalModel.getSdkVersion();

    // Transform physical model to XML.
    final String noticeXmlText = physicalModel.toXmlText(true);

    // Validate using the CVS.

    // Could pass the language of the UI for the SVRL report.
    final Optional<String> svrlLangA2 = Optional.empty();

    final Optional<String> eformsSdkVersion = Optional.empty(); // Use default.
    final Optional<CsvValidationMode> validationMode = Optional.empty(); // Use default.

    final String svrlXml = noticeValidationService.validateNoticeXmlUsingCvs(noticeXmlText,
        eformsSdkVersion, svrlLangA2, validationMode);

    if (responseOpt.isPresent()) {
      final String filenameForDownload = String.format("notice-%s-%s.svrl", sdkVersion, noticeUuid);
      serveSdkXmlStringAsDownload(responseOpt.get(), svrlXml, filenameForDownload);
    }
  }

  /**
   * @param visualRoot The notice as serialized JSON as built by the front-end form.
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for
   *        production
   * @return The physical model
   */
  public PhysicalModel buildPhysicalModel(VisualTreeElement visualRoot, boolean debug) throws ParserConfigurationException {
    SdkVersion sdkVersion = parseSdkVersion(visualRoot);
    FieldsAndNodes fieldsAndNodes = readFieldsAndNodes(sdkVersion);

    VisualModel visualModel = new VisualModel(visualRoot, fieldsAndNodes);
    if (debug) {
      visualModel.writeDotFile(fieldsAndNodes);
    }

    ConceptualModel conceptualModel = visualModel.toConceptualModel();

    if (debug) {
      conceptualModel.writeDotFile(fieldsAndNodes);
    }

    DocumentTypeInfo documentTypeInfo = new DocumentTypeInfo(sdkService.getDocumentTypeOfSubtype(conceptualModel.getNoticeSubType(), sdkVersion), sdkVersion);
    Path sdkFolder = sdkService.getSdkRootFolder().resolve(sdkVersion.toStringWithoutPatch());

    return PhysicalModel.buildPhysicalModel(conceptualModel, fieldsAndNodes, documentTypeInfo, sdkFolder, debug);
  }

  public FieldsAndNodes readFieldsAndNodes(final SdkVersion sdkVersion) {
    final JsonNode fieldsJson = sdkService.readSdkFieldsJson(sdkVersion);
    final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldsJson, sdkVersion);
    return fieldsAndNodes;
  }

  public static Map<String, JsonNode> parseDocumentTypes(final JsonNode noticeTypesJson) {
    final Map<String, JsonNode> documentInfoByType = new HashMap<>();
    final JsonNode documentTypes =
        noticeTypesJson.get(SdkConstants.NOTICE_TYPES_JSON_DOCUMENT_TYPES_KEY);
    for (final JsonNode item : documentTypes) {
      // TODO add document type id to the SDK constants.
      final String id = JsonUtils.getTextStrict(item, "id");
      documentInfoByType.put(id, item);
    }
    return documentInfoByType;
  }

  /**
   * Parses the SDK version from the visual model of the notice
   *
   * @param visualRoot The notice as serialized JSON as built by the front-end form.
   * @return SDKVersion object for the given notice
   */
  public SdkVersion parseSdkVersion(final VisualTreeElement visualRoot) {
    final String eformsSdkVersion = visualRoot.getSdkVersion();
    Validate.notBlank(eformsSdkVersion, "virtual root eFormsSdkVersion is blank");

    final SdkVersion sdkVersion = VersionHelper.parsePrefixedSdkVersion(eformsSdkVersion);
    logger.info("Found SDK version: {}, using {}", eformsSdkVersion, sdkVersion);
    return sdkVersion;
  }

  /**
   * Common SDK XML string logic.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS",
      justification = "Checked to Runtime OK here")
  public static void serveSdkXmlStringAsDownload(final HttpServletResponse response,
      final String jsonStr, final String filenameForDownload) {
    Validate.notBlank(jsonStr, "JSON string is blank");
    try {
      // As the sdkVersion and other details are in the url this can be cached for a while.
      SdkService.setResponseCacheControl(response, SdkService.CACHE_MAX_AGE_SECONDS);

      final String responseMimeType = SdkService.MIME_TYPE_XML;
      SdkService.serveStringUtf8(response, jsonStr, filenameForDownload, true, responseMimeType);

    } catch (Exception ex) {
      logger.error(ex.toString(), ex);
      throw new RuntimeException(
          String.format("Exception serving JSON file %s", filenameForDownload), ex);
    }
  }

  /**
   * Generate the filename for the notice XML file.
   *
   * @param noticeUuid The UUID of this notice (a universally unique identifier)
   * @param sdkVersion The version of the SDK
   * @return The filename for the notice XML file
   */
  @SuppressWarnings("static-method")
  private String generateNoticeFilename(final UUID noticeUuid, final SdkVersion sdkVersion) {
    // Not sure about the filename, including the sdkVersion plus the notice UUID is good enough.
    return String.format("notice-%s-%s.xml", sdkVersion, noticeUuid);
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS", justification = "Ok here")
  private static void serveJson(final HttpServletResponse response,
      final String filenameForDownload, final boolean isAsDownload, final String jsonText) {
    Validate.notBlank(jsonText, "jsonText is blank");
    Validate.notBlank(filenameForDownload, "filenameForDownload is blank");

    // ---------------------------------
    // THE FILE IS SMALL, JUST COPY IT.
    // ---------------------------------
    try (InputStream is = new ByteArrayInputStream(jsonText.getBytes(StandardCharsets.UTF_8))) {
      // Indicate the content type and encoding BEFORE writing to output.
      response.setContentType(MIME_TYPE_JSON);
      response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
      // setGzipResponse(response);

      if (isAsDownload) {
        response.setHeader("Content-Disposition",
            String.format("attachment; filename=\"%s\"", filenameForDownload));
      }

      // Write response content.
      org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
      response.flushBuffer();

    } catch (final IOException ex) {
      logger.info("Error responding with JSON for download.", ex);
      throw new RuntimeException("IOException writing JSON to output stream.", ex);
    }
  }
}
