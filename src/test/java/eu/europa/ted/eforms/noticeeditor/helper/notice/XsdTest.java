package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.junit.jupiter.api.Test;

public class XsdTest {

  @SuppressWarnings("static-method")
  @Test
  public void readXsdUsingApacheXmlSchemaTest() throws IOException {
    final Path pathToXsd =
        Path.of("src/test/resources/schemas/EFORMS-BusinessRegistrationInformationNotice.xsd");
    try (InputStream is = Files.newInputStream(pathToXsd)) {
      final XmlSchemaCollection schemaCol = new XmlSchemaCollection();
      final XmlSchema schema = schemaCol.read(new StreamSource(is));

      // schema.getElementByName("BusinessRegistrationInformationNotice");
      final String rootTagName = "BusinessRegistrationInformationNotice";
      final String rootTypeName = rootTagName + "Type";
      final XmlSchemaType type = schema.getTypeByName(rootTypeName);

      final List<String> order = getSequenceTagNamesAsList(type);
      for (final String item : order) {
        System.out.println(item);
      }

      // We are interested in the sequence for the element sort order.
      // <xsd:complexType name="BusinessRegistrationInformationNoticeType">
      // <xsd:sequence>
      // <xsd:element ref="ext:UBLExtensions" minOccurs="0" maxOccurs="1"/>
    }
  }

  /**
   * @return List containing the tag names (including prefix) in the order of the sequence.
   */
  private static List<String> getSequenceTagNamesAsList(final XmlSchemaType type) {
    final List<String> order = new ArrayList<>();
    if (type instanceof XmlSchemaComplexType) {
      // Get all particles associated with that element Type
      final XmlSchemaParticle allParticles = ((XmlSchemaComplexType) type).getParticle();
      final XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence) allParticles;
      final List<XmlSchemaSequenceMember> items = xmlSchemaSequence.getItems();
      items.forEach(item -> {
        final XmlSchemaElement itemElements = (XmlSchemaElement) item;

        final QName targetQName = itemElements.getRef().getTargetQName();
        final String tagName = targetQName.getPrefix() + ":" + targetQName.getLocalPart();
        order.add(tagName);

        // schemaElements.add(itemElements);
        // addChild(element.getQName(), itemElements);
        // Call method recursively to get all subsequent element
        // getChildElementNames(itemElements);
        // schemaElements = new ArrayList<XmlSchemaElement>();
      });
    }
    return order;
  }
}
