import { Context } from "./context.js";
import { SdkServiceClient } from "./service-clients.js";

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