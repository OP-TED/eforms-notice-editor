
/**
 * Client for the back-end SdkService.
 * 
 * Using static  methods instead of implementing a singleton pattern for simplicity,
 * as there is no need for inheritance or implementing specific interfaces in this scenario. 
 */
export class SdkServiceClient {

    static appVersion = undefined;
    static availableSdkVersions = undefined;
    static availableNoticeSubtypes = undefined;
    static sdkVersion = undefined;
    static ublVersion = undefined;
    static fields = undefined;
    static nodes = undefined;
    static codelists = undefined;
    static translations = undefined;
    static noticeTypeDefinition = undefined;

    static async fetchVersionInfo() {
        const response = await fetch("/sdk/info");
        const json = await response.json();

        SdkServiceClient.appVersion = json.appVersion;
        SdkServiceClient.availableSdkVersions = json.sdkVersions;
    }

    static async fetchFieldsAndCodelists(sdkVersion) {
        const response = await fetch("/sdk/" + sdkVersion + "/basic-meta-data");
        const json = await response.json();

        SdkServiceClient.sdkVersion = json.fieldsJson.sdkVersion;
        SdkServiceClient.ublVersion = json.fieldsJson.ublVersion;
        SdkServiceClient.fields = Object.fromEntries(json.fieldsJson.fields.map((field) => [field.id, field]));
        SdkServiceClient.nodes = Object.fromEntries(json.fieldsJson.xmlStructure.map((node) => [node.id, node]));
        SdkServiceClient.codelists = Object.fromEntries(json.codelistsJson.codelists.map((codelist) => [codelist.id, codelist]));
    }

    static async fetchAvailableNoticeSubtypes(sdkVersion) {
        const response = await fetch("/sdk/" + sdkVersion + "/notice-types");
        const json = await response.json();
        SdkServiceClient.availableNoticeSubtypes = json.noticeTypes;
    }

    static async fetchNoticeSubtype(sdkVersion, noticeId) {
        const response = await fetch("/sdk/" + sdkVersion + "/notice-types/" + noticeId)
        SdkServiceClient.noticeTypeDefinition = await response.json();
    }

    static async fetchTranslations(sdkVersion, languageCode) {
        const response = await fetch("/sdk/" + sdkVersion + "/translations/" + languageCode + ".json");
        SdkServiceClient.translations = await response.json();
    }
}

/**
 * Client for the back-end XmlWriteService.
 * 
 * Using static methods instead of implementing a singleton pattern for simplicity,
 * as there is no need for inheritance or implementing specific interfaces in this scenario. 
 */
export class XmlServiceClient {

    static async saveXml(visualModel) {
        const settings = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: visualModel
        };
        const response = await fetch("/xml/notice/save/validation/none", settings);
        return await response.text();
    }

    static async xsdValidation(visualModel) {
        const settings = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: visualModel
        };
        const response = await fetch("/xml/notice/save/validation/xsd", settings);
        return await response.text();
    }

    static async cvsValidation(visualModel) {
        const settings = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: visualModel
        };
        const response = await fetch("/xml/notice/save/validation/cvs", settings);
        return await response.text();
    }
}