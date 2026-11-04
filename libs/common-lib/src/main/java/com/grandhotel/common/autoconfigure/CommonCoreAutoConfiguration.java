package com.grandhotel.common.autoconfigure;

import com.grandhotel.common.authz.AccessGuard;
import com.grandhotel.common.authz.AuthzProperties;
import com.grandhotel.common.authz.ConfigPermissionResolver;
import com.grandhotel.common.authz.PermissionResolver;
import com.grandhotel.common.security.JwtProperties;
import com.grandhotel.common.security.JwtService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** AuthN + AuthZ building blocks for every service, servlet and reactive alike. */
@AutoConfiguration
@EnableConfigurationProperties({JwtProperties.class, AuthzProperties.class})
public class CommonCoreAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  JwtService jwtService(JwtProperties props) {
    return new JwtService(props);
  }

  /** Replace by declaring your own PermissionResolver bean (DB-backed, OPA, ...). */
  @Bean
  @ConditionalOnMissingBean
  PermissionResolver permissionResolver(AuthzProperties props) {
    return new ConfigPermissionResolver(props);
  }

  @Bean
  @ConditionalOnMissingBean
  AccessGuard accessGuard() {
    return new AccessGuard();
  }
}
