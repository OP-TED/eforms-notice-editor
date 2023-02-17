package eu.europa.ted.eforms.noticeeditor.genericode;

import javax.xml.namespace.NamespaceContext;
import org.springframework.util.xml.SimpleNamespaceContext;
import com.helger.jaxb.JAXBNamespacePrefixMapper;

/**
 * Custom prefix. Enforce use of gc to conform with EU files which also use gc.
 *
 * <p>
 * We do not want the JAXB ns3 prefix but we want gc namespace prefix.
 * </p>
 *
 * @see <a href=
 *      "https://download.oracle.com/javaee-archive/jaxb.java.net/users/2015/03/10743.html">Unmarshall
 *      and Marshalling does not produce the same XML</a>
 *
 */
public class CustomGenericodeNamespaceMapper extends JAXBNamespacePrefixMapper {

  public CustomGenericodeNamespaceMapper(final NamespaceContext anc) {
    super(anc);
  }

  public CustomGenericodeNamespaceMapper() {
    // Not sure about the namespace here but it seems to have no negative effect.
    // It is probably not used.
    super(new SimpleNamespaceContext());
  }

  @Override
  public String getPreferredPrefix(String snamespaceUri, String ssuggestion,
      boolean brequirePrefix) {
    return "gc"; // We want gc and not ns3.
  }
}
