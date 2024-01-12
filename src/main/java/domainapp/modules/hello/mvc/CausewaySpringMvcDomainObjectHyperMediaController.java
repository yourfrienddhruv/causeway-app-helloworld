package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    //=======================

    @GetMapping("/{domainType}/{instanceId}/object-icon")
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