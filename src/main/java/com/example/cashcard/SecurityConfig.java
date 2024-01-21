package com.example.cashcard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(request -> request
                .requestMatchers("/cashcards/**")
                .hasRole(Role.CardOwner.getRole()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder) {
        var users = User.builder();
        var sarah = users
                .username("sarah1")
                .password(passwordEncoder.encode("abc123"))
                .roles(Role.CardOwner.getRole())
                .build();
        var hank = users
                .username("hank")
                .password(passwordEncoder.encode("qrs456"))
                .roles(Role.NonCardOwner.getRole())
                .build();
        var kumar = users
                .username("kumar2")
                .password(passwordEncoder.encode("xyz789"))
                .roles(Role.CardOwner.getRole())
                .build();
        return new InMemoryUserDetailsManager(sarah, hank, kumar);
    }
}