package eu.europa.ted.eforms.noticeeditor.helper.validation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.helper.notice.PhysicalModel;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;

/**
 * Demo implementation of a Common Validation Service (CVS) API client.
 *
 * <p>
 * There is no dependency on Spring, this relies on Jackson for the JSON and on Apache HTTP client
 * for the API calls (network).
 * </p>
 */
public class CvsApiClient {

  private static final Logger logger = LoggerFactory.getLogger(CvsApiClient.class);

  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final String MIME_TYPE_APPLICATION_JSON = "application/json";
  private static final String DEFAULT_USER_AGENT = "Editor demo (Java-SDK)";

  private static final String CVS_API_V1_VALIDATION = "v1/notices/validation";
  private static final String CVS_API_HTTP_HEADER_API_KEY = "X-API-Key";

  /**
   * The client is reusable.
   */
  private final CloseableHttpClient closeableHttpClient;

  private final String tedDevApiKey;
  private final String userAgent;
  private final String cvsApiRootUrl;
  private final ObjectMapper objectMapper;

  /**
   * @param closeableHttpClient A closeable HTTP client, see provided static method to build one
   * @param objectMapper The object mapper to be used for the JSON payload
   * @param cvsApiRootUrl The url to the desired CVS service
   * @param tedDevApiKey Your TED DEVELOPER API key
   * @param userAgent An optional user agent of your choice, it could help find back logs on the
   *        side of CVS or inside your network.
   */
  public CvsApiClient(final CloseableHttpClient closeableHttpClient,
      final ObjectMapper objectMapper, final String cvsApiRootUrl, final String tedDevApiKey,
      final Optional<String> userAgent) {
    this.closeableHttpClient = closeableHttpClient;
    this.tedDevApiKey = tedDevApiKey;
    this.userAgent = userAgent.orElse(DEFAULT_USER_AGENT);
    this.cvsApiRootUrl = cvsApiRootUrl;
    this.objectMapper = objectMapper;
  }

  /**
   * Uses the CVS API to validate the notice, returns the response body.
   *
   * @param noticeXml The notice XML text
   * @param svrlLangA2 Language to generate the SVRL report, for example "en" for English
   * @param eformsSdkVersion Specify the eForms SDK version to use for validating the XML document
   *        encoded in base64, if not specified the version contained in the XML will be used
   * @param sdkValidationMode Specify the validation mode that will be applied, selecting the
   *        corresponding sub-group of the eForms SDK version ("static" or "dynamic")
   * @return The response body, SVRL XML as text in this case
   */
  public String validateNoticeXml(final String noticeXml, final Optional<String> svrlLangA2,
      final Optional<String> eformsSdkVersion, final Optional<CsvValidationMode> sdkValidationMode)
      throws IOException {
    if (noticeXml == null || noticeXml.isEmpty()) {
      throw new RuntimeException("Expecting notice xml but it is blank.");
    }
    final String noticeInBase64 = toBase64(noticeXml);

    // How to build the API url and the JSON is internal knowledge.
    // It will be done here.

    // See: https://cvs.ted.europa.eu/swagger-ui/index.html#/notice-rest-controller/validate
    final String postUrl = this.cvsApiRootUrl + "/" + CVS_API_V1_VALIDATION;

    final String requestContentType = MIME_TYPE_APPLICATION_JSON; // Known to be JSON.
    final String responseContentType = "*/*"; // "application/xml"; (if valid it is xml, otherwise?)

    final ObjectNode jsonPayload = this.objectMapper.createObjectNode();
    {
      jsonPayload.put("notice", noticeInBase64);
      putIfPresent(jsonPayload, "language", svrlLangA2);

      final Optional<String> validationModeOpt =
          sdkValidationMode.isPresent() ? Optional.of(sdkValidationMode.get().getText())
              : Optional.empty();
      putIfPresent(jsonPayload, "validationMode", validationModeOpt);

      putIfPresent(jsonPayload, "eFormsSdkVersion", eformsSdkVersion);
    }

    return httpPostToCvs(postUrl, requestContentType, responseContentType, jsonPayload, svrlLangA2);
  }

  private static void putIfPresent(final ObjectNode jsonPayload, final String key,
      final Optional<String> eformsSdkVersion) {
    if (eformsSdkVersion.isPresent()) {
      jsonPayload.put(key, eformsSdkVersion.get());
    }
  }

  /**
   * @param responseHandler Configures how to handle the response
   * @return The response body
   */
  private String httpPostToCvs(final String postUrl, final String requestContentType,
      final String responseContentType, final ObjectNode jsonPayload,
      final Optional<String> svrlLangA2) throws IOException {
    //
    // SETUP HTTP POST.
    //
    final HttpPost post = new HttpPost(postUrl);

    //
    // HTTP POST HEADERS.
    //
    setupHeaders(requestContentType, responseContentType, post);

    //
    // HTTP POST ENTITY (payload).
    //
    post.setEntity(new StringEntity(jsonPayload.toString(), CHARSET));

    //
    // DEFINE HOW THE RESPONSE SHOULD BE HANDLED.
    //
    // https://docs.ted.europa.eu/api/endpoints/cvs-ted-europa-eu.html#_responses
    final ResponseHandler<String> responseHandler = new ResponseHandler<>() {
      @Override
      public String handleResponse(final HttpResponse response)
          throws IOException {
        final StatusLine statusLine = response.getStatusLine();
        final int status = statusLine.getStatusCode();
        logger.info("CVS responded with status={}", status);
        final HttpEntity entity = response.getEntity(); // It could be null.

        if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
          final String svrlText = entity != null ? EntityUtils.toString(entity) : null;
          EntityUtils.consumeQuietly(entity);

          try {
            // Create a JSON report from parts of the SVRL.
            final DocumentBuilder db =
                SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(false);
            final Document doc = db.parse(IOUtils.toInputStream(svrlText, CHARSET));
            doc.normalize();
            // final ObjectNode jsonReport = createJsonReport(doc, svrlLangA2);
            // return jsonReport;
          } catch (ParserConfigurationException e) {
            logger.error(e.toString(), e);
          } catch (SAXException e) {
            logger.error(e.toString(), e);
          }

          return svrlText;
        }

        // There was a problem.
        if (entity != null) {
          EntityUtils.consumeQuietly(entity);
        }
        final String msg = String.format("CVS POST response error: reason=%s, status=%s",
            statusLine.getReasonPhrase(), status);
        throw new CvsApiException(msg, status);
      }
    };

    //
    // EXECUTE THE HTTP POST REQUEST USING THE HTTP CLIENT.
    //
    // Execute returns the response object as generated by the response handler.
    logger.info("POST url={}", post.getURI());
    logger.info("Posting to CVS: wait ... (could stall if there are network issues, proxy, ...)");
    return this.closeableHttpClient.execute(post, responseHandler);
  }

  private void setupHeaders(final String requestContentType, final String responseContentType,
      final HttpPost post) {

    // Security, authentication / authorization related.
    post.setHeader(CVS_API_HTTP_HEADER_API_KEY, this.tedDevApiKey);
    post.setHeader("User-Agent", this.userAgent);

    // Mime types.
    final String utf8 = CHARSET.toString();
    post.setHeader("Content-Type", requestContentType + ";charset=" + utf8);
    post.setHeader("Accept", responseContentType);
    post.setHeader("Accept-Encoding", utf8);

    // Other headers (up to you ...)
    post.setHeader("DNT", "1");
    post.setHeader("Upgrade-Insecure-Requests", "1");
    post.setHeader("cache-control", "no-cache, no-store, max-age=0, must-revalidate");
  }

  /**
   * Provided for convenience.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS", justification = "It is OK here.")
  public static CloseableHttpClient createDefaultCloseableHttpClient(final int timeoutSeconds,
      final boolean redirectsEnabled, final String proxyUrl) {

    final HttpClientBuilder builder =
        HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy());

    if (!StringUtils.isBlank(proxyUrl)) {
      try {
        final URL proxy = new URL(proxyUrl);
        final String proxyHost = proxy.getHost();
        Validate.notBlank(proxyHost, "Proxy hostname is blank");

        final int proxyPort = proxy.getPort();

        final String userInfo = proxy.getUserInfo();
        Validate.isTrue(userInfo.contains(":"), "Proxy userInfo should contain :");

        final String[] userInfos = userInfo.split(":");
        Validate.isTrue(userInfos.length == 2, "Proxy userInfos should have length 2");

        final String proxyUsername = userInfos[0];
        Validate.notBlank(proxyUsername, "Proxy username is blank");

        final String proxyPass = userInfos[1];
        if (proxyPass.isBlank()) {
          throw new RuntimeException("Proxy pass is blank");
        }

        // Setup proxy credentials.
        // https://hc.apache.org/httpcomponents-client-4.5.x/examples.html
        // https://stackoverflow.com/questions/6962047/apache-httpclient-4-1-proxy-authentication
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort),
            new UsernamePasswordCredentials(proxyUsername, proxyPass));
        builder.setDefaultCredentialsProvider(credsProvider);

        // Setup proxy connection.
        final HttpHost proxyHttpHost = new HttpHost(proxyHost, proxyPort);
        final int timeoutMillis = timeoutSeconds * 1000;
        final RequestConfig defaultRequestConfig = RequestConfig.custom()//
            .setProxy(proxyHttpHost)//
            .setRedirectsEnabled(redirectsEnabled)//
            .setSocketTimeout(timeoutMillis)//
            .setConnectTimeout(timeoutMillis)//
            .setConnectionRequestTimeout(timeoutMillis)//
            .build();

        // By default use this config for all requests going through the client.
        builder.setDefaultRequestConfig(defaultRequestConfig);
      } catch (@SuppressWarnings("unused") MalformedURLException e) {
        throw new RuntimeException(
            "Malformed proxy url (not logging it as it could contain passwords).", e);
      }
    }

    // Reuse connections from a pool.
    // Adapt the settings to your needs.
    final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    connManager.setDefaultMaxPerRoute(5);
    connManager.setMaxTotal(10);
    connManager.setValidateAfterInactivity(60 * 1000);
    builder.setConnectionManager(connManager);

    return builder.build();
  }

  private static String getDocAsText(final Document doc)
      throws TransformerFactoryConfigurationError, TransformerConfigurationException,
      TransformerException, IOException {
    final Transformer transformer = TransformerFactory.newInstance().newTransformer();
    try (StringWriter writer = new StringWriter(1024)) {
      transformer.transform(new DOMSource(doc), new StreamResult(writer));
      return writer.toString();
    }
  }

  private static Document getXmlAsDoc(final Path xmlPath)
      throws ParserConfigurationException, SAXException, IOException {
    final DocumentBuilder db = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(false);
    return db.parse(xmlPath.toFile());
  }

  /**
   * @return The text encoded in base 64
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS", justification = "It is OK here.")
  public static String getNoticeXmlInBase64(final Path xmlPath)
      throws TransformerFactoryConfigurationError {
    try {
      final Document doc = getXmlAsDoc(xmlPath);
      final NodeList elements =
          doc.getDocumentElement().getElementsByTagName(PhysicalModel.CBC_CUSTOMIZATION_ID);
      final String eformsSdkVersionInFile = XmlUtils.getTextNodeContentOneLine(elements.item(0));
      logger.debug("eformsSdkVersionInFile={}", eformsSdkVersionInFile);
      doc.normalize();
      final String xmlAsText = getDocAsText(doc);
      return toBase64(xmlAsText);
    } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  static ObjectNode createJsonReport(final Document svrlDoc, final Optional<String> svrlLangA2) {

    final ObjectNode jsonReport = JsonUtils.createObjectNode();
    jsonReport.put("description", "Data extracted from the svrl report");
    jsonReport.put("timestamp", Instant.now().toString());

    // Find failed assertions.
    final ArrayNode jsonArr = jsonReport.putArray("failedAsserts");
    final NodeList failedAsserts = svrlDoc.getElementsByTagName("svrl:failed-assert");
    for (int i = 0; i < failedAsserts.getLength(); i++) {

      final Element failedAssert = (Element) failedAsserts.item(i);

      final String role = failedAssert.getAttribute("role"); // ERROR, WARN

      // TODO Skip WARN level??
      // if ("WARN".equals(role)) {
      // continue;
      // }

      final String flag = failedAssert.getAttribute("flag"); // Example: LAWFULNESS
      final String id = failedAssert.getAttribute("id");
      final String test = failedAssert.getAttribute("test");
      final String locationXpath = failedAssert.getAttribute("location");

      final Element textElem = XmlUtils.getDirectChild(failedAssert, "svrl:text");
      final String labelId = XmlUtils.getTextNodeContentOneLine(textElem);

      final ObjectNode jsonItem = JsonUtils.createObjectNode();
      jsonItem.put("id", id);
      jsonItem.put("labelId", labelId);
      jsonItem.put("role", role);
      // jsonItem.put("flag", flag);
      // jsonItem.put("test", test);

      if (locationXpath != null) {
        // TODO extract instance id from location xpath .../abcd[2]/...
        jsonItem.put("location", locationXpath);
      }

      // Diagnostic ref.
      // Example of diagnostic: ND-Company_BT-514-Organization-Company
      final Element diagnosticRef =
          XmlUtils.getDirectChild(failedAssert, "svrl:diagnostic-reference");

      // There may be no diagnostic in case the element is missing from the notice.
      if (diagnosticRef != null) {
        final String diagXpath = XmlUtils.getTextNodeContentOneLine(diagnosticRef);
        jsonItem.put("diagnosticXpath", diagXpath);

        // Complete XPath location of the element.
        final String diagnostic = diagnosticRef.getAttribute("diagnostic");

        final String nodeId;
        final String fieldId;
        final int indexOfUnderscore = diagnostic.indexOf('_');
        if (indexOfUnderscore > 0) {
          nodeId = diagnostic.substring(0, indexOfUnderscore);
          // In case of something like (c) inside of an id ... those are replaced by underscore.
          // BT-01_c_-Procedure
          // fieldId = diagnostic.substring(indexOfUnderscore, diagnostic.length());
        } else {
          nodeId = null;
          // fieldId = diagnostic;
        }
        jsonItem.put("nodeId", nodeId);
        // jsonItem.put("fieldId", fieldId); // TODO wait for TEDEFO-1758

        // TODO accumulate labelIds and get them all in one call ...or get labels from the UI later?
        // jsonItem.put("label", labelId + svrlLangA2 = label);
      }
      jsonArr.add(jsonItem);
    }
    return jsonReport;
  }

  /**
   * @return The text encoded in base 64
   * @throws UnsupportedEncodingException
   */
  private static String toBase64(final String text) throws UnsupportedEncodingException {
    return new String(Base64.getEncoder().encode(text.getBytes(CHARSET)), CHARSET.toString());
  }

}
