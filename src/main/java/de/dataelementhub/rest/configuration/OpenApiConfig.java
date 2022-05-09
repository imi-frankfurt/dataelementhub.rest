package de.dataelementhub.rest.configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Config.
 */
@Configuration
@SecurityScheme(name = "basicAuth", bearerFormat = "JWT", type = SecuritySchemeType.HTTP,
    scheme = "bearer")
@SecurityRequirement(name = "basicAuth")
public class OpenApiConfig {


  /**
   * Get Custom open api.
   */
  @Bean
  public OpenAPI customOpenApi() {
    return new OpenAPI()
        .components(new Components())
        .info(apiInfo());

  }

  /**
   * Get api info.
   */
  private Info apiInfo() {
    return new Info() //TODO
        .title("DataElementHub Rest API")
        .description("REST API Documentation!\n"
            + "termsOfService: http://example.com/terms/\n"
            + "MIG\n"
            + "https://www.mig-frankfurt.de\n"
            + "vengadeswaran@med.uni-frankfurt.de");
  }
}
