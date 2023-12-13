package domainapp.modules.hello.mvc;

import lombok.RequiredArgsConstructor;
import org.apache.causeway.applib.services.xactn.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@WebFilter(urlPatterns = "/mvc/*")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CausewaySpringMvcTransactionalWebFilter extends OncePerRequestFilter {
    private final TransactionService transactionService;

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        transactionService.runWithinCurrentTransactionElseCreateNew(() -> {
            filterChain.doFilter(request, response);
        });
    }
}
