import { Context } from "./context.js";
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
    document.addEventListener(Constants.Events.languageChanged, Editor.selectedLanguageChanged);
    document.addEventListener(Constants.Events.noticeSubtypeChanged, Editor.selectedNoticeSubtypeChanged);
  }

  static get formContainerElement() {
    return document.getElementById(Constants.HtmlElements.FORM_CONTENT_ROOT_ELEMENT);
  }

  static async serializeToJson(event) {
    console.debug("Attempting to serialize form to visual model JSON.");
    event.preventDefault();

    const textAreaJson = document.getElementById("json-window");
    textAreaJson.value = "Generating visual model...";

    const textAreaXml = document.getElementById("xml-window");
    textAreaXml.value = "Generating physical model...";

    Editor.instance.showDebugOutput();

    const visualModel = new VisualModel(Editor.formContainerElement);

    // Transform visual model to JSON text and show it in the UI.
    const jsonText = JSON.stringify(visualModel, null, 2);
    textAreaJson.value = jsonText;

    const xmlText = await XmlServiceClient.saveXml(jsonText);
    textAreaXml.value = xmlText;
  };

  static async selectedNoticeSubtypeChanged() {
    Editor.instance.hideDebugOutput();
    Editor.instance.createNoticeForm();
  }

  static async selectedLanguageChanged() {
    Editor.formContainerElement.setAttribute("lang", Context.language);
    document.documentElement.setAttribute('lang', Context.language);
    Editor.instance.createNoticeForm();
  }

  static async loaded() {
    await Context.init();
    Editor.instance.displayAppVersion(SdkServiceClient.appVersion);
  }

  createNoticeForm() {
    
    this.hide();
    
    Editor.formContainerElement.innerHTML = ""; // Remove previous form.

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

    console.log("Loaded editor notice type: " + Context.noticeSubtype);
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