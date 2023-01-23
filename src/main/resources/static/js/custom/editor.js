import { LanguageSelector, NoticeSubtypeSelector, SdkVersionSelector } from "./context.js";
import { Constants, DomUtil } from "./global.js";
import { FormElement } from "./notice-form.js";
import { NoticeTypeDefinitionElement } from "./notice-type-definition.js";
import { SdkServiceClient, XmlServiceClient } from "./service-clients.js";
import { VisualModel } from "./visual-model.js";

/**
* Editor class: it stores SDK data like loaded fields, nodes, translations, ...
*/
export default class Editor {

  static instance = new Editor();

  constructor() {
  }

  static get formContainerElement() {
    return document.getElementById(Constants.HtmlElements.FORM_CONTENT_ROOT_ELEMENT);
  }

  static async serializeToJson(event, validationType) {
    console.debug("Attempting to serialize form to visual model JSON.");
    event.preventDefault();

    const textAreaJson = document.getElementById("json-window");
    const textAreaValidation = document.getElementById("validation-window");
    const textAreaXml = document.getElementById("xml-window");

    Editor.instance.showDebugOutput();

    // Transform visual model to JSON text and show it in the UI.
    textAreaJson.value = "Generating visual model...";
    const visualModel = new VisualModel(Editor.formContainerElement);
    const jsonText = JSON.stringify(visualModel, null, 2);
    textAreaJson.value = jsonText;

    if (validationType === "no-validation") {
      textAreaXml.value = "Generating physical model...";
      const xmlText = await XmlServiceClient.saveXml(jsonText);
      textAreaXml.value = xmlText;
      
    } else if (validationType === "xsd-validation") {
        textAreaValidation.value = "Generating XSD validation report...";
        const reportJson = await XmlServiceClient.xsdValidation(jsonText);
        textAreaValidation.value = reportJson;
        
    } else if (validationType === "cvs-validation") {
        textAreaValidation.value = "Generating CVS validation report...";
        const reportJson = await XmlServiceClient.cvsValidation(jsonText);
        textAreaValidation.value = reportJson;
    }
  };

  static async selectedNoticeSubtypeChanged() {

    Editor.instance.hideDebugOutput();

    const selectedSdkVersion = SdkVersionSelector.selectedSdkVersion;
    const selectedNoticeSubtype = NoticeSubtypeSelector.selectedNoticeSubtype;

    await SdkServiceClient.fetchFieldsAndCodelists(selectedSdkVersion);
    await SdkServiceClient.fetchNoticeSubtype(selectedSdkVersion, selectedNoticeSubtype);
    await SdkServiceClient.fetchTranslations(SdkVersionSelector.selectedSdkVersion, LanguageSelector.selectedLanguage);

    Editor.instance.createNoticeForm();
  }

  static async selectedLanguageChanged() {
    Editor.formContainerElement.setAttribute("lang", LanguageSelector.selectedLanguage);
    document.documentElement.setAttribute('lang', LanguageSelector.selectedLanguage);

    await SdkServiceClient.fetchTranslations(SdkVersionSelector.selectedSdkVersion, LanguageSelector.selectedLanguage);
    Editor.instance.createNoticeForm();
  }

  static async loaded() {
    // I. Get a list of available SDK versions from the back-end.
    await SdkServiceClient.fetchVersionInfo();

    // II. Populate the SdkVersionSelector dropdown.
    // After the SdkVersionSelector is populated its onchange event is raised triggering a "chain reaction":
    // 1. The NoticeSubtypeSelector picks up the event and  populates itself; then its own onchange event is raised.
    // 2. Then the Editor picks it up and loads the selected notice subtype. 
    SdkVersionSelector.populate();

    Editor.instance.displayAppVersion(SdkServiceClient.appVersion);
  }

  createNoticeForm() {
    
    this.hide();
    
    Editor.formContainerElement.innerHTML = ""; // Remove previous form.

    // await SdkServiceClient.fetchTranslations(SdkVersionSelector.selectedSdkVersion, LanguageSelector.selectedLanguage);

    // Create an empty form.

    // BUILD METADATA SECTION.
    Editor.formContainerElement.appendChild(
      NoticeTypeDefinitionElement.create({
        id: "notice-metadata",
        contentType: Constants.ContentType.METADATA_CONTAINER,
        content: SdkServiceClient.noticeTypeDefinition.metadata,
        _label: "editor.the.metadata"
      }));

    // Set UBL version in the form.
    FormElement.getElementByContentId(Constants.StandardIdentifiers.UBL_VERSION_FIELD).value = SdkServiceClient.ublVersion;

    // Set SDK version in the form.
    FormElement.getElementByContentId(Constants.StandardIdentifiers.SDK_VERSION_FIELD).value = SdkServiceClient.sdkVersion;

    // Set the version id
    FormElement.getElementByContentId(Constants.StandardIdentifiers.NOTICE_VERSION_FIELD).value = "01";

    // Set the notice id.
    FormElement.getElementByContentId(Constants.StandardIdentifiers.NOTICE_UUID_FIELD).value = DomUtil.generateRandomUuidV4();

    // BUILD NON-METADATA SECTION.
    Editor.formContainerElement.appendChild(
      NoticeTypeDefinitionElement.create({
        id: "notice-data",
        contentType: Constants.ContentType.DATA_CONTAINER,
        content: SdkServiceClient.noticeTypeDefinition.content,
        _label: "editor.the.root"
      }));

    this.show();

    console.log("Loaded editor notice type: " + NoticeSubtypeSelector.selectedNoticeSubtype);
  }
  
  displayAppVersion(version) {
    const appVersionElement = document.getElementById(Constants.HtmlElements.APP_VERSION_ELEMENT);
    if (appVersionElement) {
      appVersionElement.textContent = version;
    }
  }

  showDebugOutput() {
    document.getElementById("debug-output").classList.remove("hidden");   
    document.getElementById("debug-section")?.scrollIntoView(true);
  }

  hideDebugOutput() {
    document.getElementById("debug-output").classList.add("hidden");   
  }

  hide() {
    document.getElementById("form-section").classList.add("hidden");
    document.getElementById("loading-indicator").classList.remove("hidden");
  }

  show() {
    document.getElementById("form-section").classList.remove("hidden");
    document.getElementById("loading-indicator").classList.add("hidden");
  }
}

window.Editor = Editor;