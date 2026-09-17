package com.incode;

import com.incode.verification.config.hints.CaffeineRuntimeHints;
import com.incode.verification.config.hints.HibernateValidatorRuntimeHints;
import com.incode.verification.config.hints.ProviderRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.incode")
@ImportRuntimeHints({
  HibernateValidatorRuntimeHints.class,
  CaffeineRuntimeHints.class,
  ProviderRuntimeHints.class
})
public final class CompanyCheckApplication {
  private CompanyCheckApplication() {}

  public static void main(String[] args) {
    SpringApplication.run(CompanyCheckApplication.class, args);
  }
}
