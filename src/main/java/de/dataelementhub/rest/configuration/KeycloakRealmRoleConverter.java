package de.dataelementhub.rest.configuration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private String keycloakClient;

  public KeycloakRealmRoleConverter(String keycloakClient) {
    this.keycloakClient = keycloakClient;
  }

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {

    final Map<String, Object> resourceAccess = (Map<String, Object>) jwt.getClaims()
        .get("resource_access");
    final Map<String, Object> dehub = (Map<String, Object>) resourceAccess.get(keycloakClient);
    return ((List<String>) dehub.get("roles")).stream()
        .map(roleName -> "ROLE_" + roleName) // prefix to map to a Spring Security "role"
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
