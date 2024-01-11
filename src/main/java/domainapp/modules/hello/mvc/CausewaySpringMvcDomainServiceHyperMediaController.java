package domainapp.modules.hello.mvc;

import lombok.extern.log4j.Log4j2;
import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.services.bookmark.BookmarkService;
import org.apache.causeway.applib.services.xactn.TransactionService;
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
@Log4j2
public class CausewaySpringMvcDomainServiceHyperMediaController extends CausewaySpringMvcDomainServiceAdapter {


    public CausewaySpringMvcDomainServiceHyperMediaController(CausewaySpringMvcMetaModelAdapter adapter,
                                                              TransactionService transactionService,
                                                              BookmarkService bookmarkService) {
        super(adapter, transactionService, bookmarkService);
    }

    @GetMapping("/{serviceId}/actions/{actionId}.htmx")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String actionPromptHtmx(@PathVariable String serviceId, @PathVariable String actionId, Model model) {
        return "partials/" + super.actionPrompt(serviceId, actionId, model);
    }

    @GetMapping("/{serviceId}/actions/{actionId}.html")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String actionPromptHtml(@PathVariable String serviceId, @PathVariable String actionId, Model model) {
        model.addAttribute("partial"
                , "partials/" + super.actionPrompt(serviceId, actionId, model));
        return "layout.html";
    }

    @GetMapping("/{serviceId}/actions/{actionId}.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String actionPromptXml(@PathVariable String serviceId, @PathVariable String actionId, Model model) {
        return StringUtils.replace(
                "partials/" + super.actionPrompt(serviceId, actionId, model),
                ".html", ".xml");
    }

    //=======================

    @GetMapping("/{serviceId}/actions/{actionId}/invoke.htmx")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionQueryOnlyHtmx(@PathVariable String serviceId, @PathVariable String actionId,
                                            @RequestParam(defaultValue = "false") boolean validateOnly,
                                            @RequestParam MultiValueMap<String, String> inputs,
                                            Model model) {
        return "partials/" + super.invokeActionQueryOnly(serviceId, actionId, validateOnly, inputs, model);
    }

    @GetMapping("/{serviceId}/actions/{actionId}/invoke.html")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionQueryOnlyHtml(@PathVariable String serviceId, @PathVariable String actionId,
                                            @RequestParam(defaultValue = "false") boolean validateOnly,
                                            @RequestParam MultiValueMap<String, String> inputs,
                                            Model model) {
        model.addAttribute("partial",
                "partials/" + super.invokeActionQueryOnly(serviceId, actionId, validateOnly, inputs, model));
        return "layout.html";
    }

    @GetMapping("/{serviceId}/actions/{actionId}/invoke.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String invokeActionQueryOnlyXml(@PathVariable String serviceId, @PathVariable String actionId,
                                           @RequestParam(defaultValue = "false") boolean validateOnly,
                                           @RequestParam MultiValueMap<String, String> inputs,
                                           Model model) {
        return StringUtils.replace(
                "partials/" + super.invokeActionQueryOnly(serviceId, actionId, validateOnly, inputs, model),
                ".html", ".xml");
    }

    //=======================

    @PutMapping("/{serviceId}/actions/{actionId}/invoke.htmx")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionIdempotentHtmx(@PathVariable String serviceId, @PathVariable String actionId,
                                             @RequestParam(defaultValue = "false") boolean validateOnly,
                                             @RequestBody MultiValueMap<String, String> inputs,
                                             Model model) {
        return "partials/" + super.invokeActionIdempotent(serviceId, actionId, validateOnly, inputs, model);
    }

    @PutMapping("/{serviceId}/actions/{actionId}/invoke.html")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionIdempotentHtml(@PathVariable String serviceId, @PathVariable String actionId,
                                             @RequestParam(defaultValue = "false") boolean validateOnly,
                                             @RequestBody MultiValueMap<String, String> inputs,
                                             Model model) {
        model.addAttribute("partial",
                "partials/" + super.invokeActionIdempotent(serviceId, actionId, validateOnly, inputs, model));
        return "layout.html";
    }

    @PutMapping("/{serviceId}/actions/{actionId}/invoke.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String invokeActionIdempotentXml(@PathVariable String serviceId, @PathVariable String actionId,
                                            @RequestParam(defaultValue = "false") boolean validateOnly,
                                            @RequestBody MultiValueMap<String, String> inputs,
                                            Model model) {
        return StringUtils.replace(
                "partials/" + super.invokeActionIdempotent(serviceId, actionId, validateOnly, inputs, model)
                , ".html", ".xml");
    }

    //=======================

    @PostMapping("/{serviceId}/actions/{actionId}/invoke.htmx")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionHtmx(
            @PathVariable String serviceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            @RequestBody MultiValueMap<String, String> inputs,
            Model model) {
        return "partials/" + super.invokeAction(serviceId, actionId, validateOnly, inputs, model);
    }

    @PostMapping("/{serviceId}/actions/{actionId}/invoke.html")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String invokeActionHtml(
            @PathVariable String serviceId,
            @PathVariable String actionId,
            @RequestParam(defaultValue = "false") boolean validateOnly,
            @RequestBody MultiValueMap<String, String> inputs,
            Model model) {
        model.addAttribute("partial",
                "partials/" + super.invokeAction(serviceId, actionId, validateOnly, inputs, model));
        return "layout.html";
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
                "partials/" + super.invokeAction(serviceId, actionId, validateOnly, inputs, model),
                ".html", ".xml");
    }

}
