package de.dataelementhub.rest.configuration;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  private final int numberOfCores = Runtime.getRuntime().availableProcessors();

  /**
   * Async Configurations.
   */
  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(numberOfCores);
    executor.setMaxPoolSize(numberOfCores);
    executor.setQueueCapacity(100);
    executor.initialize();
    return executor;
  }

}
