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

import { SdkServiceClient } from "./service-clients.js";

/*******************************************************************************
 * Provides a central point of access to the application's context .
 */
export class Context {

  /**
   * Gets the selected notice subtype.
   * @returns {string}
   */
  static get noticeSubtype() {
    return NoticeSubtypeSelector.selectedNoticeSubtype;
  }

  /**
   * Gets the selected SDK version
   * @returns {string}
   */
  static get sdkVersion() {
    return SdkVersionSelector.selectedSdkVersion;
  }

  /**
   * Gets the ISO 639-1, two-letter code of the selected language
   * @returns {string}
   */
  static get language() {
    return LanguageSelector.selectedLanguage;
  } 

  /**
   * Gets the ISO 639-3, three-letter code of the selected language
   * @returns {string}
   */
  static get languageAsIso6393() {
    return I18N.Iso6391ToIso6393Map[LanguageSelector.selectedLanguage];
  }
}

/*******************************************************************************
 * Controls the dropdown which is used to select the version of the SDK to be loaded.
 */
export class SdkVersionSelector {

  static instance = new SdkVersionSelector("notice-sdk-selector");

  #element;

  constructor(id) {
    if (SdkVersionSelector.instance) {
      throw new Error("You should not try to instantiate the SdkVersionSelector yourself. It meant to be a singleton.");
    }
    this.#element = document.getElementById(id);
  }

  static get element() {
    return SdkVersionSelector.instance.#element;
  }

  /**
   * @param {((this: GlobalEventHandlers, ev: Event) => any) | null} value
   */
  static set onchange(value) {
    SdkVersionSelector.element.onchange = value;
  }

  static get selectedSdkVersion() {
    return SdkVersionSelector.element.value;
  }

  static async populate() {
    const selector = SdkVersionSelector.element;
    selector.length = 0;

    // Dynamically load the options.   
    for (const sdkVersion of SdkServiceClient.availableSdkVersions) {
      const option = document.createElement("option");
      option.setAttribute("value", sdkVersion);
      option.textContent = sdkVersion;
      selector.appendChild(option);
    }

    selector.onchange();
  }
}

/*******************************************************************************
 * Controls the dropdown which is used to select the notice subtype to be loaded.
 */
export class NoticeSubtypeSelector {

  static instance = new NoticeSubtypeSelector("notice-subtype-selector");

  #element;

  constructor(id) {
    this.#element = document.getElementById(id);
    SdkVersionSelector.onchange = NoticeSubtypeSelector.populate;
  }

  static get element() {
    return NoticeSubtypeSelector.instance.#element;
  }

  static get selectedNoticeSubtype() {
    return NoticeSubtypeSelector.element.value;
  }

  static async populate() {

    await SdkServiceClient.fetchAvailableNoticeSubtypes(SdkVersionSelector.selectedSdkVersion);

    const selector = NoticeSubtypeSelector.element;
    selector.length = 0;

    // Dynamically load the options.   
    for (const noticeSubtype of SdkServiceClient.availableNoticeSubtypes) {
      const option = document.createElement("option");
      option.setAttribute("value", noticeSubtype);
      option.textContent = noticeSubtype;
      selector.appendChild(option);
    }

    selector.onchange();
  }
}

/*******************************************************************************
 * Controls the dropdown which is used to select the language that is to be used.
 */
export class LanguageSelector {

  static instance = new LanguageSelector("language-selector");

  #element;

  constructor(id) {
    this.#element = document.getElementById(id);
  }

  static get element() {
    return LanguageSelector.instance.#element;
  }

  static get selectedLanguage() {
    return LanguageSelector.element.value;
  }
}
