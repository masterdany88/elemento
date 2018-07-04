package org.jboss.gwt.elemento.core;

import elemental2.dom.*;
import jsinterop.base.Js;

import java.util.ArrayList;
import java.util.List;

import static elemental2.dom.DomGlobal.document;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class BodyObserver {

    private static String ATTACH_UID_KEY = "on-attach-uid";
    private static String DETACH_UID_KEY = "on-detach-uid";

    private static List<ElementObserver> detachObservers = new ArrayList<>();
    private static List<ElementObserver> attachObservers = new ArrayList<>();

    static {
        MutationObserver mutationObserver = new MutationObserver((MutationRecord[] records, MutationObserver observer) -> {
            for (int i = 0; i < records.length; i++) {
                onElementsRemoved(records[i]);
                onElementsAppended(records[i]);
            }
            return null;
        });

        MutationObserverInit mutationObserverInit = MutationObserverInit.create();
        mutationObserverInit.setChildList(true);
        mutationObserverInit.setSubtree(true);

        mutationObserver.observe(document.body, mutationObserverInit);
    }

    private BodyObserver() {

    }

    private static void onElementsAppended(MutationRecord record) {
        List<ElementObserver> observed = new ArrayList<>();
        for (int i = 0; i < attachObservers.size(); i++) {
            ElementObserver elementObserver = attachObservers.get(i);
            if (isNull(elementObserver.observedElement())) {
                observed.add(elementObserver);
            } else {
                if (isAppended(elementObserver.attachId())) {
                    elementObserver.callback().onObserved(record);
                    elementObserver.observedElement().removeAttribute(ATTACH_UID_KEY);
                    observed.add(elementObserver);
                }
            }
        }

        attachObservers.removeAll(observed);
    }

    private static boolean isAppended(String attachId) {
        return nonNull(DomGlobal.document.body.querySelector("[" + ATTACH_UID_KEY + "='" + attachId + "']"));
    }

    private static void onElementsRemoved(MutationRecord record) {
        List<ElementObserver> observed = new ArrayList<>();
        for (int i = 0; i < detachObservers.size(); i++) {
            ElementObserver elementObserver = detachObservers.get(i);
            if (isNull(elementObserver.observedElement())) {
                observed.add(elementObserver);
            } else {
                if (record.removedNodes.asList().contains(elementObserver.observedElement()) || isChildOfRemovedNode(record, elementObserver.attachId())) {
                    elementObserver.callback().onObserved(record);
                    elementObserver.observedElement().removeAttribute(DETACH_UID_KEY);
                    observed.add(elementObserver);
                }
            }
        }

        detachObservers.removeAll(observed);
    }

    private static boolean isChildOfRemovedNode(MutationRecord record, final String detachId) {
        List<Node> nodes = record.removedNodes.asList();

        for (int i = 0; i < nodes.size(); i++) {
            Node node = Js.uncheckedCast(nodes.get(i));
            if(Node.ELEMENT_NODE== node.nodeType) {
                if (nonNull(node.querySelector("[" + DETACH_UID_KEY + "='" + detachId + "']"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to register a callback when the element is removed from the document body
     * note that the callback will be called only once, if the element is removed and re-appended a new callback should be registered.
     *
     * @param element
     * @param callback {@link ObserverCallback}
     */
    public static void onDetach(HTMLElement element, ObserverCallback callback) {
        detachObservers.add(createObserver(element, callback, DETACH_UID_KEY));
    }

    /**
     * Helper method to register a callback when the element is appended to the document body
     * note that the callback will be called only once, if the element is appended more than once a new callback should be registered.
     *
     * @param element
     * @param callback {@link ObserverCallback}
     */
    public static void onAttach(HTMLElement element, ObserverCallback callback) {
        attachObservers.add(createObserver(element, callback, ATTACH_UID_KEY));
    }

    private static ElementObserver createObserver(HTMLElement element, ObserverCallback callback, String idAttributeName) {
        String elementId=element.getAttribute(idAttributeName);
        if(isNull(elementId)) {
            element.setAttribute(idAttributeName, Elements.createDocumentUniqueId());
        }
        return new ElementObserver() {
            @Override
            public String attachId() {
                return element.getAttribute(idAttributeName);
            }

            @Override
            public HTMLElement observedElement() {
                return element;
            }

            @Override
            public ObserverCallback callback() {
                return callback;
            }
        };
    }

    private interface ElementObserver {

        String attachId();

        HTMLElement observedElement();

        ObserverCallback callback();
    }

}