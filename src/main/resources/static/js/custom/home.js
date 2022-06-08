(function() {
	console.log("Loading home script ...");

	const idPrefix = "editor-id-";

  const i18n = {};
	i18n["en"] = {"add.one": "Add one"};
	i18n["fr"] = {"add.one": "Ajouter"};
  function getLabel(lang, id) {
    return i18n[lang][id];
  }
	
	function funcCallbackWhenLoadedDefinition() {
	  document.getElementById("notice-info").style.display = "block";
	}
  jsonGet("/home/info", 2000, afterInitDataLoaded, jsonGetOnError);
  
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
    const elemSdkSelector = getElemSdkSelector();
    const sdkVersion = elemSdkSelector.value;
    if (!sdkVersion) {
      return;
    }
 	  // XHR to load existing notice types of selected SDK version.
    jsonGet("/home/sdk/" + sdkVersion + "/notice-types", 2000, afterSdkNoticeTypesLoaded, jsonGetOnError);
  }
  
  function afterSdkNoticeTypesLoaded(data) {
    console.log("Loaded available noticeTypes");
    const noticeTypes = data.noticeTypes;
	  const elemNoticeTypeSelector = getElemNoticeTypeSelector();
	  elemNoticeTypeSelector.innerHtml = "";

		// Dynamically load the options.   
    for (var noticeType of noticeTypes) {
      elemNoticeTypeSelector.innerHtml = "";
	    elemNoticeTypeSelector.appendChild(createOption(noticeType, noticeType));
    }	  

 	  document.getElementById("notice-type-selector").onchange = function() {
 	    const noticeId = this.value;
 	    const selectedSdkVersion = getElemSdkSelector().value;
 	    createNoticeForm(selectedSdkVersion, noticeId, funcCallbackWhenLoadedDefinition);
 	  };
  }
  
	function createNoticeForm(sdkVersion, noticeId) {
		const noticeFormElem = document.getElementById("notice-type"); // Root content container element.
	  noticeFormElem.innerHTML = ""; // Remove previous form.
	  
	  if (!sdkVersion || !noticeId) {
	    return;
	  }
	  
	  const urlToGetFieldJsonData = "/home/sdk/" + sdkVersion + "/fields";
	  const jsonOkFunc = function (dataFields) {
			if (!dataFields.sdkVersion) {
			  throw new Error("Invalid sdkVersion");
			}

			// FIELDS.
			const fields = dataFields.fields;
		  console.log("Loaded fields: " + fields.length);
		  const fieldMap = {};
		  for (var field of fields) {
		    fieldMap[field.id] = field;
		  }
			
			// NODES.
			const nodes = dataFields.xmlStructure;
		  console.log("Loaded nodes: " + nodes.length);
		  const nodeMap = {};
		  for (var node of nodes) {
		    nodeMap[node.id] = node;
		  }
	
		  // NOTE: the data could be loaded in parallel, but for this demo project we do it serial.
		  const urlToGetNoticeTypeJsonData = "/home/sdk/" + sdkVersion + "/notice-types/" + noticeId;
	    jsonGet(urlToGetNoticeTypeJsonData, 2000, function(dataNoticeType) {
	      const sdkVersion = dataNoticeType.sdkVersion;
			  if (!sdkVersion) {
			    throw new Error("Invalid sdkVersion: " + sdkVersion);
			  }
			  setText("notice-sdkVersion", sdkVersion);
			  setText("notice-noticeId", dataNoticeType.noticeId);
			  parseNoticeTypeRoot(noticeFormElem, dataNoticeType, fieldMap, nodeMap);

			  funcCallbackWhenLoadedDefinition();
			  console.log("Loaded editor notice type: " + urlToGetNoticeTypeJsonData);
		  });
	  };
	  
	  jsonGet(urlToGetFieldJsonData, 2000, jsonOkFunc, jsonGetOnError);
	}
	
	function parseNoticeTypeRoot(noticeFormElem, noticeType, fieldMap, nodeMap) {
	  const level = 1; // Top level, sub items will be on level 2, 3, 4 ...
  	readContent(noticeFormElem, noticeType.noticeId, noticeType.content, level, fieldMap, nodeMap, false, null);
	}
	
	function buildFieldContainerElem(containerElem, noticeId, content, fieldMap, nodeMap) {
    const fieldId = content.id;
    field = fieldMap[fieldId];
    if (!field) {
      throw new Error("Field is null for " + fieldId);
    }
	  
	  // TODO tttt maxLength 30 seems to be the threshold in the fields.json, we should provide this in the fields.json as an extra top level info?
	  const textAreaThreshold = 30;
		if (field.maxLength && field.maxLength > textAreaThreshold) {
		  formElem = document.createElement("textarea");
      containerElem.appendChild(formElem);

    } else if (field.type === "code") {
      // TODO
    	// select, when clicked load custom codelist json
      // home/sdk/0.7.0/codelists/accessibility/lang/en
      formElem = document.createElement("select");
      containerElem.appendChild(formElem);
      
		} else {
			formElem = document.createElement("input");
      containerElem.appendChild(formElem);
			input = formElem;
			
      if (field.type === "email") {
        input.setAttribute("type", "email");
      }
      
      // TODO tttt having to do so many ORs is annoying.
      if (field.type === "number" || field.type === "integer" || field.type === "amount") {
        input.setAttribute("type", "number");
      }
      if (field.type === "date") {
        input.setAttribute("type", "date");
      }

      if (field.pattern && field.pattern.severity === "ERROR") {
        input.setAttribute("pattern", field.pattern.value);
        
        // The browser will show: "Please match the requested format: _TITLE_HERE_"
        // TODO tttt Maybe the fields json pattern should come with english text explaining the pattern. 
        input.setAttribute("title", field.pattern.value);
      }
      
      // TODO tttt find out if multi-lang
		  input.setAttribute("lang", getSelectedLanguage());
    }
    
    if (field.maxLength) {
		  formElem.setAttribute("maxlength", field.maxLength); 
	  }

		const isRequired = isFieldValueMandatory(field, noticeId);
		if (isRequired) {
      formElem.setAttribute("required", "required");
      containerElem.classList.add("notice-content-required");
    }

    // TODO tttt repeatable this is a bit confusing ...
		isFieldRepeatable = field.repeatable.value;
		if (isFieldRepeatable) {
		  // Allow to add / remove fields.
		  containerElem.classList.add("notice-content-field-repeatable");
		}
		
		return {"containerElem" : containerElem, "formElem" : formElem, "field" : field};
	}
	
	function readContent(parentElem, noticeId, content, level, fieldMap, nodeMap, isForRepeat, elemToExpand) {
		console.debug("readContent content.id=" + content.id + ", level=" + level + ", isForRepeat=" + isForRepeat);	
	  const documentFragment = document.createDocumentFragment();
	  content.editorLevel = level; // Enrich model for later.
    
    // The editorCount will allow to make the id of this element unique.
		content.editorCount = content.editorCount >= 0 ? content.editorCount : 1;

    const isField = !content.content;
		const isSection = content.section;
		
		const isCollapsed = content.collapsed ? true : false;
	  var isContentRepeatable = content.repeatable ? true : false; // Can be reassigned if field...
		 
	  // The container element may already exist in the case of uncollapsing (expand).
    var containerElem = elemToExpand ? elemToExpand : document.createElement("div");

    var field; // Can remain undefined or null.
	  var formElem;
    if (isField) {
    	const resultMap = buildFieldContainerElem(containerElem, noticeId, content, fieldMap, nodeMap);
    	formElem = resultMap["formElem"];
    	field = resultMap["field"];
    }
    
    // Prefix the ids to avoid conflict with various other identifiers existing in the same page.
    // For repeatable fields the content editorCount ensures the ids are unique.
    containerElem.setAttribute("id", idPrefix + content.id + "-" + content.editorCount);

	  // The id will vary if the element is repeatable but the editor type will not.
    containerElem.setAttribute("data-editor-type", idPrefix + content.id);

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
    
    if (isCollapsed) {
 	    containerElem.classList.add("notice-content-collapsed"); 
    }
    
    if (isContentRepeatable) {
      containerElem.classList.add("notice-content-repeatable");
    }
    
    if (isField) {
 	    if (formElem) {
 	      formElem.classList.add("notice-content-field");

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
	      const headerText = isContentRepeatable ? content.name + " (" + content.editorCount + ")" : content.name;
	      header.appendChild(document.createTextNode(headerText));
	      headerContainer.appendChild(header);
	      containerElem.appendChild(headerContainer);
      }
    }
    
    // If collapsed the child content should not be loaded yet.
    if (isCollapsed && !elemToExpand) {
      // EXPAND LOGIC SETUP.
      // Setup of on click event so that content can be loaded into DOM on demand later on.
      const isUseCapture = true; // As the child elements do not exist yet.
	    const clickExpandFunc = createContentOnClickFunc(containerElem, noticeId, content, level, fieldMap, nodeMap, false, containerElem);
	    containerElem.addEventListener("click", clickExpandFunc, isUseCapture);
	    content.editorExpanded = false;
		} else {
		  // The content should have sub content and not have been expanded yet.
		  if (content.content && !content.editorExpanded) {
		    // Load sub items.
			  for (var contentSub of content.content) {
	  		  readContent(containerElem, noticeId, contentSub, level + 1, fieldMap, nodeMap, false, null); // Recursion on sub content.
			  }
		  }
		}
    content.editorCount++; // The content has been added.
    
		if (isContentRepeatable) {
		  // REPEAT LOGIC SETUP.
		  const elemButtonAddMore = document.createElement("button");
		  elemButtonAddMore.setAttribute("type", "button");
		  elemButtonAddMore.textContent = getLabel(getSelectedLanguage(), "add.one");
		  // NOTE: here we add the content to the same parent as this is a sibling content and not a child content.
		  const clickRepeatFunc = createContentOnClickFunc(parentElem, noticeId, content, level, fieldMap, nodeMap, true, false);
		  elemButtonAddMore.addEventListener("click", clickRepeatFunc, false);
		  containerElem.appendChild(elemButtonAddMore);
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
	
	function createContentOnClickFunc(containerElem, noticeId, content, level, fieldMap, nodeMap, isForRepeat, elemToExpand) {
	  return function onClick(ev) {
      console.debug("clicked content=" + content.id);
	    ev.stopPropagation();
	    readContent(containerElem, noticeId, content, level + 1, fieldMap, nodeMap, isForRepeat, elemToExpand);
	    containerElem.classList.add("notice-content-section-opened");
    };
  }
  
  // fields.json related functions.
  
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
		xhr.open("GET", urlGet);
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
	
})();