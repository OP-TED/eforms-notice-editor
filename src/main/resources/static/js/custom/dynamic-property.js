import { Context } from "./context.js";
import { I18N } from "./global.js";
import { InputFieldElement } from "./notice-form.js";


/**
 * @typedef {InputFieldElement}
 * @type {(HTMLInputElement|HTMLTextAreaElement|HTMLSelectElement|HTMLFieldSetElement)}
 */


/**
 * A constraint of a dynamic property as read from fields.json.
 * 
 * @typedef ConstraintJsonObject
 * @type {object}
 * @property {any} value
 * @property {string} severity
 * @property {string[]} noticeTypes
 * @property {string} condition
 * @property {string} message
 */

/**
 * 
 */
export class Constraint {

    /** @type {InputFieldElement} */
    htmlElement;

    /** @type {string[]} */
    noticeSubtypes;

    /** @type {string} */
    condition;

    /** type {any} */ 
    value;

    /** @type {string} */
    severity;

    /** @type {string} */
    message;

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {ConstraintJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        this.htmlElement = htmlElement;
        this.noticeSubtypes = new Set(jsonObject?.noticeTypes ?? []);
        this.condition = jsonObject.condition;
        this.value = jsonObject.value;
        this.severity = jsonObject.severity;
        this.message = jsonObject.message;
    }

    /**
     * Checks whether or not the constraint is applicable for the notice subtype indicated by the current Context.
     * 
     * @param {string} noticeSubtype 
     * @returns {boolean}
     */
    isApplicableForNoticeSubtype(noticeSubtype) {
        return this.noticeSubtypes.size < 1 || this.noticeSubtypes.has(noticeSubtype);
    }

    /**
     * Gets a boolean value indicating whether or not the constraint has a condition expression.
     * @type {boolean}
     */
    get hasCondition() {
        return this.condition ? true : false;
    }

    /**
     * Gets a boolean value indicating whether the condition of the constraint is met or not.
     * @type {boolean}
     */
    get isConditionMet() {
        if (!this.hasCondition) {
            return true;
        }

        // Currently we cannot evaluate conditions because they are expressed in EFX.
        // TODO: We need to add an EFX-to-Javascript translator so that we can translate and evaluate the conditions.
        // Until then we effectively "block" the conditional constraint by returning false.
        return false; // return eval(this.condition);
    }

    /**
     * Checks whether this constraint is applicable in the current context.
     * @type {boolean}  
     */
    get isApplicable() {
        return this.isApplicableForNoticeSubtype(Context.noticeSubtype) && this.isConditionMet;
    }

    /**
     * @type {(string|null)}
     */
    get validationMessage() {
        return this.message ? I18N.getLabel(this.message) : null;
    }

    /** 
     * Gets the value of the constraint.
     * Subclasses override the method to provide the correct value if it is specified indirectly.
     * For example the {@link AssertConstraint} class contains a script in its {@link Constraint.value} property
     * which it evaluates to obtain the actual value to return.
     */
    evaluate() {
        return this.value;
    }

    /**
     * Checks the validity of the HTMLElement associated with this constraint.
     * 
     * Does not check applicability of the constraint. 
     * Applicability should be checked separately by calling {@link isApplicable} before calling this method.
     * 
     * Subclasses override this method to
     */
    checkValidity() {
    }
}

export class MandatoryConstraint extends Constraint {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {ConstraintJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }

    checkValidity() {
        this.htmlElement.required = this.evaluate(); // Evaluates to true if the input-field is mandatory
    }
}

/**
 * 
 */
export class ForbiddenConstraint extends Constraint {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {ConstraintJsonObject} jsonObject 
     */
    constructor(htmlElement, constraint) {
        super(htmlElement, constraint);
    }

    checkValidity() {
        this.htmlElement.setCustomValidity(this.evaluate() && this.htmlElement.value !== "" ? this.validationMessage : "");
    }

    get validationMessage() {
        return super.validationMessage ?? "This field is forbidden";
    }
}

export class RepeatableConstraint extends Constraint {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {ConstraintJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }
}

export class InChangeNoticeConstraint extends Constraint {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {ConstraintJsonObject} jsonObject 
     */
    constructor(htmlElement, constraint) {
        super(htmlElement, constraint);
    }
}

export class CodelistConstraint extends Constraint {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {ConstraintJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }
}

export class PatternConstraint extends Constraint {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {ConstraintJsonObject} jsonObject 
     */
    constructor(htmlElement, constraint) {
        super(htmlElement, constraint);
    }

    checkValidity() {
        this.htmlElement.setAttribute("pattern", this.evaluate());
    }
}

export class AssertConstraint extends Constraint {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {ConstraintJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }

    checkValidity() {
        this.htmlElement.setCustomValidity( this.evaluate ? this.message : "");
    }

    evaluate() {
        return true;    // TODO: return eval(this.value);
    }
}


/**
 * A dynamic property as read from fields.json.
 * 
 * @typedef DynamicPropertyJsonObject
 * @type {object}
 * @property {any} value 
 * @property {string} severity
 * @property {ConstraintJsonObject[]} constraints
 */

/**
 * 
 */
export class DynamicProperty {

    /** @type {(HTMLInputElement|HTMLTextAreaElement|HTMLSelectElement|HTMLFieldSetElement)} */
    htmlElement;

    /** @type {Constraint} */
    defaultConstraint;

    /** @type {Constraint[]} */
    conditionalConstraints;

    /** @type {string} */
    validateOnEventName = "change";

    /**
     * 
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject - An object containing the dynamic property as read from fields.json. 
     */
    constructor(htmlElement, jsonObject) {
        this.htmlElement = htmlElement;
        this.defaultConstraint = jsonObject ? this.createConstraint(this.htmlElement, jsonObject) : undefined;
        this.conditionalConstraints = [];
        for (const constraint of jsonObject.constraints ?? []) {
            this.conditionalConstraints.push(this.createConstraint(this.htmlElement, constraint));
        }
    }

    createConstraint(htmlElement, jsonObject) {
        return new Constraint(htmlElement, jsonObject);
    }

    get applicableConstraint() {
        return this.conditionalConstraints.find(constraint => constraint.isApplicable) ?? this.defaultConstraint;
    }

    checkValidity() {
        this.applicableConstraint.checkValidity();
    }

    get value() {
        return this.applicableConstraint.evaluate();
    }

    get validationMessage() {
        return this.applicableConstraint.validationMessage;
    }
}

/**
 * 
 */
export class MandatoryProperty extends DynamicProperty {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     * @returns {MandatoryConstraint}
     */
    createConstraint(htmlElement, jsonObject) {
        return new MandatoryConstraint(htmlElement, jsonObject);
    }
}

/**
 * 
 */
export class ForbiddenProperty extends DynamicProperty {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     * @returns {ForbiddenConstraint}
     */
    createConstraint(htmlElement, jsonObject) {
        return new ForbiddenConstraint(htmlElement, jsonObject);
    }
}

/**
 * 
 */
export class RepeatableProperty extends DynamicProperty {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     * @returns {RepeatableConstraint}
     */
    createConstraint(htmlElement, jsonObject) {
        return new RepeatableConstraint(htmlElement, jsonObject);
    }
}

/**
 * 
 */
export class AssertProperty extends DynamicProperty {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     * @returns {AssertConstraint}
     */
    createConstraint(htmlElement, jsonObject) {
        return new AssertConstraint(htmlElement, jsonObject);
    }
}

/**
 * 
 */
export class PatternProperty extends DynamicProperty {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     * @returns {PatternConstraint}
     */
    createConstraint(htmlElement, jsonObject) {
        return new PatternConstraint(htmlElement, jsonObject);
    }
}

/**
 * 
 */
export class CodelistProperty extends DynamicProperty {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     * @returns {CodelistConstraint}
     */
    createConstraint(htmlElement, jsonObject) {
        return new CodelistConstraint(htmlElement, jsonObject);
    }
}

/**
 * 
 */
export class InChangeNoticeProperty extends DynamicProperty {

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     */
    constructor(htmlElement, jsonObject) {
        super(htmlElement, jsonObject);
    }

    /**
     * @param {InputFieldElement} htmlElement 
     * @param {DynamicPropertyJsonObject} jsonObject 
     * @returns {InChangeNoticeConstraint}
     */
    createConstraint(htmlElement, jsonObject) {
        return new InChangeNoticeConstraint(htmlElement, jsonObject);
    }
}