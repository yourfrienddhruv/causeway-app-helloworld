package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.causeway.applib.annotation.DomainServiceLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Controller
@RequestMapping("web/")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Log4j2
public class CausewaySpringMvcRootController {

    private final CausewaySpringMvcMetaModelAdapter adapter;

    @GetMapping
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String homePage(Model model) {
        return withLayout(adapter, model, "partials/home.html");
    }

    public static String withLayout(CausewaySpringMvcMetaModelAdapter adapter, Model model, String partialTemplate) {
        model.addAttribute("applicationName", adapter.getApplicationName());
        model.addAttribute("applicationLogo", adapter.getApplicationLogo());
        model.addAttribute("menuBarPrimary", adapter.getMenuBar(DomainServiceLayout.MenuBar.PRIMARY));
        model.addAttribute("menuBarSecondary", adapter.getMenuBar(DomainServiceLayout.MenuBar.SECONDARY));
        model.addAttribute("menuBarTertiary", adapter.getMenuBar(DomainServiceLayout.MenuBar.TERTIARY));

        model.addAttribute("partial", partialTemplate);
        return "layout.html";
    }

}
