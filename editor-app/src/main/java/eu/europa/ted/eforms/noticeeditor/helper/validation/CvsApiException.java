package eu.europa.ted.eforms.noticeeditor.helper.validation;

import org.apache.http.client.ClientProtocolException;

/**
 * This is used to keep the status code.
 */
@SuppressWarnings("serial")
public class CvsApiException extends ClientProtocolException {
  private final String message;
  private final int statusCode;

  public CvsApiException(final String message, final int statusCode) {
    this.message = message;
    this.statusCode = statusCode;
  }

  @Override
  public String getMessage() {
    return String.format("Message=%s, statusCode=%s", message, statusCode);
  }

  public int getStatusCode() {
    return statusCode;
  }
}
