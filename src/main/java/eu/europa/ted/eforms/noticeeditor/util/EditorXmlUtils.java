package eu.europa.ted.eforms.noticeeditor.util;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class EditorXmlUtils {

  /**
   * @param doc The document to transform to text
   * @param indented Indent if true
   *
   * @return The XML as text
   */
  public static String asText(final Document doc, final boolean indented) {
    try {
      final StringWriter stringWriter = new StringWriter();
      final TransformerFactory factory = TransformerFactory.newInstance();
      final Transformer transformer = factory.newTransformer();

      transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.toString());
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");

      if (indented) {
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      }

      transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
      return stringWriter.toString();
    } catch (Exception ex) {
      throw new RuntimeException("Error converting to String", ex);
    }
  }


  public static String getNodePath(final Node node) {
    if (node == null) {
      throw new IllegalArgumentException("Node cannot be null");
    }
    StringBuilder pathBuilder = new StringBuilder("/");
    pathBuilder.append(node.getNodeName());

    Node currentNode = node;
    if (currentNode.getNodeType() != Node.DOCUMENT_NODE) {
      while (currentNode.getParentNode() != null) {
        currentNode = currentNode.getParentNode();

        if (currentNode.getNodeType() == Node.DOCUMENT_NODE) {
          break;
        } else if (getIndexOfArrayNode(currentNode) != null) {
          pathBuilder.insert(0,
              "/" + currentNode.getNodeName() + "[" + getIndexOfArrayNode(currentNode) + "]");
        } else {
          pathBuilder.insert(0, "/" + currentNode.getNodeName());
        }
      }
    }

    return pathBuilder.toString();
  }

  private static boolean isArrayNode(Node node) {
    if (node.getNextSibling() == null && node.getPreviousSibling() == null) {
      // Node has no siblings
      return false;
    }
    // Check if node siblings are of the same name. If so, then we are inside an array.
    return (node.getNextSibling() != null
        && node.getNextSibling().getNodeName().equalsIgnoreCase(node.getNodeName()))
        || (node.getPreviousSibling() != null
            && node.getPreviousSibling().getNodeName().equalsIgnoreCase(node.getNodeName()));
  }

  private static Integer getIndexOfArrayNode(Node node) {
    if (isArrayNode(node)) {
      int leftCount = 0;
      Node currentNode = node.getPreviousSibling();
      while (currentNode != null) {
        leftCount++;
        currentNode = currentNode.getPreviousSibling();
      }
      return leftCount;
    }
    return null;
  }

}
