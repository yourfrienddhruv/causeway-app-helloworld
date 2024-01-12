package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Controller
@RequestMapping("web")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Log4j2
public class CausewaySpringMvcRootController {

    private final CausewaySpringMvcMetaModelAdapter metaModelAdapter;

    @GetMapping
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String homePage(Model model) {
        return metaModelAdapter.fullPageResponse("layout.html", model, "partials/home.html");
    }

}
