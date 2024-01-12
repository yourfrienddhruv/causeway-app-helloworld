package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import org.apache.causeway.applib.services.xactn.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.context.support.SecurityWebApplicationContextUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thymeleaf.spring5.context.SpringContextUtils;
import org.thymeleaf.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Redirects to login for un-authenticated / expired htmx requests
 * for both client-side and server-side templating
 */
@Component
@WebFilter(urlPatterns = {"/web/*", "/restful/*"})
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CausewaySpringMvcAuthorizedHtmxRequestFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return StringUtils.isEmpty(request.getHeader("HX-Request"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
        } else {
            response.addHeader("HX-Refresh", "true");//triggers client side full page refresh
            new Http403ForbiddenEntryPoint().commence(request, response, new AuthenticationServiceException("HTMX Request Not Authorized. Please Re-Login"));
        }
    }

}
