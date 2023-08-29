import { Context } from "./context.js";
import { Constants, DomUtil, I18N } from "./global.js";
import { NoticeTypeDefinitionElement } from "./notice-type-definition.js";

/*******************************************************************************
 * Base class for all elements visible in the form.
 * 
 * An element on the screen is composed of a header, a body and a footer.
 * The header typically contains the label of the element and any necessary command buttons.
 * The body typically contains the actual content of the element. 
 * - For input-fields for example, the body is typically some HTMLInput element.
 * - For display-groups, the body is just a container that hosts the groups contents.
 * The footer is meant to contain tips or error messages related to this element.
 */
export class FormElement extends DocumentFragment {

  /**
   * Factory method; Instantiates the appropriate sub-class based on the content-type of the passed content object.
   * 
   * @param {Object} content Deserialized object from notice-type-definition JSON.
   * @param {Number} level 
   * @returns the appropriate sub-class based on the content-type of the passed content
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
   * Computes and returns the identifier of an element from its content-id and instance-number.
   * 
   * @param {String} contentId 
   * @param {Number} instanceNumber 
   */
  static idFromFieldIdAndInstanceNumber(contentId, instanceNumber = -1) {
    const prefix = /^\d/.test(contentId) ? "_" : "";                                              // prefix with an underscore if the contentId starts with a number
    const identifier = contentId.replace(/\W/g,'_').toLowerCase();                                // replace all non-word characters with underscores
    const suffix = instanceNumber > -1 ? `_${instanceNumber?.toString().padStart(4, "0")}` : "";  // add an instance number if requested
    return prefix + identifier + suffix;
  }

  /**
   * Gets the element that corresponds to the given content-id and instance-number.
   * 
   * @param {String} contentId 
   * @param {Number} instanceNumber 
   */
  static getElementByContentId(contentId, instanceNumber = -1) {
    return document.getElementById(FormElement.idFromFieldIdAndInstanceNumber(contentId, instanceNumber));
  }
  
  constructor(content, level) {
    super();

    this.content = content;
    this.content.editorLevel = level;
    this.content.editorCount = (this.content.editorCount ?? 0) + 1;

    this.containerElement = this.createContainer();
    this.containerElement.classList.add("container");
    this.appendChild(this.containerElement);

    this.headerElement = this.createHeader();
    this.headerElement.classList.add("header");
    this.containerElement.appendChild(this.headerElement);

    this.bodyElement = this.createBody();
    this.bodyElement.classList.add("body");
    this.containerElement.appendChild(this.bodyElement);

    this.footerElement = this.createFooter();
    this.footerElement.classList.add("footer");
    this.containerElement.appendChild(this.footerElement);

    this.bodyElement.setAttribute("id", this.uniqueIdentifier);

    if (this.content.hidden || this.content.readOnly) {
      this.bodyElement.setAttribute("readonly", "readonly");
      //this.bodyElement.setAttribute("disabled", "disabled");
      this.bodyElement.disabled = true;
    }

    if (this.content.hidden && !Constants.DEBUG) {
      this.bodyElement.setAttribute("hidden", "hidden");
    }

    if (this.content._repeatable) {
      this.containerElement.classList.add("repeatable");
    }
  }
  
  get contentId() {
    return this.content?.id;
  }

  get displayType() {
    return this.content?.displayType;
  }

  get instanceNumber() {
    return this.content?.editorCount;
  }

  get isRepeatable() {
    return this.content?._repeatable ?? false;
  }

  get uniqueIdentifier() {
    return FormElement.idFromFieldIdAndInstanceNumber(this.content.id, this.isRepeatable ? this.instanceNumber : -1);
  }

  /**
   * Meant To be overridden by subclasses to create a container for the entire FormElement.
   * 
   * @returns A container for the entire FormElement
   */
  createContainer() {
    return document.createElement("div");
  }

  /**
   * Meant to be overridden by subclasses to create the element's header.
   * 
   * @returns The header of the element.
   */
  createHeader() {
    return this.createNameLabel();
  }

  /**
   * Meant to be overridden by subclasses to create the body of the element.
   * 
   * @returns The body of the element. 
   */
  createBody() {
    return document.createElement("div");
  }

  /**
   * Meant to be overridden by subclasses to create the element's footer.
   * 
   * @returns The footer of the element.
   */
  createFooter() {
    return document.createElement("div");
  }

  /**
   * Meant to be overridden by subclasses to create the label of the element.
   * Here is where the translation is looked-up and used
   * @returns 
   */
  createNameLabel() {
    const label = document.createElement(this.content.editorLevel + 1 > 6 ? "label" : `h${this.content.editorLevel + 1}`);
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
export class RootElement extends FormElement {
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
export class GroupElement extends FormElement {
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
    
    if (this.content._repeatable && !this.content.hidden) {
      header.appendChild(this.createAddInstanceButton());
    }

    if (this.content._repeatable && !this.content.hidden && this.content.editorCount > 1) {
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

    // Set the translation.
    label.textContent = I18N.getLabel(this.content._label) ?? this.content._label;
    if (this.content._repeatable) {
      label.textContent += ` (${String(this.content.editorCount).padStart(4, "0")})`;
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
        const newInstance = NoticeTypeDefinitionElement.create(content, content.editorLevel);
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
export class InputFieldElement extends FormElement {

  /**
   * Factory method. Instantiates the appropriate subclass based on the display-type of the passed object.
   */
  static create(content, level) {
    switch (content.displayType) {
      case Constants.DisplayType.CHECKBOX: return new CheckBoxInputElement(content, level);
      case Constants.DisplayType.COMBOBOX: return new ComboBoxInputElement(content, level); 
      case Constants.DisplayType.RADIO: return new RadioInputElement(content, level); 
      case Constants.DisplayType.TEXTAREA: return new TextAreaInputElement(content, level);
      case Constants.DisplayType.TEXTBOX: return new TextBoxInputElement(content, level); 
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
    
    if (this.content._repeatable && !this.content.hidden) {
      header.appendChild(this.createRepeatButton());
    }

    if (this.content._repeatable && !this.content.hidden && this.content.editorCount > 1) {
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

    // Set the translation.
    label.textContent = I18N.getLabel(this.content._label) ?? this.content._label;

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
      const newInstance = NoticeTypeDefinitionElement.create(content, level);
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
      this.bodyElement.appendChild(DomUtil.createOption("", "")); // Empty option, has no value.
    }

    for (const item of map) {
      this.bodyElement.appendChild(DomUtil.createOption(item[0], item[1]));
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
}