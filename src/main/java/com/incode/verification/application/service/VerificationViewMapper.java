package com.incode.verification.application.service;

import com.incode.verification.application.port.out.VerificationView;
import com.incode.verification.domain.aggregate.Verification;
import com.incode.verification.domain.type.VerificationState;
import com.incode.verification.domain.type.VerificationStatus;

/** Pure projection from aggregate state to the application read model. */
final class VerificationViewMapper {
  private VerificationViewMapper() {}

  static VerificationView from(Verification verification) {
    return switch (verification.state()) {
      case VerificationState.InProgress ignored ->
          VerificationView.builder()
              .id(verification.id())
              .rawQuery(verification.rawQuery())
              .normalizedQuery(verification.query().value())
              .startedAt(verification.startedAt())
              .expiresAt(verification.expiresAt())
              .status(VerificationStatus.IN_PROGRESS)
              .build();
      case VerificationState.Completed completed ->
          VerificationView.builder()
              .id(verification.id())
              .rawQuery(verification.rawQuery())
              .normalizedQuery(verification.query().value())
              .startedAt(verification.startedAt())
              .expiresAt(verification.expiresAt())
              .status(VerificationStatus.COMPLETED)
              .company(completed.company())
              .otherResults(completed.otherResults())
              .provider(completed.provider())
              .build();
      case VerificationState.Failed failed ->
          VerificationView.builder()
              .id(verification.id())
              .rawQuery(verification.rawQuery())
              .normalizedQuery(verification.query().value())
              .startedAt(verification.startedAt())
              .expiresAt(verification.expiresAt())
              .status(VerificationStatus.FAILED)
              .failure(failed.failure())
              .build();
    };
  }
}
