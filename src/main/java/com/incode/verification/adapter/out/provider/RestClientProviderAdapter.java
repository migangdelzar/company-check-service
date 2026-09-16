package com.incode.verification.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.type.ProviderFailure;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.type.ProviderType;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import java.net.SocketTimeoutException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public final class RestClientProviderAdapter implements ProviderLookupPort {
  private final RestClient client;
  private final ProviderType type;
  private final ProviderProperties.Endpoint endpoint;

  public RestClientProviderAdapter(
      RestClient client, ProviderType type, ProviderProperties.Endpoint endpoint) {
    this.client = client;
    this.type = type;
    this.endpoint = endpoint;
  }

  @Override
  public ProviderLookupResult lookup(NormalizedQuery query, ExecutionContext context) {
    try {
      JsonNode body =
          client
              .get()
              .uri(endpoint.path(), query.value())
              .header("X-Api-Key", endpoint.apiKey())
              .retrieve()
              .onStatus(
                  HttpStatusCode::is4xxClientError,
                  (request, response) -> {
                    throw new ProviderException(
                        new ProviderFailure.ClientError(response.getStatusCode().value()));
                  })
              .body(JsonNode.class);
      var companies = ProviderResponseMapper.companies(body, type);
      return new ProviderLookupResult.Success(companies, type);
    } catch (ProviderException e) {
      return new ProviderLookupResult.Failure(e.failure);
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().is4xxClientError())
        return new ProviderLookupResult.Failure(
            new ProviderFailure.ClientError(e.getStatusCode().value()));
      throw new ProviderTransientException(new ProviderFailure.Unavailable(), e);
    } catch (IllegalArgumentException e) {
      throw new ProviderContractException(e);
    } catch (ResourceAccessException e) {
      if (hasCause(e, SocketTimeoutException.class))
        throw new ProviderTransientException(new ProviderFailure.Timeout(), e);
      throw new ProviderTransientException(new ProviderFailure.Unavailable(), e);
    } catch (Exception e) {
      throw new ProviderTransientException(new ProviderFailure.Unavailable(), e);
    }
  }

  private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
    for (Throwable current = error; current != null; current = current.getCause())
      if (type.isInstance(current)) return true;
    return false;
  }

  private static final class ProviderException extends RuntimeException {
    final ProviderFailure failure;

    ProviderException(ProviderFailure f) {
      failure = f;
    }
  }
}
