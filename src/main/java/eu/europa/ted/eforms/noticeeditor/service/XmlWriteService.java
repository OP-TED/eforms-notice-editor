package eu.europa.ted.eforms.noticeeditor.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ted.eforms.noticeeditor.helper.notice.ConceptualModel;
import eu.europa.ted.eforms.noticeeditor.helper.notice.FieldsAndNodes;
import eu.europa.ted.eforms.noticeeditor.helper.notice.PhysicalModel;
import eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * About writing to XML format. The starting point is the web form seen in the user interface, the
 * data is transformed into a visual model (as JSON). From there it is transformed to an
 * intermediary conceptual model and finally into a physical model (XML).
 */
@Service
public class XmlWriteService {

  private static final Logger logger = LoggerFactory.getLogger(XmlWriteService.class);

  @Autowired
  private SdkService sdkService;

  /**
   * @param noticeJson The notice as JSON as built by the front-end form.
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for
   *        production
   */
  public void saveNoticeAsXml(final Optional<HttpServletResponse> responseOpt,
      final String noticeJson, final boolean debug) throws Exception {
    Validate.notBlank(noticeJson, "noticeJson is blank");

    logger.info("Attempting to save notice as XML.");
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode visualRoot = mapper.readTree(noticeJson);
    final SdkVersion sdkVersion = parseSdkVersion(visualRoot);
    final UUID noticeUuid = parseNoticeUuid(visualRoot);
    try {
      saveXmlSubMethod(responseOpt, visualRoot, sdkVersion, noticeUuid, debug);
    } catch (final Exception e) {
      // Catch any error, log some useful context and rethrow.
      logger.error("Error for notice uuid={}, sdkVersion={}", noticeUuid,
          sdkVersion.toNormalisedString(true));
      throw e;
    }
  }

  /**
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for
   *        production
   */
  private void saveXmlSubMethod(final Optional<HttpServletResponse> responseOpt,
      final JsonNode visualRoot, final SdkVersion sdkVersion, final UUID noticeUuid,
      final boolean debug) throws ParserConfigurationException, SAXException, IOException {
    Validate.notNull(visualRoot);
    Validate.notNull(noticeUuid);

    final JsonNode fieldsJson = sdkService.readSdkFieldsJson(sdkVersion);
    final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldsJson, sdkVersion);
    final VisualModel visualModel = new VisualModel(visualRoot);

    if (debug) {
      visualModel.writeDotFile(fieldsAndNodes);
    }

    final JsonNode noticeTypesJson = sdkService.readNoticeTypesJson(sdkVersion);

    final Map<String, JsonNode> noticeInfoBySubtype = new HashMap<>(512);
    {
      // TODO add noticeSubTypes to the SDK constants.
      // SdkResource.NOTICE_SUB_TYPES
      final JsonNode noticeSubTypes = noticeTypesJson.get("noticeSubTypes");
      for (final JsonNode item : noticeSubTypes) {
        // TODO add subTypeId to the SDK constants.
        final String subTypeId = JsonUtils.getTextStrict(item, "subTypeId");
        noticeInfoBySubtype.put(subTypeId, item);
      }
    }

    final Map<String, JsonNode> documentInfoByType = parseDocumentTypes(noticeTypesJson);

    // Go from visual model to conceptual model.
    final ConceptualModel conceptModel = visualModel.toConceptualModel(fieldsAndNodes);

    // Build physical model.
    final boolean buildFields = true;
    final Path sdkRootFolder = sdkService.getSdkRootFolder();
    final PhysicalModel physicalModel = PhysicalModel.buildPhysicalModel(conceptModel,
        fieldsAndNodes, noticeInfoBySubtype, documentInfoByType, debug, buildFields, sdkRootFolder);

    // Transform physical model to XML.
    final String xmlAsText = physicalModel.toXmlText(buildFields);
    final String filenameForDownload = generateNoticeFilename(noticeUuid, sdkVersion);
    if (responseOpt.isPresent()) {
      serveSdkXmlStringAsDownload(responseOpt.get(), xmlAsText, filenameForDownload);
    }
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

  /**
   * Get the SDK version.
   *
   * @param visualRoot root of the visual JSON
   */
  private static SdkVersion parseSdkVersion(final JsonNode visualRoot) {
    // Example: the SDK value looks like "eforms-sdk-1.1.0".
    // final String sdkVersionFieldId = ConceptualModel.FIELD_ID_SDK_VERSION;
    // final JsonNode visualField = visualRoot.get(sdkVersionFieldId);
    // final String eformsSdkVersion = getTextStrict(visualField, "value");

    // Use the shortcut we put at the virtual root top level:
    final String eformsSdkVersion =
        JsonUtils.getTextStrict(visualRoot, VisualModel.VIS_SDK_VERSION);
    Validate.notBlank(eformsSdkVersion, "virtual root eFormsSdkVersion is blank");

    final String sdkVersionStr = parseEformsSdkVersionText(eformsSdkVersion);

    logger.info("Found SDK version: {}, using {}", eformsSdkVersion, sdkVersionStr);

    // Load fields json depending of the correct SDK version.
    return new SdkVersion(sdkVersionStr);
  }

  public static String parseEformsSdkVersionText(final String eformsSdkVersion) {
    // If we have "eforms-sdk-1.1.0" we want "1.1.0".
    final String eformsSdkPrefix = SdkConstants.NOTICE_CUSTOMIZATION_ID_VERSION_PREFIX;
    Validate.isTrue(eformsSdkVersion.startsWith(eformsSdkPrefix),
        "Expecting sdk version to start with prefix=%s", eformsSdkPrefix);
    return eformsSdkVersion.substring(eformsSdkPrefix.length());
  }

  /**
   * Get the notice id, notice UUID.
   *
   * @param visualRoot Root of the visual JSON
   */
  private static UUID parseNoticeUuid(final JsonNode visualRoot) {
    // final JsonNode visualItem = visualRoot.get("BT-701-notice");
    // Validate.notNull(visualItem, "Json item holding notice UUID is null!");
    // final String uuidStr = getTextStrict(visualItem, "value");

    final String uuidStr = JsonUtils.getTextStrict(visualRoot, VisualModel.VIS_NOTICE_UUID);
    Validate.notBlank(uuidStr, "The notice UUID is blank!");

    final UUID uuidV4 = UUID.fromString(uuidStr);
    // The UUID supports multiple versions but we are only interested in version 4.
    // Instead of failing later, it is better to catch problems early!
    final int version = uuidV4.version();
    Validate.isTrue(version == 4, "Expecting notice UUID to be version 4 but found %s for %s",
        version, uuidStr);

    return uuidV4;
  }
}
