package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;

public class SchemaTools {

  public static SchemaInfo getSchemaInfo(final Path pathToXsd, final String rootTagName)
      throws IOException {
    try {
      // try (InputStream is = Files.newInputStream(pathToXsd)) {

      final XmlSchemaCollection schemaCol = new XmlSchemaCollection();

      final DocumentBuilder build = SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);
      final Document xsdDoc = build.parse(pathToXsd.toFile());

      final XmlSchema schema = schemaCol.read(xsdDoc);

      System.out.println("folder=" + pathToXsd.toFile().getParentFile().getAbsolutePath());
      // TODO tttt probably fails here because I ran the editor through mvn exec:java

      // Reading from input stream works but it gets lost with the <xsd:import> later.
      // Maybe this has something to do with running the project via Maven.
      // final XmlSchema schema = schemaCol.read(new StreamSource(is));

      final String rootTypeName = rootTagName + "Type";
      final XmlSchemaType type = schema.getTypeByName(rootTypeName);

      // We are interested in the sequence for the element sort order.
      // Example:
      // <xsd:complexType name="BusinessRegistrationInformationNoticeType">
      // <xsd:sequence>
      // <xsd:element ref="ext:UBLExtensions" minOccurs="0" maxOccurs="1"/>
      // <xsd:element ...

      final List<String> rootOrder = getSequenceTagNamesAsList(type);

      return new SchemaInfo(rootOrder);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return List containing the tag names (including prefix) in the order of the sequence.
   */
  static List<String> getSequenceTagNamesAsList(final XmlSchemaType type) {
    final List<String> order = new ArrayList<>();
    if (type instanceof XmlSchemaComplexType) {
      final XmlSchemaParticle allParticles = ((XmlSchemaComplexType) type).getParticle();
      final XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence) allParticles;
      final List<XmlSchemaSequenceMember> items = xmlSchemaSequence.getItems();
      items.forEach(item -> {
        final XmlSchemaElement itemElements = (XmlSchemaElement) item;
        final QName targetQName = itemElements.getRef().getTargetQName();
        final String tagName = targetQName.getPrefix() + ":" + targetQName.getLocalPart();
        order.add(tagName);
      });
    }
    return order;
  }
}
