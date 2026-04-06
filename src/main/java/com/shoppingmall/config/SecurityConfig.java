package com.shoppingmall.config;

import com.shoppingmall.global.jwt.JwtAuthenticationFilter;
import com.shoppingmall.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/h2-console/**").permitAll()
                // 관리자 전용
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, redisTemplate),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
