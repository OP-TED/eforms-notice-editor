package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.lang3.Validate;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaTools {
  private static final Logger logger = LoggerFactory.getLogger(SchemaTools.class);

  public static SchemaInfo getSchemaInfo(final Path pathToXsd, final String rootTagName)
      throws IOException {
    logger.debug("Attempting to read schema info (xsd).");
    // try {
    try (InputStream is = Files.newInputStream(pathToXsd)) {

      final XmlSchemaCollection schemaCol = new XmlSchemaCollection();
      // final DocumentBuilder build =
      // SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

      // java.xml/com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity
      // (XMLEntityManager.java:652)

      // java.io.FileNotFoundException:
      // C:\Users\rouschr\dev\eclipse-workspace\tedefo
      // \eforms-notice-editor\UBL-CommonAggregateComponents-2.3.xsd
      // (The system cannot find the file specified)

      final XmlSchema schema = schemaCol.read(new StreamSource(is));

      // final Document xsdDoc = build.parse(is);
      // final Document xsdDoc = build.parse(pathToXsd.toFile());
      // final XmlSchema schema = schemaCol.read(xsdDoc);
      // final XmlSchema schema = schemaCol.read(xsdDoc);

      final String rootTypeName = rootTagName + "Type";
      final XmlSchemaType type = schema.getTypeByName(rootTypeName);
      Validate.notNull(type, "Type is null for rootTypeName=%s", rootTypeName);

      // We are interested in the sequence for the element sort order.
      // Example:
      // <xsd:complexType name="BusinessRegistrationInformationNoticeType">
      // <xsd:sequence>
      // <xsd:element ref="ext:UBLExtensions" minOccurs="0" maxOccurs="1"/>
      // <xsd:element ...

      final List<String> rootOrder = getSequenceTagNamesAsList(type);

      return new SchemaInfo(rootOrder);
      // } catch (ParserConfigurationException e) {
      // throw new RuntimeException(e);
      // } catch (SAXException e) {
      // throw new RuntimeException(e);
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
