package domainapp.modules.hello.mvc;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.util.StringUtils;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Controller
@RequestMapping("web/objects/")
@Log4j2
public class CausewaySpringMvcDomainObjectHyperMediaController extends CausewaySpringMvcDomainObjectAdapter {

    public CausewaySpringMvcDomainObjectHyperMediaController(final CausewaySpringMvcMetaModelAdapter adapter) {
        super(adapter);
    }

    // //////////////////////////////////////////////////////////
    // domain object
    // //////////////////////////////////////////////////////////


    @GetMapping("{domainType}/{instanceId}.htmx")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String objectHtmx(@PathVariable String domainType, @PathVariable String instanceId, Model model) {
        return "partials/" + super.object(domainType, instanceId, model);
    }

    @GetMapping("{domainType}/{instanceId}.html")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String objectHtml(@PathVariable String domainType, @PathVariable String instanceId, Model model) {
        model.addAttribute("partial"
                , "partials/" + super.object(domainType, instanceId, model));
        return "layout.html";
    }

    @GetMapping("{domainType}/{instanceId}.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String objectXml(@PathVariable String domainType, @PathVariable String instanceId, Model model) {
        return StringUtils.replace(
                "partials/" + super.object(domainType, instanceId, model),
                ".html", ".xml");
    }

    //=======================
}
