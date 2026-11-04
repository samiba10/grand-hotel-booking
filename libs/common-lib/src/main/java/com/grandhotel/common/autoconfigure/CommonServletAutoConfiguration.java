package com.grandhotel.common.autoconfigure;

import com.grandhotel.common.authz.PermissionResolver;
import com.grandhotel.common.context.ContextPropagatingInterceptor;
import com.grandhotel.common.context.AuditContextFilter;
import com.grandhotel.common.security.JwtService;
import com.grandhotel.common.security.servlet.JwtAuthenticationFilter;
import com.grandhotel.common.security.servlet.SecurityProperties;
import com.grandhotel.common.web.ApiError;
import com.grandhotel.common.web.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * The ONE security chain shared by all servlet services. A service declares only its public paths
 * ({@code app.security.public-paths}) and its permission rules ({@code @PreAuthorize}).
 *
 * <p>CSRF is disabled on purpose: the API is stateless and uses Bearer tokens, never cookies. CORS
 * is handled once, at the gateway.
 */
@AutoConfiguration(
    after = CommonCoreAutoConfiguration.class,
    beforeName = {
      "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
      "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
      "org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration"
    })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SecurityFilterChain.class)
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class CommonServletAutoConfiguration {

  private static final String[] ALWAYS_PUBLIC = {
    "/actuator/health/**", "/actuator/info", "/error", "/v3/api-docs/**", "/swagger-ui/**",
    "/swagger-ui.html"
  };

  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtService jwtService,
      PermissionResolver permissions,
      SecurityProperties props,
      JsonMapper mapper)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers(ALWAYS_PUBLIC).permitAll();
              if (!props.publicPaths().isEmpty()) {
                auth.requestMatchers(props.publicPaths().toArray(String[]::new)).permitAll();
              }
              auth.anyRequest().authenticated(); // secure by default
            })
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (req, res, e) ->
                            write(mapper, req, res, HttpStatus.UNAUTHORIZED, "Authentication required"))
                    .accessDeniedHandler(
                        (req, res, e) ->
                            write(mapper, req, res, HttpStatus.FORBIDDEN, "Access denied")))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtService, permissions),
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  @ConditionalOnMissingBean
  GlobalExceptionHandler globalExceptionHandler() {
    return new GlobalExceptionHandler();
  }

  @Bean
  @ConditionalOnMissingBean
  AuditContextFilter auditContextFilter() {
    return new AuditContextFilter();
  }

  @Bean
  @ConditionalOnMissingBean
  ContextPropagatingInterceptor contextPropagatingInterceptor() {
    return new ContextPropagatingInterceptor();
  }

  private static void write(
      JsonMapper mapper,
      HttpServletRequest req,
      HttpServletResponse res,
      HttpStatus status,
      String message)
      throws IOException {
    res.setStatus(status.value());
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(
        res.getOutputStream(),
        ApiError.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI()));
  }
}
