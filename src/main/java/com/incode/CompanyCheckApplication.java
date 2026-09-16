package com.incode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public final class CompanyCheckApplication {
  private CompanyCheckApplication() {}

  public static void main(String[] args) {
    SpringApplication.run(CompanyCheckApplication.class, args);
  }
}
