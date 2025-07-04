package org.whu.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.whu.backend.filter.JwtAuthenticationFilter;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfig {

    //-------------------Encoder------------------------
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //-------------------Jwt+swagger------------------------
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        //放行登录接口
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/captcha/**",
                                "/api/public/**",
                                "/api/hello/world").permitAll()
                        //放行swagger
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                                ).permitAll()
                        // 放行 WebSocket 端点
                        .requestMatchers("/ws/**").permitAll()
                        //刷新token
                        .requestMatchers("/api/auth/refresh").permitAll()

                        .anyRequest().authenticated()
                )
                // 👇👇 添加认证失败处理器 👇👇
                // 很好的security把我的所有token过期全部判定为403 wcnmd
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 禁用默认登录页面
                .formLogin(form -> form.disable());
//                //
//                .exceptionHandling(exception -> exception
//                .authenticationEntryPoint((request, response, authException) -> {
//                    response.setContentType("application/json;charset=UTF-8");
//                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                    response.getWriter().write("{\"error\":\"Unauthorized, please login.\"}");
//                })


        return http.build();
    }
}

