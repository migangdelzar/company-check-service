package com.incode;

import com.incode.verification.config.HibernateValidatorRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.incode")
@ImportRuntimeHints(HibernateValidatorRuntimeHints.class)
public final class CompanyCheckApplication {
  private CompanyCheckApplication() {}

  public static void main(String[] args) {
    SpringApplication.run(CompanyCheckApplication.class, args);
  }
}
