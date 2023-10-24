import { Context } from "./context.js";
import { I18N } from "./i18n.js";


/**
 * An abstraction for a property that can be validated.
 * Validatable properties are commonly dynamic properties.
 * But what if you want to validate a property that is not dynamic?
 * That's the purpose that this class serves.
 *  
 */
export class Validatable {

    /**
     * The HTMLElement's event to which an event listener should be attached
     * to perform validation. 
     * 
     * Note: It would be more natural to attach this property to the HTMLElement
     * rather than the ValidationProperty. However, in an effort to contain all 
     * validation related configuration in one specification, we decided to include this
     * property here instead. If this was implemented with TypeScript where interfaces could be
     * used safely bind different validation components or if this implementation was based
     * on Web Components rather that composed HTML elements, then we would prefer to attach this
     * information to an HTML element instead. 
     * 
     *  @type {string} 
     */
    validateOnEventName = "change";

    /**
     * This method is called to prepare the given HTMLElement for validation. 
     * The method is not expected to return the validation result.
     * The validation result will be determined by checking the {@link HTMLInputElement.validity} 
     * property after an intermediate call to the {@link HTMLInputElement.checkValidity} method.  
     * 
     * @param {HTMLInputElement|HTMLSelectElement|HTMLTextAreaElement} htmlElement. 
     */
    checkValidity(htmlElement) {
        throw new Error("You must override the 'Validatable.checkValidity' method in your subclass.")
    }

    /** @type {string} */
    get validationMessage() {
        throw new Error("You must override the 'Validatable.validationMessage' property getter in your subclass.")
    }
}


/**
 * Constraints are used by  dynamic properties ({@link DynamicProperty}) to determine 
 * their value under specific conditions.
 */
export class Constraint {

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
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     */
    constructor(jsonObject) {
        this.noticeSubtypes = new Set(jsonObject?.noticeTypes ?? []);
        this.condition = jsonObject?.condition;
        this.value = jsonObject?.value;
        this.severity = jsonObject?.severity;
        this.message = jsonObject?.message;
    }

    get hasValue() {
        return this.value !== undefined;
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
     * Subclasses must override this method
     */
    checkValidity(htmlElement) {
    }
}

export class MandatoryConstraint extends Constraint {

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }

    checkValidity(htmlElement) {
        htmlElement.required = this.evaluate(); // Evaluates to true if the input-field is mandatory
    }
}

/**
 * 
 */
export class ForbiddenConstraint extends Constraint {

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     */
    constructor(constraint) {
        super(constraint);
    }

    checkValidity(htmlElement) {
        htmlElement.setCustomValidity(this.evaluate() && htmlElement.value !== "" ? this.validationMessage : "");
    }

    get validationMessage() {
        return super.validationMessage ?? "This field is forbidden";
    }
}

export class RepeatableConstraint extends Constraint {

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }
}

export class InChangeNoticeConstraint extends Constraint {

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     */
    constructor(constraint) {
        super(constraint);
    }
}

export class CodelistConstraint extends Constraint {

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }
}

export class PatternConstraint extends Constraint {

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     */
    constructor(constraint) {
        super(constraint);
    }

    checkValidity(htmlElement) {
        htmlElement.setAttribute("pattern", this.evaluate());
    }
}

export class AssertConstraint extends Constraint {

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }

    checkValidity(htmlElement) {
        htmlElement.setCustomValidity( this.evaluate ? "" : this.message);
    }

    evaluate() {
        return true;    // TODO: return eval(this.value);
    }
}

/**
 * 
 */
export class DynamicProperty extends Validatable {

    /** @type {Constraint} */
    defaultConstraint;

    /** @type {Constraint[]} */
    conditionalConstraints;

    /**
     * 
     * @param {import("./data-types.js").SDK.DynamicProperty} jsonObject - An object containing the dynamic property as read from fields.json. 
     */
    constructor(jsonObject) {
        super();
        this.defaultConstraint = jsonObject ? this.createConstraint(jsonObject) : undefined;
        this.conditionalConstraints = [];
        for (const constraint of jsonObject?.constraints ?? []) {
            this.conditionalConstraints.push(this.createConstraint(constraint));
        }
    }

    /** 
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     */
    createConstraint(jsonObject) {
        return new Constraint(jsonObject);
    }

    /** @type {Constraint} */
    get applicableConstraint() {
        return this.conditionalConstraints.find(constraint => constraint.isApplicable) ?? this.defaultConstraint;
    }

    /** @param {HTMLElement} htmlElement */
    checkValidity(htmlElement) {
        this.applicableConstraint?.checkValidity(htmlElement);
    }

    get value() {
        return this.applicableConstraint?.evaluate();
    }

    /** @type {string} */
    get validationMessage() {
        return this.applicableConstraint?.validationMessage;
    }
}

/**
 * 
 */
export class MandatoryProperty extends DynamicProperty {

    /**
     * @param {import("./data-types.js").SDK.DynamicProperty} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     * @returns {MandatoryConstraint}
     */
    createConstraint(jsonObject) {
        return new MandatoryConstraint(jsonObject);
    }
}

/**
 * 
 */
export class ForbiddenProperty extends DynamicProperty {

    /**
     * @param {import("./data-types.js").SDK.DynamicProperty} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     * @returns {ForbiddenConstraint}
     */
    createConstraint(jsonObject) {
        return new ForbiddenConstraint(jsonObject);
    }
}

/**
 * 
 */
export class RepeatableProperty extends DynamicProperty {

    /**
     * @param {import("./data-types.js").SDK.DynamicProperty} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     * @returns {RepeatableConstraint}
     */
    createConstraint(jsonObject) {
        return new RepeatableConstraint(jsonObject);
    }
}

/**
 * 
 */
export class AssertProperty extends DynamicProperty {

    /**
     * @param {import("./data-types.js").SDK.DynamicProperty} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     * @returns {AssertConstraint}
     */
    createConstraint(jsonObject) {
        return new AssertConstraint(jsonObject);
    }
}

/**
 * 
 */
export class PatternProperty extends DynamicProperty {

    /**
     * @param {import("./data-types.js").SDK.DynamicProperty} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     * @returns {PatternConstraint}
     */
    createConstraint(jsonObject) {
        return new PatternConstraint(jsonObject);
    }
}

/**
 * 
 */
export class CodelistProperty extends DynamicProperty {

    /**
     * @param {import("./data-types.js").SDK.DynamicProperty} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     * @returns {CodelistConstraint}
     */
    createConstraint(jsonObject) {
        return new CodelistConstraint(jsonObject);
    }
}

/**
 * 
 */
export class InChangeNoticeProperty extends DynamicProperty {

    /**
     * @param {import("./data-types.js").SDK.DynamicProperty} jsonObject 
     */
    constructor(jsonObject) {
        super(jsonObject);
    }

    /**
     * @param {import("./data-types.js").SDK.DynamicPropertyConstraint} jsonObject 
     * @returns {InChangeNoticeConstraint}
     */
    createConstraint(jsonObject) {
        return new InChangeNoticeConstraint(jsonObject);
    }

    /** @type {import("./data-types.js").SDK.InChangeNoticeBehaviour} */
    get value() {
        return super.value;
    }

    /** @type {boolean} */
    get canAdd() {
        return this.value.canAdd;
    }

    /** @type {boolean} */
    get canRemove() {
        return this.value.canRemove;
    }

    /** @type {boolean} */
    get canModify() {
        return this.value.canModify;
    }
}

/**
 * 
 */
export class MaxLengthProperty extends Validatable {

    /**
     * @param {number} maxLength 
     */
    constructor(maxLength) {
        super();

        /** @type {number} */
        this.maxLength = isNaN(maxLength) ? undefined : maxLength;
    }

    /**
     * 
     * @param {HTMLInputElement} htmlElement 
     */
    checkValidity(htmlElement) {
        if (this.maxLength !== undefined) {
            htmlElement.maxLength = this.maxLength;
        } else {
            htmlElement.removeAttribute("maxLength");
        }
    }
}