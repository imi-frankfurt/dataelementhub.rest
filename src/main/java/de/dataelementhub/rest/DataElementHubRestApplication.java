package de.dataelementhub.rest;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.tables.pojos.DehubUser;
import de.dataelementhub.db.migration.MigrationUtil;
import de.dataelementhub.model.handler.UserHandler;
import org.jooq.CloseableDSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication(exclude = {R2dbcAutoConfiguration.class})
public class DataElementHubRestApplication {

  @Value("${spring.datasource.url}")
  private String url;

  @Value("${spring.datasource.username}")
  private String username;

  @Value("${spring.datasource.password}")
  private String password;

  public static final String restVersion = "v1";

  /**
   * Start the rest application.
   */
  public static void main(String[] args) {
    SpringApplication.run(DataElementHubRestApplication.class, args);
  }

  /**
   * Initialize DataElementHub DAL ResourceManager for database access.
   */
  @Bean
  public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {
      ResourceManager.initialize(url, username, password);
      MigrationUtil.migrateDatabase();
    };
  }

  /**
   * Get the current user.
   */
  public static DehubUser getCurrentUser() {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return getCurrentUser(ctx);
    }
  }

  /**
   * Get the current user with the given DSLContext.
   */
  public static DehubUser getCurrentUser(CloseableDSLContext ctx) {
    return UserHandler.getUserByIdentity(
        ctx, SecurityContextHolder.getContext().getAuthentication().getName());
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
