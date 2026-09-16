package com.incode.verification.adapter.out.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.incode.verification.domain.entity.Company;
import com.incode.verification.domain.type.ProviderFailure;
import com.incode.verification.domain.type.ProviderType;
import com.incode.verification.domain.type.VerificationState;

final class VerificationStateCodec {
  private static final int VERSION = 1;
  private final ObjectMapper mapper;

  VerificationStateCodec(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  String encode(VerificationState state) {
    try {
      ObjectNode root = mapper.createObjectNode().put("version", VERSION);
      if (state instanceof VerificationState.InProgress) root.put("type", "IN_PROGRESS");
      if (state instanceof VerificationState.Completed s) {
        root.put("type", "COMPLETED");
        root.set("company", mapper.valueToTree(s.company()));
        root.set("otherResults", mapper.valueToTree(s.otherResults()));
        root.put("provider", s.provider().name());
      }
      if (state instanceof VerificationState.Failed s)
        root.put("type", "FAILED").put("failure", failureName(s.failure()));
      return mapper.writeValueAsString(root);
    } catch (Exception e) {
      throw new IllegalStateException("cannot encode verification state", e);
    }
  }

  VerificationState decode(String json) {
    try {
      JsonNode root = mapper.readTree(json);
      if (root.path("version").asInt() != VERSION)
        throw new IllegalArgumentException("unsupported state version");
      return switch (root.path("type").asText()) {
        case "IN_PROGRESS" -> new VerificationState.InProgress();
        case "COMPLETED" ->
            new VerificationState.Completed(
                mapper.treeToValue(root.get("company"), Company.class),
                mapper.readerForListOf(Company.class).readValue(root.get("otherResults")),
                ProviderType.valueOf(root.path("provider").asText()));
        case "FAILED" -> new VerificationState.Failed(failure(root.path("failure").asText()));
        default -> throw new IllegalArgumentException("unknown verification state");
      };
    } catch (Exception e) {
      throw new IllegalArgumentException("cannot decode verification state", e);
    }
  }

  private String failureName(ProviderFailure failure) {
    return switch (failure) {
      case ProviderFailure.ClientError ignored -> "CLIENT_ERROR";
      case ProviderFailure.Unavailable ignored -> "UNAVAILABLE";
      case ProviderFailure.Malformed ignored -> "MALFORMED";
      case ProviderFailure.Timeout ignored -> "TIMEOUT";
    };
  }

  private ProviderFailure failure(String name) {
    return switch (name) {
      case "CLIENT_ERROR" -> new ProviderFailure.ClientError(500);
      case "MALFORMED" -> new ProviderFailure.Malformed();
      case "TIMEOUT" -> new ProviderFailure.Timeout();
      default -> new ProviderFailure.Unavailable();
    };
  }
}
