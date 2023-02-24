import { Constants, Identifiers } from "./global.js";


/*******************************************************************************
 * @typedef {Object} ProxiedRepeater
 * @function renumber
 * @property {NodeList} repeatedElements
 */

/*******************************************************************************
 * @typedef {Object} ProxiedUIComponent
 * @property {string} contentId
 * @property {string} contentType
 * @property {boolean} isRepeatable
 * @property {ProxiedRepeater} repeater
 * @property {int} instanceCounter 
 * @property {ProxiedGroupUIComponent} repeatableParent
 * @property {string} qualifiedId
 * @property {string} instanceId
 * @property {function} renumber
 */

/*******************************************************************************
 * @typedef {Object} __ProxiedGroupUIComponent__
 * @property {string} nodeId
 * 
 * @typedef {ProxiedUIComponent & __ProxiedGroupUIComponent__} ProxiedGroupUIComponent
 */


/*******************************************************************************
 * @typedef {Object} __ProxiedFieldUIComponent__
 * @property {string} idScheme
 * @property {boolean} hasIdScheme
 * @property {ProxiedGroupUIComponent} identifiedGroup
 * @property {Function} generateId
 * 
 * @typedef {ProxiedUIComponent & __ProxiedFieldUIComponent__} ProxiedFieldUIComponent
 */


/*******************************************************************************
 * @implements {ProxyHandler<HTMLElement>}
 */
export class RepeaterProxy {

    static create(htmlElement) {
        return htmlElement ? new Proxy(htmlElement, new RepeaterProxy()) : null;
    }

    /***
     * @param {HTMLElement} target
     * @param {string} property
     */
    get(target, property) {
        switch (property) {
            case "target": return target;
            case "repeatedElements": return target.querySelectorAll(`[${Constants.Attributes.REPEATABLE_ATTRIBUTE} = "${target.id}"]`);
            case "renumber": return function () {
                var counter = 1;
                for (const node of target.querySelectorAll(`[${Constants.Attributes.REPEATABLE_ATTRIBUTE} = "${target.id}"]`)) {
                    node.setAttribute(Constants.Attributes.COUNTER_ATTRIBUTE, counter++);
                }
            };
            default: return Reflect.get(target, property);
        }
    }
}

/*******************************************************************************
 * @implements {ProxyHandler<HTMLElement>}
 */
export class UIComponentProxy {

    /**
     * 
     * @param {HTMLElement} htmlElement 
     * @returns {ProxiedUIComponent|null}
     */
    static create(htmlElement) {
        if (!htmlElement) {
            return null;
        }
    
        switch (htmlElement.getAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE)) {
            case "group": return new Proxy(htmlElement, new GroupUIComponentProxy());
            case "field": return new Proxy(htmlElement, new FieldUIComponentProxy());
            default: return new Proxy(htmlElement, new UIComponentProxy());
        }
    }

    /**
     * 
     * @param {HTMLElement} target 
     * @param {string} property 
     * @param {ProxiedUIComponent} receiver 
     * @returns {any}
     */
    get(target, property, receiver) {
        switch (property) {
            case "target": return target;
            case "contentId": return target.getAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE);
            case "contentType": return target.getAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE);
            case "instanceCounter": return target.getAttribute(Constants.Attributes.COUNTER_ATTRIBUTE);
            case "repeatableParent": return GroupUIComponentProxy.create(target.parentElement?.closest(`[${Constants.Attributes.REPEATABLE_ATTRIBUTE}]`));
            case "instanceId": return Identifiers.formatFormElementIdentifier(receiver.contentId, receiver.instanceCounter, receiver.repeatableParent?.instanceId); 
            case "repeater": return RepeaterProxy.create(document.getElementById(target.getAttribute(Constants.Attributes.REPEATABLE_ATTRIBUTE)));
            case "isRepeatable": return target.hasAttribute(Constants.Attributes.REPEATABLE_ATTRIBUTE) ? true : false;
            case "isField": return target.getAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE) === Constants.ContentType.FIELD;
            case "hasContentId": return target.hasAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE);
            case "hasContentType": return target.hasAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE);
            case "hasInstanceCounter": return target.hasAttribute(Constants.Attributes.COUNTER_ATTRIBUTE);
            case "hasParent":''
            case "renumber": return function() {

            };
            default: return Reflect.get(target, property);
        }
    }

    /**
     * 
     * @param {HTMLElement} target 
     * @param {string} property 
     * @param {any} value 
     * @param {ProxiedUIComponent} receiver
     * @returns {boolean}
     */
    set(target, property, value, receiver) {
        switch (property) {
            case "contentId": target.setAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE, value); return true;
            case "contentType": target.setAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE, value); return true;
            case "instanceCounter": target.setAttribute(Constants.Attributes.COUNTER_ATTRIBUTE, value); return true;
            default: return Reflect.set(target, property, value, receiver);
        }
    }
}


/*******************************************************************************
 * 
 */
export class GroupUIComponentProxy extends UIComponentProxy {

    /**
     * 
     * @param {HTMLElement} htmlElement 
     * @returns {ProxiedGroupUIComponent}
     */
    static create(htmlElement) {
        return htmlElement ? new Proxy(htmlElement, new GroupUIComponentProxy()) : null;
    }

    static get cssSelector() {
        return `[${Constants.Attributes.CONTENT_TYPE_ATTRIBUTE} = '${Constants.ContentType.GROUP}']`;
    }

    /**
     * 
     * @param {HTMLElement} target 
     * @param {string} property 
     * @returns {any} 
     */
    get(target, property, receiver) {
        switch (property) {
            case "nodeId": return target.getAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE);
            case "hasNodeId": return target.hasAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE);
            default: return super.get(target, property, receiver);
        }
    }
}

/*******************************************************************************
 * 
 */
export class FieldUIComponentProxy extends UIComponentProxy {

    /**
     * 
     * @param {HTMLElement} htmlElement 
     * @returns {ProxiedFieldUIComponent}
     */
    static create(htmlElement) {
        return htmlElement ? new Proxy(htmlElement, new FieldUIComponentProxy()) : null;
    }

    static get cssSelector() {
        return `[${Constants.Attributes.CONTENT_TYPE_ATTRIBUTE} = '${Constants.ContentType.FIELD}']`;
    }

    /**
     * 
     * @param {HTMLElement} target 
     * @param {string} property 
     * @param {ProxiedFieldUIComponent} receiver
     * @returns {any}
     */
    get(target, property, receiver) {
        switch (property) {
            case "idScheme": return target.getAttribute(Constants.Attributes.ID_SCHEME_ATTRIBUTE);
            case "idSchemes": return target.getAttribute(Constants.Attributes.ID_SCHEMES_ATTRIBUTE)?.split(',') ?? [];
            case "valueSource": return target.getAttribute(Constants.Attributes.VALUE_SOURCE_ATTRIBUTE);
            case "identifiedGroup": return GroupUIComponentProxy.create(target.closest(`[${Constants.Attributes.ID_FIELD_ATTRIBUTE} = "${receiver.contentId}"]`));
            case "hasIdScheme": return target.hasAttribute(Constants.Attributes.ID_SCHEME_ATTRIBUTE);
            case "hasIdSchemes": return target.hasAttribute(Constants.Attributes.ID_SCHEMES_ATTRIBUTE);
            case "hasValueSource": return target.hasAttribute(Constants.Attributes.VALUE_SOURCE_ATTRIBUTE);
            case "generateId": return function() { return Identifiers.formatSchemedIdentifier(receiver.idScheme, receiver.identifiedGroup?.instanceCounter); };
            default: return super.get(target, property, receiver);
        }
    }

    /**
     * 
     * @param {HTMLElement} target 
     * @param {string} property 
     * @param {any} value 
     * @returns {boolean}
     */
    set(target, property, value, receiver) {
        switch (property) {
            case "idScheme": target.setAttribute(Constants.Attributes.ID_SCHEME_ATTRIBUTE, value); return true;
            case "idSchemes": target.setAttribute(Constants.Attributes.ID_SCHEMES_ATTRIBUTE, value); return true;
            case "valueSource": target.setAttribute(Constants.Attributes.VALUE_SOURCE_ATTRIBUTE, value); return true;
            default: return super.set(target, property, value, receiver);
        }
    }
}
