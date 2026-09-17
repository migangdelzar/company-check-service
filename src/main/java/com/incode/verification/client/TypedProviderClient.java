package com.incode.verification.client;

import com.incode.verification.config.ProviderEndpointProperties;
import com.incode.verification.exception.ProviderContractException;
import com.incode.verification.exception.ProviderTransientException;
import com.incode.verification.service.model.Company;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Generic HTTP provider client that keeps transport and response mapping in one place. */
final class TypedProviderClient<T> implements ProviderClient {
  private final RestClient client;
  private final ProviderEndpointProperties endpoint;
  private final ProviderType provider;
  private final Class<T[]> responseType;
  private final Function<T @Nullable [], List<Company>> mapper;

  TypedProviderClient(
      RestClient client,
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
  public ProviderResult lookup(NormalizedQuery query) {
    try {
      return new ProviderResult.Success(mapper.apply(get(query)), provider);
    } catch (Exception failure) {
      return handle(failure);
    }
  }

  private T @Nullable [] get(NormalizedQuery query) {
    return client
        .get()
        .uri(endpoint.path(), query.value())
        .header("X-Api-Key", endpoint.apiKey())
        .retrieve()
        .body(responseType);
  }

  private static ProviderResult handle(Exception failure) {
    if (failure instanceof RestClientResponseException response) {
      return response(response);
    }
    if (failure instanceof IllegalArgumentException) {
      throw new ProviderContractException(failure);
    }
    if (failure instanceof ResourceAccessException access) {
      throw transientFailure(access);
    }
    throw transientFailure(failure);
  }

  private static ProviderResult response(RestClientResponseException failure) {
    if (failure.getStatusCode().is4xxClientError()) {
      return new ProviderResult.Failure(
          new ProviderFailure.ClientError(failure.getStatusCode().value()));
    }
    throw transientFailure(failure);
  }

  private static ProviderTransientException transientFailure(Exception failure) {
    if (hasCause(failure, SocketTimeoutException.class)) {
      return new ProviderTransientException(new ProviderFailure.Timeout(), failure);
    }
    return new ProviderTransientException(new ProviderFailure.Unavailable(), failure);
  }

  private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      if (type.isInstance(current)) {
        return true;
      }
    }
    return false;
  }
}
