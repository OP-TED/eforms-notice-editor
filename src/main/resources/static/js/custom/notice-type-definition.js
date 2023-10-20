import { Context } from "./context.js";
import { Constants } from "./global.js";
import { I18N } from "./i18n.js";
import { UIComponent, Repeater } from "./notice-form.js";
import { SdkServiceClient } from "./service-clients.js";
import { Validator} from "./validator.js";
import { MandatoryProperty, ForbiddenProperty, RepeatableProperty, PatternProperty, AssertProperty, CodelistProperty, InChangeNoticeProperty, MaxLengthProperty } from "./dynamic-property.js";
import { NoticeReader } from "./notice-reader.js";
import { NTDContentProxy } from "./data-types.js";


/*******************************************************************************
 * Base class for all elements found in a Notice Type Definition file.
 * NTD elements are either display-groups or input-fields.
 * 
 * The general idea is that NTD data come from the backend through the [@link ServiceClient} class.
 * These data are then turned into NoticeTypeDefinitionElements through the {@link NTDComponent.create} 
 * The general idea is that NTD data comes from the backend through the [@link ServiceClient} class.
 * This data is then turned into NoticeTypeDefinitionElements through the {@link NoticeTypeDefinitionElement.create} 
 * factory method, which instantiates the appropriate NoticeTypeDefinitionElement subclass depending the contentType of the element.
 * 
 * The NoticeTypeDefinitionElement, creates and adds as child to itself, the actual UI that corresponds to the NTD element, 
 * by instantiating the appropriate {@link UIComponent}. The NoticeTypeDefinitionElement extends DocumentFragment so that it can be 
 * added to the DOM directly.
 */
export class NTDComponent extends DocumentFragment {

  /**
   * Factory method.
   * 
   * @param {import("./data-types.js").SDK.NTDContent} content 
   * @param {import("./data-types.js").ProxiedNTDContent} parent 
   * @returns 
   */
  static create(content, parent = null) {
    switch (content?.contentType?.toLowerCase()) {
      case Constants.ContentType.GROUP: return DisplayGroup.create(content, parent);
      case Constants.ContentType.FIELD: return InputField.create(content, parent);
      case Constants.ContentType.METADATA_CONTAINER: return new RootLevelGroup(content, parent); // Used for the root-level "metadata" and "contents" sections.
      case Constants.ContentType.DATA_CONTAINER: return new RootLevelGroup(content, parent); // Used for the root-level "metadata" and "contents" sections.
      default: throw new Error("Unsupported contentType for NTD element: " + content?.contentType);
    }
  }

  detectAndAddNtdAttributeFields(content) {
    const level = this.level;
    
    if (content.contentType === "field") {
      const sdkField = SdkServiceClient.fields[content.id];
      
      // NOTE: for type "id-ref" we want to automate the attribute value choice either on the front or back.
      // Example: TPO = touchpoint, ORG = organization, ... no need for the user to do it.
      if (sdkField.attributes && sdkField.type !== "id-ref") {
      
        // There are associated attributes.
        for (const attrId of sdkField.attributes) {
          const sdkAttr = SdkServiceClient.fields[attrId];
          if (sdkAttr == null) {
            throw new Error("Attribute field not found by id: " + attrId);
          }
          
          if (sdkAttr["xpathAbsolute"].endsWith("/@listName")) {
            console.debug("Attribute already present: " + sdkAttr.id);
          	continue;
          }
          
          if (sdkAttr.presetValue) {
            // Skip if there is a presetValue, this attribute field can be handled later in the back-end.
            // Assuming the associated field has a value, the attribute preset value could be set automatically.
            continue;
          }

					// If there is no presetValue, it means the user can make a choice in the UI.
					// Add a form field for the attribute value choice (usually a code selector).
          // We do not have NTD content for this attribute.
          // Create the NTD content dynamically to do as if it was there in the NTD:

          const contentAttr = {};
          contentAttr["id"] = sdkAttr.id;
          contentAttr["description"] = sdkAttr.name;
          contentAttr["contentType"] = Constants.ContentType.FIELD;
          contentAttr["displayType"] = sdkAttr.type === "code" ? Constants.DisplayType.COMBOBOX : Constants.DisplayType.TEXTBOX;
          contentAttr["readOnly"] = false;
          contentAttr["hidden"] = false;
          
          if (sdkAttr.repeatable) {
            contentAttr["_repeatable"] = sdkAttr.repeatable.value;
          }
          contentAttr["_label"] = "field|name|" + sdkAttr.id; // Label id.
          
          console.debug("Adding attribute field: " + sdkAttr.id + ", displayType=" + contentAttr["displayType"]);
          if (sdkAttr.codeList) {
            console.debug(sdkAttr.id + " " + sdkAttr.codeList.value.id);
          }
          
          const vme = NoticeTypeDefinitionElement.create(contentAttr, level);
          this.htmlElement.appendChild(vme);
        }
      }
    }
  }

  /**
   * 
   * @param {import("./data-types.js").SDK.NTDContent} content - Content object as read from <subtype>,json.
   * @param {import("./data-types.js").ProxiedNTDContent} parent - Parent content from <subtype>.json already wrapped in a proxy.
   */
  constructor(content, parent = null) {
    super();

    this.formElement = UIComponent.create(NTDContentProxy.create(content, parent), (parent?.editorLevel ?? 0) + 1);

    if (this.isRepeatable && !this.content.readOnly && !this.content.hidden && !Repeater.exists(this.content.qualifiedId)) {
      this.repeater = new Repeater(this.content);
      this.repeater.bodyElement.appendChild(this.formElement);
      this.appendChild(this.repeater);
    } else {
      this.appendChild(this.formElement);
    }

    this.contentIdAttribute = this.content.id;
    this.instanceCountAttribute = this.content.editorCount;

    if (this.content.hidden) {
      // Hide in production, but for development it is better to see what is going on.
      const hiddenClass = Constants.DEBUG ? "notice-content-hidden-devel" : "notice-content-hidden";
      this.container.classList.add(hiddenClass);
    }

    if (this.content.readOnly) {
      this.container.classList.add("read-only");
    }

    if (this.content.content) {
      // Load child  content
      for (const childContent of this.content.content) {
        this.htmlElement.appendChild(NTDComponent.create(childContent, this.content));
     
        // tttt
        // Are there attributes for which the user may have to do a choice (like a currency...)?
        // If this is the case and the attribute is not already present, add the attribute.
        // this.detectAndAddNtdAttributeFields(contentSub);
      }
    }
  }

  /**
   * @returns {import("./data-types.js").ProxiedNTDContent}
   */
  get content() {
    return this.formElement.content;
  }

  get container() {
    return this.formElement.containerElement;
  }

  get header() {
    return this.formElement.headerElement;
  }

  get htmlElement() {
    return this.formElement.bodyElement;
  }

  get contentTypeAttribute() {
    return this.htmlElement.getAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE);
  }

  set contentTypeAttribute(type) {
    this.htmlElement.setAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE, type);
  }

  get contentIdAttribute() {
    this.htmlElement.getAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE);
  }

  set contentIdAttribute(contentId) {
    this.htmlElement.setAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE, contentId);
  }

  get instanceCountAttribute() {
    this.htmlElement.getAttribute(Constants.Attributes.COUNTER_ATTRIBUTE);
  }

  set instanceCountAttribute(count) {
    this.htmlElement.setAttribute(Constants.Attributes.COUNTER_ATTRIBUTE, count);
  }

  get isInputField() {
    return this.content.contentType == "field";
  }

  get isDisplayGroup() {
    return this.content.contentType == "group";
  }

  get level() {
    return this.content.editorLevel;
  }

  /** @type {RepeatableProperty} */
    #repeatableProperty;

  /** @type {RepeatableProperty} */
  get repeatableProperty() {
      if (this.field?.repeatable && !this.#repeatableProperty) {
        this.#repeatableProperty = new RepeatableProperty(this.field.repeatable);
        Validator.register(this.#repeatableProperty, this.htmlElement);
      }
      return this.#repeatableProperty; 
    }

    /** @type {boolean} */
    get isRepeatable() {
      return this.repeatableProperty?.value ?? false;
    }
}

/*******************************************************************************
 * Handles the creation of the top level elements that are not part of the visual model normally:
 * - Notice-metadata section,
 * - Notice-data section (a.k.a. the top level "contents" object in notice-type-definition JSON file).
 */
export class RootLevelGroup extends NTDComponent {
  constructor(content, parent = null) {
    super(content, parent);
    this.contentTypeAttribute = content.contentType;
  }

  get isRepeatable() {
    return false;
  }
}

/*******************************************************************************
 * Represents display-group elements of the visual model. 
 */
export class DisplayGroup extends NTDComponent {

  /**
   * Factory method
   */
  static create(content, parent = null) {
    switch (content?.displayType?.toLowerCase()) {
      case "section": return new FormSection(content, parent);
      case "group": return new DisplayGroup(content, parent);
      default: throw new Error("Unsupported display-type for visual model element: " + content?.displayType);
    }
  }

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("display-group");

    this.contentTypeAttribute = Constants.ContentType.GROUP;

    if (this.hasNode) {
      this.nodeIdAttribute = this.nodeId;
    }

    if (this.content.collapsed) {
      this.container.classList.add("collapsed");
    }

    if (this.isRepeatable) {
      this.container.classList.add("repeatable");
    }

    if (this.content._identifierFieldId) {
      this.htmlElement.setAttribute(Constants.Attributes.ID_FIELD_ATTRIBUTE, this.content._identifierFieldId);
    }
  }

  get nodeIdAttribute() {
    return this.htmlElement.getAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE);
  }

  set nodeIdAttribute(nodeId) {
    this.htmlElement.setAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE, nodeId);
  }

  get hasNode() {
    return this.content?.nodeId ? true : false;
  }

  get nodeId() {
    return this.content?.nodeId;
  }

  get isRepeatable() {
    return this.content?.isRepeatable;
  }
}

/*******************************************************************************
 * Display-group elements of the visual model which are marked as "sections".
 */
export class FormSection extends DisplayGroup {
  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.replace("display-group", "section");
  }
}

/*******************************************************************************
 * Represents input-field elements of the visual model.
 * 
 * Input-fields are linked with fields in the conceptual model and 
 * hold the HTML input elements where the user enters notice data. 
 */
export class InputField extends NTDComponent {

  /**
   * Factory method.
   */
  static create(content, parent = null) {
    switch (SdkServiceClient.fields[content?.id]?.type) {
      case 'amount': return new AmountInputField(content, parent);
      case 'code': return new CodeInputField(content, parent);
      case 'date': return new DateInputField(content, parent);
      case 'email': return new EmailInputField(content, parent);
      case 'id': return new IdInputField(content, parent);
      case 'id-ref': return new IdRefInputField(content, parent);
      case 'indicator': return new IndicatorInputField(content, parent);
      case 'integer': return new IntegerInputField(content, parent);
      case 'measure': return new MeasureInputField(content, parent);
      case 'number': return new NumberInputField(content, parent);
      case 'phone': return new PhoneInputField(content, parent);
      case 'text': return new TextInputField(content, parent);
      case 'text-multilingual': return new TextMultilingualInputField(content, parent);
      case 'time': return new TimeInputField(content, parent);
      case 'url': return new UrlInputField(content, parent);
      default: throw new Error("Unknown field type: " + SdkServiceClient.fields[content?.id]?.type);
    }
  }

  constructor(content, parent = null) {
    super(content, parent);

    //#region Setup CSS
    this.container.classList.add("input-field");

    if (this.isRepeatable) {
      this.container.classList.add("repeatable");
    }
    //#endregion Setup CSS

    //#region Setup HTML Element Attributes
    this.contentTypeAttribute = Constants.ContentType.FIELD;

    if (this.valueSource) {
      this.valueSourceAttribute = this.valueSource;
    }
    //#endregion Setup HTML Element Attributes
    
    //#region Setup Live-Validation
    if (this.field?.mandatory) {
      Validator.register(this.mandatoryProperty, this.htmlElement);
    }
    if (this.field?.repeatable) {
      Validator.register(this.repeatableProperty, this.htmlElement);
    }
    if (this.field?.pattern) {
      Validator.register(this.patternProperty, this.htmlElement);
    }
    if (this.field?.assert) {
      Validator.register(this.assertProperty, this.htmlElement);
    }
    if (this.field?.forbidden) {
      Validator.register(this.forbiddenProperty, this.htmlElement);
    }
    if (this.field?.inChangeNotice) {
      Validator.register(this.inChangeNoticeProperty, this.htmlElement);
    }
    if (this.field?.codelist) {
      Validator.register(this.codelistProperty, this.htmlElement);
    }
    //#endregion Setup Live-Validation



    if (this.hasPrivacy) {
      console.debug(this.field.id + ", field privacy code=" + this.field.privacy.code);
      this.container.classList.add("notice-content-field-privacy");
      // Example:
      // "code" : "cro-bor-law",
      // "unpublishedFieldId" : "BT-195(BT-09)-Procedure",
      // "reasonCodeFieldId" : "BT-197(BT-09)-Procedure",
      // "reasonDescriptionFieldId" : "BT-196(BT-09)-Procedure",
      // "publicationDateFieldId" : "BT-198(BT-09)-Procedure"
    }
  }

  loadValue() {
    if (NoticeReader.hasDocument) {
      this.formElement.value = NoticeReader.getFieldValues(this.fieldId)?.next().value;
    }
  }

  get fieldId() {
    return this.content.id;
  }

  get field() {
    return SdkServiceClient.fields[this.fieldId];
  }

  //#region Dynamic Properties

  /** @type {MandatoryProperty} */
  #mandatoryProperty;

  /** @type {MandatoryProperty} */
  get mandatoryProperty() {
    if (this.field?.mandatory && !this.#mandatoryProperty) {
      this.#mandatoryProperty = new MandatoryProperty(this.field.mandatory);
    }
    return this.#mandatoryProperty;
  }

  /** @type {boolean} */
  get isMandatory() {
    return this.mandatoryProperty?.value ?? false;
  }

  /** @type {ForbiddenProperty} */
  #forbiddenProperty;

  /** @type {ForbiddenProperty} */
  get forbiddenProperty() {
    if (this.field?.forbidden && !this.#forbiddenProperty) {
      this.#forbiddenProperty = new ForbiddenProperty(this.field.forbidden);
    }
    return this.#forbiddenProperty;
  }

  /** @type {boolean} */
  get isForbidden() {
    return this.forbiddenProperty?.value ?? false;
  }

  /** @type {AssertProperty} */
  #assertProperty;

  /** @type {AssertProperty} */
  get assertProperty() {
    if (this.field?.assert && !this.#assertProperty) {
      this.#assertProperty = new AssertProperty(this.field.assert);
    }
    return this.#assertProperty;
  }

  /** {boolean} */
  get assert() {
    return this.assertProperty?.value ?? true;
  }

  /** @type {PatternProperty} */
  #patternProperty;

  /** @type {PatternProperty} */
  get patternProperty() {
    if (this.field?.pattern && !this.#patternProperty) {
      this.#patternProperty = new PatternProperty(this.field.pattern);
    }
    return this.#patternProperty;
  }

  /**
   * Gets the pattern associated with the input-field as a {@link RegExp}.
   * If no pattern is provided for the field it returns a RegExp that matches any string. 
   * This property is not really used. Instead, the {@link patternProperty} is registered 
   * with {@link Validator} which takes care of validation directly.
   * 
   * @type {RegExp} 
   * */
  get pattern() {
    return new RegExp(this.patternProperty?.value ?? ".*");
  }

  /** 
   * This property is not used for validation. It is provided only as a concept here.
   * The {@link Validator} takes care of validation instead.
   * 
   * @type {boolean} 
   * */
  get matchesPattern() {
    return this.pattern?.test(this.htmlElement?.value ?? "") ?? true;
  }

  /** @type {CodelistProperty} */
  #codelistProperty;

  /** @type {CodelistProperty} */
  get codelistProperty() {
    if (this.field?.codelist && !this.#codelistProperty) {
      this.#codelistProperty = new CodelistProperty(this.field.codelist);
    }
    return this.#codelistProperty;
  }

  get codelist() {
    return this.codelistProperty?.value ?? undefined;
  }

  /** @type {InChangeNoticeProperty} */
  #inChangeNoticeProperty;

  /** @type {InChangeNoticeProperty} */
  get inChangeNoticeProperty() {
    if (this.field?.inChangeNotice && !this.#inChangeNoticeProperty) {
      this.#inChangeNoticeProperty = new InChangeNoticeProperty(this.field.inChangeNotice);
      Validator.register(this.#inChangeNoticeProperty, this.htmlElement);
    }
    return this.#inChangeNoticeProperty;
  }

  get inChangeNotice() {
    return this.inChangeNoticeProperty?.value ?? undefined;
  }

  //#endregion Dynamic Properties

  get valueSource() {
    return this.content?.valueSource;
  }

  get hasPrivacy() {
    return this.field?.privacy ? true : false;
  }

  get valueSourceAttribute() {
    return this.htmlElement?.getAttribute(Constants.Attributes.VALUE_SOURCE_ATTRIBUTE);
  }

  set valueSourceAttribute(valueSource) {
    this.htmlElement.setAttribute(Constants.Attributes.VALUE_SOURCE_ATTRIBUTE, valueSource);
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "id".
 */
export class IdInputField extends InputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("id");

    if (this.#hasIdScheme) {
      this.htmlElement.setAttribute(Constants.DATA_EDITOR_ID_SCHEME, this.idScheme);

      this.htmlElement.value = this.content.generateId();
    }

    this.htmlElement.classList.add("notice-content-id");
    this.loadValue();
  }

  get idScheme() {
    return this.content._idScheme;
  }

  get #hasIdScheme() {
    return this.content._idScheme ? true : false;
  }

  get idSchemeAttribute() {
    return this.htmlElement.getAttribute(Constants.Attributes.ID_SCHEME_ATTRIBUTE);
  }

  set idSchemeAttribute(idScheme) {
    this.htmlElement.setAttribute(Constants.Attributes.ID_SCHEME_ATTRIBUTE, idScheme);
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "id-ref".
 */
export class IdRefInputField extends InputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("id-ref");

    const idSchemes = content._idSchemes;
    if (idSchemes && idSchemes.length > 0) {

      // Allows to find back select even if not knowing the idScheme, to find all in use idSchemes later on.
      this.htmlElement.setAttribute(Constants.DATA_EDITOR_ID_REFERENCE, JSON.stringify(idSchemes));
      for (const idScheme of idSchemes) {
        // Allows to find back select by idScheme later on.
        this.htmlElement.setAttribute(Constants.DATA_EDITOR_ID_REF_PREFIX + idScheme, "true");
      }
    }

    this.htmlElement.classList.add("id-ref-input-field");

    if (this.idSchemes) {
      this.idSchemesAttribute = this.idSchemes.join(',');
      this.populate();
    }
    this.loadValue();
  }

  get idSchemes() {
    return this.content?._idSchemes ?? [];
  }

  get idSchemesAttribute() {
    return this.htmlElement.getAttribute(Constants.Attributes.ID_SCHEMES_ATTRIBUTE);
  }

  set idSchemesAttribute(idSchemes) {
    this.htmlElement.setAttribute(Constants.Attributes.ID_SCHEMES_ATTRIBUTE, idSchemes);
  }

  /**
   * Populates the control with any existing identifies that comply with the idSchemes it requires.
   */
  populate() {
    if (this.idSchemes?.length < 1) {
      return;
    }

    var query = `[${Constants.Attributes.ID_SCHEME_ATTRIBUTE} = "${this.idSchemes[0]}"]`;
    for (var i = 1; i < this.idSchemes.length; i++) {
      query += `, [${Constants.Attributes.ID_SCHEME_ATTRIBUTE} = "${this.idSchemes[i]}"]`;
    }

    const elements = document.querySelectorAll(query);
    this.formElement.populate(Array.from(elements).map(element => [element.value, element.value]));
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "code".
 */
export class CodeInputField extends InputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("code");

    if (this.getCodelist().type === "hierarchical") {
      // TODO the data could be loaded in two steps (big category, then sub items).
      // Currently the editor demo does not suppose this feature.
      console.log("Editor: hierarchical codelists are not handled yet, codelistId=" + this.getCodelistId());
    }

    this.populate(Context.sdkVersion, Context.language);
  }

  async populate(sdkVersion, language) {

    if (typeof this.formElement.populate === 'function') {
      const response = await fetch("sdk/" + sdkVersion + "/codelists/" + this.getCodelist().filename + "/lang/" + language);
      const json = await response.json();

      this.formElement.populate(json.codes.map((code) => [code.codeValue, code[language]]), true);
    } else {
      //throw new Error(msg);
	  var msg = "this.formElement.populate not found for field id=" + this.formElement.fieldId;
      msg += ", expecting displayType " + Constants.DisplayType.COMBOBOX + " but found " + this.formElement.displayType;
      console.warn(msg);
    }

    this.loadValue();

    // After the select options have been set, an option can be selected.
    // Special case for some of the metadata fields.
    if (this.getCodelistId() === "notice-subtype") {
      this.formElement.select(Context.noticeSubtype);
    } else if (this.getCodelistId() === "language_eu-official-language" && "BT-702(a)-notice" === content.id) {
      this.formElement.select(I18N.Iso6391ToIso6393Map[Context.language]);
    }
  }

  getCodelistId() {
    return this.field.codeList.value.id;
  }

  getCodelist() {
    return SdkServiceClient.codelists[this.getCodelistId()];
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "date".
 */
export class DateInputField extends InputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("date");
    this.htmlElement.setAttribute("type", "date"); // Nice to have but not required.

    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "time".
 */
export class TimeInputField extends InputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("time");
    this.htmlElement.setAttribute("type", "time"); // Nice to have but not required.
    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "measure".
 */
export class MeasureInputField extends InputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("measure");
    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "indicator".
 */
export class IndicatorInputField extends InputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("indicator");

    const whenTrue = I18N.getLabel(`indicator|when-true|${this.content.id}`);
    const whenFalse = I18N.getLabel(`indicator|when-false|${this.content.id}`);
    this.formElement.populate(new Map([["true", whenTrue], ["false", whenFalse]]));
    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "number".
 */
export class NumberInputField extends InputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("number");

    // Nice to have but not required.
    this.htmlElement.setAttribute("type", "number");
    this.htmlElement.setAttribute("step", "any"); // Allow decimals like 3.1415

    // Min of zero would make sense in a lot of situations but a temperature could be negative.
    // TODO should we use range / intervals like MinZero [0, NULL] for that?
    //this.htmlElement.setAttribute("min", "0");
    //this.htmlElement.setAttribute("max", ...);
    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "integer".
 */
export class IntegerInputField extends NumberInputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("integer");
    this.htmlElement.setAttribute("step", "1"); // only integers
    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "amount".
 */
export class AmountInputField extends NumberInputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("amount");
    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "text".
 */
export class TextInputField extends InputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("text");

    // Let the browser know in which language the text is, for example for spell checkers orscreen readers.
    this.htmlElement.setAttribute("lang", Context.language);

    if (this.field.maxLength) {
      Validator.register(this.maxLengthProperty, this.htmlElement);
    }
    this.loadValue();
  }

  /** @type {number} */
  #maxLengthProperty;

  /** @type {number} */
  get maxLengthProperty() {
    return this.#maxLengthProperty ??= new MaxLengthProperty(this.field.maxLength);
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "text-multilingual".
 */
export class TextMultilingualInputField extends TextInputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("text-multilingual");
    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "phone".
 */
export class PhoneInputField extends TextInputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("phone");
    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "email".
 */
export class EmailInputField extends TextInputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("email");
    this.loadValue();
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "url".
 */
export class UrlInputField extends TextInputField {

  constructor(content, parent = null) {
    super(content, parent);
    this.container.classList.add("url");
    this.loadValue();
  }
}