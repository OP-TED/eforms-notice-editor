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
        ID_FIELD_ATTRIBUTE: "data-id-field",
        VALUE_SOURCE_ATTRIBUTE: "data-value-source",
        COUNTER_ATTRIBUTE: "data-counter",
        REPEATABLE_ATTRIBUTE: "data-repeatable",
    },

    HtmlElements: {
        FORM_ELEMENT: "form-element",
        FORM_CONTENT_ROOT_ELEMENT: "form-content-root",
        APP_VERSION_ELEMENT: "app-version"
    },

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
    },

    Events: {
        languageChanged: "context-language-changed",
        sdkVersionChanged: "context-sdk-version-changed",
        noticeSubtypeChanged: "context-notice-subtype-changed",
        noticeLoading: "context-notice-loading",
        noticeLoaded: "context-notice-loaded",
      },


}

export class Identifiers {

    /**
     * 
     * @param {string} contentId 
     * @param {number} instanceNumber 
     * @param {string|null} qualifier? 
     * @returns 
     */
    static formatFormElementIdentifier(contentId, instanceNumber, qualifier = null) {
        // replace all non-word characters with underscores
        const properQualifier = qualifier?.trim().replace(/\W/g, '_').toLowerCase() ?? "";
        const properContentId = contentId?.trim().replace(/\W/g, '_').toLowerCase() ?? "";

        // add the instance number if provided
        const suffix = instanceNumber > -1 ? `_${instanceNumber.toString().padStart(4, "0")}` : "";

        // add an underscore to separate the qualifier from identifier if both of them are present
        const delimiter = properQualifier && properContentId ? "_" : "";

        // put the identifier together
        const identifier = properQualifier + delimiter + properContentId + suffix

        // prefix with an underscore if the contentId starts with a number
        const prefix = /^\d/.test(identifier) ? "_" : "";
        return prefix + identifier;
    }

    /**
     * 
     * @param {string} idScheme 
     * @param {number} instanceNumber 
     * @returns 
     */
    static formatSchemedIdentifier(idScheme, instanceNumber) {
        return `${idScheme}-${instanceNumber?.toString().padStart(4, "0")}`;
    }

    static generateRandomUuidV4() {
        // This is for demo purposes, you could generate this UUID on the server-side using Java, C#,...
        // https://stackoverflow.com/questions/105034/how-do-i-create-a-guid-uuid/2117523#2117523
        return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
            (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
        );
    }
}

