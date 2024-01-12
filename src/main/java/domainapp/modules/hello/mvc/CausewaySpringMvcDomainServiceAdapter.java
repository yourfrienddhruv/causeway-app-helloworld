package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.causeway.applib.services.bookmark.BookmarkService;
import org.apache.causeway.applib.services.xactn.TransactionService;
import org.apache.causeway.core.metamodel.interactions.managed.ActionInteraction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Log4j2
public class CausewaySpringMvcDomainServiceAdapter {

    final CausewaySpringMvcMetaModelAdapter metaModelAdapter;
    final TransactionService transactionService;
    final BookmarkService bookmarkService;


    /*
     * Not used for UIs, use menu instead
     *   @GetMapping
     */
    public String services(Model model) {
        val serviceAdapters = metaModelAdapter.getServices();
        model.addAttribute("serviceAdapters", serviceAdapters);

        return "services.html :: services";
    }

    // //////////////////////////////////////////////////////////
    // domain service
    // //////////////////////////////////////////////////////////

    public String service(String serviceId, Model model) {
        val serviceAdapter = metaModelAdapter.getServiceAdapter(serviceId).orElseThrow();
        val serviceActions = metaModelAdapter.getServiceActions(serviceAdapter);
        model.addAttribute("serviceAdapter", serviceAdapter);
        model.addAttribute("serviceActions", serviceActions);

        return "services :: service";
    }

    // //////////////////////////////////////////////////////////
    // domain service action
    // //////////////////////////////////////////////////////////

    public String actionPrompt(String serviceId, String actionId, Model model) {
        val serviceAction = metaModelAdapter.getServiceAdapter(serviceId).orElseThrow();
        val possibleAction = metaModelAdapter.getActionInteraction(serviceAction, actionId).getManagedAction();

        if (possibleAction.isPresent()) {
            var action = possibleAction.get();
            model.addAttribute("action", action);
            return "services :: action";
        } else {
            model.addAttribute("action", serviceAction);
            return "services :: action-unavailable";
        }
    }

    // //////////////////////////////////////////////////////////
    // domain service action invoke
    // //////////////////////////////////////////////////////////

    public String invokeActionQueryOnly(String serviceId, String actionId, boolean validateOnly,
                                        MultiValueMap<String, String> inputs, Model model) {
        BindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), actionId);
        return invokeMethod(ActionInteraction.SemanticConstraint.SAFE, serviceId, actionId, validateOnly,
                bindingResult, inputs, model);
    }


    public String invokeActionIdempotent(String serviceId, String actionId, boolean validateOnly,
                                         MultiValueMap<String, String> inputs, Model model) {
        BindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), actionId);
        return invokeMethod(ActionInteraction.SemanticConstraint.IDEMPOTENT, serviceId, actionId, validateOnly,
                bindingResult, inputs, model);

    }

    public String invokeAction(String serviceId, String actionId, boolean validateOnly,
                               MultiValueMap<String, String> inputs, Model model) {
        BindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), actionId);
        return invokeMethod(ActionInteraction.SemanticConstraint.NONE, serviceId, actionId, validateOnly,
                bindingResult, inputs, model);
    }

    private String invokeMethod(ActionInteraction.SemanticConstraint semanticConstraint,
                                String serviceId, String actionId, boolean validateOnly,
                                BindingResult bindingResult, MultiValueMap<String, String> inputs, Model model) {
        val serviceAction = metaModelAdapter.getServiceAdapter(serviceId).orElseThrow();
        val actionInteraction = metaModelAdapter.getActionInteraction(serviceAction, actionId);

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
                return "services :: action";
            } else {
                model.addAttribute("actionResult", actionResult);
                if (actionResult.getActionReturnedObject().getSpecialization().isEmpty()) {
                    return "services :: actionResultEmpty";
                } else if (actionResult.getActionReturnedObject().getPojo() instanceof Collection) {
                    return "services :: actionResultList";
                } else {
                    return "services :: actionResultObject";
                }
            }
        } catch (Exception executionException) {
            bindingResult.rejectValue(actionId,
                    "execution-failed", "Execution Failed : " + ExceptionUtils.getRootCauseMessage(executionException));
            return "services :: action";
        }
    }
}
