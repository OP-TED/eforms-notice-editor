package eu.europa.ted.eforms.noticeeditordemo.genericode;

import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.helger.genericode.Genericode10CodeListMarshaller;

/**
 * Based on the Genericode10CodeListMarshaller.
 *
 * <p>
 * In case there is a new genericode standard 1.1 or 2.0, we can just change the extends.
 * </p>
 *
 * <p>
 * Enforces the gc prefix.
 * </p>
 */
public class CustomGenericodeMarshaller extends Genericode10CodeListMarshaller {

  private static final Logger logger = LoggerFactory.getLogger(CustomGenericodeMarshaller.class);

  @Override
  protected void customizeMarshaller(final Marshaller marsh) {
    super.customizeMarshaller(marsh);
    try {
      marsh.setProperty("com.sun.xml.bind.namespacePrefixMapper",
          new CustomGenericodeNamespaceMapper());
    } catch (PropertyException e) {
      logger.error(e.toString(), e);
    }
  }

}
