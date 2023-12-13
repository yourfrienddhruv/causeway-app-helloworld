package domainapp.webapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Not using WebSecurityConfigurerAdapter : https://www.baeldung.com/spring-deprecated-websecurityconfigureradapter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Only for demo purpose, use SecMan in production
     *
     * @param passwordEncoder
     * @return
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("sven")
                .password(passwordEncoder.encode("pass"))
                .roles("causeway-ext-secman-admin")
                .build());
        return manager;
    }

    /**
     * Only for demo purpose, to support InMemoryUserDetailsManager
     *
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}