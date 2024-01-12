package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Controller
@RequestMapping("web/objects/")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Log4j2
public class CausewaySpringMvcDomainObjectHyperMediaController {

    final CausewaySpringMvcDomainObjectAdapter objectAdapter;
    final CausewaySpringMvcMetaModelAdapter metaModelAdapter;

    // //////////////////////////////////////////////////////////
    // domain object
    // //////////////////////////////////////////////////////////


    @GetMapping(path = "{domainType}/{instanceId}", headers = {"HX-Request"})
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String objectHtmx(@PathVariable String domainType, @PathVariable String instanceId, Model model) {
        return "partials/" + objectAdapter.object(domainType, instanceId, model);
    }

    @GetMapping("{domainType}/{instanceId}")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String objectHtml(@PathVariable String domainType, @PathVariable String instanceId, Model model) {
        return metaModelAdapter.fullPageResponse("layout.html", model
                , "partials/" + objectAdapter.object(domainType, instanceId, model));
    }

    @GetMapping("{domainType}/{instanceId}.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String objectXml(@PathVariable String domainType, @PathVariable String instanceId, Model model) {
        return StringUtils.replace(
                "partials/" + objectAdapter.object(domainType, instanceId, model),
                ".html", ".xml");
    }

    // Actions

    @GetMapping(path = "{domainType}/{instanceId}/actions/{actionId}", headers = {"HX-Request"})
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String actionPromptHtmx(@PathVariable String domainType, @PathVariable String instanceId,
                                   @PathVariable String actionId, Model model) {
        return "partials/" + objectAdapter.actionPrompt(domainType, instanceId, actionId, model);
    }

    @GetMapping("{domainType}/{instanceId}/actions/{actionId}")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String actionPromptHtml(@PathVariable String domainType, @PathVariable String instanceId,
                                   @PathVariable String actionId, Model model) {
        return metaModelAdapter.fullPageResponse("layout.html", model,
                "partials/" + objectAdapter.actionPrompt(domainType, instanceId, actionId, model));
    }

    @GetMapping("{domainType}/{instanceId}/actions/{actionId}.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String actionPromptXml(@PathVariable String domainType, @PathVariable String instanceId,
                                  @PathVariable String actionId, Model model) {
        return StringUtils.replace(
                "partials/" + objectAdapter.actionPrompt(domainType, instanceId, actionId, model),
                ".html", ".xml");
    }

    //=======================

    @PutMapping(path = "{domainType}/{instanceId}/actions/{actionId}/invoke", headers = {"HX-Request"})
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionIdempotentHtmx(@PathVariable String domainType, @PathVariable String instanceId,
                                             @PathVariable String actionId,
                                             @RequestParam(defaultValue = "false") boolean validateOnly,
                                             @RequestBody MultiValueMap<String, String> inputs,
                                             Model model) {
        return "partials/" + objectAdapter.invokeActionIdempotent(domainType, instanceId, actionId, validateOnly, inputs, model);
    }

    @PutMapping("{domainType}/{instanceId}/actions/{actionId}/invoke")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionIdempotentHtml(@PathVariable String domainType, @PathVariable String instanceId,
                                             @PathVariable String actionId,
                                             @RequestParam(defaultValue = "false") boolean validateOnly,
                                             @RequestBody MultiValueMap<String, String> inputs,
                                             Model model) {
        return metaModelAdapter.fullPageResponse("layout.html", model,
                "partials/" + objectAdapter.invokeActionIdempotent(domainType, instanceId, actionId, validateOnly, inputs, model));
    }

    @PutMapping("{domainType}/{instanceId}/actions/{actionId}/invoke.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String invokeActionIdempotentXml(@PathVariable String domainType, @PathVariable String instanceId,
                                            @PathVariable String actionId,
                                            @RequestParam(defaultValue = "false") boolean validateOnly,
                                            @RequestBody MultiValueMap<String, String> inputs,
                                            Model model) {
        return StringUtils.replace(
                "partials/" + objectAdapter.invokeActionIdempotent(domainType, instanceId, actionId, validateOnly, inputs, model)
                , ".html", ".xml");
    }

    //=======================

    @PostMapping(path = "{domainType}/{instanceId}/actions/{actionId}/invoke", headers = {"HX-Request"})
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionHtmx(
            @PathVariable String domainType, @PathVariable String instanceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            @RequestBody MultiValueMap<String, String> inputs,
            Model model) {
        return "partials/" + objectAdapter.invokeAction(domainType, instanceId, actionId, validateOnly, inputs, model);
    }

    @PostMapping("{domainType}/{instanceId}/actions/{actionId}/invoke.html")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionHtml(
            @PathVariable String domainType, @PathVariable String instanceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            @RequestBody MultiValueMap<String, String> inputs,
            Model model) {
        return metaModelAdapter.fullPageResponse("layout.html", model,
                "partials/" + objectAdapter.invokeAction(domainType, instanceId, actionId, validateOnly, inputs, model));
    }

    @PostMapping("{domainType}/{instanceId}/actions/{actionId}/invoke.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String invokeActionXml(
            @PathVariable String domainType, @PathVariable String instanceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            @RequestBody MultiValueMap<String, String> inputs,
            Model model) {
        return StringUtils.replace(
                "partials/" + objectAdapter.invokeAction(domainType, instanceId, actionId, validateOnly, inputs, model),
                ".html", ".xml");
    }
    //======================= meta

    @GetMapping("{domainType}/{instanceId}/object-icon")
    @Produces({
            "image/png",
            "image/gif",
            "image/jpeg",
            "image/jpg",
            "image/svg+xml"
    })
    public Response objectIcon(@PathVariable final String domainType, @PathVariable final String instanceId) {
        val found = metaModelAdapter.getObject(domainType, instanceId);
        if (found.isPresent()) {
            val objectIcon = found.get().getIcon();
            return Response.ok(objectIcon.asBytes(), objectIcon.getMimeType().getBaseType()).build();
        } else {
            return Response.noContent().build();//default icon?
        }
    }
}