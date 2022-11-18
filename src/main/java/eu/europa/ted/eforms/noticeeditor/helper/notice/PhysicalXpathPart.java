package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.Optional;

/**
 * Holds data about an xpath fragment.
 */
public class PhysicalXpathPart {
  private final String xpathExpr;
  private final Optional<String> schemeNameOpt;
  private final String tagOrAttr;

  public PhysicalXpathPart(final String xpathExpr, final String tagOrAttr,
      final Optional<String> schemeNameOpt) {
    this.xpathExpr = xpathExpr;
    this.schemeNameOpt = schemeNameOpt;
    this.tagOrAttr = tagOrAttr;
  }

  /**
   * @return An xml tag, or in rare cases an attribute
   */
  public String getTagOrAttribute() {
    return tagOrAttr;
  }

  /**
   * @return An xpath expression
   */
  public String getXpathExpr() {
    return xpathExpr;
  }

  /**
   * @return An optional extracted schemeName, use it if if present
   */
  public Optional<String> getSchemeNameOpt() {
    return schemeNameOpt;
  }
}
