/**
 * The classes in this file are used to encapsulate the code necessary for controlling the context of the editor.
 * There are three controls providing context to the editor. All of them are hosted in the toolbar at the top of the
 * application page:
 * - The SDK version selector: allows you to switch between different versions of the SDK.
 * - The notice subtype selector: allows you to select the notice subtype for which the editor will display a form.
 * - The language selector: allows you to select one of the available languages in which to display the notice.
 * 
 * To keep things simple all classes are implemented as singletons, using a static property to instantiate themselves autonomously
 */

import { Constants } from "./global.js";
import { SdkServiceClient } from "./service-clients.js";

/*******************************************************************************
 * Controls the dropdown which is used to select the language that is to be used.
 */
export class LanguageSelector {

  #element;

  constructor(id) {
    this.#element = document.getElementById(id);
  }

  get element() {
    return this.#element;
  }

  get selectedLanguage() {
    return this.#element.value;
  }
}

/*******************************************************************************
 * Controls the dropdown which is used to select the version of the SDK to be loaded.
 */
export class SdkVersionSelector {

  #element;

  constructor(id) {
    this.#element = document.getElementById(id);
  }

  get element() {
    return this.#element;
  }

  get selectedSdkVersion() {
    return this.element.value;
  }

  async populate() {
    this.element.length = 0;

    // Dynamically load the options.   
    for (const sdkVersion of SdkServiceClient.availableSdkVersions) {
      const option = document.createElement("option");
      option.setAttribute("value", sdkVersion);
      option.textContent = sdkVersion;
      this.element.appendChild(option);
    }

    Context.raiseSdkVersionChanged(this.element.value);
  }
}

/*******************************************************************************
 * Controls the dropdown which is used to select the notice subtype to be loaded.
 */
export class NoticeSubtypeSelector {

  #element;

  constructor(id) {
    this.#element = document.getElementById(id);
  }

  get element() {
    return this.#element;
  }

  get selectedNoticeSubtype() {
    return this.element.value;
  }

  async populate() {

    await SdkServiceClient.fetchAvailableNoticeSubtypes(Context.sdkVersion);

    this.element.length = 0;

    // Dynamically load the options.   
    for (const noticeSubtype of SdkServiceClient.availableNoticeSubtypes) {
      const option = document.createElement("option");
      option.setAttribute("value", noticeSubtype);
      option.textContent = noticeSubtype;
      this.element.appendChild(option);
    }

    Context.raiseNoticeSubtypeChanged(this.element.value);
  }
}

export class NoticeSelector {

  static instance = new NoticeSelector("notice-selector");

  /** @type HTMLInputElement */
  #element;

  constructor(noticeSelectorId) {
    this.#element = document.getElementById(noticeSelectorId);
  }

  get element() {
    return this.#element;
  }
}

/*******************************************************************************
 * Provides a central point of access to the application's context .
 */
export class Context {

  static noticeLoadedEventName = "noticeloaded";
  static noticeLoadedEvent = new CustomEvent(Context.noticeLoadedEventName);

  /** @type {LanguageSelector} */
  static #languageSelector;

  /** @type {SdkVersionSelector} */
  static #sdkVersionSelector;

  /** @type {NoticeSubtypeSelector} */
  static #noticeSubtypeSelector;

  /** @type {NoticeSelector} */
  static #noticeSelector;

  static {
    this.#languageSelector = new LanguageSelector("language-selector");
    this.#languageSelector.element.addEventListener("change", (event) => Context.raiseLanguageChanged(event.target.value));

    this.#sdkVersionSelector = new SdkVersionSelector("notice-sdk-selector");
    this.#sdkVersionSelector.element.addEventListener("change", (event) => Context.raiseSdkVersionChanged(event.target.value));

    this.#noticeSubtypeSelector = new NoticeSubtypeSelector("notice-subtype-selector");
    this.#noticeSubtypeSelector.element.addEventListener("change", (event) => Context.raiseNoticeSubtypeChanged(event.target.value));

    this.#noticeSelector = new NoticeSelector("notice-selector");
    this.#noticeSelector.element.addEventListener("change", (event) => { Context.raiseNoticeLoading(event.target.files[0]) });
  }

  /**
   * Gets the selected notice subtype.
   * @returns {string}
   */
  static get noticeSubtype() {
    return this.#noticeSubtypeSelector.selectedNoticeSubtype;
  }

  /**

   * Gets the selected SDK version
   * @returns {string}
   */
  static get sdkVersion() {
    return this.#sdkVersionSelector.selectedSdkVersion;
  }

  /**
   * Gets the ISO 639-1, two-letter code of the selected language
   * @returns {string}
   */
  static get language() {
    return this.#languageSelector.selectedLanguage;
  }

  static async init() {
    // I. Get a list of available SDK versions from the back-end.
    await SdkServiceClient.fetchVersionInfo();

    // II. Populate the SdkVersionSelector dropdown.
    // After the SdkVersionSelector is populated its onchange event is raised triggering a "chain reaction":
    // 1. The NoticeSubtypeSelector picks up the event and  populates itself; then its own onchange event is raised.
    // 2. Then the Editor picks it up and loads the selected notice subtype. 
    this.#sdkVersionSelector.populate();

    // Editor.instance.displayAppVersion(SdkServiceClient.appVersion);
  }


  /**
   * 
   * @param {string} sdkVersion 
   */
  static async raiseSdkVersionChanged(sdkVersion) {
    await this.#noticeSubtypeSelector.populate();
    document.dispatchEvent(new CustomEvent(Constants.Events.sdkVersionChanged, { detail: { sdkVersion: sdkVersion } } ));
  }

  /**
   * 
   * @param {string} noticeSubtype 
   */
  static async raiseNoticeSubtypeChanged(noticeSubtype) {
    await SdkServiceClient.fetchFieldsAndCodelists(Context.sdkVersion);
    await SdkServiceClient.fetchNoticeSubtype(Context.sdkVersion, Context.noticeSubtype);
    await SdkServiceClient.fetchTranslations(Context.sdkVersion, Context.language);
    document.dispatchEvent(new CustomEvent(Constants.Events.noticeSubtypeChanged, { detail: { noticeSubtype: noticeSubtype } } ));
  }

  static async raiseLanguageChanged(language) {
    await SdkServiceClient.fetchTranslations(Context.sdkVersion, Context.language);
    document.dispatchEvent(new CustomEvent(Constants.Events.languageChanged, { detail: { language: language } } ));
  }

  /**
   * @param {File} file
   */
  static raiseNoticeLoading(file) {
    if (file?.type !== "text/xml") {
      return;
    }
    document.dispatchEvent(new CustomEvent(Constants.Events.noticeLoading, { detail: { file: file } } ));
  }

  /**
   * 
   * @param {string} xml 
   */
  static async raiseNoticeLoaded(xml, sdkVersion, noticeSubtype) {
    document.getElementById("notice-xml").textContent = xml;

    Context.#sdkVersionSelector.element.value = sdkVersion;
    await this.raiseSdkVersionChanged(sdkVersion);

    Context.#noticeSubtypeSelector.element.value = noticeSubtype; 
    await Context.raiseNoticeSubtypeChanged(noticeSubtype);
    document.dispatchEvent(new CustomEvent(Constants.Events.noticeLoaded, { detail: { xml: xml, sdkVersion: sdkVersion, noticeSubtype: noticeSubtype } } ));
  }
}
