package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.List;

public class XmlSchemaInfo {
  private final List<String> rootOrder;

  public XmlSchemaInfo(List<String> rootOrder) {
    this.rootOrder = rootOrder;
  }

  public List<String> getRootOrder() {
    return rootOrder;
  }
}
