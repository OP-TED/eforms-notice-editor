package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.Optional;

public class PhysicalXpath {
  private final String xpathExpr;
  private final Optional<String> schemeNameOpt;
  private final String tag;

  public PhysicalXpath(final String xpathExpr, final String tag,
      final Optional<String> schemeNameOpt) {
    super();
    this.xpathExpr = xpathExpr;
    this.schemeNameOpt = schemeNameOpt;
    this.tag = tag;
  }

  public String getTag() {
    return tag;
  }

  public String getXpathExpr() {
    return xpathExpr;
  }

  public Optional<String> getSchemeNameOpt() {
    return schemeNameOpt;
  }
}
