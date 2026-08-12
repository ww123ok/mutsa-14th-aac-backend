package mutsa.hackathon.config;

import mutsa.hackathon.handler.CustomLogoutHandler;
import mutsa.hackathon.handler.CustomLogoutSuccessHandler;
import mutsa.hackathon.handler.OAuth2AuthenticationFailureHandler;
import mutsa.hackathon.handler.OAuth2AuthenticationSuccessHandler;
import mutsa.hackathon.security.ApiAccessDeniedHandler;
import mutsa.hackathon.security.CustomOAuth2UserService;
import mutsa.hackathon.security.JwtAuthenticationEntryPoint;
import mutsa.hackathon.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain
    securityFilterChain(
            HttpSecurity http,

            CustomOAuth2UserService
                    customOAuth2UserService,

            JwtAuthenticationFilter
                    jwtAuthenticationFilter,

            OAuth2AuthenticationSuccessHandler
                    successHandler,

            OAuth2AuthenticationFailureHandler
                    failureHandler,

            CustomLogoutHandler
                    customLogoutHandler,

            CustomLogoutSuccessHandler
                    customLogoutSuccessHandler,

            JwtAuthenticationEntryPoint
                    jwtAuthenticationEntryPoint,

            ApiAccessDeniedHandler
                    apiAccessDeniedHandler,

            CorsConfigurationSource
                    corsConfigurationSource,

            CsrfTokenRepository
                    csrfTokenRepository

    ) throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource
                        )
                )

                /*
                 * HttpOnly JWT Cookie 인증을 사용하므로
                 * unsafe HTTP 요청에 CSRF 보호를 적용.
                 * CookieCsrfTokenRepository에 저장된 token과
                 * X-XSRF-TOKEN header의 token이 모두 필요.
                 */
                .csrf(csrf ->
                        csrf
                                .csrfTokenRepository(
                                        csrfTokenRepository
                                )
                )

                .sessionManagement(
                        session ->
                                session
                                        .sessionCreationPolicy(
                                                SessionCreationPolicy
                                                        .STATELESS
                                        )
                )

                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(
                                                jwtAuthenticationEntryPoint
                                        )
                                        .accessDeniedHandler(
                                                apiAccessDeniedHandler
                                        )
                )

                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(
                                                "/",
                                                "/api/auth/csrf",
                                                "/api/auth/signup",
                                                "/api/auth/login",
                                                "/api/auth/refresh",
                                                "/error",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-resources/**"
                                        )
                                        .permitAll()

                                        .anyRequest()
                                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter
                                .class
                )

                .oauth2Login(
                        oauth2 ->
                                oauth2
                                        .userInfoEndpoint(
                                                userInfo ->
                                                        userInfo
                                                                .userService(
                                                                        customOAuth2UserService
                                                                )
                                        )
                                        .successHandler(
                                                successHandler
                                        )
                                        .failureHandler(
                                                failureHandler
                                        )
                )

                .logout(
                        logout ->
                                logout
                                        .logoutUrl(
                                                "/api/logout"
                                        )
                                        .addLogoutHandler(
                                                customLogoutHandler
                                        )
                                        .logoutSuccessHandler(
                                                customLogoutSuccessHandler
                                        )
                )

                .formLogin(
                        AbstractHttpConfigurer
                                ::disable
                )

                .httpBasic(
                        AbstractHttpConfigurer
                                ::disable
                );

        return http.build();
    }
}