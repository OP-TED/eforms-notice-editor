package eu.europa.ted.eforms.noticeeditor.helper.validation;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XsdValidator {

  private static final Logger logger = LoggerFactory.getLogger(XsdValidator.class);

  public static List<SAXParseException> validateXml(final String xmlAsText, final Path mainXsdPath)
      throws SAXException, IOException {
    logger.info("Attempting to validate using schema: {}", mainXsdPath);

    final XsdCustomErrorHandler xsdErrorHandler = new XsdCustomErrorHandler();

    // validateUsingDom(xmlAsText, xsdErrorHandler);
    validateUsingSchema(xmlAsText, mainXsdPath, xsdErrorHandler);

    // Show exceptions.
    final List<SAXParseException> exceptions = xsdErrorHandler.getExceptions();
    exceptions.forEach(ex -> logger.error(ex.getMessage()));

    return exceptions;
  }

  private static void validateUsingSchema(final String xmlAsText, final Path mainXsdPath,
      final org.xml.sax.ErrorHandler xsdErrorHandler)
      throws SAXException, IOException {

    final SchemaFactory schemaFactory =
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    // schemaFactory.setResourceResolver(new ResourceResolver);
    schemaFactory.setErrorHandler(xsdErrorHandler);
    schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
    schemaFactory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);

    final Schema schema = schemaFactory.newSchema(mainXsdPath.toFile());
    final Validator validator = schema.newValidator();
    validator.validate(new StreamSource(new StringReader(xmlAsText)));
  }

}
