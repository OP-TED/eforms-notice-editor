package eu.europa.ted.eforms.noticeeditor.genericode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.helger.genericode.v10.CodeListDocument;

/**
 * Helps loading .gc files (in XML but specific schema).
 */
public class GenericodeTools {

  public static final CodeListDocument parseGenericode(final InputStream is,
      @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "OCP_OVERLY_CONCRETE_PARAMETER",
          justification = "We want to be precise here.") //
      final CustomGenericodeMarshaller marshaller) {
    // NOTE: this relies on com.helger.genericode
    return marshaller.read(is);
  }

  public static final CustomGenericodeMarshaller getMarshaller() {
    // https://stackoverflow.com/questions/7400422/jaxb-creating-context-and-marshallers-cost

    // JAXBContext is thread safe and should only be created once and reused to avoid the cost of
    // initializing the metadata multiple times. Marshaller and Unmarshaller are not thread safe,
    // but are lightweight to create and could be created per operation.

    final CustomGenericodeMarshaller marshaller = new CustomGenericodeMarshaller();
    marshaller.setCharset(StandardCharsets.UTF_8);
    marshaller.setFormattedOutput(true);
    marshaller.setIndentString(" "); // Official EU .gc files seem to be using one tab.

    return marshaller;
  }
}
