package de.dataelementhub.rest.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan("de.dataelementhub.model")
public class Config implements WebMvcConfigurer {

  /**
   * Configure CORS mapping.
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**").allowedOrigins("*").allowedMethods("*")
        .exposedHeaders(HttpHeaders.LOCATION);
  }
}
