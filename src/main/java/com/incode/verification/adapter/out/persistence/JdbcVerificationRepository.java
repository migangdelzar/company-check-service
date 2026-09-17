package com.incode.verification.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.domain.verification.Verification;
import com.incode.verification.domain.query.NormalizedQuery;
import com.incode.verification.domain.identity.UuidV7;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcVerificationRepository implements VerificationRepository {
  private final JdbcClient jdbc;
  private final VerificationStateCodec codec;

  public JdbcVerificationRepository(JdbcClient jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.codec = new VerificationStateCodec(mapper);
  }

  @Override
  public boolean insertInProgress(Verification verification) {
    var entity =
        VerificationEntity.fromVerification(verification, codec.encode(verification.state()), null);
    int inserted =
        jdbc.sql(
                "INSERT INTO verifications(id,raw_query,normalized_query,started_at,expires_at,status,state) "
                    + "VALUES (:id,:raw,:normalized,:started,:expires,:status,CAST(:state AS jsonb)) "
                    + "ON CONFLICT (id) DO NOTHING")
            .param("id", entity.id())
            .param("raw", entity.rawQuery())
            .param("normalized", entity.normalizedQuery())
            .param("started", Timestamp.from(entity.startedAt()))
            .param("expires", Timestamp.from(entity.expiresAt()))
            .param("status", entity.status())
            .param("state", entity.stateJson())
            .update();
    return inserted == 1;
  }

  @Override
  public @Nullable UUID claim(UUID id) {
    UUID token = UuidV7.generate();
    int changed =
        jdbc.sql(
                "UPDATE verifications SET claim_token=:token,claimed_at=CURRENT_TIMESTAMP,"
                    + "updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status='IN_PROGRESS' "
                    + "AND claim_token IS NULL")
            .param("id", id)
            .param("token", token)
            .update();
    if (changed != 1) {
      return null;
    }
    return token;
  }

  @Override
  public boolean complete(UUID id, UUID token, Verification verification) {
    if (!id.equals(verification.id())
        || verification.state()
            instanceof com.incode.verification.domain.verification.VerificationState.InProgress) {
      return false;
    }
    var entity =
        VerificationEntity.fromVerification(verification, codec.encode(verification.state()), token);
    return jdbc.sql(
                "UPDATE verifications SET status=:status,state=CAST(:state AS jsonb),"
                    + "claim_token=NULL,claimed_at=NULL,updated_at=CURRENT_TIMESTAMP "
                    + "WHERE id=:id AND status='IN_PROGRESS' "
                    + "AND claim_token=:token")
            .param("status", entity.status())
            .param("state", entity.stateJson())
            .param("id", id)
            .param("token", token)
            .update()
        == 1;
  }

  @Override
  public int expireBatch(Instant now, int limit) {
    return jdbc.sql(
            "WITH candidates AS (SELECT id FROM verifications WHERE status='IN_PROGRESS' "
                + "AND expires_at<=:now ORDER BY expires_at "
                + "FOR UPDATE SKIP LOCKED LIMIT :limit) UPDATE verifications v SET status='FAILED',"
                + "state=CAST(:state AS jsonb),claim_token=NULL,claimed_at=NULL,updated_at=CURRENT_TIMESTAMP "
                + "FROM candidates c "
                + "WHERE v.id=c.id")
        .param("now", Timestamp.from(now))
        .param("limit", limit)
        .param(
            "state",
            codec.encode(
                new com.incode.verification.domain.verification.VerificationState.Failed(
                    new com.incode.verification.domain.provider.ProviderFailure.Timeout())))
        .update();
  }

  @Override
  public Optional<Verification> findById(UUID id) {
    return jdbc.sql(
            "SELECT id,raw_query,normalized_query,started_at,expires_at,state "
                + "FROM verifications WHERE id=:id")
        .param("id", id)
        .query(this::map)
        .optional();
  }

  @Override
  public Optional<Verification> findByQuery(NormalizedQuery query) {
    return jdbc.sql(
            "SELECT id,raw_query,normalized_query,started_at,expires_at,state "
                + "FROM verifications WHERE normalized_query=:normalized "
                + "AND status IN ('COMPLETED','FAILED') ORDER BY updated_at DESC LIMIT 1")
        .param("normalized", query.value())
        .query(this::map)
        .optional();
  }

  private Verification map(ResultSet resultSet, int row) throws java.sql.SQLException {
    return new Verification(
        UUID.fromString(resultSet.getString("id")),
        resultSet.getString("raw_query"),
        new NormalizedQuery(resultSet.getString("normalized_query")),
        resultSet.getTimestamp("started_at").toInstant(),
        resultSet.getTimestamp("expires_at").toInstant(),
        codec.decode(resultSet.getString("state")));
  }
}
