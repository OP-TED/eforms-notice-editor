import { Constants, Identifiers } from "./global.js";
import { GroupUIComponentProxy, UIComponentProxy, FieldUIComponentProxy } from "./proxies.js";
import { Validator } from "./validator.js";

/**
 * Some elements in notice-type-definition files, have dependencies to each other.
 * For example, some elements have a valueSource property that indicates that their value is a copy of the value of another element.  
 * Other elements represent fields of type "id-ref" and need to "discover" the identifiers available for them to reference.
 *  
 * The Orchestrator class is a singleton that we use to synchronise values across HTML elements as needed.
 * It is not a generic reusable class but rather focuses on the needs of the eForms Notice Editor.
 * 
 * The Orchestrator works by monitoring the root element of the notice form, for mutations in its subtree (additions or removals of HTML elements).
 * When an HTML element is added or removed, {@link Orchestrator.mutationObserved} method is called which inspects the HTML element
 * and acts accordingly based on its data attributes.
 * 
 * The values that need to be synchronised include:
 * - All input-fields of type "id-ref" use a combobox to select a referenced identifier. 
 *   A list with all relevant identifiers (as specified by the idSchemes property), is needed to populate the combobox.
 *   As new identifiers are created (or removed) while the user is filling-in the form, id-ref input-fields need to update
 *   the options available in their comboboxes accordingly. The Orchestrator takes care of this synchronisation. 
 *   (see {@link Orchestrator.subscribeToIdentifierNotifications}).
 * - Input-fields with a valueSource property, need to synchronise their value with the value of another input-field.
 */
export class Orchestrator extends MutationObserver {

    /**
     * Being declared as static, when this property is initialized it creates the single instance of
     * the Orchestrator and sets it up to monitor (observe) changes in the subtree of the HTML element that contains 
     * the entire notice form.
     */
    static instance = new Orchestrator(document.getElementById(Constants.HtmlElements.FORM_CONTENT_ROOT_ELEMENT));

    constructor(htmlElement) {
        if (Orchestrator.instance) {
            throw new Error("You should not instantiate the Orchestrator yourself. It's meant to be a singleton.");
        }

        // Create the MutationObserver and connect it to the mutationObserved handler.
        super(Orchestrator.mutationObserved);

        // This is not a typical publisher/subscriber pattern implementation, but it is inspired by it.
        // Some elements need to be updated when changes happen on other elements. 
        // There is a need to track these dependencies and trigger the necessary updates.  
        // 
        // Because the order in which these elements are created is arbitrary, and does not necessarily follow the
        // order of their dependencies, it is hard to connect these elements directly using event handlers.
        //
        // To overcome this problem, the idea of subscriptions is used from the publisher/subscriber pattern:
        // - HTML elements that need to be updated, are subscribed to specific "updates" by using a subscription key.
        // - HTML elements causing/triggering these "updates" are treated as publishers for their respective subscription key(s).
        // The subscriptions property maintains a map where every subscription key is mapped to an array of all its subscribers.
        // Subscribers are stored as references to HTML elements.
        this.subscriptions = new Map();

        // The publications property, is used to keep track of event handlers attached to publisher events.
        // We need to keep track of them so that we can remove them when they are no longer needed.
        this.publications = new Map();

        // Start observing mutations
        this.observe(htmlElement, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ["id", "data-counter"],
            attributeOldValue: true
        });
    }

    /**
     * Called whenever any mutations are observed by the MutationObserver (Orchestrator).
     * 
     * @param {MutationRecord[]} mutationList Array of {@link MutationRecord} objects listing observed mutations.
     * @param {Orchestrator} observer 
     */
    static mutationObserved(mutationList, observer) {
        for (const mutation of mutationList) {                                  // Go through each mutation observed.
            switch (mutation.type) {
                case "attributes":
                    var newValue = mutation.target.getAttribute(mutation.attributeName);
                    if (mutation.oldValue && mutation.oldValue !== newValue) {
                        switch (mutation.attributeName) {
                            case "id": observer.htmlElementIdChanged(mutation.target, mutation.oldValue, newValue); break;
                            case Constants.Attributes.COUNTER_ATTRIBUTE: observer.instanceNumberChanged(mutation.target, mutation.oldValue, newValue); break;
                        }
                    }
                    break;
                case "childList":
                    mutation.addedNodes.forEach(node => observer.nodeAdded(node));      // First, process added nodes,
                    mutation.removedNodes.forEach(node => observer.nodeRemoved(node));  // then, process removed nodes.
                    break;
            }
        }
    }

    /**
     * Called by {@link Orchestrator.mutationObserved} for each node added in the subtree being monitored.
     * 
     * @param {Node} addedNode 
     */
    nodeAdded(addedNode) {
        if (!(addedNode instanceof HTMLElement)) {
            return; // We are only interested in HTML elements. Ignore everything else (i.e. text nodes etc.).
        }

        for (const node of addedNode.querySelectorAll(GroupUIComponentProxy.cssSelector)) {

            const proxy = GroupUIComponentProxy.create(node);

            if (proxy.isRepeatable) {
                this.renumberRepeatableContents(proxy.repeater);
            }
        }

        // The added node may have child nodes. We need to look at all of them.
        // We are interested in all the nodes that have a CONTENT_TYPE_ATTRIBUTE because these are the ones 
        // that may represent input-fields.
        for (const node of addedNode.querySelectorAll(FieldUIComponentProxy.cssSelector)) {

            // To make code more readable here we use a Proxy to extract the values from the node's attributes.
            const proxy = FieldUIComponentProxy.create(node);

            if (proxy.hasIdScheme) {
                // This node was just added and has an idScheme attribute. 
                // Therefore a new identifier has been added for this idScheme.
                // We need to add this new identifier to all comboboxes that subscribed to this idScheme.
                this.identifierAdded(proxy.idScheme, proxy.value);
            }

            if (proxy.hasIdSchemes) {
                // This node was just added and has an idSchemes attribute. 
                // Therefore a new combobox has been added which will need to be updated 
                // whenever identifiers are added or removed for any of its idSchemes.
                // We need to subscribe this new combobox to updates of all its idSchemes.
                this.subscribeToIdentifierNotifications(proxy.target, ...proxy.idSchemes);
            }

            if (proxy.hasValueSource) {
                // This node was just added and has a valueSource attribute.
                // Therefore, the value of this new node needs to be updated every time the 
                // value of the node indicated by the valueSource changes.
                // We need to subscribe this node to changes of the value of its valueSource node.
                this.subscribeToChangesOfValueSource(proxy.target, proxy.valueSource);
            }

            if (proxy.isField) {
                // Changes of the value of the input-field represented by the node that was just added,
                // need to be captured so that any subscribers to these changes can be updated.
                // We will attach an event listener to this publisher so that we can update any subscribers
                // when changes occur later.
                this.addPublisherOfValueSourceChanges(proxy.target, proxy.contentId);
            }

            if (proxy.isRepeatable) {
                this.renumberRepeatableContents(proxy.repeater);
            }
        }
    }


    /**
     * Called by {@link Orchestrator.mutationObserved} for each Node removed from the subtree being monitored.
     * 
     * @param {Node} removedNode 
     */
    nodeRemoved(removedNode) {
        if (!(removedNode instanceof HTMLElement)) {
            return; // We are only interested in HTML elements. Ignore everything else (i.e. text nodes etc.).
        }

        for (const node of removedNode.querySelectorAll(GroupUIComponentProxy.cssSelector)) {

            const proxy = GroupUIComponentProxy.create(node);

            if (proxy.isRepeatable) {
                this.renumberRepeatableContents(proxy.repeater);
            }
        }

        // The removedNode may have childNodes. We need to look at all of them.
        // We are interested in all the Nodes that have a CONTENT_TYPE_ATTRIBUTE because these are the ones 
        // that represent input-fields.

        for (const node of removedNode.querySelectorAll(`[${Constants.Attributes.CONTENT_TYPE_ATTRIBUTE} = '${Constants.ContentType.FIELD}']`)) {

            // To make code more readable here we use a Proxy to extract the values from the node's attributes.
            const proxy = FieldUIComponentProxy.create(node);

            if (proxy.hasIdScheme) {
                // This node was just removed and has an idScheme attribute. 
                // Therefore an identifier no longer exists for this idScheme.
                // We need to remove this identifier from all comboboxes that subscribed to this idScheme.
                this.identifierRemoved(proxy.idScheme, proxy.target.value);
            }

            if (proxy.hasIdSchemes) {
                // This node was just removed and has an idSchemes attribute. 
                // Therefore a combobox has been removed which will no longer need to be updated 
                // whenever identifiers are added or removed for any of its idSchemes.
                // The subscription we created earlier for it, is no longer needed and should be dropped.
                this.unsubscribeFromIdentifierNotifications(proxy.target, ...proxy.idSchemes);
            }

            if (proxy.hasValueSource) {
                // This node has a valueSource, but will not longer need to be updated since it was just removed.
                // We should drop its subscription.
                this.unsubscribeFromChangesOfValueSource(proxy.target, proxy.valueSource);
            }

            if (proxy.isField) {
                // An input-field was just removed. We should disconnect the event listener we attached earlier.
                this.removePublisherOfValueSourceChanges(proxy.target, proxy.contentId);

                // Also remove the input-field from form validation.
                Validator.unregister(proxy.target);
            }

            if (proxy.isRepeatable) {
                this.renumberRepeatableContents(proxy.repeater);
            }
        }
    }

    /**
     * Adds the passed htmlElement as a subscriber to additions/removals of identifiers conforming to any of the passed idSchemes.
     * 
     * @param {HTMLElement} htmlElement 
     * @param  {...String} idSchemes 
     */
    subscribeToIdentifierNotifications(htmlElement, ...idSchemes) {
        for (const idScheme of idSchemes) {
            if (!this.subscriptions.has(idScheme)) {
                this.subscriptions.set(idScheme, new Set());
            }
            this.subscriptions.get(idScheme).add(htmlElement);
        }
    }

    /**
     * Removes the passed htmlElement from the subscribers to additions/removals of identifiers conforming to the passed idSchemes.
     * 
     * @param {HTMLElement} htmlElement 
     * @param  {...String} idSchemes 
     */
    unsubscribeFromIdentifierNotifications(htmlElement, ...idSchemes) {
        for (const idScheme of idSchemes) {
            this.subscriptions.get(idScheme).delete(htmlElement);
        }
    }

    /**
     * Adds the passed htmlElement as a subscriber to updates of the input-field indicated by fieldId.
     * 
     * @param {HTMLElement} htmlElement 
     * @param {String} fieldId 
     */
    subscribeToChangesOfValueSource(htmlElement, fieldId) {
        if (!this.subscriptions.has(fieldId)) {
            this.subscriptions.set(fieldId, new Set());
        }
        this.subscriptions.get(fieldId).add(htmlElement);
    }

    /**
     * Removes the passed htmlElement from the subscribers to updates of the input-field indicated by fieldId.
     * 
     * @param {HTMLElement} htmlElement 
     * @param {String} fieldId 
     */
    unsubscribeFromChangesOfValueSource(htmlElement, fieldId) {
        this.subscriptions.get(fieldId)?.delete(htmlElement);
    }

    /**
     * Adds an input-field as a publisher of notifications for valueSource changes. 
     * 
     * @param {HTMLElement} htmlElement 
     * @param {String} fieldId 
     */
    addPublisherOfValueSourceChanges(htmlElement, fieldId) {
        if (!this.publications.has(fieldId)) {
            this.publications.set(fieldId, new Map());
        }
        const publishers = this.publications.get(fieldId); 
        if (!publishers.has(htmlElement)) {
            // 1. Update any existing subscribers
            this.valueSourceChanged(fieldId, htmlElement.value);

            // 2. Attach an event handler to update subscribers when publisher's value changes
            publishers.set(htmlElement, (event) => { this.valueSourceChanged(fieldId, event.target.value) })
            htmlElement.addEventListener("change", publishers.get(htmlElement));
        }
    }


    /**
     * Removes the attached event handler from an input-field that was previously added as a publisher of valueSource changes.  
     * 
     * @param {HTMLElement} htmlElement 
     * @param {String} fieldId 
     */
    removePublisherOfValueSourceChanges(htmlElement, fieldId) {
        const publishers = this.publications.get(fieldId); 
        if (publishers?.has(htmlElement)) {
            htmlElement.removeEventListener("change", publishers.get(htmlElement));
            publishers.delete(htmlElement);
        }
    }

    /**
     * Call whenever an identifier complying to the passed idScheme has been added, to update all subscribers of this idScheme.
     * 
     * @param {String} idScheme 
     * @param {String} identifier 
     */
    identifierAdded(idScheme, identifier) {
        for (const subscriber of this.subscriptions.get(idScheme) ?? []) {
            subscriber.tomselect?.addOption({ value: identifier, text: identifier });
        }
    }

    /**
     * Call whenever an identifier complying to the passed idScheme has been removed to update all subscribers of the idScheme.
     * 
     * @param {String} idScheme 
     * @param {String} identifier 
     */
    identifierRemoved(idScheme, identifier) {
        for (const subscriber of this.subscriptions.get(idScheme) ?? []) {
            subscriber.tomselect?.removeOption(identifier);
        }
    }

    /**
     * Called whenever the valueSource field has changed value to update the values of all
     * subscribers to the valueSource.
     * 
     * @param {String} fieldId 
     * @param {*} value 
     */
    valueSourceChanged(fieldId, value) {
        if (this.subscriptions.has(fieldId)) {
            this.subscriptions.get(fieldId)?.forEach(subscriber => subscriber.value = value);
        }
    }

    /**
     * When a repeatable element is added or removed, we need to update
     * the instance numbers of all other instances of the element, so that
     * we leave no "gaps". This will allow us to match each element with
     * validation messages later based on properly ordered instance numbers 
     * matched ont-to-one with the ones provided by the XML validation engine.
     * 
     * @param {import("./proxies.js").ProxiedRepeater} repeater 
     */
    renumberRepeatableContents(repeater) {
        var counter = 1;
        for (const node of repeater?.repeatedElements ?? []) {
            const proxy = UIComponentProxy.create(node);
            proxy.instanceCounter = counter++;
        }
    }

    /**
     * The instance number is used to form the identifiers.
     * Therefore when the instance number changes, we need to update the "id" attribute of
     * the element.
     * 
     * @param {HTMLElement} target 
     * @param {string} oldValue
     * @param {string} newValue 
     */
    instanceNumberChanged(target, oldValue, newValue) {
        const proxy = UIComponentProxy.create(target);
        target.id = Identifiers.formatFormElementIdentifier(proxy.contentId, proxy.instanceCounter, proxy.repeatableParent?.instanceId);
   }

    /**
     * When the "id" attribute of an HTMLElement changes, we need to update
     * the "for" attribute value of any the labels that point to the element.
     * 
     * There are better ways to do this of course. For example if we used Web Components
     * we would be able to handle these updates more elegantly without the need for a 
     * MutationObserver.
     * 
     * @param {HTMLElement} target 
     * @param {string} oldValue 
     * @param {string} newValue
     */
    htmlElementIdChanged(target, oldValue, newValue) {
        const proxy = UIComponentProxy.create(target);

        // Find all labels that point to the old id and update them.
        document.querySelectorAll(`.label[for="${oldValue}"]`).forEach(e => {
            e.setAttribute("for", newValue);
            e.textContent = `#${proxy.instanceCounter}`
        });

        // Also update all labels that point to HTML "select" elements wrapped by TomSelect.
        document.querySelectorAll(`.label[for="${oldValue}-ts-control"]`).forEach(e => {
            e.setAttribute("for", `${newValue}-ts-control`);
            e.textContent = `#${proxy.instanceCounter}`
        });
    }
}
