package eu.europa.ted.eforms.noticeeditor.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.helper.validation.CsvValidationMode;
import eu.europa.ted.eforms.noticeeditor.helper.validation.CvsApiClient;
import eu.europa.ted.eforms.noticeeditor.helper.validation.CvsConfig;
import eu.europa.ted.eforms.noticeeditor.helper.validation.XsdValidator;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Demo implementation of how to validate a notice.
 */
@Service
public class NoticeValidationService {
  private static final Logger logger = LoggerFactory.getLogger(NoticeValidationService.class);

  @org.springframework.beans.factory.annotation.Value("${eforms.sdk.path}")
  private String eformsSdkPath;

  private final CvsConfig cvsConfig;
  private final ObjectMapper objectMapper;
  private final CloseableHttpClient httpClient;

  @Autowired
  public NoticeValidationService(final CvsConfig cvsConfig,
      @Value("${proxy.url:}") final String proxyUrl) {
    this.cvsConfig = cvsConfig;

    // It can be reused.
    this.objectMapper = JsonUtils.getStandardJacksonObjectMapper();

    final int timeoutSeconds = 8;
    this.httpClient = CvsApiClient.createDefaultCloseableHttpClient(timeoutSeconds, true, proxyUrl);
  }

  public ObjectNode validateNoticeUsingXsd(final UUID noticeUuid, final SdkVersion sdkVersion,
      final String noticeXmlText, final Optional<Path> mainXsdPathOpt)
      throws SAXException, IOException {

    // Create a JSON report about the errors.
    logger.info("Attempting to validate notice using XSD.");
    final ObjectNode xsdReport = objectMapper.createObjectNode();
    xsdReport.put("noticeUuid", noticeUuid.toString());
    xsdReport.put("sdkVersion", sdkVersion.toString());
    xsdReport.put("timestamp", Instant.now().toString());

    if (mainXsdPathOpt.isPresent()) {
      final Path mainXsdPath = mainXsdPathOpt.get();

      final List<SAXParseException> validationExceptions = new ArrayList<>();
      try {
        validationExceptions.addAll(XsdValidator.validateXml(noticeXmlText, mainXsdPath));
      } catch (SAXParseException e) {
        validationExceptions.add(e);
      }
      xsdReport.put("errorCount", validationExceptions.size());

      if (!validationExceptions.isEmpty()) {
        final ArrayNode xsdErrors = xsdReport.putArray("xsdErrors");
        for (SAXParseException ex : validationExceptions) {
          logger.error(ex.toString(), ex);
          final ObjectNode xsdError = objectMapper.createObjectNode();
          xsdError.put("lineNumber", ex.getLineNumber());
          xsdError.put("columnNumber", ex.getColumnNumber());
          xsdError.put("message", ex.getMessage());
          xsdErrors.add(xsdError);
        }
      }
    } else {
      // This problem is related to how the XML is build and sorted, relying on the SDK definition
      // of all namespaces.
      final String message = String.format(
          "This XSD validation feature is not supported in the editor demo for SDK version=%s",
          sdkVersion);
      xsdReport.put("Unsupported", message);
    }
    return xsdReport;
  }

  /**
   * @param noticeSdkVersion The notice SDK version, this is required information!
   * @param noticeXml The notice XML text
   * @param csvSdkVersionOverride An optional SDK version in case it does not work with the desired
   *        version, if not provided the version found in the notice XML will be used
   * @param svrlLangA2 The language the svrl messages should be in
   * @return The response body
   */
  public String validateNoticeXmlUsingCvs(
      final SdkVersion noticeSdkVersion, final String noticeXml,
      final Optional<SdkVersion> csvSdkVersionOverride, final Optional<String> svrlLangA2,
      final Optional<CsvValidationMode> sdkValidationMode) throws IOException {
    logger.info(
        "Attempting to validate notice using the CVS, notice SDK version={}, eformsSdkVersionOverride={}",
        noticeSdkVersion, csvSdkVersionOverride);
    Validate.notNull(noticeSdkVersion, "noticeSdkVersion is null");

    // https://docs.ted.europa.eu/api/index.html
    // TED Developer Portal API KEY.
    final String tedDevApiKey = cvsConfig.getApiKey();
    Validate.notBlank(tedDevApiKey, "Your CVS API key is not configured, see application.yaml");

    final String cvsApiRootUrl = cvsConfig.getUrl();
    Validate.notBlank(cvsApiRootUrl, "The CVS URL is not configured, see application.yaml");

    final CvsApiClient cvsClient =
        new CvsApiClient(httpClient, objectMapper, cvsApiRootUrl, tedDevApiKey, Optional.empty());

    //
    // Call the CVS API.
    //
    final String responseBody =
        cvsClient.validateNoticeXml(noticeSdkVersion, noticeXml, svrlLangA2,
            csvSdkVersionOverride, sdkValidationMode, Path.of(eformsSdkPath));

    return responseBody;
  }
}

