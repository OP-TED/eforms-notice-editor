import { Constants } from "./global.js";

/**
 * 
 */
export class VisualModelElement {

    static create(htmlElement) {
        switch (htmlElement.getAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE)) {
            case Constants.ContentType.GROUP: return VisualModelGroupElement.create(htmlElement);
            case Constants.ContentType.FIELD: return new VisualModelFieldElement(htmlElement);
            case Constants.ContentType.METADATA_CONTAINER: return new VisualModelTopLevelElement(htmlElement);
            case Constants.ContentType.DATA_CONTAINER: return new VisualModelTopLevelElement(htmlElement);
            default: return VisualModelElement.fromChildren(htmlElement);
        }
    }

    constructor(htmlElement) {

        this.contentId = htmlElement.getAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE);
        this.contentType = htmlElement.getAttribute(Constants.Attributes.CONTENT_TYPE_ATTRIBUTE);
        this.contentCount = htmlElement.getAttribute(Constants.Attributes.COUNTER_ATTRIBUTE);

        if (htmlElement.hasChildNodes()) {
            const visualModelElements = VisualModelElement.fromChildren(htmlElement);
            if (visualModelElements.length) {
                this.children = visualModelElements;
            }
        }
    }

    findByContentId(contentId) {
        if (this.contentId === contentId) {
            return this;
        }
        if (this.children) {
            for (const childElement of this.children) {
                const foundElement = childElement.findByContentId(contentId);
                if (foundElement) {
                    return foundElement;
                }
            }
        }
        return null;
    }

    static fromChildren(htmlElement) {
        var visualModelElements = [];
        for (const childElement of htmlElement.children) {
            const visualModelElement = VisualModelElement.create(childElement);
            if (visualModelElement instanceof Array) {
                visualModelElements = visualModelElements.concat(visualModelElement);
            } else {
                visualModelElements.push(visualModelElement);
            }
        }
        return visualModelElements;
    }
}

/**
 * The visual-model has two top-level "sections": The "Notice-Metadata" section and the "Notice-Data" section.
 * These two "sections" are contained directly under the root element of the visual-model.
 * The root element of teh visual-model, is also a VisualModelTopLevelElement (see {@link VisualModel} class).
 */
export class VisualModelTopLevelElement extends VisualModelElement {
    constructor(htmlElement) {
        super(htmlElement);
        this.visType = Constants.VIS_TYPE_NON_FIELD;
    }
}

/**
 * A VisualModelGroupElement is created for every display-group that is associated with a Node.
 * Display-groups that have no associated Node are flattened-out (omitted) in the visual-model.
 */
export class VisualModelGroupElement extends VisualModelElement {

    /**
     * Factory method.
     * If the passed htmlElement is associated with a Node, then it will create and return a new VisualModelGroupElement.
     * Otherwise it will return an array of VisualModelElements created from the child nodes of the passed htmlElement.
     */
    static create(htmlElement) {
        switch (htmlElement.hasAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE)) {
            case true: return new VisualModelGroupElement(htmlElement);
            default: return VisualModelElement.fromChildren(htmlElement);
        }
    }

    constructor(htmlElement) {
        super(htmlElement);
        this.visType = Constants.VIS_TYPE_NON_FIELD;
        if (htmlElement.hasAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE)) {
            this.visNodeId = htmlElement.getAttribute(Constants.Attributes.NODE_ID_ATTRIBUTE);
        }
    }
}

export class VisualModelFieldElement extends VisualModelElement {
    constructor(htmlElement) {
        super(htmlElement);
        this.visType = Constants.VIS_TYPE_FIELD;
        // TODO: upcoming TEDEFO-1727
        this.value = VisualModelFieldElement.getValue(htmlElement);
    }

    static getValue(htmlElement) {

        if (htmlElement instanceof HTMLInputElement || htmlElement instanceof HTMLSelectElement || htmlElement instanceof HTMLTextAreaElement) {
            return htmlElement.value;
        }

        const radioButtons = document.getElementsByName(htmlElement.getAttribute(Constants.Attributes.CONTENT_ID_ATTRIBUTE));
        if (radioButtons.length > 0) {
            for (const radioButton of radioButtons) {
                if (radioButton.checked) {
                    return radioButton.value;
                }
            }
            return "";
        }
    }
}

/**
 * Extends the VisualModelTopLevelElement to define the root element of the VisualModel 
 * which, after having being constructed, contains the entire visual-model.  
 */
export class VisualModel extends VisualModelTopLevelElement {

    constructor(rootElement) {
        super(rootElement);

        // Put some info at the top level for convenience.
        this.contentId = "notice-root"; // visualRoot.
        this.visNodeId = Constants.StandardIdentifiers.ND_ROOT;
        this.visType = Constants.VIS_TYPE_NON_FIELD;
        this.contentCount = "1";

        // Put the notice SDK version at top level.
        this.sdkVersion = this.findByContentId(Constants.StandardIdentifiers.SDK_VERSION_FIELD).value;

        // Put the notice sub type at top level.
        this.noticeSubType = this.findByContentId(Constants.StandardIdentifiers.NOTICE_SUBTYPE_FIELD).value;

        // Put the notice UUID top level.
        this.noticeUuid = this.findByContentId(Constants.StandardIdentifiers.NOTICE_UUID_FIELD).value;
    }
}