import { Context } from "./context.js";
import { Constants, Identifiers } from "./global.js";
import { I18N } from "./i18n.js";
import { NTDComponent } from "./notice-type-definition.js";

/*******************************************************************************
 * Base class for all UI elements visible in the form. UI components provide 
 * the user interface through which the user interacts with each input-field and 
 * display-group while filling-out the form.
 * 
 * Every UI component is composed of a header, a body and a footer.
 * The header typically contains the label of the element and any necessary command buttons.
 * The body typically contains the actual content of the element. 
 * - For input-fields for example, the body is typically some HTMLInput element.
 * - For display-groups, the body is just a container that hosts the group's contents.
 * The footer is meant to contain tips or error messages related to the element.
 */
export class UIComponent extends DocumentFragment {

  /**
   * Factory method; 
   * Instantiates the appropriate sub-class based on the content-type of the passed content object.
   * 
   * @param {import("./data-types.js").ProxiedNTDContent} content
   * @param {Number} level 
   * @returns 
   */
  static create(content, level) {
    switch (content?.contentType) {
      case Constants.ContentType.GROUP: return GroupElement.create(content, level);
      case Constants.ContentType.FIELD: return InputFieldElement.create(content, level);
      case Constants.ContentType.METADATA_CONTAINER: return new RootElement(content, level);
      case Constants.ContentType.DATA_CONTAINER: return new RootElement(content, level);
      default: throw new Error("Unsupported content-type for visual model element: " + content?.contentType);
    }
  }

    /**
     * Gets the element that corresponds to the given content-id and instance-number.
     * 
     * @param {String} contentId 
     * @param {Number} instanceNumber 
     */
    static getElementByContentId(contentId, instanceNumber = 1) {
      return document.getElementById(Identifiers.formatFormElementIdentifier(contentId, instanceNumber));
    }

  /**
   * 
   * @param {import("./data-types.js").ProxiedNTDContent} content 
   * @param {Number} level 
   */
  constructor(content, level) {
    super();

    /** @type {import("./data-types.js").ProxiedNTDContent} */
    this.content = content;
    this.content.editorLevel = level;
    this.content.editorCount = this.content.countInstances() + 1;

    this.containerElement = this.createContainer();
    this.containerElement.classList.add("container");
    this.appendChild(this.containerElement);

    this.headerElement = this.createHeader();
    this.headerElement.classList.add("header");
    this.containerElement.appendChild(this.headerElement);

    this.bodyElement = this.createBody();
    this.bodyElement.classList.add("body");
    this.bodyElement.setAttribute("id", this.uniqueIdentifier);
    this.containerElement.appendChild(this.bodyElement);

    this.footerElement = this.createFooter();
    this.footerElement.classList.add("footer");
    this.containerElement.appendChild(this.footerElement);


    if (this.content.hidden || this.content.readOnly) {
      this.bodyElement.setAttribute("readonly", "readonly");
      this.bodyElement.disabled = true;
    }

    if (this.content.hidden && !Constants.DEBUG) {
      this.bodyElement.setAttribute("hidden", "hidden");
    }

    if (this.isRepeatable) {
      this.containerElement.classList.add("repeatable");
      this.bodyElement.setAttribute(Constants.Attributes.REPEATABLE_ATTRIBUTE, this.content.qualifiedId);
    }
  }
  
  get contentId() {
    return this.content?.id;
  }

  get instanceNumber() {
    return this.content?.editorCount;
  }

  get isRepeatable() {
    return this.content?.isRepeatable ?? false;
  }

  get uniqueIdentifier() {
    return this.content.instanceId;
  }

  /**
   * Meant To be overridden by subclasses to create a container for the entire UIComponent.
   * 
   * @returns {HTMLElement} A container for the entire UIComponent
   */
  createContainer() {
    return document.createElement("div");
  }

  /**
   * Meant to be overridden by subclasses to create the element's header.
   * 
   * @returns {HTMLElement} The header of the element.
   */
  createHeader() {
    return this.createNameLabel();
  }

  /**
   * Meant to be overridden by subclasses to create the body of the element.
   * 
   * @returns {HTMLElement} The body of the element. 
   */
  createBody() {
    return document.createElement("div");
  }

  /**
   * Meant to be overridden by subclasses to create the element's footer.
   * 
   * @returns {HTMLElement} The footer of the element.
   */
  createFooter() {
    return document.createElement("div");
  }

  /**
   * Meant to be overridden by subclasses to create the label of the element.
   * Here is where the translation is looked-up and used
   * @returns {HTMLElement}
   */
  createNameLabel() {
    const label = document.createElement(this.content.editorLevel > 6 ? "label" : `h${this.content.editorLevel}`);
    label.classList.add("label");
    label.setAttribute("for", this.uniqueIdentifier);
    label.textContent = I18N.getLabel(this.content._label) ?? this.content._label;
    return label;
  }
};

/*******************************************************************************
 * Used to create the top level groups: 
 * - Notice-Metadata 
 * - Notice-Data (a.k.a. the top level "contents" object of the notice-type-definition JSON file). 
 */
export class RootElement extends UIComponent {
  constructor(content, level) {
    super(content, level);
  }

  createHeader() {
    const div = document.createElement("div");
    div.appendChild(this.createNameLabel());
    return div;
  }

  get uniqueIdentifier() {
    return this.contentId;
  }
}

/*******************************************************************************
 * Used to create display-groups. Also as a base class for notice sections.
 */
export class GroupElement extends UIComponent {
  constructor(content, level) {
    super(content, level);
    this.containerElement.classList.add("display-group");
  }

  /**
   * Factory method. Creates the appropriate subclass based on the display-type of the passed display-group.
   *  
   * @param {Object} content A display-group object deserialized from the notice-type-definition JSON file. 
   * @param {Number} level 
   * @returns 
   */
  static create(content, level) {
    switch (content.displayType) {
      case "SECTION": return new SectionElement(content, level);
      case "GROUP": return new GroupElement(content, level);
    }
  }

  /**
   * Overridden to create the display-group's header.
   * For repeatable-groups the header, apart from their 
   * label, contains the buttons necessary to add and remove instances.
   */
  createHeader() {
    const header = document.createElement("div");
    header.classList.add("header");
    const label = this.createNameLabel();
    label.classList.add("label");
    header.appendChild(label);

    if (this.isRepeatable && !this.content.hidden && !this.content.readOnly) {
      header.appendChild(this.createRemoveInstanceButton());
    }

    return header;
  }

  /**
   * Overridden to create the display-group's body. 
   */
  createBody() {
    return document.createElement("div"); 
  }

  /**
   * Overridden to create the display-group's label
   */
  createNameLabel() {
    super.createNameLabel();
    const label = document.createElement(`h${Math.min(this.content.editorLevel, 6)}`);
    label.classList.add("label");
      
    label.setAttribute("for", this.uniqueIdentifier);
    label.classList.add("label");

    if (this.isRepeatable) {
      label.textContent += `#${this.content.editorCount}`;
    } else {
      // Set the translation.
      label.textContent = I18N.getLabel(this.content._label) ?? this.content._label;
    }

    return label;
  }

  /**
   * Creates a button used by repeatable-groups to add one more instance.
   */
  createAddInstanceButton() {

    const button = document.createElement("button");
    button.setAttribute("type", "button");
    button.classList.add("button");
    button.textContent = I18N.getLabel("editor.add.more");

    ((element, content) => {
      button.addEventListener("click", function (event) {
        event.stopPropagation();
        const newInstance = NTDComponent.create(content, content.parent);
        element.parentNode.insertBefore(newInstance, element.nextSibling);
      }, false);
    })(this.containerElement, this.content);

    return button;
  }

  /**
   * Creates a button used by repeatable-groups to remove an instance.
   */
  createRemoveInstanceButton() {
    const button = document.createElement("button");
    button.setAttribute("type", "button");
    button.classList.add("button");
    button.textContent = I18N.getLabel("editor.remove");

    ((element) => {
      button.addEventListener("click", function () {
        element.parentNode.removeChild(element);
      }, false);
    })(this.containerElement);

    return button;
  }
}

/*******************************************************************************
 * Extends {@link GroupElement} to create a notice-section.
 */
export class SectionElement extends GroupElement {
  constructor(content, level) {
    super(content, level);
    this.containerElement.classList.add("section");
  }
}

/*******************************************************************************
 * Base class of all input-field elements.
 */
export class InputFieldElement extends UIComponent {

  /**
   * Factory method. Instantiates the appropriate subclass based on the display-type of the passed object.
   */
  static create(content, level) {
    switch (content.displayType) {
      case "CHECKBOX": return new CheckBoxInputElement(content, level);
      case "COMBOBOX": return new ComboBoxInputElement(content, level); 
      case "RADIO": return new RadioInputElement(content, level); 
      case "TEXTAREA": return new TextAreaInputElement(content, level);
      case "TEXTBOX": return new TextBoxInputElement(content, level); 
    }
  }

  constructor(content, level) {
    super(content, level);
    this.containerElement.classList.add("input-field");
  }

  get fieldId() {
    return this.contentId;
  }

  get field() {
    return SdkServiceClient.fields[this.fieldId];
  }

  createHeader() {
    const header = document.createElement("div");
    header.classList.add("header");
    const label = this.createNameLabel();
    label.classList.add("label");
    header.appendChild(label);
    
    if (this.isRepeatable && !this.content.hidden) {
      header.appendChild(this.createRemoveButton());
    }

    return header;
  }

  createBody() {
    return document.createElement("div");
  }

  createNameLabel() {
    const label = document.createElement("label");
    label.classList.add("label");


    label.setAttribute("for", this.uniqueIdentifier);
    label.classList.add("label");

    if (this.isRepeatable) {
      label.textContent += `#${this.content.editorCount}`;
    } else {
      // Set the translation.
      label.textContent = I18N.getLabel(this.content._label) ?? this.content._label;
    }

    return label;
  }

  createRepeatButton() {
  
    // REPEAT LOGIC SETUP.
    const addButton = document.createElement("button");
    addButton.setAttribute("type", "button");
    addButton.textContent = I18N.getLabel("editor.add.more", Context.language);
    addButton.classList.add("button");
    // this.container.appendChild(addButton);

    // NOTE: here we add the content to the same parent as this is a sibling content and not a child content.
    const container = this.containerElement;
    const content = this.content;
    const level = this.content.editorLevel;

    const eventHandler = function repeat(event) {
      console.debug("clicked content=" + content.id);
      event.stopPropagation();
      const newInstance = NTDComponent.create(content, content.parent);
      container.parentNode.insertBefore(newInstance, container.nextSibling);   
      // container.style.backgroundColor = 'yellow';
      // newInstance.container.style.backgroundColor = 'red';  
    };
    addButton.addEventListener("click", eventHandler, false);

    return addButton;
  }

  createRemoveButton() {
    const button = document.createElement("button");
    button.setAttribute("type", "button");
    button.textContent = I18N.getLabel("editor.remove");
    button.classList.add("button");

    const container = this.containerElement;

    button.addEventListener("click", function () {
      container.parentNode.removeChild(container);
    }, false);

    return button;
  }

  set value(value) {
    this.bodyElement.value = value;    
  }
}

/*******************************************************************************
 * 
 */
export class TextBoxInputElement extends InputFieldElement {
  constructor(content, level) {
    super(content, level);
  }

  createBody() {
    const input = document.createElement("input");
    input.setAttribute("type", "text");
    
    const presetValue = this.content._presetValue;
    if (presetValue) {
      input.value = presetValue;
    }
    
    return input;
  }

  set value(value) {
    switch (this.bodyElement.getAttribute("type")) {
      case "date": this.bodyElement.value = value?.substr(0,10) ?? ""; break;
      case "time": this.bodyElement.value = value?.substr(0,8) ?? ""; break;
      default: this.bodyElement.value = value ?? ""; 
    }
  }
}

/*******************************************************************************
 * 
 */
export class CheckBoxInputElement extends InputFieldElement {
  constructor(content, level) {
    super(content, level);
  }

  createBody() {
    const input = document.createElement("input");
    input.setAttribute("type", "checkbox");
    
    const presetValue = this.content._presetValue;
    if (presetValue && presetValue === key) {
      input.setAttribute("checked", "checked");
    }
    
    return input;
  }

  set value(value) {
    this.bodyElement.checked = value ? true : false;    
  }
}

/*******************************************************************************
 * 
 */
export class RadioInputElement extends InputFieldElement {
  constructor(content, level) {
    super(content, level);
  }

  createBody() {
    return document.createElement("fieldset");
  }

  populate(map) {
    for (const item of map) {
      this.bodyElement.appendChild(this.createRadioButton(item[0], item[1]));
    }
  }

  createRadioButton(key, label, group) {

    const radioButtonElement = document.createElement("input");
    radioButtonElement.setAttribute("type", "radio");
    radioButtonElement.setAttribute("value", key);
    radioButtonElement.setAttribute("name", this.fieldId);
    radioButtonElement.setAttribute("id", `${this.uniqueIdentifier}-${key}`);
    
    const labelElement = document.createElement("label");
    labelElement.textContent = label;
    labelElement.appendChild(radioButtonElement);
    
    const presetValue = this.content._presetValue;
    if (presetValue && presetValue === key) {
      radioButtonElement.setAttribute("checked", "checked");
    }

    return labelElement;
  }

  set value(value) {
    this.bodyElement.querySelectorAll("[type='radio']").forEach( radio => radio.checked = radio.key === value ? true : false);    
  }

}

/*******************************************************************************
 * Extends {@link InputFieldElement} to create a combobox.
 */
export class ComboBoxInputElement extends InputFieldElement {
  constructor(content, level) {
    super(content, level);
    new TomSelect(this.bodyElement, {});
  }

  populate(map, addEmptyOption = false) {

    if (addEmptyOption) {
      this.bodyElement.appendChild(this.createOption("", "")); // Empty option, has no value.
    }

    for (const item of map) {
      this.bodyElement.appendChild(this.createOption(item[0], item[1]));
    }
    
    const presetValue = this.content._presetValue;
    if (presetValue) {
      this.select(presetValue);
    }

    this.bodyElement.tomselect?.sync();
  }

  createBody() {
    return document.createElement("select");
  }

  select(value) {
    this.bodyElement.value = value;
  }

  createNameLabel() {
    const label = super.createNameLabel();
      label.setAttribute("for", `${this.uniqueIdentifier}-ts-control`);
      return label;
  }

  createOption(key, label) {
    const option = document.createElement("option");
    option.setAttribute("value", key);
    option.textContent = label;
    return option;
  }

  set value(value) {
    this.bodyElement.value = value;    
    this.bodyElement.tomselect?.sync();
  }
}

/*******************************************************************************
 * Extends {@link InputFieldElement} to create a textarea for multi-line text input-fields.
 */
export class TextAreaInputElement extends InputFieldElement {
  constructor(content, level) {
    super(content, level);
  }

  createBody() {
    const bodyElement = document.createElement("textarea");
    bodyElement.setAttribute("rows", "2");
    
    const presetValue = this.content._presetValue;
    if (presetValue) {
      bodyElement.value = presetValue;
    }
    
    return bodyElement;
  }

  set value(value) {
    this.bodyElement.value = value ?? "";    
  }
}

/*******************************************************************************
 * The Repeater is used to wrap instances of repeatable elements.
 * It provides a button to add new instances of the repeatable element. 
 * Existing instances are removed through buttons provided by each instance 
 * of the repeatable element itself. 
 */
export class Repeater extends DocumentFragment {

  /**
   * Checks whether a repeater exists with the given qualified identifier.
   * 
   * @param {string} qualifiedId 
   * @returns {boolean}
   */
  static exists(qualifiedId) {
    return document.querySelectorAll(`.repeater#${qualifiedId}`).length > 0; 
  }

  /** @type {import("./data-types.js").SDK.NTDContent} */
  content;

  /** @param {import("./data-types.js").SDK.NTDContent} content */
  constructor(content) {
    super();
    this.content = content;

    this.containerElement.classList.add("container");
    this.appendChild(this.containerElement);

    this.headerElement.classList.add("header");
    this.containerElement.appendChild(this.headerElement);

    this.bodyElement.classList.add("body");
    this.containerElement.appendChild(this.bodyElement);

    this.footerElement.classList.add("footer");
    this.containerElement.appendChild(this.footerElement);
  }


  /** @type {HTMLElement} */
  #containerElement;

  /** @type {HTMLElement} */
  get containerElement() {
    return this.#containerElement ??= this.createContainer();
  }


  /** @type {HTMLElement} */
  #headerElement;

  /** @type {HTMLElement} */
  get headerElement() {
    return this.#headerElement ??= this.createHeader();
  }

  /** @type {HTMLElement} */
  #bodyElement;

  /** @type {HTMLElement} */
  get bodyElement() {
    return this.#bodyElement ??= this.createBody();
  }


  /** @type {HTMLElement} */
  #footerElement;

  /** @type {HTMLElement} */
  get footerElement() {
    return this.#footerElement ??= this.createFooter();
  }

  get uniqueIdentifier() {
    return this.content.qualifiedId;
  }

  createContainer() {
    var container = document.createElement("div");
    container.classList.add("repeater");
    container.id = this.uniqueIdentifier;
    return container;
  }

  /**
   * Creates the header of the repeater.
   */
  createHeader() {
    const header = document.createElement("div");
    header.classList.add("header");
    header.appendChild(this.createNameLabel());
    header.appendChild(this.createAddInstanceButton());
    return header;
  }

  createBody() {
    return document.createElement("div");
  }

  createFooter() {
    return document.createElement("div");
  }

  /**
   * Creates the button used by the repeater to add one more instance of its repeatable content.
   */
  createAddInstanceButton() {

    const button = document.createElement("button");
    button.setAttribute("type", "button");
    button.classList.add("button");
    button.textContent = I18N.getLabel("editor.add.more");

    ((element, content) => {
      button.addEventListener("click", function (event) {
        event.stopPropagation();
        const newInstance = NTDComponent.create(content, content.parent);
        element.appendChild(newInstance);
      }, false);
    })(this.bodyElement, this.content);

    return button;
  }

  /**
   * Creates the label of the repeater.
   * @returns {HTMLElement}
   */
  createNameLabel() {
    const label = document.createElement(this.content.editorLevel > 6 ? "label" : `h${this.content.editorLevel}`);
    label.classList.add("label");
    label.textContent = I18N.getLabel(this.content._label) ?? this.content._label;
    return label;
  }
}
