package com.vipin.lottery.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
public class WebApplication {
    public static void main(String[] args) { SpringApplication.run(WebApplication.class, args); }

    // A local desktop-style application. CSRF remains enabled for every mutation.
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'")))
                .build();
    }
}
