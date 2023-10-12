import { Context } from "./context.js";
import { SdkServiceClient } from "./service-clients.js";

export const Constants = {
    DEBUG: true,

    DATA_EDITOR_ID_REFERENCE: "data-editor-id-reference",
    DATA_EDITOR_ID_REF_PREFIX: "data-editor-id-ref-",
    DATA_EDITOR_ID_SCHEME: "data-id-scheme",

    Attributes: {
        CONTENT_ID_ATTRIBUTE: "data-content-id",
        CONTENT_TYPE_ATTRIBUTE: "data-content-type", // For the VIS TYPE
        NODE_ID_ATTRIBUTE: "data-node-id",
        ID_SCHEME_ATTRIBUTE: "data-id-scheme",
        ID_SCHEMES_ATTRIBUTE: "data-id-schemes",
        VALUE_SOURCE_ATTRIBUTE: "data-value-source",
        COUNTER_ATTRIBUTE: "data-counter",
    },

    InputFieldProxyHandler: {
        get(target, property) {
            switch (property) {
                case "target": return target;
                case "contentId": return target.getAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE);
                case "contentType": return target.getAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE);
                case "idScheme": return target.getAttribute(Constants.Attributes.ID_SCHEME_ATTRIBUTE);
                case "idSchemes": return target.getAttribute(Constants.Attributes.ID_SCHEMES_ATTRIBUTE)?.split(',') ?? [];
                case "nodeId": return target.getAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE);
                case "valueSource": return target.getAttribute(Constants.Attributes.VALUE_SOURCE_ATTRIBUTE);
                case "instanceCounter": return target.getAttribute(Constants.Attributes.COUNTER_ATTRIBUTE);
                case "isField": return target.getAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE) === Constants.ContentType.FIELD;
                case "hasContentId": return target.hasAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE);
                case "hasContentType": return target.hasAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE);
                case "hasIdScheme": return target.hasAttribute(Constants.Attributes.ID_SCHEME_ATTRIBUTE);
                case "hasIdSchemes": return target.hasAttribute(Constants.Attributes.ID_SCHEMES_ATTRIBUTE);
                case "hasNodeId": return target.hasAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE);
                case "hasValueSource": return target.hasAttribute(Constants.Attributes.VALUE_SOURCE_ATTRIBUTE);
                case "hasInstanceCounter": return target.hasAttribute(Constants.Attributes.COUNTER_ATTRIBUTE);
                default: return Reflect.get(target, property);
            }
        },
        set(target, property, value) {
            switch (property) {
                case "contentId": target.setAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE); break;
                case "contentType": target.setAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE); break;
                case "idScheme": target.setAttribute(Constants.Attributes.ID_SCHEME_ATTRIBUTE, value); break;
                case "idSchemes": target.setAttribute(Constants.Attributes.ID_SCHEMES_ATTRIBUTE, value); break;
                case "nodeId": target.setAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE, value); break;
                case "valueSource": target.setAttribute(Constants.Attributes.VALUE_SOURCE_ATTRIBUTE, value); break;
                case "instanceCounter": target.setAttribute(Constants.Attributes.COUNTER_ATTRIBUTE, value); break;
                default: Reflect.set(target, property, value); break;
            }
        }
    },

    HtmlElements: {
        FORM_ELEMENT: "form-element",
        FORM_CONTENT_ROOT_ELEMENT: "form-content-root",
        APP_VERSION_ELEMENT: "app-version"
    },

    // Avoids conflicts with other identifiers in the context of this browser tab.
    EDITOR_ID_PREFIX: "editor-id-",

    VIS_CONTENT_COUNT: "contentCount",
    VIS_VALUE: "value",
    VIS_TYPE: "visType", // Has nothing to do with HTML element type!
    VIS_TYPE_FIELD: "field",
    VIS_TYPE_NON_FIELD: "non-field",

    NTD_LABEL: "_label",
    
    ContentType: {
        FIELD: "field",
        GROUP: "group",
        DATA_CONTAINER: "notice-data",
        METADATA_CONTAINER: "notice-metadata",
        ROOT: "notice"
    },
    
    DisplayType: {
        COMBOBOX: "COMBOBOX",
        CHECKBOX: "CHECKBOX",
        GROUP: "GROUP",
        TEXTAREA: "TEXTAREA",
        TEXTBOX: "TEXTBOX",
        RADIO: "RADIO"
    },

    // TODO: HARDCODED => These should be added to the SDK.
    StandardIdentifiers: {
        ND_ROOT: "ND-Root",
        SDK_VERSION_FIELD: "OPT-002-notice",
        UBL_VERSION_FIELD: "OPT-001-notice",
        NOTICE_SUBTYPE_FIELD: "OPP-070-notice",
        NOTICE_UUID_FIELD: "BT-701-notice",
        NOTICE_VERSION_FIELD: "BT-757-notice",
    }
}

export class DomUtil {
    static findElementsHavingAttribute(attributeName) {
        const selector = "[" + attributeName + "]";
        return document.querySelectorAll(selector);
    }

    static findElementsWithAttributeValue(attributeName, attributeText) {
        const selector = '[' + attributeName + '="' + attributeText + '"]';
        return document.querySelectorAll(selector);
    }

    static findElementsWithContentId(contentId) {
        return DomUtil.findElementsWithAttributeValue(Constants.Attributes.CONTENT_ID_ATTRIBUTE, contentId);
    }

    /**
     * Find the HTML elements having the idSchemes (search HTML element by attribute).
     */
    static findElementsWithAttributeIdSchemes(idSchemes) {
        const allFoundElements = [];
        for (const idScheme of idSchemes) {
            const foundElements = DomUtil.findElementsWithAttributeValue(Constants.DATA_EDITOR_ID_SCHEME, idScheme);
            for (const element of foundElements) {
                allFoundElements.push(element);
            }
        }
        return allFoundElements;
    }

    static findElementsWithAttributeIdScheme(idScheme) {
        return DomUtil.findElementsWithAttributeIdSchemes([idScheme]);
    }

    static findElementsWithAttributeIdRef(idScheme) {
        // Find HTML elements that reference this kind of idScheme.
        return DomUtil.findElementsWithAttributeValue(Constants.DATA_EDITOR_ID_REF_PREFIX + idScheme.toLowerCase(), "true");
    }

    static generateRandomUuidV4() {
        // This is for demo purposes, you could generate this UUID on the server-side using Java, C#,...
        // https://stackoverflow.com/questions/105034/how-do-i-create-a-guid-uuid/2117523#2117523
        return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
            (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
        );
    }

    static createOption(key, label) {
        const option = document.createElement("option");
        option.setAttribute("value", key);
        option.textContent = label;
        return option;
    }
}

export class I18N {

    // This could be covered by "auxiliary labels" or be something fully custom.
    // The i18nOfEditor is loaded before other translations, thus it can be used to override them.
    // This could be used to fix a typo while waiting for the next version of the SDK.
    // This can also be used to define arbitrary translated texts for use in the editor.
    static labels = {
        "en": {
            "editor.are.you.sure": "Are you sure?",
            "editor.the.metadata": "Notice Metadata",
            "editor.the.root": "Notice Form",
            "editor.add.more": "Add one",
            "editor.remove": "Remove",
            "editor.select": "Select",
        },
        "fr": {
            "editor.are.you.sure": "Êtes-vous sûr ?",
            "editor.the.metadata": "Méta données",
            "editor.the.root": "Contenu",
            "editor.add.more": "En ajouter",
            "editor.remove": "Enlever",
            "editor.select": "Choisir"
        }
    };

    /** 
     * For demo purposes only two editor demo UI languages are provided.
     * You could also separate the UI language from the notice language if desired.
     */
    static Iso6391ToIso6393Map = {
        "en": "ENG",
        "fr": "FRA"
    };

    static getLabel(labelId) {
        var applicationLabels = I18N.labels[Context.language] ?? I18N.labels["en"];
        return applicationLabels[labelId] ?? SdkServiceClient.translations[labelId] ?? labelId;
    }
}

