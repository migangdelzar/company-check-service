package com.incode.verification.adapter.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ApplicationConfiguration {
  @Bean
  Clock applicationClock() {
    return Clock.systemUTC();
  }
}
