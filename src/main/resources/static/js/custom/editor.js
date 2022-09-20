(function() {
  console.log("Loading editor script.");

  // Avoids conflicts with other identifiers in the context of this browser tab.
  const idPrefix = "editor-id-";

  const DATA_EDITOR_ID_REFERENCE = 'data-editor-id-reference';
  const DATA_EDITOR_ID_REF_PREFIX = 'data-editor-id-ref-';
  const DATA_EDITOR_INSTANCE_ID_FIELD = 'data-editor-instance-id-field';

  const displayTypeToElemInfo = {};
  displayTypeToElemInfo["CHECKBOX"] = {"tag": "input", "type" : "checkbox"};
  displayTypeToElemInfo["COMBOBOX"] = {"tag": "select", "type" : "select"};
  displayTypeToElemInfo["RADIO"] = {"tag": "input", "type" : "radio"};
  displayTypeToElemInfo["TEXTAREA"] = {"tag": "textarea"};
  displayTypeToElemInfo["TEXTBOX"] = {"tag": "input", "type": "text"};
  
  function buildFormElem(content) {
    var elemInfo = displayTypeToElemInfo[content.displayType];
    if (!elemInfo) {
      elemInfo = displayTypeToElemInfo["TEXTBOX"]; // Fallback.
    }
		const elem = document.createElement(elemInfo.tag);
		if (elemInfo.type) {
  		elem.setAttribute("type", elemInfo.type);
		}
		return elem;
  }
  
  const i18n = {};
  i18n["en"] = {
    "add.more": "Add one more",
    "remove": "Remove",
    "select": "Select"
  };
  
  i18n["fr"] = {
    "add.more": "En ajouter un",
    "remove": "Enlever",
    "select": "Choisir"
  };

  function getLabel(labelKey) {
    getLabel(labelKey, getSelectedLanguage());
  }
  
  function getLabel(labelKey, lang) {
    // How you get and handle i18n label data is up to you.
    var dataForLang = i18n[lang];
    if (!dataForLang && i18n["en"] && lang !== "en") {
      dataForLang = i18n["en"];
    }
    return dataForLang[labelKey];
  }
  
  // --- i18n ---
  function downloadSdkFieldsTranslations(languageCode, callbackFunc) {
    const sdkVersion = getSdkVersion();
    if (!sdkVersion) {
      return;
    }
    // XHR to load translations (i18n) for fields.
    jsonGet("/sdk/" + sdkVersion + "/translations/fields/" + languageCode + ".json", 5000, callbackFunc, jsonGetOnError);
  }
  
  // --- EDITOR ---
  
  class Editor {
    constructor(dataFieldsJson, dataNoticeType, dataI18n, noticeFormElement) {
    
      if (!dataFieldsJson.sdkVersion) {
        throw new Error("Invalid sdkVersion");
      }

      // FIELDS BY ID.
      const fields = dataFieldsJson.fields;
      console.log("Loaded fields: " + fields.length);
      const fieldMap = {};
      for (var field of fields) {
        fieldMap[field.id] = field;
      }
      this.fieldMap = fieldMap;
      
      // NODES BY ID.
      const nodes = dataFieldsJson.xmlStructure;
      console.log("Loaded nodes: " + nodes.length);
      const nodeMap = {};
      for (var node of nodes) {
        nodeMap[node.id] = node;
      }
      this.nodeMap = nodeMap;
      
      this.dataI18n = dataI18n;
      
      // The root content is an array.
      this.noticeRootContent = {"id" : "THE_ROOT", "_label" : "", "content" : dataNoticeType.content};
      this.noticeId = dataNoticeType.noticeId;
      
      this.noticeFormElement = noticeFormElement;
      
      const serializeBtnElem = document.getElementById("id-editor-log-json");
      const that = this;
      const serializeToJsonFunc = function(event) {
        console.debug("Attempting to serialize form to JSON.");
        event.preventDefault();
        that.toModel();
      };
      serializeBtnElem.addEventListener("click", serializeToJsonFunc, false);
    }
    
    getTranslationById(labelId) {
      const label = this.dataI18n[labelId];
      return label ? label : null;
    }
    
    toModel() {
      console.info("toModel");
      const textArea = document.getElementById("id-editor-log-json-area");
      textArea.value = '';

      // TODO idScheme id increment and handling of repeat, should be done after adding to page.

		  // OUT
      // 0. create array
      // 1. use selector on custom attributes (put parentId into custom data-attribute)
      // Select array of all element storing a value.
      // 2. fill array (in order)
      // 3. order should already be ok, just use data-...-parentId to rebuild the tree
      
      const dataModel = {};
      const fieldElems = document.querySelectorAll("[data-editor-value-field='true']");
      for (var fieldElem of fieldElems) {
        const contentId = fieldElem.getAttribute("data-editor-content-id");
        const contentParentId = fieldElem.getAttribute("data-editor-content-parent-id");
        dataModel[contentId]= {"id" : contentId, "parentId" : contentParentId, "value" : fieldElem.value};
        // dataModel parentId
      }
      //console.dir(dataModel);
      textArea.value = JSON.stringify(dataModel, null, 2);
      textArea.style.display = 'block';
    }
    
    fromModel() {
      // IN
      // Load form
      // Loading of form data into form
      // 0. recurse through data
    }
    
    buildForm() {
      const rootLevel = 1; // Top level, sub items will be on level 2, 3, 4 ...
      
      // This builds the form.
      // TODO readContent could become part of the class as we pass "this" to it.
      this.readContentRecur(this.noticeFormElement, this.noticeRootContent, rootLevel, false, null);
      
      // Fills selects that have id references.
      this.populateIdRefSelectsAll();
      
      this.handleValueLogic(this.noticeRootContent);
    }
    
    handleValueLogic(content) {
      // Handle content "value" logic.
      const that = this;
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
      that.visitContentRec(content, [visitorFunc]);
    }
    
    buildIdPartial(content) {
      return this.buildIdPartialFromContentId(content.id);
    }

    buildIdPartialFromContentId(contentId) {
      return idPrefix + contentId;
    }
    
    // This is called during creation.
    buildIdUniqueNew(content) {
      const paddedNumber = this.buildPaddedIdNumber(content);
      return this.buildIdPartial(content) + "-" + paddedNumber;
    }

    // This allows to build the id of any other field (for example).    
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
    
    visitContentRec(content, contentVisitorFuncs) {
      if (!content) {
        throw new Error("Invalid content");
      }
      if (!contentVisitorFuncs) {
        throw new Error("Invalid contentVisitorFuncs");
      }
      for (var contentVisitorFunc of contentVisitorFuncs) {
        contentVisitorFunc(content);
      }
      if (content.content && !content.editorExpanded) {
	      // Visit sub items.
	      for (var contentSub of content.content) {
	        this.visitContentRec(contentSub, contentVisitorFuncs); // Recursion on sub content.
	      }
	    }
    }
    
    findElementWithAttributeIdScheme(idScheme) {
      return this.findElementWithAttributeIdSchemes([idScheme]);
    }
    
    findElementWithAttributeIdSchemes(idSchemes) {
      // Find the HTML elements having the idScheme (search HTML element by attribute).
      const allFoundElements = [];
      for (var idScheme of idSchemes) {
        const selector = "[" + DATA_EDITOR_INSTANCE_ID_FIELD + '="' + idScheme + '"]';
        const foundElements = document.querySelectorAll(selector);
        for (var element of foundElements) {
          allFoundElements.push(element);
        }
      }
      return allFoundElements;
    }
    
    findElementWithAttributeIdRef(idScheme) {
      // Find HTML elements that reference this kind of idScheme.
      const selector = '[' + DATA_EDITOR_ID_REF_PREFIX + idScheme.toLowerCase() + '="true"]';
      return document.querySelectorAll(selector);
    }
    
    populateIdRefSelectsForIdScheme(idScheme) {
      const foundReferencedElements = this.findElementWithAttributeIdScheme(idScheme);
      var foundReferencingElements = this.findElementWithAttributeIdRef(idScheme);

      for (var selectElem of foundReferencingElements) {
         const selectedValue = selectElem.value;
        selectElem.innerHtml = "";
        for (var inputElem of foundReferencedElements) {
          selectElem.appendChild(createOption(inputElem.value, inputElem.value));
        }
        // TODO what happens if the value is not there anymore?
        selectElem.value = selectedValue;
      }
    }
    
    populateIdRefSelectsAll() {

      // Find all in use id schemes.
      const selector = "[" + DATA_EDITOR_ID_REFERENCE + "]";
      const selectElements = document.querySelectorAll(selector);
      const inUseIdSchemeSet = new Set();
      for (var selectElem of selectElements) {
        const inUseIdSchemes = selectElem.getAttribute(DATA_EDITOR_ID_REFERENCE);
        const idSchemes = JSON.parse(inUseIdSchemes);
        for (var idScheme of idSchemes) {
          inUseIdSchemeSet.add(idScheme);
        }
      }

      // The id schemes are unique, for each find values and populate selects.
      for (var idScheme of inUseIdSchemeSet) {
        this.populateIdRefSelectsForIdScheme(idScheme);
      }
    }
    
    readContentRecur(parentElem, content, level, isForRepeat, elemToExpand) {
	    const noticeId = this.noticeId;
	  
	    //console.debug("readContentRecur content.id=" + content.id + ", level=" + level + ", isForRepeat=" + isForRepeat);  
	    const documentFragment = document.createDocumentFragment();
	    content.editorLevel = level; // Enrich model for later.
	    
	    // The editorCount will allow to make the id of this element unique.
	    content.editorCount = content.editorCount >= 0 ? content.editorCount : 1;
	
	    const isField =  content.contentType === "field" // !content.content;
	    const isSection = content.section;
	    
	    const isCollapsed = content.collapsed ? true : false;
	    
	    // TODO compare content repeatable and node.repeatable (editor.nodeMap[...]), show conflicts
	    var isContentRepeatable = content.repeatable ? true : false; // Can be reassigned if field...
	     
	    // The container element may already exist in the case of uncollapsing (expand).
	    var containerElem = elemToExpand ? elemToExpand : document.createElement("div");
	
	    var field; // Can remain undefined or null.
	    var formElem;
	    if (isField) {
	      const resultMap = this.buildFieldContainerElem(containerElem, content);
	      formElem = resultMap["formElem"];
	      field = resultMap["field"];
	    }
	    
	    // Prefix the ids to avoid conflict with various other identifiers existing in the same page.
	    // For repeatable fields the content editorCount ensures the ids are unique.
	    
	    // This is the container and not the actual element that will contain the field value.
	    containerElem.setAttribute("id", this.buildIdUniqueNew(content) + "-container-elem");
	    
	    containerElem.setAttribute("data-editor-content-id", content.id + "-container-elem");
	    containerElem.setAttribute("data-editor-content-parent-id", parentElem.getAttribute("data-editor-content-id"));
	     
	    containerElem.setAttribute("data-editor-count", content.editorCount);
	
	    // The id will vary if the element is repeatable but the editor type will not.
	    containerElem.setAttribute("data-editor-type",  this.buildIdPartial(content));
	
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
	      containerElem.classList.add("notice-content-hidden"); 
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
	        formElem.classList.add("notice-content-field");
	         
	        if (field.type === "id") {
	          formElem.classList.add("notice-content-idRef");
	        }
	        if (field.type === "id-ref") {
	          formElem.classList.add("notice-content-id");
	        }
	        
	        // Set the translation.
	        const i18n = this.getTranslationById(content._label);
	        formElem.setAttribute("placeholder", i18n);
	       
	        if (!formElem.getAttribute("title")) {
	          formElem.setAttribute("title", i18n + " (" + field.id + ")");
	        }
	      }
	    } else {
	      // This content is not a field.
	      if (!elemToExpand) { // Always add title except if expanding.
	        // We use the word "header" here to avoid confusion with the HTML title attribute.
	      
	        const headerContainer = document.createElement("div");
	        headerContainer.classList.add("notice-content-container");
	       
	        const header = document.createElement("h4");
	        header.classList.add("notice-content-header");
	        
	        const paddedEditorCount = this.buildPaddedIdNumber(content);
	        
	        // Set the translation.
	        // TODO get translations for group|name|...
	        const i18n = content._label;
	        
	        const headerText = isContentRepeatable ? i18n + " (" + paddedEditorCount + ")" : i18n;
	        if (headerText === undefined) {
	          alert("header text is undefined: " + content.id);
	        }
	        header.appendChild(document.createTextNode(headerText));
	        headerContainer.appendChild(header);
	        containerElem.appendChild(headerContainer);
	        containerElem.setAttribute("title", i18n); // Mouse over text on any section.
	      }
	    }
	    
	    // If collapsed the child content should not be loaded yet.
	    if (isCollapsed && !elemToExpand) {
	      // EXPAND LOGIC SETUP.
	      // Setup of on click event so that content can be loaded into DOM on demand later on.
	      const clickExpandFunc = this.createContentOnClickFunc(containerElem, content, level, false, containerElem);

	      const isUseCapture = true; // As the child elements do not exist yet.
	      containerElem.addEventListener("click", clickExpandFunc, isUseCapture);

	      content.editorExpanded = false;
	    } else {
	      // The content should have sub content and not have been expanded yet.
	      if (content.content && !content.editorExpanded) {
	        // Load sub items.
	        for (var contentSub of content.content) {
	          this.readContentRecur(containerElem, contentSub, level + 1, false, null); // Recursion on sub content.
	        }
	      }
	    }
	    
	    if (isContentRepeatable) {
	      // REPEAT LOGIC SETUP.
	      const elemButtonAddMore = document.createElement("button");
	      elemButtonAddMore.setAttribute("type", "button");
	      elemButtonAddMore.textContent = getLabel("add.more");
	      elemButtonAddMore.classList.add("notice-content-button");
	      elemButtonAddMore.classList.add("notice-content-button-add");
	      
	      // NOTE: here we add the content to the same parent as this is a sibling content and not a child content.
	      const clickRepeatFunc = this.createContentOnClickFunc(parentElem, content, level, true, false);
	      elemButtonAddMore.addEventListener("click", clickRepeatFunc, false);
	      containerElem.appendChild(elemButtonAddMore);
	    }
	    
	    if (isContentRepeatable && content.editorCount > 2) {
	      // This element should have a remove button.
	      const elemButtonRemove = document.createElement("button");
	      elemButtonRemove.setAttribute("type", "button");
	      elemButtonRemove.textContent = getLabel("remove");
	      elemButtonRemove.classList.add("notice-content-button");
	      elemButtonRemove.classList.add("notice-content-button-remove");
	      
	      elemButtonRemove.addEventListener("click", function(){
	        parentElem.removeChild(containerElem);
	        // TODO really or just keep incrementing?
	        //content.editorCount--; // Decrease the counter.
	      }, false);
	      containerElem.appendChild(elemButtonRemove);    
	    }
	    
	    if (elemToExpand) {
	      // The existing element has been expanded.
	      content.editorExpanded = true;
	    } else {
	      // Add fragment to DOM (browser will update).
	      documentFragment.appendChild(containerElem);
	      parentElem.appendChild(documentFragment); // Add to fragment (not in DOM yet).
	      // The element is in the page now.
	    }
	    
	    // This is the container and not the actual element that will contain the field value.
	    if (formElem) {
	      formElem.setAttribute("id", this.buildIdUniqueNew(content));

	      formElem.setAttribute("data-editor-content-id", content.id);
	      formElem.setAttribute("data-editor-content-parent-id", parentElem.getAttribute("data-editor-content-id")); 

	      formElem.setAttribute("data-editor-count", content.editorCount);
	      formElem.setAttribute("data-editor-value-field", "true"); 
	    }
	    
	    if (isForRepeat) {
	      this.handleValueLogic(content);
	    }
	    
	    // DO THIS AT THE VERY END.
	    content.editorCount++; // The content has been added.
	  }
	  
	  createContentOnClickFunc(containerElem, content, level, isForRepeat, elemToExpand) {
	    const that = this;
      return function onClick(event) {
        console.debug("clicked content=" + content.id);
        event.stopPropagation();
        that.readContentRecur(containerElem, content, level + 1, isForRepeat, elemToExpand);
        containerElem.classList.add("notice-content-section-opened");
      };
    }
    
    buildFieldContainerElem(containerElem, content) {
  
	    const noticeId = this.noticeId;
	    const fieldMap = this.fieldMap;
	
	    // Find the fields.json field associated with this notice type definition field.
	    const fieldId = content.id;
	    const field = fieldMap[fieldId];
	    if (!field) {
	      throw new Error("Field is null for " + fieldId);
	    }
	    
	    var formElem = null;
	    if (field.type === "code") {
	
	      formElem = buildFormElem(content);
	      containerElem.appendChild(formElem);
	      
	      const fieldCodeListVal = field.codeList.value;
	      
	      var codelistId = fieldCodeListVal.id;
	      const parentId = fieldCodeListVal.parentId;
	      if (parentId) {
	        // This codelist is tailored.
	        // TODO having to do this here is clearly not ideal.
	        codelistId = parentId + "_" + codelistId;
	      }
	      
	      const isHierarchical = fieldCodeListVal.type === "hierarchical";
	      if (isHierarchical) {
	        // TODO the data could be loaded in two steps (big category, then sub items).
	        // Currently the editor demo does not suppose this feature.
	      }
	      
	      const select = formElem;
	      const sdkVersion = getSdkVersion();
	      
	      var that = this;
	      // TODO codelist language: get only label for desired language instead of /en
	      var urlToCodelistJson = "sdk/" + sdkVersion + "/codelists/" + codelistId + "/lang/en";
	      var afterCodelistLoad = function(data) {
	        // Dynamically load the options.
	         const i18n = that.getTranslationById(content._label);
	         select.appendChild(createOption("", i18n)); // Empty option.
	         for (var code of data.codes) {
	           select.appendChild(createOption(code.codeValue, code.en));
	         }
	      };
	      
	      // Give this a larger timeout as some codelists could be quite big.
	      // Ideally the JSON response should be cached for a while, you have to allow this server-side.
	      jsonGet(urlToCodelistJson, 6000, afterCodelistLoad, jsonGetOnError);
	      
	    } else if (field.type === "indicator") {
	      formElem = buildFormElem(content);
	      const input = formElem;
	      containerElem.appendChild(formElem);
	      
	    } else if (field.type === "id-ref") {
	      // TODO in theory it should be only "id-ref"
	      formElem = buildFormElem(content);
	      containerElem.appendChild(formElem);
	      const select = formElem;
	      const idSchemes = content._idSchemes;
	      if (idSchemes && idSchemes.length > 0) {

	        select.appendChild(createOption("", getLabel("select") + " " + String(idSchemes))); // Empty option.
	
	        // Allows to find back select even if not knowing the idScheme, to find all in use idSchemes later on.
	        select.setAttribute(DATA_EDITOR_ID_REFERENCE, JSON.stringify(idSchemes));
	        for (var idScheme of idSchemes) {
	          // Allows to find back select by idScheme later on.
	          select.setAttribute(DATA_EDITOR_ID_REF_PREFIX + idScheme, "true");
	        }
	
	        const foundElements = this.findElementWithAttributeIdSchemes(idSchemes);
	        for (var foundElement of foundElements) {
	          select.appendChild(createOption(foundElement.value, foundElement.value));
	        }
	      } else {
	        if (!content.valueSource) {
    	      console.error("content _idSchemes not found for contentId=" + content.id);
	        }
	      }
	
	    } else {
	      // Fallback.

	      formElem = buildFormElem(content);
	      containerElem.appendChild(formElem);
	      const input = formElem;
	      
	      // The provided pattern will be used instead.
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
	        // TODO Maybe the fields json pattern should come with english text explaining the pattern for error messages. 
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
	          var foundReferencingElements = this.findElementWithAttributeIdRef(idScheme);
	          for (var selectElem of foundReferencingElements) {
	            selectElem.appendChild(createOption(input.value, input.value));
	          }
	        }
	      }
	    }
	    
	    if (!formElem) {
	      throw new Error("A form element should have been defined at this point, for fieldId=" + fieldId);
	    }
	    
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
	      formElem.setAttribute("readonly", "readonly");
	    }
	
	    const isRequired = isFieldValueMandatory(field, noticeId);
	    if (isRequired) {
	      formElem.setAttribute("required", "required");
	      containerElem.classList.add("notice-content-required");
	    }
	
	    // TODO repeatable, severity is a bit confusing ...
	    const isFieldRepeatable = field.repeatable.value;
	    if (isFieldRepeatable) {
	      // Allow to add / remove fields.
	      containerElem.classList.add("notice-content-field-repeatable");
	      
	      if (content.repeatable && !isFieldRepeatable) {
	        console.error("fields.json repeatable mismatch on: " + field.id);
	        containerElem.classList.add("notice-content-field-repeatable-mismatch");
	      }
	    }
	    
	    if (field.privacy) {
	      console.debug(field.id + ", field privacy code=" + field.privacy.code);
	      containerElem.classList.add("notice-content-field-privacy");
	       
	      // "code" : "cro-bor-law",
        // "unpublishedFieldId" : "BT-195(BT-09)-Procedure",
        // "reasonCodeFieldId" : "BT-197(BT-09)-Procedure",
        // "reasonDescriptionFieldId" : "BT-196(BT-09)-Procedure",
        // "publicationDateFieldId" : "BT-198(BT-09)-Procedure"
	    }
			
	    return {"containerElem" : containerElem, "formElem" : formElem, "field" : field};
	  }
	  
  } // End of Editor class.
  
  function funcCallbackWhenLoadedDefinition() {
    document.getElementById("notice-info").style.display = "block";
  }
  jsonGet("/sdk/info", 3000, afterInitDataLoaded, jsonGetOnError);
  
  function afterInitDataLoaded(data) {
    const sdkVersions = data.sdkVersions;
    const appVersion = data.appVersion;
    if (appVersion === undefined || appVersion === null) {
      throw new Error("Invalid appVersion");
    }
    setText("editor-version", appVersion);
    
    const elemSdkSelector = getElemSdkSelector();
    elemSdkSelector.innerHtml = "";
    
     // Dynamically load the options.   
    for (var sdkVersion of sdkVersions) {
      elemSdkSelector.appendChild(createOption(sdkVersion, sdkVersion));
    }
    elemSdkSelector.onchange = function() {
      sdkVersionChanged();
    }
    sdkVersionChanged(); // Initialize.
    console.log("Loaded editor init info.");
  }
  
  function sdkVersionChanged() {
    const sdkVersion = getSdkVersion();
    if (!sdkVersion) {
      return;
    }
    // XHR to load existing notice types of selected SDK version.
    jsonGet("/sdk/" + sdkVersion + "/notice-types", 3000, afterSdkNoticeTypesLoaded, jsonGetOnError);
  }
  
  function afterSdkNoticeTypesLoaded(data) {
    console.log("Loaded available noticeTypes");
    const noticeTypes = data.noticeTypes;
    const elemNoticeTypeSelector = getElemNoticeTypeSelector();
    elemNoticeTypeSelector.innerHtml = "";
    elemNoticeTypeSelector.value = ""; // Reset the value.
    
    // Dynamically load the options.   
    for (var noticeType of noticeTypes) {
      elemNoticeTypeSelector.innerHtml = "";
      elemNoticeTypeSelector.appendChild(createOption(noticeType, noticeType));
    } 

    document.getElementById("notice-type-selector").onchange = function() {
      const noticeId = this.value;
      const selectedSdkVersion = getSdkVersion();
      createNoticeForm(selectedSdkVersion, noticeId, funcCallbackWhenLoadedDefinition);
    };
    document.getElementById("notice-type-selector").onchange();
  }
  
  function createNoticeForm(sdkVersion, noticeId) {
    const noticeFormElem = document.getElementById("notice-type"); // Root content container element.
    noticeFormElem.innerHTML = ""; // Remove previous form.
    
    if (!sdkVersion || !noticeId) {
      return;
    }
    
    downloadSdkFieldsTranslations("en", function(dataI18n) {
	    const urlToGetFieldJsonData = "/sdk/" + sdkVersion + "/fields";
	    const jsonOkFieldsFunc = function(dataFieldsJson) {
	      if (!dataFieldsJson.sdkVersion) {
	        throw new Error("Invalid sdkVersion");
	      }
	  
	      // NOTE: the data could be loaded in parallel, but for this demo project we do it serial.
	      const urlToGetNoticeTypeJsonData = "/sdk/" + sdkVersion + "/notice-types/" + noticeId;
	      
	      const jsonOkNoticeTypeFunc = function(dataNoticeType) {
	        const sdkVersion = dataNoticeType.sdkVersion;
	        if (!sdkVersion) {
	          throw new Error("Invalid sdkVersion: " + sdkVersion);
	        }
	        setText("notice-sdkVersion", sdkVersion);
	        setText("notice-noticeId", dataNoticeType.noticeId);
	        
	        const editor = new Editor(dataFieldsJson, dataNoticeType, dataI18n, noticeFormElem);
	
	        editor.buildForm(); // Build the form. Initialize.
	        
	        funcCallbackWhenLoadedDefinition();
	        console.log("Loaded editor notice type: " + urlToGetNoticeTypeJsonData);
	      };
	      jsonGet(urlToGetNoticeTypeJsonData, 4000, jsonOkNoticeTypeFunc, jsonGetOnError);
	    };
	    jsonGet(urlToGetFieldJsonData, 4000, jsonOkFieldsFunc, jsonGetOnError);
    });
  }
  
  // fields.json related functions.
  
  function isFieldTypeNumeric(fieldType) {
    // TODO having to do so many ORs is annoying.
    return fieldType === "number" || fieldType === "integer" || fieldType === "amount";
  }
  
  function isFieldValueMandatory(field, noticeId) {
    const mandatory = field.mandatory;
    if (mandatory && mandatory.severity === "ERROR") {
      const constraints = mandatory.constraints;
      for (var constraint of constraints) {
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
  
  // Functions specific to the HTML.
  
  function getElemSdkSelector() {
    return document.getElementById("notice-sdk-selector");
  }
  
  function getSdkVersion() {
    return getElemSdkSelector().value;
  }
  
  function getElemNoticeTypeSelector() {
    return document.getElementById("notice-type-selector");
  }

  function getSelectedLanguage() {
    return document.getElementById("notice-lang-selector").value;
  }
   
  // Generic reusable helper functions.
  
  function show(id) {
    document.getElementById(id).style.display = "block";
  }
  
  function setText(id, text) {
    document.getElementById(id).textContent = text;
  }
  
  function createOption(valueTechnical, valueLabel) {
    const elemOption = document.createElement("option");
    elemOption.setAttribute("value", valueTechnical);
    elemOption.textContent = valueLabel;
    return elemOption;
  }
  
  function jsonGetOnError(xhr) {
    const msg = "Error loading data.";
    if (console.error) {
      console.error(msg);
    } else {
      console.log(msg);
    }
    alert(msg);
  }
    
  function jsonGet(urlGet, timeoutMillis, fnOk, fnErr) {
    buildJsonGet(urlGet, timeoutMillis, fnOk, fnErr).send();
  }
  
  function buildJsonGet(urlGet, timeoutMillis, fnOk, fnErr) {
    const xhr = new XMLHttpRequest();
    xhr.open("GET", urlGet, true);  // Asnyc HTTP GET request by default.
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.timeout = timeoutMillis;
    xhr.onload = function() {
      if (xhr.status === 200) {
        const jsonData = JSON.parse(xhr.responseText);
         fnOk(jsonData);
      } else {
        fnErr(xhr);
      }
    };
    return xhr;
  }
  
  function lpad(padText, textToPad, length) {
    while (textToPad.length < length) {
      textToPad = padText + textToPad;
    }
    return textToPad;
  }
  
})();