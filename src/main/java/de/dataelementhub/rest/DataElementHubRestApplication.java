package de.dataelementhub.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Dataelement Hub Rest Application.
 */
@SpringBootApplication(exclude = {R2dbcAutoConfiguration.class})
public class DataElementHubRestApplication {

  /**
   * Start the rest application.
   */
  public static void main(String[] args) {
    SpringApplication.run(DataElementHubRestApplication.class, args);
  }

  public static String getCurrentUserName() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }

  /**
   * Loads the git.properties file for build information that can be displayed in the about section.
   */
  @Bean
  public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
    PropertySourcesPlaceholderConfigurer c = new PropertySourcesPlaceholderConfigurer();
    c.setLocation(new ClassPathResource("git.properties"));
    c.setIgnoreResourceNotFound(true);
    c.setIgnoreUnresolvablePlaceholders(true);
    return c;
  }
}
