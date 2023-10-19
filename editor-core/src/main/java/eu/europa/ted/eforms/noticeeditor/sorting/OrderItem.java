package eu.europa.ted.eforms.noticeeditor.sorting;

import java.util.Objects;

public class OrderItem implements Comparable<OrderItem> {
  private final String fieldOrNodeId;
  private final String xmlName;
  private final Integer order;

  public OrderItem(String fieldOrNodeId, String xmlName, Integer order) {
    this.fieldOrNodeId = fieldOrNodeId;
    this.xmlName = xmlName;
    this.order = order;
  }

  public String getFieldOrNodeId() {
    return fieldOrNodeId;
  }

  public String getXmlName() {
    return xmlName;
  }

  public Integer getOrder() {
    return order;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldOrNodeId, order, xmlName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    OrderItem other = (OrderItem) obj;
    return Objects.equals(fieldOrNodeId, other.fieldOrNodeId) && Objects.equals(order, other.order)
        && Objects.equals(xmlName, other.xmlName);
  }

  @Override
  public int compareTo(OrderItem o) {
    return this.order.compareTo(o.getOrder());
  }

  @Override
  public String toString() {
    return "OrderItem [fieldOrNodeId=" + fieldOrNodeId + ", xmlName=" + xmlName + ", order=" + order
        + "]";
  }
}
