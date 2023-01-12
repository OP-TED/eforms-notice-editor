package eu.europa.ted.eforms.noticeeditor.helper.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "proxy")
public class ProxyConfig {

  private final String hostname;
  private final int port;
  private final String username;
  private final String password;

  public ProxyConfig(String hostname, int port, String username, String password) {
    this.hostname = hostname;
    this.port = port;
    this.username = username;
    this.password = password;
  }

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String toString() {
    return hostname;
  }
}
