package eu.europa.ted.eforms.noticeeditor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlUtils {

  public static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);

  private XmlUtils() {
    throw new AssertionError("Utility class.");
  }

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
   * @param attributeName Name of the XML element attribute
   *
   * @return The text value of the attribute, it fails with a special message if null
   */
  public static String getAttrText(final Element elem, final String attributeName) {
    final Attr attr = elem.getAttributeNode(attributeName);
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

  /**
   * Evaluates xpath and returns a nodelist. Note: this works when the required notice values are
   * present as some xpath may rely on their presence (codes, indicators, ...).
   *
   * @param xpathInst The XPath instance (reusable)
   * @param contextElem The XML context element in which the xpath is evaluated
   * @param xpathExpr The XPath expression relative to the passed context
   * @param idForError An identifier which is shown in case of errors
   * @return The result of evaluating the XPath expression as a NodeList
   */
  public static NodeList evaluateXpath(final XPath xpathInst, final Object contextElem,
      final String xpathExpr, String idForError) {
    Validate.notBlank(xpathExpr, "xpathExpr is blank for %s, %s", contextElem, idForError);
    try {
      // A potential optimization would be to reuse some of the compiled xpath for some expressions.
      // final NodeList nodeList =
      // (NodeList) xPathInst.compile(xpathExpr).evaluate(contextElem, XPathConstants.NODESET);

      final NodeList nodeList =
          (NodeList) xpathInst.evaluate(xpathExpr, contextElem, XPathConstants.NODESET);

      return nodeList;
    } catch (XPathExpressionException e) {
      logger.error("Problem with xpathExpr={}, %s", xpathExpr, idForError);
      throw new RuntimeException(e);
    }
  }

}
