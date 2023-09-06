package eu.europa.ted.eforms.noticeeditor.helper.validation;

import org.apache.http.client.ClientProtocolException;

/**
 * This is used to keep the status code.
 */
@SuppressWarnings("serial")
public class CvsApiException extends ClientProtocolException {
  private final String message;
  private final int statusCode;
  private final String entityJson;

  public CvsApiException(final String message, final int statusCode, final String entityJson) {
    this.statusCode = statusCode;
    this.message = message;
    this.entityJson = entityJson;
  }

  @Override
  public String getMessage() {
    return String.format("errorCode=%s, message=%s", statusCode, message);
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getEntityJson() {
    return entityJson;
  }

}
