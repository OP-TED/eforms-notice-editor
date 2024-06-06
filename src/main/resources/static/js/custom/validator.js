import { Validatable } from "./dynamic-property.js";
import { Constants } from "./global.js";

/**
 * @typedef {HTMLInputElement|HTMLTextAreaElement|HTMLSelectElement} ValidatableHTMLElement
 */

/**
 * The Validator is used for live-validation (a.k.a. client-side validation).
 * 
 * The general idea is that the validity of the value of an HTML element needs to 
 * be checked to verify that it conforms with some kind of restriction (a.k.a. constraint).
 * The constraints that restrict the validity of the HTMLElement's value are expressed through 
 * some property of some object somewhere (we do not care about which object the property belongs to).
 * The logic that needs to be applied in order to check the validity of the HTMLElement's value
 * is encoded inside the property, whereas the data that needs to be validated are held within 
 * the HTMLElement.  
 * 
 * So what we do, is we "register" with the Validator, an HTMLElement, plus a so-called {@link Validatable}.
 * This "registration" tells the Validator that we want it to monitor the HTMLElement for some event (typically "onchange"
 * or "oninput"), and apply the validation logic encapsulated within the Validatable, every time that event is
 * fired. 
 * 
 * The Validator relies on the HTML Form Validation mechanism. So what it does every time the monitored 
 * event is fired on the HTMLElement is, it calls the {@link Validatable.checkValidity} method,
 * passing along a reference to the HTMLElement being validated. The property should use HTML form validation
 * attributes to set the appropriate constraints on the HTMLElement. Then control is handed back to the Validator,
 * who will now call the {@link HTMLInputElement.checkValidity} method. This is a "native" JavaScript method
 * part of the HTML Form Validation API, that will check the validity of the HTMLElement and set its 
 * {@link HTMLInputElement.validity} property which eventually tells us whether the HTMLElement is in a
 * valid state or not. If the HTMLElement is not in a valid state then the Validator will display the 
 * appropriate validation message as determined earlier by the ValidityProperty.  
 */
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
      *  @type {Map<String, Validatable[]>} 
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
            property.checkValidity(htmlElement);
            htmlElement.checkValidity();

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
     * Registers a HTML element for validation of a given property.
     * An event listener will be added to the {@link htmlElement} for 
     * the event indicated by the property. When the event is triggered the
     * {@link property} will be asked to {@link Validatable.checkValidity()}
     *  
     * @param {Validatable} property 
     * @param {HTMLElement} htmlElement
     */
    static register(property, htmlElement) {

        const eventName = property.validateOnEventName;

        if (Validator.instance.validationRoster.has(htmlElement.id)) {
            const registeredProperties = Validator.instance.validationRoster.get(htmlElement.id);
            const isSameEventUsedByOtherRegisteredProperties = registeredProperties.find(p => p.validateOnEventName == eventName) ? true : false;
            
            registeredProperties.push(property);
            
            if (!isSameEventUsedByOtherRegisteredProperties) {
                htmlElement.addEventListener(eventName, Validator.onValidate);
            }
        } else {
            Validator.instance.validationRoster.set(htmlElement.id, [ property ]);
            htmlElement.addEventListener(eventName, Validator.onValidate);
        }

        property.checkValidity(htmlElement);
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
