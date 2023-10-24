/**
 * This file declares data types used by the eForms SDK. 
 * 
 * For the moment we are only providing JSDoc to document the datatypes 
 * and enable intellisense support for them in your IDE. 
 */

import { RepeatableProperty } from "./dynamic-property.js";
import { Constants, Identifiers } from "./global.js";
import { SdkServiceClient } from "./service-clients.js";

/**
 * Dynamic property constraints as defined/used in SDK/fields/fields.json
 * 
 * @template T
 * @typedef {Object} SDK.DynamicPropertyConstraint
 * @property {T} value
 * @property {string} severity
 * @property {string} [message]
 * @property {string[]} [noticeTypes]
 * @property {string} [condition]
 */

/**
 * Dynamic properties as defined/used in SDK/fields/fields.json
 * 
 * @template T
 * @typedef {Object} SDK.DynamicProperty
 * @property {T} value
 * @property {string} severity
 * @property {string} message?
 * @property {SDK.DynamicPropertyConstraint<T>[]} [constraints]
 */

/**
 * @typedef {string} SDK.EFXExpression
 */

/**
 * The object returned by the inChangeNotice dynamic property as defined/used in SDK/fields/fields.json
 * 
 * @typedef {object} SDK.InChangeNoticeBehaviour
 * @property {boolean} canAdd
 * @property {boolean} canRemove
 * @property {boolean} canModify
 */

/**
 * A codelist as defined/used in the {@link SDK.Field.codeList} dynamic property of SDK/fields/fields.json.
 * 
 * @typedef {Object} SDK.Codelist
 * @property {string} id
 * @property {string} type
 * @property {string} [parentId]
 */

/** 
 * A conceptual model node as defined/used in SDK/fields/fields.json.
 * 
 * @typedef SDK.Node
 * @property {string} id
 * @property {SDK.Node} parentId
 * @property {string} xpathAbsolute
 * @property {string} xpathRelative
 * @property {boolean} repeatable
 * @property {string} identifierFieldId
 */

/**
 * A conceptual model field as defined/used in SDK/fields/fields.json.
 * 
 * @typedef SDK.Field
 * @property {string} id
 * @property {SDK.Node} parentNodeId
 * @property {string} xpathAbsolute
 * @property {string} xpathRelative
 * @property {string} name
 * @property {string} btId
 * @property {string} type
 * @property {string} legalType
 * @property {number} [maxLength]
 * @property {SDK.DynamicProperty<boolean>} [forbidden] 
 * @property {SDK.DynamicProperty<boolean>} [mandatory] 
 * @property {SDK.DynamicProperty<boolean>} [repeatable] 
 * @property {SDK.DynamicProperty<string>} [pattern] 
 * @property {SDK.DynamicProperty<SDK.Codelist>} [codeList] 
 * @property {SDK.DynamicProperty<SDK.EFXExpression>} [assert] 
 */

/*******************************************************************************
 * A content object as fetched from the SDK/notice-types/<subtype>.json.
 * 
 * @typedef {Object} SDK.NTDContent
 * @property {string} id
 * @property {SDK.NTDContent[]} [content]
 * @property {string} contentType
 * @property {string} displayType
 * @property {boolean} [_repeatable]
 * @property {boolean} [hidden]
 * @property {boolean} [readOnly]
 */

/*******************************************************************************
 * @typedef {Object} __ProxiedNTDContent__
 * @property {number} editorLevel
 * @property {number} editorCount
 * @property {boolean} isRepeatable
 * @property {boolean} isField
 * @property {boolean} isGroup
 * @property {string} fieldId
 * @property {SDK.Field} field
 * @property {RepeatableProperty} repeatableProperty
 * @property {boolean} hasParent
 * @property {ProxiedNTDContent} parent
 * @property {ProxiedNTDContent} repeatableParent
 * @property {string} qualifiedId
 * @property {string} instanceId
 * @property {ProxiedNTDContent} identifiedGroup
 * @property {Function} generateId
 * @property {Function} countInstances
 * 
 * Adds some functionality to an {@link SDK.NTDContent} object by wrapping it in 
 * a {@link Proxy} using {@link NTDContentProxy} as a handler.
 * 
 * @typedef {SDK.NTDContent & __ProxiedNTDContent__} ProxiedNTDContent
 */

/*******************************************************************************
 * @implements {ProxyHandler<SDK.NTDContent>}
 */
export class NTDContentProxy {

    /**
     * 
     * @param {SDK.NTDContent} content 
     * @param {ProxiedNTDContent} parent - The parent is expected to be already wrapped in a proxy.
     * @returns {ProxiedNTDContent}
     */
    static create(content, parent = null) {
        var proxy = new Proxy(content, new NTDContentProxy());
        proxy.parent = parent;
        return proxy;
    }

    /**
     * 
     * @param {SDK.NTDContent} target 
     * @param {string} property 
     * @param {ProxiedNTDContent} receiver 
     * @returns 
     */
    get(target, property, receiver) {
        switch (property) {
            case "hasParent": return target.parent ? true : false;
            case "repeatableParent": return receiver.parent?._repeatable ? receiver.parent : receiver.parent?.repeatableParent;
            case "qualifiedId": return Identifiers.formatFormElementIdentifier(target.id, -1, receiver.repeatableParent?.instanceId);
            case "instanceId": return Identifiers.formatFormElementIdentifier(target.id, target.editorCount, receiver.repeatableParent?.instanceId);
            case "identifiedGroup": return receiver.findIdentifiedGroup(target.id);
            case "findIdentifiedGroup": return function (identifierFieldId) {
                if (receiver.hasIdentifierField && target._identifierFieldId === identifierFieldId) {
                    return receiver;
                }
                return target.parent?.findIdentifiedGroup(identifierFieldId) ?? null;
            };
            case "generateId": return function () { return Identifiers.formatSchemedIdentifier(target._idScheme, receiver.identifiedGroup?.editorCount); };
            case "countInstances": return function() { return document.querySelectorAll(`[${Constants.Attributes.CONTENT_ID_ATTRIBUTE} = "${target.id}"]`).length; };
            case "fieldId": return receiver.isField ? target.id : undefined;
            case "field": return receiver.isField ? SdkServiceClient.fields[target.id] : undefined;
            case "hasRepeatableProperty": return target._repeatable !== undefined;
            case "repeatableProperty": return receiver.isField ? new RepeatableProperty(receiver.field.repeatable) : undefined;
            case "isField": return target.contentType === "field";
            case "isGroup": return target.contentType === "group";
            case "hasIdentifierField": return target._identifierFieldId ? true : false;
            case "isRepeatable": return receiver.isField ? receiver.repeatableProperty.value ?? false : target._repeatable ?? false;
            default: return Reflect.get(target, property);
        }
    }
}
