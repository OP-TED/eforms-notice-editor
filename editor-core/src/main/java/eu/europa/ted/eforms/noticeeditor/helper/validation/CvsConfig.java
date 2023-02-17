package eu.europa.ted.eforms.noticeeditor.helper.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "client.cvs")
public class CvsConfig {

  private final String apiKey;
  private final String url;

  public CvsConfig(final String apiKey, final String url) {
    this.apiKey = apiKey;
    this.url = url;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getUrl() {
    return url;
  }

}
