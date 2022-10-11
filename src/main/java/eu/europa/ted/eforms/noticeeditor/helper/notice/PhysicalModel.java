package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.w3c.dom.Document;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;

/**
 * Holds the XML representation.
 */
public class PhysicalModel {
  private final Document domDocument;

  public PhysicalModel(final Document document) {
    this.domDocument = document;
  }

  public Document getDomDocument() {
    return domDocument;
  }

  /**
   * @param indented True if the xml text should be indented, false otherwise.
   *
   * @return The XML as text.
   */
  public String getXmlAsText(final boolean indented) {
    return EditorXmlUtils.asText(domDocument, indented);
  }
}
