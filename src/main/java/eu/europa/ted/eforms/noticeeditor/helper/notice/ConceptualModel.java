package eu.europa.ted.eforms.noticeeditor.helper.notice;

public class ConceptualModel {
  private ConceptNode rootNode;

  public ConceptualModel(final ConceptNode rootNode) {
    this.rootNode = rootNode;
  }

  public ConceptNode getRoot() {
    return rootNode;
  }

  public String getNoticeSubType() {
    final ConceptNode rootExtension = rootNode.getConceptNodes().stream()
        .filter(item -> item.getId().equals("ND-RootExtension")).findFirst().get();
    return rootExtension.getConceptFields().stream()
        .filter(item -> item.getId().equals("OPP-070-notice")).findFirst().get().getValue();
  }
}
