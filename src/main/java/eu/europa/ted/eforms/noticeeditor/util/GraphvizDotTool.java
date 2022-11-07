package eu.europa.ted.eforms.noticeeditor.util;

import org.apache.commons.lang3.StringUtils;

public class GraphvizDotTool {

  public static void appendEdge(final String label, final String color, final String from,
      final String to, final StringBuilder sb) {
    sb.append(String.format("\"%s\" -> \"%s\"", from, to));
    boolean notBlankLabel = StringUtils.isNotBlank(label);
    boolean notBlankColor = StringUtils.isNotEmpty(color);
    if (notBlankLabel || notBlankColor) {
      sb.append(" [ ");
      if (notBlankLabel) {
        sb.append(String.format("label=\"%s\"", label));
      }
      if (notBlankColor) {
        if (notBlankLabel) {
          sb.append(", ");
        }
        sb.append("color=").append(color);
      }
      sb.append(" ]");
    }
    sb.append(";\n");
  }

  /**
   * @param title Title of the graph
   * @param description Description or legend of the graph
   * @param shapeBox Box shape around text, can be a bit more compact
   * @param leftToRight Root is left and goes right, more like a file system
   */
  public static void appendDiGraph(final String dotContent, final StringBuilder sb,
      final String title, final String description, final boolean shapeBox,
      final boolean leftToRight) {
    sb//
        .append("digraph " + title + " {\n")//
        .append("label=\"" + description + "\"\n");

    if (shapeBox) {
      sb.append("node [shape=box]\n");
    }
    if (leftToRight) {
      sb.append("rankdir=\"LR\"\n");
      // sb.append("splines=\"ortho\"\n"); // Nice but hard to read on bigger graphs.
    }

    sb.append(dotContent)//
        .append("}\n");//
  }

}
