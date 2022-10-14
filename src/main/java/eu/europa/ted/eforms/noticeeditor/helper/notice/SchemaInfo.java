package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.util.List;

public class SchemaInfo {
  private List<String> rootOrder;

  public SchemaInfo(List<String> rootOrder) {
    super();
    this.rootOrder = rootOrder;
  }

  public List<String> getRootOrder() {
    return rootOrder;
  }

  public void setRootOrder(List<String> rootOrder) {
    this.rootOrder = rootOrder;
  }


}
