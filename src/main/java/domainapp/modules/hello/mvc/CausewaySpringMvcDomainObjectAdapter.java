package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedCollection;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.ManagedObjects;
import org.apache.causeway.core.metamodel.spec.feature.MixedIn;
import org.apache.causeway.core.metamodel.spec.feature.ObjectAction;
import org.apache.causeway.core.metamodel.spec.feature.ObjectAssociation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Log4j2
public class CausewaySpringMvcDomainObjectAdapter {

    final CausewaySpringMvcMetaModelAdapter metaModelAdapter;


    // //////////////////////////////////////////////////////////
    // domain object
    // //////////////////////////////////////////////////////////

    public String object(@PathVariable String domainType, @PathVariable String instanceId, Model model) {
        val found = metaModelAdapter.getObject(domainType, instanceId);
        if (found.isPresent()) {
            val object = found.get();
            model.addAttribute("object", object);
            val topBarActions = ObjectAction.Util.streamTopBarActions(object);
            model.addAttribute("topBarActions", topBarActions);
            val propertyGroups = getPropertyGroups(object);
            model.addAttribute("propertyGroups", propertyGroups);
            val collections = getCollections(object);
            model.addAttribute("collections", collections);
        } else {
            return "objects :: object-not-found";
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
}
