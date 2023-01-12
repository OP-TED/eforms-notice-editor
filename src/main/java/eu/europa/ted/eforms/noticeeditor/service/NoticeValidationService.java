package eu.europa.ted.eforms.noticeeditor.service;

import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ted.eforms.noticeeditor.helper.validation.CsvValidationMode;
import eu.europa.ted.eforms.noticeeditor.helper.validation.CvsApiClient;
import eu.europa.ted.eforms.noticeeditor.helper.validation.CvsConfig;
import eu.europa.ted.eforms.noticeeditor.helper.validation.ProxyConfig;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

/**
 * Demo implementation of how to validate a notice.
 */
@Service
public class NoticeValidationService {
  private static final Logger logger = LoggerFactory.getLogger(NoticeValidationService.class);

  private final CvsConfig cvsConfig;
  private final ObjectMapper objectMapper;
  private final CloseableHttpClient httpClient;

  @Autowired
  public NoticeValidationService(final CvsConfig cvsConfig, final ProxyConfig proxyConfig) {
    this.cvsConfig = cvsConfig;

    // It can be reused.
    this.objectMapper = JsonUtils.getStandardJacksonObjectMapper();

    // The HTTP client and established connections of the client can also be reused.
    // The disadvantage would be that if the proxy config changes the server would need to be
    // restarted as the config is set inside the client.
    final Optional<ProxyConfig> proxyConfigOpt;
    if (proxyConfig.getUsername() != null && proxyConfig.getPassword() != null) {
      proxyConfigOpt = Optional.of(proxyConfig);
    } else {
      proxyConfigOpt = Optional.empty();
    }
    final int timeoutSeconds = 8;
    this.httpClient =
        CvsApiClient.createDefaultCloseableHttpClient(timeoutSeconds, true, proxyConfigOpt);
  }

  /**
   * @param noticeXml The notice XML text
   * @param eformsSdkVersion An optional SDK version in case it does not work with the desired
   *        version, if not provided the version found in the notice XML will be used
   * @param svrlLangA2 The language the svrl messages should be in
   * @return The response body
   */
  public String validateNoticeXml(final String noticeXml, final Optional<String> eformsSdkVersion,
      final Optional<String> svrlLangA2, final Optional<CsvValidationMode> sdkValidationMode)
      throws IOException {
    logger.info("Attempting to validate notice using the CVS");

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
    final String responseBody = cvsClient.validateNoticeXmlUsing(noticeXml, svrlLangA2,
        eformsSdkVersion, sdkValidationMode);

    return responseBody;
  }
}

