package eu.europa.ted.eforms.noticeeditor.util;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

public class EditorXmlUtils {

  public static String asText(final Document doc) {
    try {
      final StringWriter stringWriter = new StringWriter();
      final TransformerFactory factory = TransformerFactory.newInstance();
      final Transformer transformer = factory.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.toString());
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
      return stringWriter.toString();
    } catch (Exception ex) {
      throw new RuntimeException("Error converting to String", ex);
    }
  }

}
