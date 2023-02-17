package eu.europa.ted.eforms.noticeeditor.helper.validation;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public final class XsdCustomErrorHandler implements ErrorHandler {
  private final List<SAXParseException> exceptions = new ArrayList<>();

  public List<SAXParseException> getExceptions() {
    return exceptions;
  }

  @Override
  public void warning(SAXParseException exception) {
    exceptions.add(exception);
  }

  @Override
  public void error(SAXParseException exception) {
    exceptions.add(exception);
  }

  @Override
  public void fatalError(SAXParseException exception) {
    exceptions.add(exception);
  }
}
