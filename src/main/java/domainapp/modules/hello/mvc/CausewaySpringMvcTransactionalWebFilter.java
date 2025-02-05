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

/**
 * Run modifying operations under transactions
 */
@Component
@WebFilter(urlPatterns = "/web/*")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CausewaySpringMvcTransactionalWebFilter extends OncePerRequestFilter {
    private final TransactionService transactionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !request.getMethod().startsWith("P");
        // !StringUtils.equalsAny(HttpPost.METHOD_NAME, HttpPut.METHOD_NAME, request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        transactionService.runWithinCurrentTransactionElseCreateNew(() -> {
            filterChain.doFilter(request, response);
        });
    }
}
