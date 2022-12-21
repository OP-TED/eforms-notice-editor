//
// The front-end editor script written in plain JavaScript (ECMAscript) and using plain HTML for demo purposes.
// The script is responsible for loading SDK data from the back-end for the front-end UI and creating the dynamic form (HTML DOM manipulation).
// index.html <- document -> editor.js <- XHR ("ajax") -> back-end REST API
// NOTE: For bigger scripts and maintainability you could also use something like TypeScript instead.
//
(async function() {
  console.log("Loading editor script.");

  /**
   * Controls the dropdown which is used to select the version of the SDK to be loaded.
   */
  class SdkVersionSelector {

    static getElement() {
      return document.getElementById("notice-sdk-selector")
    }
    
    static getSelectedSdkVersion() {
      return SdkVersionSelector.getElement().value;
    }

    static async populate() {
      const selector = SdkVersionSelector.getElement();
      selector.length = 0;

      // Dynamically load the options.   
      for (const sdkVersion of SdkServiceClient.availableSdkVersions) {
        selector.appendChild(createOption(sdkVersion, sdkVersion));
      }

      selector.onchange();
    }
  }

  /**
   * Controls the dropdown which is used to select the notice subtype to be loaded.
   */
  class NoticeSubtypeSelector {

    /**
     * Used instead of a static initialiser because static initialisers are not supported by Safari.
     */
    static setup() {
      SdkVersionSelector.getElement().onchange = NoticeSubtypeSelector.populate;
    }

    static getElement() {
      return document.getElementById("notice-sub-type-selector")
    }
    
    static getSelectedNoticeSubtype() {
      return NoticeSubtypeSelector.getElement().value;
    }

    static async populate() {
  
      await SdkServiceClient.fetchAvailableNoticeSubtypes(SdkVersionSelector.getSelectedSdkVersion());
        
      const selector = NoticeSubtypeSelector.getElement();
      selector.length = 0;

      // Dynamically load the options.   
      for (const noticeSubtype of SdkServiceClient.availableNoticeSubtypes) {
        selector.appendChild(createOption(noticeSubtype, noticeSubtype));
      }

      selector.onchange();
    }
  }

  
  const MIME_TYPE_XML = "application/xml";
  const MIME_TYPE_JSON = "application/json"; // Used in other parts of the editor for xml.

  const timeOutDefaultMillis = 3000;
  const timeOutLargeMillis = 6000;

  const SDK_SERVICE_URL = "/sdk";
  const XML_SERVICE_URL = "/xml";
 
  // Initiation code has been moved to the bottom of this file.

  // Avoids conflicts with other identifiers in the context of this browser tab.
  const EDITOR_ID_PREFIX = "editor-id-";

  const NODE_ID_FOR_ROOT = "ND-Root";
  const FIELD_ID_SDK_VERSION = "OPT-002-notice";
  const FIELD_ID_NOTICE_SUB_TYPE = "OPP-070-notice";
  const FIELD_ID_NOTICE_UUID = "BT-701-notice";
  
  const VIS_CONTENT_COUNT = "contentCount";
  const VIS_VALUE = "value";
  const VIS_TYPE = "visType"; // Has nothing to do with HTML element type!
  const VIS_TYPE_FIELD = "field";
  const VIS_TYPE_NON_FIELD = "non-field";
  
  const DATA_EDITOR_CONTENT_ID = "data-editor-content-id";
  const DATA_EDITOR_ID_REFERENCE = "data-editor-id-reference";
  const DATA_EDITOR_ID_REF_PREFIX = "data-editor-id-ref-";
  const DATA_EDITOR_INSTANCE_ID_FIELD = "data-editor-instance-id-field";
  const DATA_EDITOR_VALUE_FIELD = "data-editor-value-field";
  const DATA_EDITOR_COUNT = "data-editor-count";
  const DATA_EDITOR_CONTENT_PARENT_ID = "data-editor-content-parent-id";
  const DATA_EDITOR_CONTENT_NODE_ID = "data-editor-content-node-id";
  const DATA_EDITOR_TYPE = "data-editor-type"; // For the VIS TYPE.
  
  // A cosmetic item is something we use for the visual part in the HTML to group. 
  // For example a form input label and form input field.
  // We do not want it in the visual model as it would only add unnecessary tree levels.
  const EDITOR_COSMETIC = "editor-cosmetic";
  
  const displayTypeToElemInfo = {};
  displayTypeToElemInfo["CHECKBOX"] = {"tag": "input", "type" : "checkbox"};
  displayTypeToElemInfo["COMBOBOX"] = {"tag": "select", "type" : "select"};
  displayTypeToElemInfo["RADIO"] = {"tag": "input", "type" : "checkbox"}; // TODO fully support radio.
  displayTypeToElemInfo["TEXTAREA"] = {"tag": "textarea"};
  displayTypeToElemInfo["TEXTBOX"] = {"tag": "input", "type": "text"};
  
  const FALLBACK_LANGUAGE = "en"; // English, recommended for now.
  const NTD_LABEL = "_label";
  const NTD_CONTENT_TYPE_FIELD = "field";
  const NTD_CONTENT_TYPE_GROUP = "group";
  
  // This could be covered by "auxiliary labels" or be something fully custom.
  // The i18nOfEditor is loaded before other translations, thus it can be used to override them.
  // This could be used to fix a typo while waiting for the next version of the SDK.
  // This can also be used to define arbitrary translated texts for use in the editor.
  const i18nOfEditor = {};

  /**
  * Some custom english translations for the editor itself.
  */
  i18nOfEditor["en"] = {
    "editor.are.you.sure": "Are you sure?",
    "editor.the.metadata": "Metadata",
    "editor.the.root": "Content",
    "editor.add.more": "Add one",
    "editor.remove": "Remove",
    "editor.select": "Select",
  };
  
  /**
  * Some custom french translations for the editor itself.
  */
  i18nOfEditor["fr"] = {
    "editor.are.you.sure": "Êtes-vous sûr ?",
    "editor.the.metadata": "Méta données",
    "editor.the.root": "Contenu",
    "editor.add.more": "En ajouter",
    "editor.remove": "Enlever",
    "editor.select": "Choisir"
  };

  /** 
  * For demo purposes only two editor demo UI languages are provided.
  * You could also separate the UI language from the notice language if desired.
  */ 
  const lang2To3Map = {
    "en" : "ENG",
    "fr" : "FRA"
  };
  
  function getSelectedLanguage() {
    const lang = document.getElementById("notice-lang-selector").value;
    if (!lang) {
      throw new Error("No language selected!");
    }
    return lang;
  }

   /*
   * Gets editor specific label for given key and language. If the language is not specified it takes the selected language.
   */
  function getEditorLabel(labelKey, lang) {
    if (!lang) {
      lang = getSelectedLanguage();
    }
    // How you get and handle i18n label data is up to you.
    var dataForLang = i18nOfEditor[lang];
    if (!dataForLang && i18nOfEditor[FALLBACK_LANGUAGE] && lang !== FALLBACK_LANGUAGE) {
      dataForLang = i18nOfEditor[FALLBACK_LANGUAGE]; // Fallback to english.
    }
    return dataForLang[labelKey];
  }
  
  /**
  * Editor class: it stores SDK data like loaded fields, nodes, translations, ...
  */
  class Editor {
    constructor() {
      this.sdkVersion = SdkServiceClient.sdkVersion;
      this.ublVersion = SdkServiceClient.ublVersion;
      this.dataI18n = SdkServiceClient.translations;
      
      this.isDebug = true;
      console.log("Editor, isDebug=" + this.isDebug);
      console.log("sdkVersion=" + this.sdkVersion);

      this.fieldMap = SdkServiceClient.fields;
      this.nodeMap = SdkServiceClient.nodes;
      this.codelistMap = SdkServiceClient.codelists;
      
      // The top section containing the metadata, most of these fields are hidden or read only.
      this.noticeMetadata = {"id" : "THE_METADATA", "content" : SdkServiceClient.noticeTypeDefinition.metadata};
      this.noticeMetadata[NTD_LABEL] = "editor.the.metadata";

      // The root content is an array. Build a dummy element to hold the content.
      this.noticeRootContent = {"id" : "THE_ROOT", "content" : SdkServiceClient.noticeTypeDefinition.content};
      this.noticeRootContent[NTD_LABEL] = "editor.the.root";

      this.noticeId = SdkServiceClient.noticeTypeDefinition.noticeId;
      this.noticeFormElement = document.getElementById("notice-type");;
      
      const serializeBtnElem = document.getElementById("id-editor-log-json");
      const that = this;
      const serializeToJsonFunc = async function(event) {
        console.debug("Attempting to serialize form to visual model JSON.");
        event.preventDefault();
        
        const textAreaJson = document.getElementById("id-editor-log-json-area");
        textAreaJson.value = "";

        const textAreaXml = document.getElementById("id-editor-log-xml-area");
        textAreaXml.value = "";

        const dataModel = that.toVisualModel();
        
        // Transform visual model to JSON text and show it in the UI.
        const jsonText = JSON.stringify(dataModel, null, 2);
        textAreaJson.value = jsonText;
        textAreaJson.style.display = "block"; // Unhide.

        const xml = await XmlServiceClient.saveXml(jsonText);
        textAreaXml.value = xml;
        textAreaXml.style.display = "block";

      };
      serializeBtnElem.addEventListener("click", serializeToJsonFunc, false);
    }

    /**
     * Used instead of a static initialiser because static initialisers are not supported by Safari.
     */
    static setup() {
      NoticeSubtypeSelector.getElement().onchange = Editor.selectedNoticeSubtypeChanged; 
    }

    static async selectedNoticeSubtypeChanged() {
      const selectedSdkVersion = SdkVersionSelector.getSelectedSdkVersion();
      const selectedNoticeSubtype = NoticeSubtypeSelector.getSelectedNoticeSubtype();

      await SdkServiceClient.fetchFieldsAndCodelists(selectedSdkVersion);
      await SdkServiceClient.fetchNoticeSubtype(selectedSdkVersion, selectedNoticeSubtype);
      createNoticeForm(selectedSdkVersion, selectedNoticeSubtype);
      }
    
    getTranslationById(labelId) {
      const customLabel = getEditorLabel(labelId);
      const label = customLabel ? customLabel : this.dataI18n[labelId];
      if (!label) {
        console.log("Missing label for key='" + labelId + "'");
      }
      // Showing the labelId instead of null helps debugging.
      // return label ? label : null;
      return label ? label : labelId; 
    }

    /**
    * Builds a form element from an NTD content item.
    */
    buildFormElem(content) {
      var elemInfo = displayTypeToElemInfo[content.displayType];
      if (!elemInfo) {
        elemInfo = displayTypeToElemInfo["TEXTBOX"]; // Fallback.
      }
      // This works well for a single input or textarea.
      // But for radio multiple inputs are needed.
      // For now the editor will not support radio for simplification reasons.
      const elem = document.createElement(elemInfo.tag);
      if (elemInfo.type) {
        elem.setAttribute("type", elemInfo.type);
      }
      if (content.hidden || content.readOnly) {
        elem.setAttribute("readonly", "readonly");
      }
      if (content.hidden && !this.isDebug) {
        elem.setAttribute("hidden", "hidden");
      }
      return elem;
    }
    
    /**
     * Reads from the HTML form and returns a visual model object from it.
     */
    toVisualModel() {
      console.log("toVisualModel called");
      
      // Extract tree of data from the form.
      const domRoot = this.noticeFormElement;
      const vm = this.toVisualModelRecursive(domRoot, null);
      
      // Put some info at the top level for convenience.
      vm["contentId"] = "the_visual_root"; // visualRoot.
      vm["visNodeId"] = NODE_ID_FOR_ROOT;
      vm[VIS_TYPE] = VIS_TYPE_NON_FIELD;
      vm[VIS_CONTENT_COUNT] = "1";
   
      // Put the notice SDK version at top level.
      vm["sdkVersion"] = this.findInVisualModelRec(vm, FIELD_ID_SDK_VERSION).value;
      if (!vm["sdkVersion"]) {
        throw new Error("SDK version is not specified in visual model!");
      }
      
      // Put the notice sub type at top level.
      vm["noticeSubType"] = this.findInVisualModelRec(vm, FIELD_ID_NOTICE_SUB_TYPE).value;
      if (!vm["noticeSubType"]) {
        throw new Error("The notice sub type is not specified in visual model!");
      }

      // Put the notice UUID top level.
      vm["noticeUuid"] = this.findInVisualModelRec(vm, FIELD_ID_NOTICE_UUID).value;
      if (!vm["noticeSubType"]) {
        throw new Error("The notice sub type is not specified in visual model!");
      }
      
      return vm;
    }
    
    /**
    * Starting from the passed DOM element and parent data. Initially the parent data is null.
    */
    toVisualModelRecursive(elem, parentData) {

      // The elem is a DOM element. 
      // The data is the tree item we want to create.
      const data = {};

      // Maybe for debug.
      //const domId = elem.getAttribute("id");
      //data["domId"] = domId;

      const contentId = elem.getAttribute(DATA_EDITOR_CONTENT_ID);
      data["contentId"] = contentId;
      
      if (contentId === EDITOR_COSMETIC) {
        const parentChildren = parentData.children;
        const children = this.findElementsHavingAttributeInElem(elem, DATA_EDITOR_CONTENT_ID);
        for (const child of children) {
          parentChildren.push(this.toVisualModelRecursive(child, null));
        }
        return null;
      }

      const contentCount = elem.getAttribute(DATA_EDITOR_COUNT);        
      data[VIS_CONTENT_COUNT] = contentCount;

      const contentType = elem.getAttribute(DATA_EDITOR_TYPE);
      data[VIS_TYPE] = contentType;

      if (contentType === NTD_CONTENT_TYPE_FIELD) {
        // The form value (text inside of form fields, input, select, textarea, ...).
        const value = elem.value;
          
        // TODO: upcoming TEDEFO-1727
        data[VIS_VALUE] = value;
          
        const sdkFieldMeta = this.fieldMap[contentId];
        if (!sdkFieldMeta) {
          throw new Error("Unknown fieldId=" + contentId);
        }
      } else {
        const nodeId = elem.getAttribute(DATA_EDITOR_CONTENT_NODE_ID);
        if (nodeId) {
          data["visNodeId"] = nodeId;
        }
      }
    
      if (elem.hasChildNodes()) {
        const visualModelChildren = [];
        const children = this.findElementsHavingAttributeInElem(elem, DATA_EDITOR_CONTENT_ID);
        data["children"] = visualModelChildren;
        for (const child of children) {
          const dataChild = this.toVisualModelRecursive(child, data);
          // There may be no data in case of a cosmetic item, in that case the data has already been pushed.
          if (dataChild) {
            visualModelChildren.push(dataChild);
          }
        }
      }

      // Remove children array if it is empty.
      if (data["children"] && data["children"].length === 0) {
        delete data["children"];
      }
      
      return data;
    }
    
    /**
    * Looks for the content id to search in the visual model item.
    */
    findInVisualModelRec(visualModelItem, idToSearch) {
      if (visualModelItem.contentId === idToSearch) {
        return visualModelItem;
      }
      if (visualModelItem.children) {
        for (const child of visualModelItem.children) {
          const found = this.findInVisualModelRec(child, idToSearch);
          if (found) {
            return found;
          }
        }
      }
      return null;
    }
    
    /**
    * Load data from an existing model.
    */
    fromModel() {
      // Loading of model data into the HTML form
      // create an empty form
      // recurse through data
      // fill the form (repeat on elements)
      // This will be done later.
    }
    
    /**
    * Builds the HTML form dynamically.
    */
    buildForm() {
      const rootLevel = 1; // Top level, sub items will be on level 2, 3, 4 ...

      // Create an empty form.

      // BUILD METADATA SECTION.
      // This builds the initial empty form metadata section on top.
      // Most of these fields are either readonly and/or hidden.
      this.readContentRecur(this.noticeFormElement, this.noticeMetadata, rootLevel, false, null, null);

      // Set UBL version in the form.
      this.getContentElemByIdUnique("OPT-001-notice").value = this.ublVersion;

      // Set SDK version in the form.
      this.getContentElemByIdUnique(FIELD_ID_SDK_VERSION).value = this.sdkVersion;

      // Set the version id
      this.getContentElemByIdUnique("BT-757-notice").value = "01";

      // Set the notice id.
      this.getContentElemByIdUnique(FIELD_ID_NOTICE_UUID).value = buildRandomUuidv4();

      // BUILD NON-METADATA SECTION.
      // This builds the initial empty form, the content part (non-metadata).
      this.readContentRecur(this.noticeFormElement, this.noticeRootContent, rootLevel, false, null, null);
      
      // Post process: Fills selects that have id references.
      this.populateIdRefSelectsAll();
      
      // Handle valueSource related logic.
      this.handleValueSourceLogic(this.noticeRootContent);
    }

    /**
     * Handles content "valueSource" starting from the passed content.
     */
    handleValueSourceLogic(content) {
      const that = this;
      
      // Define the content visitor.
      const visitorFunc = function(visitedContent) {
        if (visitedContent.contentType !== "group") {
          const valueExpr = visitedContent.valueSource;
          if (valueExpr) {
            if (visitedContent.editorCount > 0) {
              console.debug("Attempty to copy value of " + valueExpr + " for fieldId=" + visitedContent.id);
           
              // TODO this only works the first time and first item.
              
              // To fix this it should go through all the contained repeated elements. Use editor count?
              // TODO In general the selects are the trickest part as the values could come from async loaded codelists...
              const contentIdNumberMax = visitedContent.editorCount - 1;
              
              const visitedContentId = visitedContent.id;
              for (var i = 1; i <= contentIdNumberMax; i++) {
                const contentElem = that.getContentElemByIdPair(visitedContentId, i);
                const otherElem = that.getContentElemByIdPair(valueExpr, i);
              
                const valueToCopy = otherElem.value;
                console.debug("fieldId=" + visitedContent.id + " value=" + valueToCopy);
                contentElem.value = valueToCopy;
              } 
            }        
          }
        }
      };
      // Apply content visitor.
      that.visitContentRec(content, [visitorFunc]);
    }
    
    buildIdPartial(content) {
      return this.buildIdPartialFromContentId(content.id);
    }

    buildIdPartialFromContentId(contentId) {
      return EDITOR_ID_PREFIX + contentId;
    }
    
    /**
     * This is called during creation.
     */
    buildIdUniqueNew(content) {
      const paddedNumber = this.buildPaddedIdNumber(content);
      return this.buildIdPartial(content) + "-" + paddedNumber;
    }

    /**
     *  This allows to build the id of any other field (for example).   
    */
    buildIdUniqueFromPair(contentId, contentIdNum) {
      const paddedNumber = this.buildPaddedIdNumberForIdNumber(String(contentIdNum));
      return this.buildIdPartialFromContentId(contentId) + "-" + paddedNumber;
    }

    buildPaddedIdNumber(content) {
      return this.buildPaddedIdNumberForIdNumber(content.editorCount);
    }

    buildPaddedIdNumberForIdNumber(idNumber) {
      return lpad("0", String(idNumber), 4); // 0001, 0002, ...
    }
    
    getContentElemByIdPair(contentId, contentIdNum) {
      const elementId = this.buildIdUniqueFromPair(contentId, contentIdNum);
      return document.getElementById(elementId);
    }

    getContentElemByIdUnique(contentId) {
      const elementId = this.buildIdUniqueFromPair(contentId, 1);
      return document.getElementById(elementId);
    }
    
    /**
     * Applies the contentVistorFunctions in tree depth first order to the content.
     */
    visitContentRec(content, contentVisitorFuncs) {
      if (!content) {
        throw new Error("Invalid content");
      }
      if (!contentVisitorFuncs) {
        throw new Error("Invalid contentVisitorFuncs");
      }
      for (const contentVisitorFunc of contentVisitorFuncs) {
        contentVisitorFunc(content);
      }
      // Is there more sub content?
      if (content.content && !content.editorExpanded) {
        // Visit sub items. Recursion.
        for (const contentSub of content.content) {
          this.visitContentRec(contentSub, contentVisitorFuncs);
        }
      }
    }
    
    /**
    * Get direct children of the element filtered by the passed selector.
    */
    getDirectChildren(elem, selector){
      const tempId = buildRandomUuidv4();
      elem.dataset.tempid = tempId; // Sets data-tempId
      const fullSelector = '[data-tempid="' + tempId + '"] > ' + selector;
      const returnObj = elem.parentElement.querySelectorAll(fullSelector);
      elem.dataset.tempid = "";
      return returnObj == null ? [] : returnObj;
    }
    
    findElementsHavingAttributeInElem(elem, attributeName) {
      const selector = "[" + attributeName + "]";
      return this.getDirectChildren(elem, selector);
    }

    findElementsHavingAttribute(attributeName) {
      const selector = "[" + attributeName + "]";
      return document.querySelectorAll(selector);
    }

    findElementsWithAttribute(attributeName, attributeText) {
      const selector = '[' + attributeName + '="' + attributeText + '"]';
      return document.querySelectorAll(selector);
    }
    
    findElementsWithAttributeIdScheme(idScheme) {
      return this.findElementsWithAttributeIdSchemes([idScheme]);
    }

    findElementsWithContentId(contentId) {
      return this.findElementsWithAttribute(DATA_EDITOR_CONTENT_ID, contentId);
    }
    
    /**
     * Find the HTML elements having the idSchemes (search HTML element by attribute).
     */
    findElementsWithAttributeIdSchemes(idSchemes) {
      const allFoundElements = [];
      for (const idScheme of idSchemes) {
        const foundElements = this.findElementsWithAttribute(DATA_EDITOR_INSTANCE_ID_FIELD, idScheme);
        for (const element of foundElements) {
          allFoundElements.push(element);
        }
      }
      return allFoundElements;
    }
    
    findElementsWithAttributeIdRef(idScheme) {
      // Find HTML elements that reference this kind of idScheme.
      return this.findElementsWithAttribute(DATA_EDITOR_ID_REF_PREFIX + idScheme.toLowerCase(), "true");
    }
    
    populateIdRefSelectsForIdScheme(idScheme) {
      const foundReferencedElements = this.findElementsWithAttributeIdScheme(idScheme);
      const foundReferencingElements = this.findElementsWithAttributeIdRef(idScheme);
      for (const selectElem of foundReferencingElements) {
         const selectedValue = selectElem.value;
        selectElem.innerHTML = "";
        for (const inputElem of foundReferencedElements) {
          selectElem.appendChild(createOption(inputElem.value, inputElem.value));
        }
        // TODO what happens if the value is not there anymore?
        selectElem.value = selectedValue;
      }
    }

    /**
     * Find all in use id schemes.
     */
    populateIdRefSelectsAll() {
      const selectElements = this.findElementsHavingAttribute(DATA_EDITOR_ID_REFERENCE);
      const inUseIdSchemeSet = new Set();
      for (const selectElem of selectElements) {
        const inUseIdSchemes = selectElem.getAttribute(DATA_EDITOR_ID_REFERENCE);
        const idSchemes = JSON.parse(inUseIdSchemes);
        for (const idScheme of idSchemes) {
          inUseIdSchemeSet.add(idScheme);
        }
      }
      // The id schemes are unique, for each find values and populate selects.
      for (const idScheme of inUseIdSchemeSet) {
        this.populateIdRefSelectsForIdScheme(idScheme);
      }
    }
    
    readContentRecur(parentElem, content, level, isForRepeat, elemToExpandOpt, siblingOpt) {
      const noticeId = this.noticeId;
    
      const documentFragment = document.createDocumentFragment();
      content.editorLevel = level; // Enrich model for later.
      
      // The editorCount will allow to make the id of this element unique.
      content.editorCount = content.editorCount >= 0 ? content.editorCount : 1;
  
      const isField =  content.contentType === NTD_CONTENT_TYPE_FIELD; // !content.content;
      const isSection = content.section;
      const isCollapsed = content.collapsed ? true : false;
      const isContentRepeatable = content._repeatable ? true : false; // Can be reassigned if field...
       
      // The container element may already exist in the case of uncollapsing (expand).
      const containerElem = elemToExpandOpt ? elemToExpandOpt : document.createElement("div");
  
      var field; // Can remain undefined or null.
      var formElem;
      var labelElem;
      if (isField) {
        const resultMap = this.buildFieldContainerElem(containerElem, content);
        labelElem = resultMap["labelElem"];
        formElem = resultMap["formElem"];
        field = resultMap["theField"];
        formElem.setAttribute(DATA_EDITOR_TYPE, VIS_TYPE_FIELD);
      }
      containerElem.setAttribute(DATA_EDITOR_TYPE, VIS_TYPE_NON_FIELD);
       
      // Prefix the ids to avoid conflict with various other identifiers existing in the same page.
      // For repeatable fields the content editorCount ensures the ids are unique.
      
      // This is the container and not the actual element that will contain the field value.
      containerElem.setAttribute("id", this.buildIdUniqueNew(content) + "-container-elem");
      
      // If it is a field: containerElem is the container DOM element, not the field element.
      // If is is not a field: containerElem is an instance of an NTD group and we want the raw content id.
      containerElem.setAttribute(DATA_EDITOR_CONTENT_ID, isField ? EDITOR_COSMETIC : content.id);
      
      containerElem.setAttribute(DATA_EDITOR_CONTENT_PARENT_ID, parentElem.getAttribute(DATA_EDITOR_CONTENT_ID));
      containerElem.setAttribute(DATA_EDITOR_COUNT, content.editorCount);
      
      if (content.nodeId) {
        containerElem.setAttribute(DATA_EDITOR_CONTENT_NODE_ID, content.nodeId);
      }

      // 
      // Style: CSS classes and more.
      //
      
      containerElem.classList.add("notice-content"); 
      containerElem.classList.add("notice-content-level" + level);
      
      if (isSection) {
        containerElem.classList.add("notice-content-section");
      } else {
        containerElem.classList.add("notice-content-non-section");
      }
      
      if (content.hidden) {
        // Hide in production, but for development it is better to see what is going on.
        const hiddenClass = this.isDebug ? "notice-content-hidden-devel" : "notice-content-hidden";
        containerElem.classList.add(hiddenClass); 
      }
  
      if (content.readOnly) {
        containerElem.classList.add("notice-content-readOnly"); 
      }
      
      if (isCollapsed) {
        containerElem.classList.add("notice-content-collapsed"); 
      }
      
      if (isContentRepeatable) {
        containerElem.classList.add("notice-content-repeatable");
      }
      
      if (isField) {
        // The content is a field.
        
        if (formElem) {
          if (formElem.getAttribute("type") != "checkbox") {
            formElem.classList.add("notice-content-field");
          }
          if (field.type === "id") {
            formElem.classList.add("notice-content-idRef");
          }
          if (field.type === "id-ref") {
            formElem.classList.add("notice-content-id");
          }
        }
        
      } else {
        // This content is not a field.
        
        if (!elemToExpandOpt) { // Always add title except if expanding.
          // We use the word "header" here to avoid confusion with the HTML title attribute.
        
          const headerContainer = document.createElement("div");
          headerContainer.classList.add("notice-content-container");
         
          const header = document.createElement("h4");
          header.classList.add("notice-content-header");
          
          const paddedEditorCount = this.buildPaddedIdNumber(content);
          
          // Set the translation.
          // Get translations for group|name|...
          // There is an exception for the root dummy element.
          const i18nText = this.getTranslationById(content[NTD_LABEL]);
          
          const headerText = isContentRepeatable ? i18nText + " (" + paddedEditorCount + ")" : i18nText;
          if (headerText === undefined) {
            alert("header text is undefined: " + content.id);
          }
          if (headerText === null) {
            alert("header text is null: " + content.id);
          }
          header.appendChild(document.createTextNode(headerText));
          headerContainer.appendChild(header);
          containerElem.appendChild(headerContainer);
          containerElem.setAttribute("title", i18nText); // Mouse over text on any section.
        }
      }
      
      // If collapsed the child content should not be loaded yet.
      if (isCollapsed && !elemToExpandOpt) {
        // EXPAND LOGIC SETUP.
        // Setup of on click event so that content can be loaded into DOM on demand later on.
        const clickExpandFunc = this.createContentOnClickFunc(containerElem, content, level, false, containerElem, null);

        const isUseCapture = true; // As the child elements do not exist yet.
        containerElem.addEventListener("click", clickExpandFunc, isUseCapture);

        content.editorExpanded = false;

      } else {
        // The content should have sub content and not have been expanded yet.
        if (content.content && !content.editorExpanded) {
          // Load sub items.
          for (const contentSub of content.content) {
            // Recursion on sub content.
            this.readContentRecur(containerElem, contentSub, level + 1, false, null, null); 
          }
        }
      }
      
      if (isContentRepeatable && !content.hidden) {

        // REPEAT LOGIC SETUP.
        const elemButtonAddMore = document.createElement("button");
        elemButtonAddMore.setAttribute("type", "button");
        elemButtonAddMore.textContent = getEditorLabel("editor.add.more");
        elemButtonAddMore.classList.add("notice-content-button");
        elemButtonAddMore.classList.add("notice-content-button-add");
        
        // NOTE: here we add the content to the same parent as this is a sibling content and not a child content.
        const siblingOpt = containerElem; // Also specify the sibling so it can be added in the correct place inside the parent.
        const clickRepeatFunc = this.createContentOnClickFunc(parentElem, content, level, true, null, siblingOpt);
        elemButtonAddMore.addEventListener("click", clickRepeatFunc, false);
        containerElem.appendChild(elemButtonAddMore);
      }
      
      if (isContentRepeatable && !content.hidden && content.editorCount > 1) {
        // This element should have a remove button.
        const elemButtonRemove = document.createElement("button");
        elemButtonRemove.setAttribute("type", "button");
        elemButtonRemove.textContent = getEditorLabel("editor.remove");
        elemButtonRemove.classList.add("notice-content-button");
        elemButtonRemove.classList.add("notice-content-button-remove");
      
        elemButtonRemove.addEventListener("click", function() {
          parentElem.removeChild(containerElem);
          // TODO really or just keep incrementing?
          //content.editorCount--; // Decrease the counter.
        }, false);

        containerElem.appendChild(elemButtonRemove);    
      }
      
      if (elemToExpandOpt) {
        // The existing element has been expanded.
        content.editorExpanded = true;
        
      } else {
        // Add fragment to DOM (browser will update).
        documentFragment.appendChild(containerElem);

        if (siblingOpt) {
          // Add to parent next to sibling.
          editorInsertAfter(documentFragment, siblingOpt);
        } else {
          // Add to parent.
          parentElem.appendChild(documentFragment);
        }
        // The element is in the page now.
      }
      
      // This is the container and not the actual element that will contain the field value.
      if (formElem) {
        const formElemDomIdNew = this.buildIdUniqueNew(content);
        formElem.setAttribute("id", formElemDomIdNew);

        // IMPORTANT: this links the form items with the NTD and thus in some cases with the field and node map.
        formElem.setAttribute(DATA_EDITOR_CONTENT_ID, content.id);

        formElem.setAttribute(DATA_EDITOR_CONTENT_PARENT_ID, parentElem.getAttribute(DATA_EDITOR_CONTENT_ID)); 

        formElem.setAttribute(DATA_EDITOR_COUNT, content.editorCount);
        
        // This is used later on to retrieve form values.
        formElem.setAttribute(DATA_EDITOR_VALUE_FIELD, "true");

        // Set the translation.
        const i18nText = this.getTranslationById(content._label);
        if (i18nText && labelElem) {

          // The placeholder attribute works for some types of inputs only, but will not work for radio, checkbox, ...
          // It can be used to reduce the size of the form.
          //formElem.setAttribute("placeholder", i18n);
          // A label tag is used instead.

          labelElem.textContent = i18nText;
        }

        if (!formElem.getAttribute("title")) {
          formElem.setAttribute("title", i18nText + " (" + field.id + ")");
        }
      }
      
      if (isForRepeat) {
        this.handleValueSourceLogic(content);
      }
      
      // DO THIS AT THE VERY END.
      content.editorCount++; // The content has been added.
    }
    
    createContentOnClickFunc(containerElem, content, level, isForRepeat, elemToExpandOpt, siblingOpt) {
      const that = this;
      return function onClick(event) {
        console.debug("clicked content=" + content.id);
        event.stopPropagation();
        that.readContentRecur(containerElem, content, level + 1, isForRepeat, elemToExpandOpt, siblingOpt);
        containerElem.classList.add("notice-content-section-opened");
      };
    }
    
    buildFieldContainerElem(containerElem, content) {
  
      const noticeId = this.noticeId;
      const fieldMap = this.fieldMap;
      const codelistMap = this.codelistMap;
  
      // Find the fields.json field associated with this notice type definition field.
      const fieldId = content.id;
      const field = fieldMap[fieldId];
      if (!field) {
        throw new Error("Field is null for " + fieldId);
      }
      
      var formElem = null;
      if (field.type === "code") {
  
        formElem = this.buildFormElem(content);
        containerElem.appendChild(formElem);
        
        const fieldCodeListVal = field.codeList.value;
        
        var codelistId = fieldCodeListVal.id;
        const codelistInfo = codelistMap[codelistId];
         const codelistGc = codelistInfo.filename;
        
        const isHierarchical = fieldCodeListVal.type === "hierarchical";
        if (isHierarchical) {
          // TODO the data could be loaded in two steps (big category, then sub items).
          // Currently the editor demo does not suppose this feature.
          console.log("Editor: hierarchical codelists are not handled yet, codelistId=" + codelistId);
        }
        
        const select = formElem;
        const sdkVersion = SdkVersionSelector.getSelectedSdkVersion();
        const selectedLanguage = getSelectedLanguage();

        // TODO use getSelectedLanguage() after /lang, use "en" for now as translations are missing.
        const urlLang = selectedLanguage;
        //const urlLang = FALLBACK_LANGUAGE;
        
        const that = this;
        const urlToCodelistJson = "sdk/" + sdkVersion + "/codelists/" + codelistGc + "/lang/" + urlLang;

        (async () => {
          const response = await fetch(urlToCodelistJson);
          const data = await response.json();
          
          // Dynamically load the options.
          // const i18nText = that.getTranslationById(content._label);
          select.appendChild(createOption("", "")); // Empty option, has no value.
          for (const code of data.codes) {
            const codeLabelDesired = code[urlLang];
            const codeLabel = codeLabelDesired ? codeLabelDesired : code.en; // TODO always load lang + en ?
            select.appendChild(createOption(code.codeValue, codeLabel));
          }

          // After the select options have been set, an option can be selected.
          // Special case for some of the metadata fields.
          if (codelistId === "notice-subtype") {
            const value = NoticeSubtypeSelector.getSelectedNoticeSubtype();
            select.value = value;
          } else if (codelistId === "language_eu-official-language" && "BT-702(a)-notice" === content.id) {
            const value = getSelectedLanguage();
            select.value = lang2To3Map[value];
          }
          
          applyNiceSelector(select);        
        })();

      } else if (field.type === "indicator") {
        formElem = this.buildFormElem(content);
        const input = formElem;
        containerElem.appendChild(formElem);
        
      } else if (field.type === "id-ref") {
        formElem = this.buildFormElem(content);
        containerElem.appendChild(formElem);
        const select = formElem;
        const idSchemes = content._idSchemes;
        if (idSchemes && idSchemes.length > 0) {

          select.appendChild(createOption("", "")); // Empty option, has no value.
          //select.appendChild(createOption("", getEditorLabel("editor.select") + " " + String(idSchemes))); // Empty option, has no value.

          // Allows to find back select even if not knowing the idScheme, to find all in use idSchemes later on.
          select.setAttribute(DATA_EDITOR_ID_REFERENCE, JSON.stringify(idSchemes));
          for (const idScheme of idSchemes) {
            // Allows to find back select by idScheme later on.
            select.setAttribute(DATA_EDITOR_ID_REF_PREFIX + idScheme, "true");
          }
  
          const foundElements = this.findElementsWithAttributeIdSchemes(idSchemes);
          for (const foundElement of foundElements) {
            select.appendChild(createOption(foundElement.value, foundElement.value));
          }
        } else {
          if (!content.valueSource) {
            console.error("content _idSchemes not found for contentId=" + content.id);
          }
        }
  
      } else {
        // Fallback.
        formElem = this.buildFormElem(content);
        containerElem.appendChild(formElem);
        const input = formElem;
        
        // The provided pattern is be used instead.
        //if (field.type === "email") {
        //  input.setAttribute("type", "email");
        //}
  
        if (field.type === "url") {
          input.classList.add("notice-content-field-url");
        }
        
        if (isFieldTypeNumeric(field.type)) {
        
          // Nice to have but not required.
          input.setAttribute("type", "number");
          if (field.type !== "integer") {
            input.setAttribute("steps", "any"); // Allow decimals like 3.1415
          }
          
          // Min of zero would make sense in a lot of situations but a temperature could be negative.
          // TODO should we use range / intervals like MinZero [0, NULL] for that?
          //input.setAttribute("min", "0");
          //input.setAttribute("max", ...);
        }
        
        // DATE.
        if (field.type === "date") {
          input.setAttribute("type", "date"); // Nice to have but not required.
        }
        // TIME.
        if (field.type === "time") {
          input.setAttribute("type", "time"); // Nice to have but not required.
        }
        
        // Pattern, regex for validation.
        if (field.pattern && field.pattern.severity === "ERROR") {
          input.setAttribute("pattern", field.pattern.value);
          
          // The browser will show: "Please match the requested format: _TITLE_HERE_"
          // TODO the fields json pattern should come with english text explaining the pattern for error messages. 
          input.setAttribute("title", field.pattern.value);
        }
        
        if (field.type === "id") {
          const idScheme = content._idScheme; // In this case there is only one element, not an array.
          if (!idScheme) {
            console.warn("no content._idScheme found for contentId=" + content.id + ". This may be OK.");
          } else {
            input.setAttribute(DATA_EDITOR_INSTANCE_ID_FIELD, idScheme);
            const countStr = this.buildPaddedIdNumber(content);
            input.value = idScheme + "-" + countStr; // Something like "XYZ-0001"
            
            // TODO remove options if they are removed? This is problematic for a select.
        
            // NOTE: this will not work on the first pass during creation as elements are not yet in the DOM.
            // This will work during addition of extra elements 0002 and so on.
            const foundReferencingElements = this.findElementsWithAttributeIdRef(idScheme);
            for (const selectElem of foundReferencingElements) {
              selectElem.appendChild(createOption(input.value, input.value));
            }
          }
        }
      }
      
      if (!formElem) {
        throw new Error("A form element should have been defined at this point, for fieldId=" + fieldId);
      }

      // Add a label tag.
      const labelElem = document.createElement("label");
      labelElem.classList.add("notice-content-field-label");
      if (formElem.getAttribute("type") != "radio" && formElem.getAttribute("type") != "checkbox") {
        labelElem.classList.add("notice-content-field-label-block"); 
      }
      if (formElem.getAttribute("id")) {
        labelElem.setAttribute("for", formElem.getAttribute("id"));
      }
      containerElem.insertBefore(labelElem, formElem);
      
      // Set the language of the input text.
      if (field.type === "text" || field.type === "text-multilingual") {
        
        // Let the browser know in which language the text is, for example for spell checkers orscreen readers.
        formElem.setAttribute("lang", getSelectedLanguage());
        
        if (field.type === "text-multilingual") {
          // TODO the text and the associated language should be given by the form.
        }
      }
      
      if (field.maxLength) {
        formElem.setAttribute("maxlength", field.maxLength); 
      }
  
      if (content.readOnly) {
        // TODO is there a default technical value to set or is readOnly only for edition?
        // TODO there is another part of the code which sets readonly, see if this can be harmonized.
        formElem.setAttribute("readonly", "readonly");
      }
  
      const isRequired = isFieldValueMandatory(field, noticeId);
      if (isRequired) {
        formElem.setAttribute("required", "required");
        if (labelElem) {
          labelElem.classList.add("notice-content-required");
        }
      }
  
      // TODO repeatable, severity is a bit confusing ...
      const isFieldRepeatable = field.repeatable.value;
      if (isFieldRepeatable) {
        // Allow to add / remove fields.
        containerElem.classList.add("notice-content-field-repeatable");
        
        if (content._repeatable && !isFieldRepeatable) {
          console.error("fields.json repeatable mismatch on: " + field.id);
          containerElem.classList.add("notice-content-field-repeatable-mismatch");
        }
      }
      
      if (field.privacy) {
        console.debug(field.id + ", field privacy code=" + field.privacy.code);
        containerElem.classList.add("notice-content-field-privacy");
        // Example:
        // "code" : "cro-bor-law",
        // "unpublishedFieldId" : "BT-195(BT-09)-Procedure",
        // "reasonCodeFieldId" : "BT-197(BT-09)-Procedure",
        // "reasonDescriptionFieldId" : "BT-196(BT-09)-Procedure",
        // "publicationDateFieldId" : "BT-198(BT-09)-Procedure"
      }
      
      // Return multiple values.
      return {
        "containerElem" : containerElem, 
        "formElem" : formElem, 
        "labelElem" : labelElem, 
        "theField" : field
      };
    }
    
  } // End of Editor class.

  /**
   * Displays the notice info after loading.
   */
  function funcCallbackWhenLoadedDefinition() {
    document.getElementById("notice-info").style.display = "block";
  }


   async function createNoticeForm(sdkVersion, noticeId) {
    const noticeFormElem = document.getElementById("notice-type"); // Root content container element.
    noticeFormElem.innerHTML = ""; // Remove previous form.
    if (!sdkVersion || !noticeId) {
      return;
    }

    const selectedLanguage = getSelectedLanguage();
    noticeFormElem.setAttribute("lang", selectedLanguage);
    document.documentElement.setAttribute('lang', selectedLanguage);

    const htmlFormElem = document.getElementById("editor-id-form");

    // Spell check.
    // SECURITY: You should consider setting spellcheck to false for elements 
    // that can contain sensitive information.
    // Browsers have different behavior in how they deal with spellchecking in combination with the the lang attribute. 
    // Generally spelling is based on the browser language, not the language of the document.
    // Up to you.
    // This can be done on each input, here we do it on the entire form.
    // https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/spellcheck
    // https://caniuse.com/?search=spellcheck
    const activateSpellCheck = false;
    htmlFormElem.setAttribute("spellcheck", activateSpellCheck);
    htmlFormElem.setAttribute("translate", "no");
    
    // Form auto complete.
    // SECURITY: maybe deactivate autocomplete for sensitive information.
    // Up to you.
    // This can be done on each input, here we do it on the entire form.
    // https://caniuse.com/?search=autocomplete
    //htmlFormElem.setAttribute("autocomplete", "off");
    
    await SdkServiceClient.fetchTranslations(SdkVersionSelector.getSelectedSdkVersion(), getSelectedLanguage());
            
      // Use the Editor class.
      const editor = new Editor();
      editor.buildForm(); // Build the form. Initialize.
      funcCallbackWhenLoadedDefinition();

     
      console.log("Loaded editor notice type: " + NoticeSubtypeSelector.getSelectedNoticeSubtype());
  }

  // fields.json data related functions.

  function isFieldTypeNumeric(fieldType) {
    // TODO having to do so many ORs is annoying.
    // It would be better to have main type and and more precise sub type.
    return fieldType === "number" || fieldType === "integer" || fieldType === "amount";
  }

  function isFieldValueMandatory(field, noticeId) {
    const mandatory = field.mandatory;
    if (mandatory && mandatory.severity === "ERROR") {
      const constraints = mandatory.constraints;
      for (const constraint of constraints) {
        const noticeTypes = constraint.noticeTypes;
        if (constraint.severity === "ERROR" && noticeTypes) {
          if (noticeTypes.includes(noticeId)) {
            return constraint.value;
          } 
        }
      }
      return mandatory.value;
    }
    return false;
  }

  // HELPER FUNCTIONS.


  // Generic reusable helper functions.

  function show(id) {
    document.getElementById(id).style.display = "block";
  }

  function setText(id, text) {
    document.getElementById(id).textContent = text;
  }

  /**
   * Insert node after the existing node. This only works if the existing node has a parent.
   */
  function editorInsertAfter(nodeToInsert, existingNode) {
    existingNode.parentNode.insertBefore(nodeToInsert, existingNode.nextSibling);
  }

  /**
   * Creates an option tag for use in a select.
   */
  function createOption(valueTechnical, valueLabel) {
    const elemOption = document.createElement("option");
    elemOption.setAttribute("value", valueTechnical);
    elemOption.textContent = valueLabel;
    return elemOption;
  }

  /**
   * Generic GET error handling, provided as a demo.
   */
  function jsonGetOnError(xhr) {
    const msg = "Error loading data, error=" + xhr.responseText;
    if (console.error) {
      console.error(msg);
    } else {
      console.log(msg);
    }
    alert(msg); // For demo purposes alert is good enough.
  }

  /**
   * Generic POST error handling, provided as a demo.
   */
  function jsonPostOnError(xhr) {
    const msg = "Error posting data, error=" + xhr.responseText;
    if (console.error) {
      console.error(msg);
    } else {
      console.log(msg);
    }
    alert(msg); // For demo purposes alert is good enough.
  }


  /**
   * Left pad text.
   */
  function lpad(padText, textToPad, length) {
    while (textToPad.length < length) {
      textToPad = padText + textToPad;
    }
    return textToPad;
  }

  function buildRandomUuidv4() {
    // This is for demo purposes, you could generate this UUID on the server-side using Java, C#,...
    // https://stackoverflow.com/questions/105034/how-do-i-create-a-guid-uuid/2117523#2117523
    return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
      (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
  }
  
  function applyNiceSelector(elemSelector) {
    const settings = {};
    new TomSelect(elemSelector, settings);
  }

  function showAppVersion(appVersion) {
    setText("editor-version", appVersion);
  }

  // We don't use static initializers because they are not supported yet by Safari
  NoticeSubtypeSelector.setup();
  Editor.setup();
  
  // Launch
  (async () => {

    // I. Get a list of available SDK versions from the back-end.
    await SdkServiceClient.fetchVersionInfo();
  
    // II. Populate the SdkVersionSelector dropdown.
    // After the SdkVersionSelector is populated its onchange event is raised triggering a "chain reaction":
    // 1. The NoticeSubtypeSelector picks up the event and  populates itself; then its own onchange event is raised.
    // 2. Then the Editor picks it up and loads the selected notice subtype. 
    SdkVersionSelector.populate();

    showAppVersion(SdkServiceClient.appVersion);

  })();

})();