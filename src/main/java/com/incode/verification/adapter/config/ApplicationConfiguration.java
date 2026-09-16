package com.incode.verification.adapter.config;

import com.incode.verification.application.port.in.ExpireVerificationsUseCase;
import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.port.in.StartVerificationUseCase;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.application.port.out.VerificationLifecycle;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.application.service.VerificationApplicationService;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
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

  @Bean
  VerificationLifecycle verificationLifecycle(VerificationRepository repository) {
    return new VerificationLifecycle() {
      public void start(com.incode.verification.domain.aggregate.Verification verification) {
        repository.insertInProgress(verification);
      }

      public void transition(
          com.incode.verification.domain.aggregate.Verification verification,
          java.util.function.Consumer<VerificationRepository> write) {
        write.accept(repository);
      }
    };
  }

  @Bean
  VerificationApplicationService verificationApplicationService(
      VerificationRepository repository,
      VerificationLifecycle lifecycle,
      CoordinationPort coordination,
      @Qualifier("freeProvider") ProviderLookupPort free,
      @Qualifier("premiumProvider") ProviderLookupPort premium,
      Clock clock) {
    return new VerificationApplicationService(
        repository, lifecycle, coordination, free, premium, clock, Duration.ofMinutes(10));
  }

  @Bean
  StartVerificationUseCase startVerificationUseCase(VerificationApplicationService service) {
    return service;
  }

  @Bean
  GetVerificationUseCase getVerificationUseCase(VerificationApplicationService service) {
    return service;
  }

  @Bean
  ExpireVerificationsUseCase expireVerificationsUseCase(VerificationRepository repository) {
    return new com.incode.verification.application.service.ExpireVerificationsService(repository);
  }
}
