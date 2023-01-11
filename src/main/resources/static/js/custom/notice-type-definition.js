
/*******************************************************************************
 * Base class for all elements found in a Notice Type Definition file.
 * NTD elements are either display-groups or input-fields.
 * 
 * The general idea is that NTD data come from the backend through the [@link ServiceClient} class.
 * These data are then turned into NoticeTypeDefinitionElements through the {@link NoticeTypeDefinitionElement.create} 
 * factory method, which instantiates the appropriate NoticeTypeDefinitionElement subclass depending the contentType of the element.
 * 
 * The NoticeTypeDefinitionElement, creates and adds as child to itself, the actual UI that corresponds to the NTD element, 
 * by instantiating the appropriate {@link FormElement}. The NoticeTypeDefinitionElement extends DocumentFragment so that it can be 
 * added to the DOM directly.
 */
class NoticeTypeDefinitionElement extends DocumentFragment {

  /**
   * Factory method.
   */
  static create(content, level = 0) {
    switch (content?.contentType?.toLowerCase()) {
      case Constants.ContentType.GROUP: return DisplayGroup.create(content, level);
      case Constants.ContentType.FIELD: return InputField.create(content, level)
      case Constants.ContentType.METADATA_CONTAINER: return new RootLevelGroup(content, level); // Used for the root-level "metadata" and "contents" sections.
      case Constants.ContentType.DATA_CONTAINER: return new RootLevelGroup(content, level); // Used for the root-level "metadata" and "contents" sections.
      default: throw new Error("Unsupported contentType for NTD element: " + content?.contentType);
    }
  }

  constructor(content, level = 0, tag = "div") {
    super();

    this.formElement = FormElement.create(content, level);
    this.appendChild(this.formElement);

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
      // Load sub items.
      for (const contentSub of this.content.content) {
        // Recursion on sub content.
        const vme = NoticeTypeDefinitionElement.create(contentSub, this.level + 1);
        this.htmlElement.appendChild(vme);
      }
    }
  }

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
}

/*******************************************************************************
 * Handles the creation of the top level elements that are not part of the visual model normally:
 * - Notice-metadata section,
 * - Notice-data section (a.k.a. the top level "contents" object in notice-type-definition JSON file).
 */
class RootLevelGroup extends NoticeTypeDefinitionElement {
  constructor(content, level = 0) {
    super(content, level, "div");
    this.contentTypeAttribute = content.contentType;
  }
}

/*******************************************************************************
 * Represents display-group elements of the visual model. 
 */
class DisplayGroup extends NoticeTypeDefinitionElement {

  /**
   * Factory method
   */
  static create(content, level = 0) {
    switch (content?.displayType?.toLowerCase()) {
      case "section": return new FormSection(content, level);
      case "group": return new DisplayGroup(content, level);
      default: throw new Error("Unsupported display-type for visual model element: " + content?.displayType);
    }
  }

  constructor(content, level = 0, tag = "div") {
    super(content, level, tag);
    this.container.classList.add("display-group");

    this.contentTypeAttribute = Constants.ContentType.GROUP;

    if (this.hasNode) {
      this.nodeIdAttribute = this.nodeId;
    }

    if (this.content.collapsed) {
      this.container.classList.add("collapsed");
    }

    if (this.content._repeatable) {
      this.container.classList.add("repeatable");
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
}

/*******************************************************************************
 * Display-group elements of the visual model that are marked as "sections".
 */
class FormSection extends DisplayGroup {
  constructor(content, level = 0) {
    super(content, level, "div");

    this.container.classList.replace("display-group", "section");
  }
}

/*******************************************************************************
 * Represents input-field elements of the visual model.
 * 
 * Input-fields are linked with fields in the conceptual model and are used to
 * and hold the input-controls where the user enters notice data. 
 */
class InputField extends NoticeTypeDefinitionElement {

  /**
   * Factory method.
   */
  static create(content, level = 0) {
    switch (SdkServiceClient.fields[content?.id]?.type) {
      case 'amount': return new AmountInputField(content, level);
      case 'code': return new CodeInputField(content, level);
      case 'date': return new DateInputField(content, level);
      case 'email': return new EmailInputField(content, level);
      case 'id': return new IdInputField(content, level);
      case 'id-ref': return new IdRefInputField(content, level);
      case 'indicator': return new IndicatorInputField(content, level);
      case 'integer': return new IntegerInputField(content, level);
      case 'measure': return new MeasureInputField(content, level);
      case 'number': return new NumberInputField(content, level);
      case 'phone': return new PhoneInputField(content, level);
      case 'text': return new TextInputField(content, level);
      case 'text-multilingual': return new TextMultilingualInputField(content, level);
      case 'time': return new TimeInputField(content, level);
      case 'url': return new UrlInputField(content, level);
      default: throw new Error("Unknown field type: " + SdkServiceClient.fields[content?.id]?.type);
    }
  }

  constructor(content, level = 0) {
    super(content, level, "div");

    this.container.classList.add("input-field");

    this.contentTypeAttribute = Constants.ContentType.FIELD;

    this.fieldId = content.id;

    // Pattern, regex for validation.
    const field = this.field;
    if (field.pattern && field.pattern.severity === "ERROR") {
      this.htmlElement.setAttribute("pattern", field.pattern.value);

      // The browser will show: "Please match the requested format: _TITLE_HERE_"
      // TODO the fields json pattern should come with english text explaining the pattern for error messages. 
      this.htmlElement.setAttribute("title", field.pattern.value);
    }

    if (this.isMandatory) {
      this.htmlElement.setAttribute("required", "required");
      if (this.label) {
        this.label.classList.add("notice-content-required");
      }
    }

    // TODO repeatable, severity is a bit confusing ...
    if (this.isRepeatable) {
      // Allow to add / remove fields.
      this.container.classList.add("repeatable");

      if (!content._repeatable) {
        console.error("fields.json repeatable mismatch on: " + this.field.id);
        this.container.classList.add("repeatable-mismatch");
      }
    }

    if (this.valueSource) {
      this.valueSourceAttribute = this.valueSource;
    }

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

  get field() {
    return SdkServiceClient.fields[this.fieldId];
  }

  get isMandatory() {
    const mandatory = this.field?.mandatory;
    if (mandatory?.severity === "ERROR") {
      for (const constraint of mandatory.constraints) {
        if (constraint?.severity === "ERROR" && constraint?.noticeTypes) {
          if (constraint.noticeTypes.includes(NoticeSubtypeSelector.selectedNoticeSubtype)) {
            return constraint?.value ? true : false;
          }
        }
      }
      return mandatory?.value ? true : false;
    }
    return false;
  }

  get valueSource() {
    return this.content?.valueSource;
  }

  get isRepeatable() {
    return this.field?.repeatable?.value ? true : false;
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
class IdInputField extends InputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("id");

    if (this.#hasIdScheme) {
      this.htmlElement.setAttribute(Constants.DATA_EDITOR_ID_SCHEME, this.idScheme);

      this.htmlElement.value = this.#generateId();
    }

    this.htmlElement.classList.add("notice-content-id");
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

  #generateId() {
    return this.idScheme + "-" + this.content.editorCount.toString().padStart(4, "0");
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "id-ref".
 */
class IdRefInputField extends InputField {

  constructor(content, level = 0) {
    super(content, level);
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
    }
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
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "code".
 */
class CodeInputField extends InputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("code");

    if (this.getCodelist().type === "hierarchical") {
      // TODO the data could be loaded in two steps (big category, then sub items).
      // Currently the editor demo does not suppose this feature.
      console.log("Editor: hierarchical codelists are not handled yet, codelistId=" + this.getCodelistId());
    }

    this.populate(SdkVersionSelector.selectedSdkVersion, LanguageSelector.selectedLanguage);
  }

  async populate(sdkVersion, language) {

    const response = await fetch("sdk/" + sdkVersion + "/codelists/" + this.getCodelist().filename + "/lang/" + language);
    const json = await response.json();

    this.formElement.populate(json.codes.map((code) => [code.codeValue, code[language]]), true);

    // After the select options have been set, an option can be selected.
    // Special case for some of the metadata fields.
    if (this.getCodelistId() === "notice-subtype") {
      this.formElement.select(NoticeSubtypeSelector.selectedNoticeSubtype);
    } else if (this.getCodelistId() === "language_eu-official-language" && "BT-702(a)-notice" === content.id) {
      this.formElement.select(LanguageSelector.selectedLanguageAsIso6393);
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
class DateInputField extends InputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("date");
    this.htmlElement.setAttribute("type", "date"); // Nice to have but not required.

  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "time".
 */
class TimeInputField extends InputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("time");
    this.htmlElement.setAttribute("type", "time"); // Nice to have but not required.
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "measure".
 */
class MeasureInputField extends InputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("measure");
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "indicator".
 */
class IndicatorInputField extends InputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("indicator");

    const whenTrue = I18N.getLabel(`indicator|when-true|${this.content.id}`)
    const whnFalse =  I18N.getLabel(`indicator|when-false|${this.content.id}`)
    this.formElement.populate(new Map([["true", whenTrue], ["false", whnFalse]]));
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "number".
 */
class NumberInputField extends InputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("number");

    // Nice to have but not required.
    this.htmlElement.setAttribute("type", "number");
    this.htmlElement.setAttribute("step", "any"); // Allow decimals like 3.1415

    // Min of zero would make sense in a lot of situations but a temperature could be negative.
    // TODO should we use range / intervals like MinZero [0, NULL] for that?
    //this.htmlElement.setAttribute("min", "0");
    //this.htmlElement.setAttribute("max", ...);
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "integer".
 */
class IntegerInputField extends NumberInputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("integer");
    this.htmlElement.setAttribute("step", "1"); // only integers
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "amount".
 */
class AmountInputField extends NumberInputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("amount");
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "text".
 */
class TextInputField extends InputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("text");

    // Let the browser know in which language the text is, for example for spell checkers orscreen readers.
    this.htmlElement.setAttribute("lang", LanguageSelector.selectedLanguage);

    if (this.field.maxLength) {
      this.htmlElement.setAttribute("maxlength", this.field.maxLength);
    }
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "text-multilingual".
 */
class TextMultilingualInputField extends TextInputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("text-multilingual");
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "phone".
 */
class PhoneInputField extends TextInputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("phone");
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "email".
 */
class EmailInputField extends TextInputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("email");
  }
}

/*******************************************************************************
 * Visual model element (input-field) used for fields of type "url".
 */
class UrlInputField extends TextInputField {

  constructor(content, level = 0) {
    super(content, level);
    this.container.classList.add("url");
  }
}