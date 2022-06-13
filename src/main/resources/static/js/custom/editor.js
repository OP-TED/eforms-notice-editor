(function() {
	console.log("Loading editor script.");

  // Avoids conflicts with other identifiers in the context of this browser tab.
	const idPrefix = "editor-id-";

  // TODO tttt maxLength 30 seems to be the threshold in the fields.json, we should provide this in the fields.json as an extra top level info?
  const textAreaThreshold = 30;
  
  const DATA_EDITOR_ID_REFERENCE = 'data-editor-id-reference';
  const DATA_EDITOR_ID_REF_PREFIX = 'data-editor-id-ref-';
  const DATA_EDITOR_INSTANCE_ID_FIELD = 'data-editor-instance-id-field';
  
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
  
  class Editor {
    constructor(dataFieldsJson, noticeType, noticeFormElement) {
    
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
			
			// NODES BY ID.
			const nodes = dataFieldsJson.xmlStructure;
		  console.log("Loaded nodes: " + nodes.length);
		  const nodeMap = {};
		  for (var node of nodes) {
		    nodeMap[node.id] = node;
		  }
    
      this.fieldMap = fieldMap;
      this.nodeMap = nodeMap;
      
      this.noticeRootContent = noticeType.content;
      this.noticeId = noticeType.noticeId;
      
      this.noticeFormElement = noticeFormElement;
    }
    
    buildForm() {
  	  const level = 1; // Top level, sub items will be on level 2, 3, 4 ...
  	  
      // This builds the form.
  	  // TODO readContent could become part of the class as we pass "this" to it.
    	readContent(this, this.noticeFormElement, this.noticeRootContent, level, false, null);
    	
    	// Fills selects that have id references.
    	this.populateIdRefSelectsAll();
    }
    
    buildIdPartial(content) {
      return idPrefix + content.id;
    }
    
    buildIdUnique(content) {
      const countStr = lpad("0", String(content.editorCount), 4); // 0001, 0002, ...
      return this.buildIdPartial(content) + "-" + countStr;
    }
    
    findElementWithAttributeTag(tag) {
      return this.findElementWithAttributeTags([tag]);
    }
    
    findElementWithAttributeTags(tags) {
      // Find the HTML elements having the tag (search HTML element by attribute).
      const allFoundElements = [];
      for (var tag of tags) {
        const selector = "[" + DATA_EDITOR_INSTANCE_ID_FIELD + '="' + tag + '"]';
        const foundElements = document.querySelectorAll(selector);
        for (var element of foundElements) {
          allFoundElements.push(element);
        }
      }
      return allFoundElements;
    }
    
    findElementWithAttributeIdRef(tag) {
      // Find HTML elements that reference this kind of tag.
      const selector = '[' + DATA_EDITOR_ID_REF_PREFIX + tag.toLowerCase() + '="true"]';
      return document.querySelectorAll(selector);
    }
    
    populateIdRefSelectsForTag(tag) {
   	  const foundReferencedElements = this.findElementWithAttributeTag(tag);
    	var foundReferencingElements = this.findElementWithAttributeIdRef(tag);

      for (var selectElem of foundReferencingElements) {
  	 		const selectedValue = selectElem.value;
  		  selectElem.innerHtml = "";
        for (var inputElem of foundReferencedElements) {
  	      selectElem.appendChild(createOption(inputElem.value, inputElem.value));
        }
        // TODO tttt what happens if the value is not there anymore ????
        selectElem.value = selectedValue;
      }
    }
    
    populateIdRefSelectsAll() {

      // Find all in use tags.
      const selector = "[" + DATA_EDITOR_ID_REFERENCE + "]";
      const selectElements = document.querySelectorAll(selector);
      const inUseTagSet = new Set();
      for (var selectElem of selectElements) {
      	const inUseTags = selectElem.getAttribute(DATA_EDITOR_ID_REFERENCE);
      	const tags = JSON.parse(inUseTags);
      	for (var tag of tags) {
      	  inUseTagSet.add(tag);
      	}
      }

      // The tags are unique, for each tag find values and populate selects.
      for (var tag of inUseTagSet) {
        this.populateIdRefSelectsForTag(tag);
      }
    }
 
  }
	
	function funcCallbackWhenLoadedDefinition() {
	  document.getElementById("notice-info").style.display = "block";
	}
  jsonGet("/sdk/info", 2000, afterInitDataLoaded, jsonGetOnError);
  
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
    jsonGet("/sdk/" + sdkVersion + "/notice-types", 2000, afterSdkNoticeTypesLoaded, jsonGetOnError);
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
			  
			  const editor = new Editor(dataFieldsJson, dataNoticeType, noticeFormElem);

			  editor.buildForm(); // Build the form. Initialize.
			  
			  funcCallbackWhenLoadedDefinition();
			  console.log("Loaded editor notice type: " + urlToGetNoticeTypeJsonData);
		  };
	    jsonGet(urlToGetNoticeTypeJsonData, 2000, jsonOkNoticeTypeFunc, jsonGetOnError);
	  };
	  
	  jsonGet(urlToGetFieldJsonData, 2000, jsonOkFieldsFunc, jsonGetOnError);
	}
	
	function buildFieldContainerElem(containerElem, content, editor) {
	
		const noticeId = editor.noticeId;
    const fieldMap = editor.fieldMap;

	  // Find the fields.json field associated with this notice type definition field.
    const fieldId = content.id;
    field = fieldMap[fieldId];
    if (!field) {
      throw new Error("Field is null for " + fieldId);
    }
	  
	  var formElem = null;
	  
		if (field.maxLength && field.maxLength > textAreaThreshold && field.type !== "url") {
		  formElem = document.createElement("textarea");
      containerElem.appendChild(formElem);
      
    } else if (field.type === "code" || field.type === "internal-code") {

      formElem = document.createElement("select");
      containerElem.appendChild(formElem);
      
      const fieldCodeListVal = field.codeList.value;
      
      var codelistId = fieldCodeListVal.id;
      const parentId = fieldCodeListVal.parentId;
      if (parentId) {
        // This codelist is tailored.
        // TODO tttt having to do this here is clearly not ideal.
        codelistId = parentId + "_" + codelistId;
      }
      
      const isHierarchical = fieldCodeListVal.type === "hierarchical";
      if (isHierarchical) {
        // TODO the data could be loaded in two steps.
      }
     
      // TODO tttt codelist language
      
      const select = formElem;
      const sdkVersion = getSdkVersion();
      
      // TODO tttt maybe that data could be cached and reused.
      var urlToCodelistJson = "sdk/" + sdkVersion + "/codelists/" + codelistId + "/lang/en";
      var afterCodelistLoad = function(data) {
        // Dynamically load the options.
 	      select.appendChild(createOption("", field.name)); // Empty option.
		 	  for (var code of data.codes) {
  	      select.appendChild(createOption(code.codeValue, code.en));
		 	  }
      };
      
      // Give this a larger timeout as some codelists could be quite big.
      // Ideally the JSON response should be cached for a while, you have to allow this server-side.
      jsonGet(urlToCodelistJson, 6000, afterCodelistLoad, jsonGetOnError);
      
    } else if (field.type === "indicator") {
    
      // TODO tttt indicator
			formElem = document.createElement("input");
		  input.setAttribute("type", "text");
			
      containerElem.appendChild(formElem);
			input = formElem;
			
		} else if (field.type === "id-ref") {
      // TODO tttt in theory it should be only "id-ref"
		 	formElem = document.createElement("select");
      containerElem.appendChild(formElem);
		  const select = formElem;
		  const valueList = content.valueList;
      if (valueList && valueList.length > 0) {
      
 	      select.appendChild(createOption("", getLabel("select") + " " + String(valueList))); // Empty option.

  			// Allows to find back select even if not knowing the tag, to find all in use tags later on.
  	    select.setAttribute(DATA_EDITOR_ID_REFERENCE, JSON.stringify(valueList));
			  for (var tag of valueList) {
			    // Allows to find back select by tag later on.
			    select.setAttribute(DATA_EDITOR_ID_REF_PREFIX + tag, "true");
			  }

 	      const foundElements = editor.findElementWithAttributeTags(valueList);
	    	for (var foundElement of foundElements) {
   	      select.appendChild(createOption(foundElement.value, foundElement.value));
	    	}
      } else {
        // TODO tttt Is this case even possible ???
      }

		} else {
			formElem = document.createElement("input");
      containerElem.appendChild(formElem);
			input = formElem;
			
			// Default to type text, it can be changed later on.
		  input.setAttribute("type", "text");
		  
      if (field.type === "email") {
        input.setAttribute("type", "email");
      }

      if (field.type === "url") {
        input.setAttribute("type", "url");
        input.classList.add("notice-content-field-url");
      }
      
      if (isFieldTypeNumeric(field.type)) {
        input.setAttribute("type", "number");
        if (field.type !== "integer") {
          input.setAttribute("steps", "any"); // Allow decimals like 3.1415
        }
        
        // Min of zero would make sense in a lot of situations but a temperature could be negative.
        // TODO tttt should we use range / intervals like MinZero [0, NULL] for that ????
        //input.setAttribute("min", "0");
        //input.setAttribute("max", ...);
      }
      
      // DATE.
      if (field.type === "date") {
        input.setAttribute("type", "date");
      }
      if (field.type === "zoned-date") {
        // TODO tttt what about zoned-date ????
        input.setAttribute("type", "datetime-local");
      }
      
      // TIME.
      if (field.type === "time") {
        input.setAttribute("type", "time");
      }
      if (field.type === "zoned-time") {
        // TODO tttt what about zoned-time ????
        input.setAttribute("type", "datetime-local");
      }
      
			// Pattern, regex for validation.
      if (field.pattern && field.pattern.severity === "ERROR") {
        input.setAttribute("pattern", field.pattern.value);
        
        // The browser will show: "Please match the requested format: _TITLE_HERE_"
        // TODO tttt Maybe the fields json pattern should come with english text explaining the pattern for error messages. 
        input.setAttribute("title", field.pattern.value);
      }
      
      if (content.instanceIdField) {
        input.setAttribute(DATA_EDITOR_INSTANCE_ID_FIELD, content.instanceIdField);
        const countStr = lpad("0", String(content.editorCount), 4); // 0001, 0002, ...
        const tag = content.instanceIdField;
        input.value = tag + "-" + countStr;
        
 	  	  // TODO tttt propose items from that other data XYZ-0001 and so on.
			  // TODO tttt remove options if they are removed ? This is problematic for a select.
		
		 		// NOTE: this will not work on the first pass during creation as elements are not yet in the DOM.
		 		// This will work during addition of extra elements 0002 and so on.
        var foundReferencingElements = editor.findElementWithAttributeIdRef(tag);
        for (var selectElem of foundReferencingElements) {
   	      selectElem.appendChild(createOption(input.value, input.value));
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
	    // TODO tttt is there a default technical value to set or is readOnly only for edition ????
		  formElem.setAttribute("readonly", "readonly");
		}

		const isRequired = isFieldValueMandatory(field, noticeId);
		if (isRequired) {
      formElem.setAttribute("required", "required");
      containerElem.classList.add("notice-content-required");
    }

    // TODO tttt repeatable, severity is a bit confusing ...
		isFieldRepeatable = field.repeatable.value;
		if (isFieldRepeatable) {
		  // Allow to add / remove fields.
		  containerElem.classList.add("notice-content-field-repeatable");
		  
		  if (content.repeatable && !isFieldRepeatable) {
		    console.error("fields.json repeatable mismatch on: " + field.id);
   		  containerElem.classList.add("notice-content-field-repeatable-mismatch");
		  }
		}
		
		formElem.setAttribute("id", editor.buildIdUnique(content));
		
		return {"containerElem" : containerElem, "formElem" : formElem, "field" : field};
	}
	
	function readContent(editor, parentElem, content, level, isForRepeat, elemToExpand) {
	  const noticeId = editor.noticeId;
	
		//console.debug("readContent content.id=" + content.id + ", level=" + level + ", isForRepeat=" + isForRepeat);	
	  const documentFragment = document.createDocumentFragment();
	  content.editorLevel = level; // Enrich model for later.
    
    // The editorCount will allow to make the id of this element unique.
		content.editorCount = content.editorCount >= 0 ? content.editorCount : 1;

    const isField = !content.content;
		const isSection = content.section;
		
		const isCollapsed = content.collapsed ? true : false;
		
		// TODO tttt compare content repeatable and node.repeatable (editor.nodeMap[...]), show conflicts
	  var isContentRepeatable = content.repeatable ? true : false; // Can be reassigned if field...
		 
	  // The container element may already exist in the case of uncollapsing (expand).
    var containerElem = elemToExpand ? elemToExpand : document.createElement("div");

    var field; // Can remain undefined or null.
	  var formElem;
    if (isField) {
    	const resultMap = buildFieldContainerElem(containerElem, content, editor);
    	formElem = resultMap["formElem"];
    	field = resultMap["field"];
    }
    
    // Prefix the ids to avoid conflict with various other identifiers existing in the same page.
    // For repeatable fields the content editorCount ensures the ids are unique.
    
    containerElem.setAttribute("id", editor.buildIdUnique(content));

	  // The id will vary if the element is repeatable but the editor type will not.
    containerElem.setAttribute("data-editor-type",  editor.buildIdPartial(content));

    containerElem.setAttribute("data-editor-count", content.editorCount);

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
 	    if (formElem) {
 	      formElem.classList.add("notice-content-field");
 	      
        // TODO tttt keep valueList or change name ????
		    if (content.valueList && content.valueList.length > 0) {
		      formElem.classList.add("notice-content-valueList");
		    }
 	      
   	    // TODO tttt should we even keep content.name in the content json ???
        formElem.setAttribute("placeholder", field.name); // Use field.name instead of content.name
       
        if (!formElem.getAttribute("title")) {
          formElem.setAttribute("title", field.name + " (" + field.id + ")"); // Use field.name instead of content.name
        }
 	    }
    } else {
      // This content is not a field.
      if (!elemToExpand) { // Always add title except if expanding.
        // We use the word "header" here to avoid confusion with the HTML title attribute.
	      headerContainer = document.createElement("div");
	      headerContainer.classList.add("notice-content-container")
	      const header = document.createElement("h4");
	      header.classList.add("notice-content-header");
	      const paddedEditorCount = lpad("0", String(content.editorCount), 4); // 0001, 0002, ...
	      const headerText = isContentRepeatable ? content.name + " (" + paddedEditorCount + ")" : content.name;
	      header.appendChild(document.createTextNode(headerText));
	      headerContainer.appendChild(header);
	      containerElem.appendChild(headerContainer);
	      containerElem.setAttribute("title", content.name); // Mouse over text on any section.
      }
    }
    
    // If collapsed the child content should not be loaded yet.
    if (isCollapsed && !elemToExpand) {
      // EXPAND LOGIC SETUP.
      // Setup of on click event so that content can be loaded into DOM on demand later on.
      const isUseCapture = true; // As the child elements do not exist yet.
	    const clickExpandFunc = createContentOnClickFunc(editor, containerElem, content, level, false, containerElem);
	    containerElem.addEventListener("click", clickExpandFunc, isUseCapture);
	    content.editorExpanded = false;
		} else {
		  // The content should have sub content and not have been expanded yet.
		  if (content.content && !content.editorExpanded) {
		    // Load sub items.
			  for (var contentSub of content.content) {
	  		  readContent(editor, containerElem, contentSub, level + 1, false, null); // Recursion on sub content.
			  }
		  }
		}
    content.editorCount++; // The content has been added.
    
		if (isContentRepeatable) {
		  // REPEAT LOGIC SETUP.
		  const elemButtonAddMore = document.createElement("button");
		  elemButtonAddMore.setAttribute("type", "button");
		  elemButtonAddMore.textContent = getLabel("add.more");
		  elemButtonAddMore.classList.add("notice-content-button");
		  elemButtonAddMore.classList.add("notice-content-button-add");
		  
		  // NOTE: here we add the content to the same parent as this is a sibling content and not a child content.
		  const clickRepeatFunc = createContentOnClickFunc(editor, parentElem, content, level, true, false);
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
  	  documentFragment.appendChild(containerElem); // Add to fragment (not in DOM yet).
      parentElem.appendChild(documentFragment);
    }
	}
	
	function createContentOnClickFunc(editor, containerElem, content, level, isForRepeat, elemToExpand) {
	  return function onClick(ev) {
      console.debug("clicked content=" + content.id);
	    ev.stopPropagation();
	    readContent(editor, containerElem, content, level + 1, isForRepeat, elemToExpand);
	    containerElem.classList.add("notice-content-section-opened");
    };
  }
  
  // fields.json related functions.
  
  function isFieldTypeNumeric(fieldType) {
    // TODO tttt having to do so many ORs is annoying.
    return field.type === "number" || field.type === "integer" || field.type === "amount";
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