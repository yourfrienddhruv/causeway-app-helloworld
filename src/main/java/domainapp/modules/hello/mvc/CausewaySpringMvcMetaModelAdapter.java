package domainapp.modules.hello.mvc;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.causeway.applib.annotation.DomainServiceLayout;
import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.applib.id.LogicalType;
import org.apache.causeway.applib.layout.component.ServiceActionLayoutData;
import org.apache.causeway.applib.layout.menubars.bootstrap.BSMenu;
import org.apache.causeway.applib.layout.menubars.bootstrap.BSMenuBar;
import org.apache.causeway.applib.layout.menubars.bootstrap.BSMenuSection;
import org.apache.causeway.applib.services.bookmark.Bookmark;
import org.apache.causeway.applib.services.bookmark.BookmarkService;
import org.apache.causeway.applib.services.iactnlayer.InteractionService;
import org.apache.causeway.applib.services.menu.MenuBarsService;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.io.UrlUtils;
import org.apache.causeway.core.metamodel.context.MetaModelContext;
import org.apache.causeway.core.metamodel.facets.object.domainservice.DomainServiceFacet;
import org.apache.causeway.core.metamodel.interactions.managed.ActionInteraction;
import org.apache.causeway.core.metamodel.interactions.managed.InteractionVeto;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.interactions.managed.MemberInteraction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.spec.ActionScope;
import org.apache.causeway.core.metamodel.spec.feature.MixedIn;
import org.apache.causeway.core.metamodel.spec.feature.ObjectAction;
import org.apache.causeway.viewer.commons.applib.services.branding.BrandingUiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Log4j2
public class CausewaySpringMvcMetaModelAdapter {

    private final MetaModelContext metaModelContext;
    private final InteractionService interactionService;

    private static final Predicate<ManagedObject> NATURE_REST
            = (final ManagedObject input) -> DomainServiceFacet.isContributing(input.getSpecification());

    public Collection<ManagedObject> getServices() {
        return metaModelContext.streamServiceAdapters().filter(NATURE_REST).collect(Collectors.toList());
    }

    // //////////////////////////////////////////////////////////
    // domain service
    // //////////////////////////////////////////////////////////


    public Optional<ManagedObject> getServiceAdapter(final @NotNull String serviceIdOrAlias) {
        return metaModelContext.getSpecificationLoader()
                .lookupLogicalType(serviceIdOrAlias)
                .map(LogicalType::getLogicalTypeName)
                .map(metaModelContext::lookupServiceAdapterById);
    }

    public List<ObjectAction> getServiceActions(final @NotNull ManagedObject serviceAdapter) {
        return serviceAdapter.getSpecification().streamActions(ActionScope.PRODUCTION, MixedIn.INCLUDED)
                .collect(Collectors.toList());
    }

    // //////////////////////////////////////////////////////////
    // domain service action
    // //////////////////////////////////////////////////////////

    public ActionInteraction getActionInteraction(@NotNull ManagedObject serviceAction, @NotNull String actionId) {
        return ActionInteraction.start(serviceAction, actionId, Where.OBJECT_FORMS).checkVisibility()
                .checkUsability(MemberInteraction.AccessIntent.ACCESS).
                checkSemanticConstraint(ActionInteraction.SemanticConstraint.NONE);
    }

    // //////////////////////////////////////////////////////////
    // domain service action invoke
    // //////////////////////////////////////////////////////////

    public ActionInteraction.Result invokeAction(
            final @NonNull String actionId,
            final @NonNull ActionInteraction actionInteraction,
            final @NonNull boolean validateOnly,
            final @NonNull BindingResult bindingResult,
            final @NonNull MultiValueMap<String, String> inputs) {
        val interactionVeto = actionInteraction.getInteractionVeto();
        if (interactionVeto.isPresent()) {
            bindingResult.rejectValue(actionId,
                    "invalid-invocation", interactionVeto.get().getReasonAsString().orElse("Action not allowed to be executed"));
            return null;
        }

        val pendingArgs = actionInteraction.startParameterNegotiation().orElse(null);

        if (pendingArgs == null) {
            // no such action or not visible or not usable
            bindingResult.rejectValue(actionId,
                    "action-unavailable", actionId + " action is not available");
            return null;
        }

        val hasParams = pendingArgs.getParamCount() > 0;


        if (hasParams) {

            // parse parameters ...

            val actionMetaModel = pendingArgs.getHead().getMetaModel();

            val argAdapters = parseArguments(actionMetaModel, inputs);
            pendingArgs.setParamValues(argAdapters);

            // validate parameters ...

            val individualParamConsents = pendingArgs.validateParameterSetForParameters();

            pendingArgs.getParamModels().zip(individualParamConsents, (managedParam, consent) -> {
                if (consent.isVetoed()) {
                    val veto = InteractionVeto.actionParamInvalid(consent);
                    //TODO find actual member and set that as field to show errors in exactly field
                    bindingResult.rejectValue(managedParam.getFriendlyName(),
                            "invalid-parameter", veto.getReasonAsString().orElse("Invalid Value"));
                }
            });

        }

        val actionConsent = pendingArgs.validateParameterSetForAction();
        if (actionConsent.isVetoed()) {
            bindingResult.reject("invalid-parameter-set",
                    actionConsent.getReasonAsString()
                            .orElse("Invalid Parameters for this action"));
        }

        if (!bindingResult.hasErrors()) {
            if (validateOnly) {
                return ActionInteraction.Result.of(
                        actionInteraction.getManagedAction().orElse(null),
                        pendingArgs.getParamValues(),
                        ManagedObject.empty(actionInteraction.getMetamodel()
                                .orElseThrow().getReturnType()));
            }

            val resultOrVeto = actionInteraction.invokeWith(pendingArgs);

            if (resultOrVeto.isFailure()) {
                bindingResult.reject("action-execution-failed",
                        resultOrVeto.getFailureElseFail().getReasonAsString()
                                .orElse("Invalid Parameters for this action"));
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
            final MultiValueMap<String, String> inputs) {

        val parameters = action.getParameters();
        val arguments = inputs.entrySet();
        val parsedArguments = new ArrayList<ManagedObject>();

        int index = 0;
        for (val arg : arguments) {
            if (StringUtils.startsWithIgnoreCase(arg.getKey(), "_")) {
                //ignore technical parameters required for spring-web e.g. _csrf
            } else {
                //FIXME handle multiple values
                val argRepr = arg.getValue().get(0);

                val paramMeta = parameters.getElseFail(index);
                val paramSpec = paramMeta.getElementType();
                if ((paramMeta.isOptional() && argRepr == null)) {
                    parsedArguments.add(ManagedObject.empty(paramSpec));
                } else {
                    final Matcher matcher = OBJECT_OID.matcher((String) argRepr);
                    if (matcher.matches()) {
                        String domainType = matcher.group(1);
                        String instanceId = matcher.group(2);
                        parsedArguments.add(action.getMetaModelContext().getObjectManager()
                                .loadObjectElseFail(Bookmark.forLogicalTypeNameAndIdentifier(domainType, instanceId)));
                    } else {
                        parsedArguments.add(ManagedObject.value(paramSpec, argRepr));
                    }
                }
                index++;
            }
        }

        for (; index <= parameters.size(); index++) {
            val paramMeta = parameters.getElseFail(index);
            val paramSpec = paramMeta.getElementType();
            parsedArguments.add(ManagedObject.empty(paramSpec));
        }
        return Can.ofCollection(parsedArguments);
    }

    //-----HELPERS to simplify complicated bean look-ups by type from SPeL ----//
    // e.g. #{beanFactory.getBean(T(org.apache.causeway.applib.services.bookmark.BookmarkService)).bookmarkForElseFail(x)}

    private final BookmarkService bookmarkService;

    public String getBookmarkAsUrl(Object o) {
        val bookmark = bookmarkService.bookmarkForElseFail(o);
        return "/web/objects/" + bookmark.getLogicalTypeName() + '/' + bookmark.getIdentifier();
    }

    protected Optional<ManagedObject> getObject(
            final String domainType,
            final String instanceIdEncoded) {
        final String instanceIdDecoded = UrlUtils.urlDecodeUtf8(instanceIdEncoded);

        val bookmark = Bookmark.forLogicalTypeNameAndIdentifier(domainType, instanceIdDecoded);
        return metaModelContext.getObjectManager().loadObject(bookmark);
    }

    // //////////////////////////////////////////////////////////
    // menus
    // //////////////////////////////////////////////////////////

    private final BrandingUiService brandingUiService;


    public String fullPageResponse(String layoutTemplate, Model model, String partialTemplate) {
        val user = interactionService.currentInteractionContextElseFail().getUser();
        model.addAttribute("applicationUserName", user.getName());
        model.addAttribute("applicationUserNamePrefix", user.getName().substring(0, 1));
        model.addAttribute("applicationUserAvatarUrl", user.getAvatarUrl());
        model.addAttribute("applicationName", getApplicationName());
        model.addAttribute("applicationLogo", getApplicationLogo());
        model.addAttribute("menuBarPrimary", getMenuBar(DomainServiceLayout.MenuBar.PRIMARY));
        model.addAttribute("menuBarSecondary", getMenuBar(DomainServiceLayout.MenuBar.SECONDARY));
        model.addAttribute("menuBarTertiary", getMenuBar(DomainServiceLayout.MenuBar.TERTIARY));

        model.addAttribute("partial", partialTemplate);
        return layoutTemplate;
    }

    String getApplicationName() {
        return brandingUiService.getHeaderBranding().getName().orElse("App");
    }

    String getApplicationLogo() {
        return '/' + brandingUiService.getHeaderBranding().getLogoHref().orElse("images/favicon.png");
    }


    private final MenuBarsService menuBarsService;

    LinkedHashMap<BSMenu,
            LinkedHashMap<BSMenuSection,
                    LinkedHashMap<ServiceActionLayoutData, ManagedAction>>> getMenuBar(
            DomainServiceLayout.MenuBar menuBarType) {
        val menuBar = (BSMenuBar) menuBarsService.menuBars().menuBarFor(menuBarType);
        if (menuBar != null) {
            return getMenus(menuBar);
        } else {
            return new LinkedHashMap<BSMenu,
                    LinkedHashMap<BSMenuSection, LinkedHashMap<ServiceActionLayoutData, ManagedAction>>>();
        }
    }

    LinkedHashMap<BSMenu,
            LinkedHashMap<BSMenuSection,
                    LinkedHashMap<ServiceActionLayoutData, ManagedAction>>> getMenus(BSMenuBar bsMenuBar) {
        val menuBar = new LinkedHashMap<BSMenu,
                LinkedHashMap<BSMenuSection,
                        LinkedHashMap<ServiceActionLayoutData, ManagedAction>>>();
        for (val bsMenu : bsMenuBar.getMenus()) {
            val sections = new LinkedHashMap<BSMenuSection,
                    LinkedHashMap<ServiceActionLayoutData, ManagedAction>>();
            for (val bsSection : bsMenu.getSections()) {
                val actions = new LinkedHashMap<ServiceActionLayoutData, ManagedAction>();
                for (val serviceActionLayoutData : bsSection.getServiceActions()) {
                    val serviceAction = getServiceAdapter(
                            serviceActionLayoutData.getLogicalTypeName());
                    if (serviceAction.isPresent()) {
                        val possibleAction = getActionInteraction(serviceAction.get(),
                                serviceActionLayoutData.getId()).getManagedAction();
                        possibleAction.ifPresent(managedAction
                                -> actions.put(serviceActionLayoutData, managedAction));
                    }//else not visible
                }
                if (!actions.isEmpty()) {
                    sections.put(bsSection, actions);
                }//skip empty
            }
            if (!sections.isEmpty()) {
                menuBar.put(bsMenu, sections);
            }//skip empty
        }
        return menuBar;
    }
}
