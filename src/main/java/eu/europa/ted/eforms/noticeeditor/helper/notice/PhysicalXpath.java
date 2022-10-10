package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.Optional;

public class PhysicalXpath {
  private final String xpath;
  private final Optional<String> schemeNameOpt;

  public PhysicalXpath(String xpath, Optional<String> schemeNameOpt) {
    super();
    this.xpath = xpath;
    this.schemeNameOpt = schemeNameOpt;
  }

  public String getXpath() {
    return xpath;
  }

  public Optional<String> getSchemeNameOpt() {
    return schemeNameOpt;
  }

}
