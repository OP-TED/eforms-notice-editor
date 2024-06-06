import { Context, NoticeSelector } from "./context.js"
import { Constants } from "./global.js";
import { SdkServiceClient } from "./service-clients.js";

export class NoticeReader {

    //#region Singleton initialisation

    /** @type {NoticeReader} */
    static #instance;

    /** @type {NoticeReader} */
    static get instance() {
        return this.#instance;
    }

    static {
        this.#instance = new NoticeReader();
    }

    //#endregion

    /** @type {DOMParser} */
    parser;

    /** @type {Document} */
    document;

    /** @type {XPathNSResolver} */
    nsResolver;

    constructor() {
        this.parser = new DOMParser();

        document.addEventListener(Constants.Events.noticeLoading, (event) => {
            NoticeReader.instance.load(event.detail.file);
        });
    }

    #read(xmlFile) {
        return new Promise((resolve) => {
            var reader = new FileReader();
            reader.onload = (event) => {
                resolve(event.target.result);
            };
            reader.readAsText(xmlFile);
        });
    }

    /**
     * 
     * @param {string} xmlString 
     */
    #parse(xmlString) {
        this.document = $.parseXML(xmlString);
        this.nsResolver = this.document.createNSResolver(this.document.documentElement);
    }

    /**
     * 
     * @param {File} xmlFile 
     */
    async load(xmlFile) {
        if (xmlFile?.type !== "text/xml") {
            return;
        }

        var xmlText = await this.#read(xmlFile);
        NoticeReader.instance.#parse(xmlText);
        console.debug("customizationID=" + NoticeReader.customizationID);
        Context.raiseNoticeLoaded(xmlText, NoticeReader.customizationID?.replace("eforms-sdk-",""), NoticeReader.noticeSubtype); 
    }


    static * getFieldValues(fieldId) {
        var xpath = SdkServiceClient.fields[fieldId].xpathAbsolute;
        try {
            var results = $(this.instance.document).xpath(xpath, ns => this.instance.nsResolver.lookupNamespaceURI(ns));
            for (var result of results) {
                yield result.textContent;
            }
        } catch (e) {
            console.log(`Failed to evaluate XPath "${xpath}"`);
        }
    }

    static get hasDocument() {
        return NoticeReader.instance.document ? true : false;
    }

    static get customizationID() {
        return this.getFieldValues(Constants.StandardIdentifiers.SDK_VERSION_FIELD).next()?.value;
    }

    static get noticeSubtype() {
        return this.getFieldValues(Constants.StandardIdentifiers.NOTICE_SUBTYPE_FIELD).next()?.value;
    }
}