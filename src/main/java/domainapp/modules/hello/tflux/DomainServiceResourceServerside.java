/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package domainapp.modules.hello.tflux;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.applib.id.LogicalType;
import org.apache.causeway.applib.services.bookmark.Bookmark;
import org.apache.causeway.applib.services.iactnlayer.InteractionContext;
import org.apache.causeway.applib.services.iactnlayer.InteractionService;
import org.apache.causeway.applib.services.user.UserMemento;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.context.MetaModelContext;
import org.apache.causeway.core.metamodel.facets.object.domainservice.DomainServiceFacet;
import org.apache.causeway.core.metamodel.interactions.managed.ActionInteraction;
import org.apache.causeway.core.metamodel.interactions.managed.InteractionVeto;
import org.apache.causeway.core.metamodel.interactions.managed.MemberInteraction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.spec.feature.ObjectAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("mvc/services")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Log4j2
public class DomainServiceResourceServerside {
    private static final Predicate<ManagedObject> NATURE_REST = (final ManagedObject input) -> {
        return DomainServiceFacet.isContributing(input.getSpecification());
    };


    public static final String VALIDATE_ONLY = "x-ro-validate-only";

    private final MetaModelContext metaModelContext;
    private final InteractionService interactionService;

    @GetMapping("/")
    public String services(Model model) {
        val serviceAdapters = metaModelContext.streamServiceAdapters().filter(NATURE_REST);
        model.addAttribute("serviceAdapters", serviceAdapters);

        return "services :: services";
    }

    // //////////////////////////////////////////////////////////
    // domain service
    // //////////////////////////////////////////////////////////

    @GetMapping("/{serviceId}")
    public String service(@PathVariable String serviceId, Model model) {

        val serviceAdapter = getServiceAdapter(serviceId);

        model.addAttribute("serviceAdapter", serviceAdapter);

        return "services :: service";
    }

    // //////////////////////////////////////////////////////////
    // domain service action
    // //////////////////////////////////////////////////////////

    @GetMapping("/{serviceId}/actions/{actionId}")
    public String actionPrompt(
            @PathVariable String serviceId,
            @PathVariable String actionId,
            Model model) {

        val serviceAction = getServiceAdapter(serviceId);
        val action = ActionInteraction.start(serviceAction, actionId, Where.OBJECT_FORMS).checkVisibility()
                .checkUsability(MemberInteraction.AccessIntent.ACCESS).checkSemanticConstraint(ActionInteraction.SemanticConstraint.NONE)
                .getManagedActionElseFail();

        model.addAttribute("action", action);

        return "services :: action";
    }

    // //////////////////////////////////////////////////////////
    // domain service action invoke
    // //////////////////////////////////////////////////////////

    @GetMapping("/{serviceId}/actions/{actionId}/invoke")
    public String invokeActionQueryOnly(
            @PathVariable String serviceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            final Model model) {
        BindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), actionId);
        return invokeMethod(ActionInteraction.SemanticConstraint.SAFE, serviceId, actionId, validateOnly,
                bindingResult, model);

    }


    @PutMapping("/{serviceId}/actions/{actionId}/invoke")
    public String invokeActionIdempotent(
            @PathVariable String serviceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            final Model model) {
        BindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), actionId);
        return invokeMethod(ActionInteraction.SemanticConstraint.IDEMPOTENT, serviceId, actionId, validateOnly,
                bindingResult, model);

    }

    @PostMapping("/{serviceId}/actions/{actionId}/invoke")
    public String invokeAction(
            @PathVariable String serviceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            final Model model) {
        BindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), actionId);
        return invokeMethod(ActionInteraction.SemanticConstraint.NONE, serviceId, actionId, validateOnly,
                bindingResult, model);

    }

    private String invokeMethod(ActionInteraction.SemanticConstraint semanticConstraint,
                                String serviceId, String actionId, boolean validateOnly,
                                BindingResult bindingResult, Model model) {

        val svenMockup = UserMemento.ofNameAndRoleNames("mvc-user", "causeway-ext-secman-admin", "demo", "iniRealm:admin", "org.apache.causeway.security.AUTHORIZED_USER");
        val interactionContextMockup = InteractionContext.ofUserWithSystemDefaults(svenMockup);

        val actionResult = interactionService.call(interactionContextMockup, () -> {

            val serviceAction = getServiceAdapter(serviceId);
            val actionInteraction = ActionInteraction.start(serviceAction, actionId, Where.STANDALONE_TABLES);
            //TODO .checkVisibility()
            //TODO .checkUsability(MemberInteraction.AccessIntent.MUTATE).checkSemanticConstraint(semanticConstraint);

            return invokeAction(actionId, actionInteraction, validateOnly, bindingResult, model);
        });

        if (bindingResult.hasErrors()) {
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
    }


    private ManagedObject getServiceAdapter(final @Nullable String serviceIdOrAlias) {

        final ManagedObject serviceAdapter = metaModelContext.getSpecificationLoader()
                .lookupLogicalType(serviceIdOrAlias)
                .map(LogicalType::getLogicalTypeName)
                .map(metaModelContext::lookupServiceAdapterById)
                .orElse(null);

        if (serviceAdapter == null) {
            throw new NotFoundException(String.format("Could not locate service '%s'", serviceIdOrAlias));
        }
        return serviceAdapter;
    }

    private ActionInteraction.Result invokeAction(
            final @NonNull String actionId,
            final @NonNull ActionInteraction actionInteraction,
            final @NonNull boolean validateOnly,
            final @NonNull BindingResult bindingResult,
            final @NonNull Model model) {

        val pendingArgs = actionInteraction.startParameterNegotiation().orElse(null);

        if (pendingArgs == null) {
            // no such action or not visible or not usable
            throw new IllegalArgumentException("no such action or not visible or not usable : " + actionId); // unexpected code reach
        }

        val hasParams = pendingArgs.getParamCount() > 0;


        if (hasParams) {

            // parse parameters ...

            val actionMetaModel = pendingArgs.getHead().getMetaModel();

            val argAdapters = parseArguments(actionMetaModel, model);
            pendingArgs.setParamValues(argAdapters);

            // validate parameters ...

            val individualParamConsents = pendingArgs.validateParameterSetForParameters();

            pendingArgs.getParamModels().zip(individualParamConsents, (managedParam, consent) -> {
                if (consent.isVetoed()) {
                    val veto = InteractionVeto.actionParamInvalid(consent);
                    bindingResult.rejectValue(managedParam.getIdentifier().getMemberLogicalName(),
                            "invalid-parameter", veto.getReasonAsString().orElse("Invalid Value"));
                }
            });

        }

        val actionConsent = pendingArgs.validateParameterSetForAction();
        if (actionConsent.isVetoed()) {
            bindingResult.reject("invalid-parameter-set", actionConsent.getReasonAsString().orElse("Invalid Parameters for this action"));
        }

        if (!bindingResult.hasErrors()) {
            if (validateOnly) {
                return ActionInteraction.Result.of(
                        actionInteraction.getManagedAction().orElse(null),
                        pendingArgs.getParamValues(),
                        ManagedObject.empty(actionInteraction.getMetamodel().orElseThrow().getReturnType()));
            }

            val resultOrVeto = actionInteraction.invokeWith(pendingArgs);

            if (resultOrVeto.isFailure()) {
                bindingResult.reject("action-execution-failede", resultOrVeto.getFailureElseFail().getReasonAsString().orElse("Invalid Parameters for this action"));
                return null;//TODO better way to notify that action failed to run
            } else {

                return ActionInteraction.Result.of(
                        actionInteraction.getManagedActionElseFail(),
                        pendingArgs.getParamValues(),
                        resultOrVeto.getSuccessElseFail());
            }
        } else {
            return null;//TODO better way to notify that action has not ran
        }
    }

    private static final Pattern OBJECT_OID = Pattern.compile(".*objects\\/([^/]+)\\/(.+)");

    public static Can<ManagedObject> parseArguments(
            final ObjectAction action,
            final Model model) {

        val parameters = action.getParameters();
        val arguments = model.asMap().entrySet();
        val parsedArguments = new ArrayList<ManagedObject>();

        int index = 0;
        for (val arg : arguments) {
            val argRepr = arg.getValue();
            val paramMeta = parameters.getElseFail(index);
            val paramSpec = paramMeta.getElementType();
            if ((paramMeta.isOptional() && argRepr == null)) {
                parsedArguments.add(ManagedObject.empty(paramSpec));
            } else {
                final Matcher matcher = OBJECT_OID.matcher((String) argRepr);
                if (matcher.matches()) {
                    String domainType = matcher.group(1);
                    String instanceId = matcher.group(2);
                    parsedArguments.add(action.getMetaModelContext().getObjectManager().loadObjectElseFail(Bookmark.forLogicalTypeNameAndIdentifier(domainType, instanceId)));
                } else {
                    parsedArguments.add(ManagedObject.value(paramSpec, argRepr));
                }
            }
            index++;
        }
        ;

        for (; index <= parameters.size(); index++) {
            val paramMeta = parameters.getElseFail(index);
            val paramSpec = paramMeta.getElementType();
            parsedArguments.add(ManagedObject.empty(paramSpec));
        }
        return Can.ofCollection(parsedArguments);
    }
}
