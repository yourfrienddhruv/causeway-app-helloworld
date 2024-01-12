package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.applib.services.bookmark.BookmarkService;
import org.apache.causeway.applib.services.xactn.TransactionService;
import org.apache.causeway.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.causeway.core.metamodel.interactions.managed.ActionInteraction;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedCollection;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.ManagedObjects;
import org.apache.causeway.core.metamodel.spec.feature.MixedIn;
import org.apache.causeway.core.metamodel.spec.feature.ObjectAction;
import org.apache.causeway.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Log4j2
public class CausewaySpringMvcDomainObjectAdapter {

    final CausewaySpringMvcMetaModelAdapter metaModelAdapter;
    final TransactionService transactionService;
    final BookmarkService bookmarkService;


    // //////////////////////////////////////////////////////////
    // domain object
    // //////////////////////////////////////////////////////////

    public String object(String domainType, String instanceId, Model model) {
        val found = metaModelAdapter.getObject(domainType, instanceId);
        if (found.isPresent()) {
            val object = found.get();
            model.addAttribute("object", object);
            if (object.getSpecialization().isEmpty()) {
                return "objects :: object-not-found";
            } else {
                val topBarActions = ObjectAction.Util.streamTopBarActions(object).collect(Collectors.toList());
                model.addAttribute("topBarActions", topBarActions);
                val propertyGroups = getPropertyGroups(object);
                model.addAttribute("propertyGroups", propertyGroups);
                val collections = getCollections(object);
                model.addAttribute("collections", collections);
            }
        } else {
            return "objects :: object-type-not-found";
        }
        return "objects :: object";
    }


    private HashMap<String, Collection<Map<String, ManagedObject>>> getCollections(final ManagedObject owner) {
        val collections = new HashMap<String, Collection<Map<String, ManagedObject>>>();
        owner.getSpecification()
                .streamCollections(MixedIn.INCLUDED)
                .forEach(association -> {
                    val visibility = association.isVisible(owner, InteractionInitiatedBy.USER, Where.OBJECT_FORMS);
                    val collection = new ArrayList<Map<String, ManagedObject>>();
                    if (visibility.isAllowed()) {
                        collections.put(association.getId(), collection);

                        val managedCollection = ManagedCollection.of(owner, association, Where.OBJECT_FORMS);
                        managedCollection.streamElements(InteractionInitiatedBy.USER).
                                forEach(e -> collection.add(getElements(e)));
                    }//else skip hidden
                });
        return collections;
    }

    private HashMap<String, HashMap<String, ManagedObject>> getPropertyGroups(final ManagedObject parent) {
        val associations = parent.getSpecification().streamProperties(MixedIn.INCLUDED).collect(Collectors.toList());
        val associationGroups = ObjectAssociation.Util.groupByMemberOrderName(
                associations.stream().map(ObjectAssociation.class::cast).collect(Collectors.toList()));
        val propertyGroups = new HashMap<String, HashMap<String, ManagedObject>>();
        associationGroups.forEach((groupId, associationList) -> {
            val properties = getProperties(associationList, parent);
            if (!properties.isEmpty()) {
                propertyGroups.put(groupId, properties);
            }//else skip empty groups
        });
        return propertyGroups;
    }

    private HashMap<String, ManagedObject> getProperties(
            final List<ObjectAssociation> associationList,
            final ManagedObject parent) {
        val properties = new HashMap<String, ManagedObject>();
        associationList.forEach(property -> {
            val visibility = property.isVisible(parent, InteractionInitiatedBy.USER, Where.OBJECT_FORMS);
            if (visibility.isAllowed()) {
                properties.put(property.getId(), getValue(property, parent));
            }//else skip hidden
        });
        return properties;
    }

    private HashMap<String, ManagedObject> getElements(final ManagedObject parent) {
        val associations = parent.getSpecification().streamProperties(MixedIn.INCLUDED)
                .collect(Collectors.toList());
        val properties = new HashMap<String, ManagedObject>();
        associations.forEach(property -> {
            val visibility = property.isVisible(parent, InteractionInitiatedBy.USER, Where.OBJECT_FORMS);
            if (visibility.isAllowed()) {
                properties.put(property.getId(), getValue(property, parent));
            }//else skip hidden
        });
        return properties;
    }

    private ManagedObject getValue(final ObjectAssociation property, final ManagedObject parent) {
        val valueAdapterIfAny = property.get(parent, InteractionInitiatedBy.USER);

        // use the runtime type if we have a value, otherwise fallback to the compile time type of the member
        val valueAdapter = ManagedObjects.isSpecified(valueAdapterIfAny)
                ? valueAdapterIfAny
                : ManagedObject.empty(property.getElementType());
        return valueAdapter;
    }

    // //////////////////////////////////////////////////////////
    // domain object action
    // //////////////////////////////////////////////////////////

    public String actionPrompt(String domainType, String instanceId, String actionId, Model model) {
        val owner = metaModelAdapter.getObject(domainType, instanceId).orElseThrow();
        val possibleAction = metaModelAdapter.getActionInteraction(owner, actionId).getManagedAction();

        if (possibleAction.isPresent()) {
            var action = possibleAction.get();
            model.addAttribute("action", action);
            return "objects :: action";
        } else {
            model.addAttribute("action", owner);
            return "objects :: action-unavailable";
        }
    }


    // //////////////////////////////////////////////////////////
    // domain service action invoke
    // //////////////////////////////////////////////////////////

    public String invokeActionQueryOnly(String domainType, String instanceId, String actionId, boolean validateOnly,
                                        MultiValueMap<String, String> inputs, Model model) {
        BindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), actionId);
        return invokeMethod(ActionInteraction.SemanticConstraint.SAFE, domainType, instanceId, actionId, validateOnly,
                bindingResult, inputs, model);
    }


    public String invokeActionIdempotent(String domainType, String instanceId, String actionId, boolean validateOnly,
                                         MultiValueMap<String, String> inputs, Model model) {
        BindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), actionId);
        return invokeMethod(ActionInteraction.SemanticConstraint.IDEMPOTENT, domainType, instanceId, actionId, validateOnly,
                bindingResult, inputs, model);

    }

    public String invokeAction(String domainType, String instanceId, String actionId, boolean validateOnly,
                               MultiValueMap<String, String> inputs, Model model) {
        BindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), actionId);
        return invokeMethod(ActionInteraction.SemanticConstraint.NONE, domainType, instanceId, actionId, validateOnly,
                bindingResult, inputs, model);
    }

    private String invokeMethod(ActionInteraction.SemanticConstraint semanticConstraint,
                                String domainType, String instanceId, String actionId, boolean validateOnly,
                                BindingResult bindingResult, MultiValueMap<String, String> inputs, Model model) {
        val owner = metaModelAdapter.getObject(domainType, instanceId).orElseThrow();
        val actionInteraction = metaModelAdapter.getActionInteraction(owner, actionId);

        try {
            val actionResult = metaModelAdapter.invokeAction(actionId, actionInteraction, validateOnly, bindingResult, inputs);
            val action = actionInteraction.getManagedAction().orElseThrow();
            model.addAttribute("action", action);
            model.addAttribute("actionInteraction", actionInteraction);
            model.addAttribute("bindingResult", bindingResult);

            //flush to catch DB constrains error by triggering JPA/JDO flush here, so we can catch and report
            transactionService.flushTransaction();
            log.info("getReturnType {} {}", action.getMetaModel().getReturnType(), bookmarkService);

            if (bindingResult.hasErrors() || actionResult == null) {
                //no invocation thus no actionResult
                return "objects :: action";
            } else {
                model.addAttribute("actionResult", actionResult);
                if (actionResult.getActionReturnedObject().getSpecialization().isEmpty()) {
                    return "objects :: actionResultEmpty";
                } else if (actionResult.getActionReturnedObject().getPojo() instanceof Collection) {
                    return "objects :: actionResultList";
                } else {
                    return "objects :: actionResultObject";
                }
            }
        } catch (Exception executionException) {
            bindingResult.rejectValue(actionId,
                    "execution-failed", "Execution Failed : " + ExceptionUtils.getRootCauseMessage(executionException));
            return "objects :: action";
        }
    }
}
