package com.incode.verification.client;

import com.incode.verification.config.provider.ProviderEndpointProperties;
import com.incode.verification.exception.ProviderContractException;
import com.incode.verification.exception.ProviderTransientException;
import com.incode.verification.service.model.Company;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/** Generic HTTP provider client that keeps transport and response mapping in one place. */
final class TypedProviderClient<T> implements ProviderClient {
  private final WebClient client;
  private final ProviderEndpointProperties endpoint;
  private final ProviderType provider;
  private final Class<T[]> responseType;
  private final Function<T @Nullable [], List<Company>> mapper;

  TypedProviderClient(
      WebClient client,
      ProviderEndpointProperties endpoint,
      ProviderType provider,
      Class<T[]> responseType,
      Function<T @Nullable [], List<Company>> mapper) {
    this.client = client;
    this.endpoint = endpoint;
    this.provider = provider;
    this.responseType = responseType;
    this.mapper = mapper;
  }

  @Override
  public Mono<ProviderResult> lookup(NormalizedQuery query) {
    return get(query)
        .<ProviderResult>map(
            response -> new ProviderResult.Success(mapper.apply(response), provider))
        .onErrorResume(ClientErrorException.class, failure -> Mono.just(clientError(failure)))
        .onErrorMap(
            IllegalArgumentException.class, failure -> new ProviderContractException(failure))
        .onErrorMap(
            failure ->
                !(failure instanceof ClientErrorException)
                    && !(failure instanceof ProviderContractException)
                    && !(failure instanceof ProviderTransientException),
            TypedProviderClient::transientFailure);
  }

  private Mono<T @Nullable []> get(NormalizedQuery query) {
    return client
        .get()
        .uri(endpoint.path(), query.value())
        .header("X-Api-Key", endpoint.apiKey())
        .retrieve()
        .bodyToMono(responseType)
        .onErrorMap(
            WebClientResponseException.class,
            failure -> responseFailure((WebClientResponseException) failure))
        .onErrorMap(
            WebClientRequestException.class,
            failure -> transientFailure((WebClientRequestException) failure));
  }

  private static RuntimeException responseFailure(WebClientResponseException failure) {
    if (failure.getStatusCode().is4xxClientError()) {
      return new ClientErrorException(failure.getStatusCode().value());
    }
    return transientFailure(failure);
  }

  private static ProviderTransientException transientFailure(Throwable failure) {
    if (hasCause(failure, java.util.concurrent.TimeoutException.class)
        || hasCause(failure, java.io.IOException.class)) {
      return new ProviderTransientException(new ProviderFailure.Timeout(), failure);
    }
    return new ProviderTransientException(new ProviderFailure.Unavailable(), failure);
  }

  private static ProviderResult clientError(Throwable failure) {
    return new ProviderResult.Failure(
        new ProviderFailure.ClientError(((ClientErrorException) failure).status));
  }

  private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      if (type.isInstance(current)) {
        return true;
      }
    }
    return false;
  }

  private static final class ClientErrorException extends RuntimeException {
    private final int status;

    private ClientErrorException(int status) {
      this.status = status;
    }
  }
}
