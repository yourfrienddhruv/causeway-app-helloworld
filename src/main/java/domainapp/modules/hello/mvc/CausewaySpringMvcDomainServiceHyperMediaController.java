package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Controller
@Order(PriorityPrecedence.MIDPOINT)
@RequestMapping("web/services/")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Log4j2
public class CausewaySpringMvcDomainServiceHyperMediaController {

    private final CausewaySpringMvcDomainServiceAdapter serviceAdapter;
    private final CausewaySpringMvcMetaModelAdapter metaModelAdapter;

    @GetMapping(path = "/{serviceId}/actions/{actionId}", headers = {"HX-Request"})
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String actionPromptHtmx(@PathVariable String serviceId, @PathVariable String actionId, Model model) {
        return "partials/" + serviceAdapter.actionPrompt(serviceId, actionId, model);
    }

    @GetMapping("/{serviceId}/actions/{actionId}")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String actionPromptHtml(@PathVariable String serviceId, @PathVariable String actionId, Model model) {
        return metaModelAdapter.fullPageResponse("layout.html", model,
                "partials/" + serviceAdapter.actionPrompt(serviceId, actionId, model));
    }

    @GetMapping("/{serviceId}/actions/{actionId}.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String actionPromptXml(@PathVariable String serviceId, @PathVariable String actionId, Model model) {
        return StringUtils.replace(
                "partials/" + serviceAdapter.actionPrompt(serviceId, actionId, model),
                ".html", ".xml");
    }

    //=======================

    @GetMapping(path = "/{serviceId}/actions/{actionId}/invoke", headers = {"HX-Request"})
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionQueryOnlyHtmx(@PathVariable String serviceId, @PathVariable String actionId,
                                            @RequestParam(defaultValue = "false") boolean validateOnly,
                                            @RequestParam MultiValueMap<String, String> inputs,
                                            Model model) {
        return "partials/" + serviceAdapter.invokeActionQueryOnly(serviceId, actionId, validateOnly, inputs, model);
    }

    @GetMapping("/{serviceId}/actions/{actionId}/invoke")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionQueryOnlyHtml(@PathVariable String serviceId, @PathVariable String actionId,
                                            @RequestParam(defaultValue = "false") boolean validateOnly,
                                            @RequestParam MultiValueMap<String, String> inputs,
                                            Model model) {
        return metaModelAdapter.fullPageResponse("layout.html", model,
                "partials/" + serviceAdapter.invokeActionQueryOnly(serviceId, actionId, validateOnly, inputs, model));
    }

    @GetMapping("/{serviceId}/actions/{actionId}/invoke.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String invokeActionQueryOnlyXml(@PathVariable String serviceId, @PathVariable String actionId,
                                           @RequestParam(defaultValue = "false") boolean validateOnly,
                                           @RequestParam MultiValueMap<String, String> inputs,
                                           Model model) {
        return StringUtils.replace(
                "partials/" + serviceAdapter.invokeActionQueryOnly(serviceId, actionId, validateOnly, inputs, model),
                ".html", ".xml");
    }

    //=======================

    @PutMapping(path = "/{serviceId}/actions/{actionId}/invoke", headers = {"HX-Request"})
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionIdempotentHtmx(@PathVariable String serviceId, @PathVariable String actionId,
                                             @RequestParam(defaultValue = "false") boolean validateOnly,
                                             @RequestBody MultiValueMap<String, String> inputs,
                                             Model model) {
        return "partials/" + serviceAdapter.invokeActionIdempotent(serviceId, actionId, validateOnly, inputs, model);
    }

    @PutMapping("/{serviceId}/actions/{actionId}/invoke")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionIdempotentHtml(@PathVariable String serviceId, @PathVariable String actionId,
                                             @RequestParam(defaultValue = "false") boolean validateOnly,
                                             @RequestBody MultiValueMap<String, String> inputs,
                                             Model model) {
        return metaModelAdapter.fullPageResponse("layout.html", model,
                "partials/" + serviceAdapter.invokeActionIdempotent(serviceId, actionId, validateOnly, inputs, model));
    }

    @PutMapping("/{serviceId}/actions/{actionId}/invoke.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String invokeActionIdempotentXml(@PathVariable String serviceId, @PathVariable String actionId,
                                            @RequestParam(defaultValue = "false") boolean validateOnly,
                                            @RequestBody MultiValueMap<String, String> inputs,
                                            Model model) {
        return StringUtils.replace(
                "partials/" + serviceAdapter.invokeActionIdempotent(serviceId, actionId, validateOnly, inputs, model)
                , ".html", ".xml");
    }

    //=======================

    @PostMapping(path = "/{serviceId}/actions/{actionId}/invoke", headers = {"HX-Request"})
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionHtmx(
            @PathVariable String serviceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            @RequestBody MultiValueMap<String, String> inputs,
            Model model) {
        return "partials/" + serviceAdapter.invokeAction(serviceId, actionId, validateOnly, inputs, model);
    }

    @PostMapping("/{serviceId}/actions/{actionId}/invoke.html")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionHtml(
            @PathVariable String serviceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            @RequestBody MultiValueMap<String, String> inputs,
            Model model) {
        return metaModelAdapter.fullPageResponse("layout.html", model,
                "partials/" + serviceAdapter.invokeAction(serviceId, actionId, validateOnly, inputs, model));
    }

    @PostMapping("/{serviceId}/actions/{actionId}/invoke.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String invokeActionXml(
            @PathVariable String serviceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            @RequestBody MultiValueMap<String, String> inputs,
            Model model) {
        return StringUtils.replace(
                "partials/" + serviceAdapter.invokeAction(serviceId, actionId, validateOnly, inputs, model),
                ".html", ".xml");
    }

}
