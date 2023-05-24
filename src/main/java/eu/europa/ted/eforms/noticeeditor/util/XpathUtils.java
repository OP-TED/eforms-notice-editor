package eu.europa.ted.eforms.noticeeditor.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.apache.commons.lang3.Validate;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeInfo;
import net.sf.saxon.lib.NamespaceConstant;

public class XpathUtils {

  private XpathUtils() {
    throw new AssertionError("Utility class.");
  }

  /**
   * The xpath instance is namespace aware and reusable. It allows to evaluate xpath expression
   * taking configured namespaces into account.
   *
   * @param docTypeInfo If the optional map is not present it will fallback on this to get the map
   * @param mapPreSdk16Opt Optional map of namespace URI by prefix
   */
  public static XPath setupXpathInst(final DocumentTypeInfo docTypeInfo,
      final Optional<Map<String, String>> mapPreSdk16Opt) {

    final Map<String, String> namespaceUriByPrefix =
        mapPreSdk16Opt.isPresent() ? mapPreSdk16Opt.get()
            : docTypeInfo.buildAdditionalNamespaceUriByPrefix();

    return setupXpathInst(namespaceUriByPrefix);
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS",
      justification = "Checked to Runtime OK here")
  private static XPath setupXpathInst(final Map<String, String> namespaceUriByPrefix) {
    // Also allow reading XSD files using the same xpath instance.
    namespaceUriByPrefix.put("xsd", XMLConstants.W3C_XML_SCHEMA_NS_URI);

    //
    // NAMESPACES FOR XPATH.
    //
    try {
      // Why Saxon HE lib: namespaces were not working with the JDK (Java 15).
      final String objectModelSaxon = NamespaceConstant.OBJECT_MODEL_SAXON;
      System.setProperty("javax.xml.xpath.XPathFactory:" + objectModelSaxon,
          "net.sf.saxon.xpath.XPathFactoryImpl");
      final XPath xpathInst = XPathFactory.newInstance(objectModelSaxon).newXPath();

      // Custom namespace context.
      // https://stackoverflow.com/questions/13702637/xpath-with-namespace-in-java
      final NamespaceContext namespaceCtx = new NamespaceContext() {
        @Override
        public String getNamespaceURI(final String prefix) {
          final String namespaceUri = namespaceUriByPrefix.get(prefix);
          Validate.notBlank(namespaceUri, "Namespace is blank for prefix=%s", prefix);
          return namespaceUri;
        }

        @Override
        public String getPrefix(final String uri) {
          return null;
        }

        @Override
        public Iterator<String> getPrefixes(final String namespaceURI) {
          return null;
        }
      };
      xpathInst.setNamespaceContext(namespaceCtx);
      return xpathInst;

    } catch (final XPathFactoryConfigurationException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * @param xpath A valid xpath string but it should not start with / except if it is just the root
   *        "/*", example "efbc:ParameterCode[@listName='number-threshold']"
   * @return The xpath string split by slash but with predicates ignored
   */
  public static String[] getXpathPartsWithoutPredicates(final String xpath) {
    Validate.notBlank(xpath, "xpath is blank");
    if ("/*".equals(xpath)) {
      return new String[] {"/*"};
    }
    Validate.isTrue(!xpath.startsWith("/"));
    final StringBuilder sb = new StringBuilder(xpath.length());
    int stacked = 0;
    for (int i = 0; i < xpath.length(); i++) {
      final char ch = xpath.charAt(i);
      if (ch == '[') {
        stacked++;
      }
      if (stacked == 0) {
        sb.append(ch);
      }
      if (ch == ']') {
        stacked--;
        Validate.isTrue(stacked >= 0, "stacked is < 0 for %s", xpath);
      }
    }
    return sb.toString().split("/");
  }
}

