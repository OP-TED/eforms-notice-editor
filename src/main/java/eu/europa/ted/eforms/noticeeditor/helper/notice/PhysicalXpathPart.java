package eu.europa.ted.eforms.noticeeditor.helper.notice;

/**
 * Holds data about an xpath fragment.
 */
public class PhysicalXpathPart {
  private final String xpathExpr;
  private final String tagOrAttr;

  public PhysicalXpathPart(final String xpathExpr, final String tagOrAttr) {
    this.xpathExpr = xpathExpr;
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

}
