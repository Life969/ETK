package ProjectForJob.example.Job.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import static org.springframework.security.config.Customizer.withDefaults;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Включает возможность использовать @PreAuthorize
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/",
                                "/welcome",
                                "/css/**",
                                "/js/**",
                                "/uploads/**",
                                "/images/**",
                                "/favicon.ico").permitAll()
                        .requestMatchers("/home",
                                "/employees/**",
                                "/couplings/**",
                                "/production-records/**",
                                "/orders/**",
                                "/machines/**",
                                "/pipe-adapters/**",
                                "/additionalWork/**",
                                "/commercial-offer/**",
                                "/reports/**").authenticated()  // всё, что требует входа
                        .anyRequest().denyAll() // на всякий случай, чтобы непредусмотренные пути не были открыты
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/home", true)  // после успешного входа всегда на /home
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );
        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        successHandler.setPostLogoutRedirectUri("http://localhost:8080/welcome");  // после логаута идём на публичную страницу
        return successHandler;
    }
}