package eu.europa.ted.eforms.noticeeditor.helper.validation;

public enum CsvValidationMode {
  STATIC("static"), DYNAMIC("dynamic");

  private final String text;

  CsvValidationMode(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }
}
