package de.dataelementhub.rest.configuration;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.tables.pojos.DehubUser;
import de.dataelementhub.model.handler.UserHandler;
import java.util.Map;
import org.jooq.CloseableDSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Security Config.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Value("${dehub.keycloakClient}")
  private String keycloakClient;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests().antMatchers(HttpMethod.GET, "/**")
        .permitAll()
        .and().authorizeRequests().antMatchers(HttpMethod.OPTIONS, "/**")
        .permitAll()
        .and().authorizeRequests().antMatchers(HttpMethod.HEAD, "/**")
        .permitAll()
        .and().authorizeRequests().anyRequest().fullyAuthenticated()
        .and().oauth2ResourceServer(
            oauth2ResourceServer -> oauth2ResourceServer.jwt(
                jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
            )).cors();
  }

  private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
    JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
    jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter(keycloakClient));
    return jwtConverter;
  }

  /**
   * Authentication Success Listener.
   */
  @Component
  public class AuthenticationSuccessListener
      implements ApplicationListener<AuthenticationSuccessEvent> {

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
      try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
        Map<String, Object> claims =
            ((JwtAuthenticationToken) event.getAuthentication()).getTokenAttributes();
        String sub = (String) claims.get("sub");
        String email = (String) claims.get("email");
        String name = (String) claims.get("name");
        DehubUser user = new DehubUser();
        user.setAuthId(sub);
        user.setUserName(name);
        user.setEmail(email);
        UserHandler.upsertUser(ctx, user);
      }
    }
  }
}
