import { DynamicProperty } from "./dynamic-property.js";
import { Constants } from "./global.js";

export class Validator {

    /** @type {Validator} */
    static #instance;

    /** @type {Validator} */
    static get instance() {
        return Validator.#instance ??= new Validator(Constants.HtmlElements.FORM_ELEMENT);
    }

    /** @type {HTMLFormElement} */
    form;

     /**
      * The validation roster contains all the validations that need to be performed before the form is submitted.
      * They are organised in a Map by HTMLElement (using the element's id as a key). 
      * For each HTMLElement we maintain a list of the dynamic-properties that need to be validated.
      * 
      * There are two reasons we need to keep a roster:
      * 1. So that we can trigger all validations without dispatching "change" events.
      * 2. So that we can remove event handlers after an HTMLElement has been removed from the DOM.
      * 
      *  @type {Map<String, DynamicProperty[]>} 
      */
    validationRoster = new Map();

    /**
     * 
     * @param {String} formElementId 
     */
    constructor(formElementId) {

        if (Validator.#instance) {
            throw new Error("You should not instantiate the Validator class yourself. It is mean to be used as singleton.")
        }

        this.form = document.getElementById(formElementId);
        if (this.form === null) {
            throw new Error(`Form element "${formElementId}" not found.`)
        }

        this.form.setAttribute("novalidate", "");
        this.form.addEventListener("submit", Validator.onSubmit);
    }

    checkValidity() {
        for(const elementId of this.validationRoster.keys()) {
            this.checkValidityOfElement(document.getElementById(elementId))
        }
    }

    checkValidityOfElement(htmlElement) {
        if (htmlElement.readOnly || htmlElement.disabled) {
            return;
        }

        for (const property of this.validationRoster.get(htmlElement.id)) {
            property.checkValidity();
            property.htmlElement.checkValidity();

            if (htmlElement.validity.valid) {
                htmlElement.closest(".container").querySelector(".footer").textContent = "";
            } else {
                // The element is not valid.
                
                // 1. Display the validation message to the user.
                htmlElement.closest(".container").querySelector(".footer").textContent = property.validationMessage ?? htmlElement.validationMessage;
                
                // 2. Don't try to find more errors for this element.  
                break;
            }
        }
    }

    /**
     * 
     * @param {Event} event 
     */
    static onValidate(event) {
        Validator.instance.checkValidityOfElement(event.currentTarget);
    }

    /**
     * 
     * @param {Event} event 
     */
    static onSubmit(event) {
        Validator.instance.checkValidity();
        const invalidFields = Validator.instance.form.querySelectorAll(":invalid");
        if (invalidFields.length > 0) {
            event.preventDefault();
            invalidFields[0].closest(".container").scrollIntoView(true);
        }
    }

    /**
     * 
     * @param {DynamicProperty} dynamicProperty 
     */
    static register(dynamicProperty) {

        const htmlElement = dynamicProperty.htmlElement;
        const eventName = dynamicProperty.validateOnEventName;

        if (Validator.instance.validationRoster.has(htmlElement.id)) {
            const registeredProperties = Validator.instance.validationRoster.get(htmlElement.id);
            const isSameEventUsedByOtherRegisteredProperties = registeredProperties.find(p => p.validateOnEventName == eventName) ? true : false;
            
            registeredProperties.push(dynamicProperty);
            
            if (!isSameEventUsedByOtherRegisteredProperties) {
                htmlElement.addEventListener(eventName, Validator.onValidate);
            }
        } else {
            Validator.instance.validationRoster.set(htmlElement.id, [ dynamicProperty ]);
            htmlElement.addEventListener(eventName, Validator.onValidate);
        }

        dynamicProperty.checkValidity();
    }

    /**
     * 
     * @param {HTMLElement} htmlElement 
     */
    static unregister(htmlElement) {
        if (Validator.instance.validationRoster.has(htmlElement.id)) {
            const usedEventNames = new Set(Validator.instance.validationRoster.get(htmlElement.id).map(p => p.validateOnEventName));
            for (const eventName of usedEventNames) {
                htmlElement.removeEventListener(eventName.validateOnEventName, Validator.onValidate); 
            }
            Validator.instance.validationRoster.delete(htmlElement.id);
        }
    }
}
