package eu.europa.ted.eforms.noticeeditor.domain;

import java.util.Arrays;
import java.util.Locale;

public enum Language {

  // 24 official EU languages.

  BG(new Locale("bg"), "Bulgarian", "bul_label"), //
  CS(new Locale("cs"), "Czech", "ces_label"), //
  DA(new Locale("da"), "Danish", "dan_label"), //
  DE(new Locale("de"), "German", "deu_label"), //
  EL(new Locale("el"), "Greek", "ell_label"), //

  EN(new Locale("en"), "English", "eng_label"), //
  ES(new Locale("es"), "Spanish", "spa_label"), //
  ET(new Locale("et"), "Estonian", "est_label"), //
  FI(new Locale("fi"), "Finnish", "fin_label"), //
  FR(new Locale("fr"), "French", "fra_label"), //

  GA(new Locale("ga"), "Galician", "gle_label"), //
  HR(new Locale("hr"), "Croatian", "hrv_label"), //
  HU(new Locale("hu"), "Hungarian", "hun_label"), //
  IT(new Locale("it"), "Italian", "ita_label"), //
  LT(new Locale("lt"), "Lithuanian", "lit_label"), //

  LV(new Locale("lv"), "Latvian", "lav_label"), //
  MT(new Locale("mt"), "Maltese", "mlt_label"), //
  NL(new Locale("nl"), "Dutch", "nld_label"), //
  PL(new Locale("pl"), "Polish", "pol_label"), //
  PT(new Locale("pt"), "Portuguese", "por_label"), //

  RO(new Locale("ro"), "Romanian", "ron_label"), //
  SK(new Locale("sk"), "Slovak", "slk_label"), //
  SL(new Locale("sl"), "Slovene", "slv_label"), //
  SV(new Locale("sv"), "Swedish", "swe_label");

  public static Language valueOfFromGenericode(final String value) {
    return Arrays.stream(values())//
        .filter(item -> item.genericodeLanguage.equals(value))//
        .findFirst()//
        .orElseThrow(
            () -> new IllegalArgumentException(String.format("Unknown genericode: '%s'", value)));
  }

  public static Language valueOfFromLocale(final String lang) {
    return Arrays.stream(values())//
        .filter(item -> item.locale.getLanguage().equals(lang))//
        .findFirst()//
        .orElseThrow(
            () -> new IllegalArgumentException(String.format("Unknown locale: '%s':", lang)));
  }

  public final Locale locale;
  public final String englishLanguage;
  public final String genericodeLanguage;

  private Language(Locale locale, String englishLanguage, String genericodeLanguage) {
    this.locale = locale;
    this.englishLanguage = englishLanguage;
    this.genericodeLanguage = genericodeLanguage;
  }

  public String getEnglishLanguage() {
    return englishLanguage;
  }

  public String getGenericodeLanguage() {
    return genericodeLanguage;
  }

  public Locale getLocale() {
    return locale;
  }

}
