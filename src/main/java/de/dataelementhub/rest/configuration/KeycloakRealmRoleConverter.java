package de.dataelementhub.rest.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Keycloak Realm Role Converter.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private String keycloakClient;

  public KeycloakRealmRoleConverter(String keycloakClient) {
    this.keycloakClient = keycloakClient;
  }

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    List<String> roles;
    try {
      final Map<String, Object> resourceAccess = (Map<String, Object>) jwt.getClaims()
          .get("resource_access");
      final Map<String, Object> dehub = (Map<String, Object>) resourceAccess.get(keycloakClient);
      roles = (List<String>) dehub.get("roles");
    } catch (NullPointerException e) {
      roles = new ArrayList<>();
    }
    return roles.stream()
        .map(roleName -> "ROLE_" + roleName) // prefix to map to a Spring Security "role"
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
