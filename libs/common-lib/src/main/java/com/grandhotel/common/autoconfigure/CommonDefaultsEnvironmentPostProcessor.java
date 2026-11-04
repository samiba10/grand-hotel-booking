package com.grandhotel.common.autoconfigure;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Loads {@code common-defaults.yml} as the LOWEST-priority property source: shared settings are
 * written once, and any service or environment variable can still override them.
 */
public class CommonDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String RESOURCE = "common-defaults.yml";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
    ClassPathResource resource = new ClassPathResource(RESOURCE);
    if (!resource.exists()) {
      return;
    }
    try {
      new YamlPropertySourceLoader()
          .load("common-defaults", resource)
          .forEach(source -> env.getPropertySources().addLast(source));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot load " + RESOURCE, e);
    }
  }
}
