package com.ragassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile({"rest", "webui"})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties props) throws Exception {
        if (Boolean.FALSE.equals(props.getSecurity().getEnabled())) {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                    "/swagger-ui.html").permitAll()
                            .anyRequest().permitAll());
            return http.build();
        }
        UserDetails user = User.withUsername(props.getSecurity().getUsername())
                .password("{noop}" + props.getSecurity().getPassword())
                .roles("USER")
                .build();
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager(user);
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(manager);
        return http.build();
    }
}
