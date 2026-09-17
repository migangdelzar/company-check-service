package com.incode.verification.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.provider.ProviderFailure;
import com.incode.verification.domain.provider.ProviderResult;
import com.incode.verification.domain.provider.ProviderType;
import com.incode.verification.domain.query.NormalizedQuery;
import java.net.SocketTimeoutException;
import org.jspecify.annotations.Nullable;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public final class RestClientProviderAdapter implements ProviderLookupPort {
  private final RestClient client;
  private final ProviderType type;
  private final ProviderEndpointProperties endpoint;

  public RestClientProviderAdapter(
      RestClient client, ProviderType type, ProviderEndpointProperties endpoint) {
    this.client = client;
    this.type = type;
    this.endpoint = endpoint;
  }

  @Override
  public ProviderResult lookup(NormalizedQuery query) {
    try {
      @Nullable JsonNode body =
          client
              .get()
              .uri(endpoint.path(), query.value())
              .header("X-Api-Key", endpoint.apiKey())
              .retrieve()
              .body(JsonNode.class);
      var companies = ProviderResponseMapper.mapCompanies(body, type);
      return new ProviderResult.Success(companies, type);
    } catch (Exception e) {
      return handle(e);
    }
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
