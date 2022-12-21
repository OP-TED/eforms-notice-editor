package eu.europa.ted.eforms.noticeeditor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlUtils {

  /**
   * @return A string based on the given id, that is safe to use in XML for the xsd:ID type. As this
   *         type does not allow parentheses or '|', they are replaced by '_'
   */
  public static String makeIdSafe(String id) {
    final String safeId = id.replace('(', '_').replace(')', '_').replace('|', '_');
    return safeId;
  }

  /**
   * @param attributeName Name of the XML element attribute
   *
   * @return The text value of the attribute, it fails with a special message if null
   */
  public static String getAttrText(final NamedNodeMap attributes, final String attributeName) {
    final Node attr = attributes.getNamedItem(attributeName);
    Validate.notNull(attr, "attribute is null for attributeName=%s", attributeName);
    return attr.getTextContent();
  }

  /**
   * @return The first element found by tag name, it will fail if there is not exactly one element
   */
  public static Node getElementByTagName(final Document doc, final String tagName) {
    final NodeList elements = doc.getElementsByTagName(tagName);
    Validate.isTrue(elements.getLength() == 1, "Assuming one element for tagName=%s", tagName);
    return elements.item(0);
  }

  /**
   * @return The text content of first element found by tag name, otherwise empty. It will fail if
   *         there is more than one element
   */
  public static Optional<String> getTextByTagNameOpt(final Document doc, final String tagName) {
    final NodeList elements = doc.getElementsByTagName(tagName);
    final int len = elements.getLength();
    Validate.isTrue(len == 1 || len == 0, "Assuming max one element for tagName=%s", tagName);
    if (len == 1) {
      return Optional.of(elements.item(0).getTextContent());
    }
    return Optional.empty();
  }

  /**
   * @return The first element found by the tag name if present, empty otherwise. It will fail if
   *         there is more than one element
   */
  public static Optional<Element> getElementByTagNameOpt(final Document doc, final String tagName) {
    final NodeList elements = doc.getElementsByTagName(tagName);
    final int len = elements.getLength();
    Validate.isTrue(len == 1 || len == 0, "Assuming max one element for tagName=%s", tagName);
    if (len == 1) {
      return Optional.of((Element) elements.item(0));
    }
    return Optional.empty();
  }

  /**
   * @return The first direct child matching the tag name inside of parent, null otherwise
   */
  public static Element getDirectChild(final Element parent, final String tagName) {
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element && tagName.equals(child.getNodeName())) {
        return (Element) child;
      }
    }
    return null;
  }

  /**
   * @return The direct children matching the tag name inside of parent, the list may be empty if
   *         nothing matched
   */
  public static List<Element> getDirectChildren(final Element parent, final String tagName) {
    final List<Element> elements = new ArrayList<>();
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element && tagName.equals(child.getNodeName())) {
        elements.add((Element) child);
      }
    }
    return elements;
  }

}
