package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * XSD related tests.
 */
public class SchemaToolsTest {

  public static final String TEST_XSD =
      "src/test/resources/schemas/modified-EFORMS-BusinessRegistrationInformationNotice.xsd";

  @SuppressWarnings("static-method")
  @Test
  public void readXsdUsingApacheXmlSchemaTest() throws IOException {
    final XmlSchemaInfo schemaInfo = getTestSchemaInfo();
    final List<String> rootOrder = schemaInfo.getRootOrder();
    for (final String item : rootOrder) {
      System.out.println(item);
    }
    assertTrue(rootOrder.contains("ext:UBLExtensions"));
    assertTrue(rootOrder.contains("efac:NoticeSubType"));
  }

  public static XmlSchemaInfo getTestSchemaInfo() throws IOException {
    final Path pathToXsd = Path.of(TEST_XSD);
    final String rootTagName = "BusinessRegistrationInformationNotice";
    final XmlSchemaInfo schemaInfo = XmlSchemaTools.getSchemaInfo(pathToXsd, rootTagName);
    return schemaInfo;
  }

}
