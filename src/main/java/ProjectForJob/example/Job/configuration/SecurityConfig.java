package ProjectForJob.example.Job.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // TODO: перед продом — вынести в БД + BCrypt-хеши
    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder encoder) {
        UserDetails admin   = User.builder().username("admin")
                .password(encoder.encode("admin123")).roles("ADMIN").build();
        UserDetails manager = User.builder().username("manager")
                .password(encoder.encode("manager123")).roles("MANAGER").build();
        UserDetails master  = User.builder().username("master")
                .password(encoder.encode("master123")).roles("MASTER").build();
        UserDetails worker  = User.builder().username("worker")
                .password(encoder.encode("worker123")).roles("WORKER").build();

        return new InMemoryUserDetailsManager(admin, manager, master, worker);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // TODO: включить CSRF перед продом
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/welcome", "/login", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**",
                                "/video/**", "/uploads/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // любые авторизованные
                        .requestMatchers("/layout", "/fragments/**", "/home").authenticated()
                        .requestMatchers("/orders/**").authenticated()
                        .requestMatchers("/reports/**").authenticated()

                        // справочники — админ/менеджер/мастер
                        .requestMatchers("/couplings/**", "/pipe-adapters/**",
                                "/employees/**", "/machines/**", "/additionalWork/**")
                        .hasAnyRole("ADMIN", "MANAGER", "MASTER")

                        // журнал — админ/мастер/работник
                        .requestMatchers("/production-records/**")
                        .hasAnyRole("ADMIN", "MASTER", "WORKER")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/layout", true)   // ← прилетаем сюда
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}