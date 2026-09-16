package com.incode.verification.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.domain.aggregate.Verification;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import com.incode.verification.domain.valueobject.UuidV7;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public final class JdbcVerificationRepository implements VerificationRepository {
  private final JdbcClient jdbc;
  private final VerificationStateCodec codec;

  public JdbcVerificationRepository(JdbcClient jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.codec = new VerificationStateCodec(mapper);
  }

  @Override
  public void insertInProgress(Verification v) {
    var e = VerificationEntity.from(v, codec.encode(v.state()), null);
    try {
      jdbc.sql(
              "INSERT INTO verifications(id,raw_query,normalized_query,started_at,expires_at,status,state) "
                  + "VALUES (:id,:raw,:normalized,:started,:expires,:status,CAST(:state AS jsonb))")
          .param("id", e.id())
          .param("raw", e.rawQuery())
          .param("normalized", e.normalizedQuery())
          .param("started", e.startedAt())
          .param("expires", e.expiresAt())
          .param("status", e.status())
          .param("state", e.stateJson())
          .update();
    } catch (DataIntegrityViolationException exception) {
      throw new com.incode.verification.application.port.out.VerificationAlreadyExistsException(
          exception);
    }
  }

  @Override
  public void update(Verification v) {
    throw new UnsupportedOperationException("terminal updates require a claim token");
  }

  @Override
  public UUID claim(UUID id) {
    UUID token = UuidV7.generate();
    int changed =
        jdbc.sql(
                "UPDATE verifications SET claim_token=:token,claimed_at=CURRENT_TIMESTAMP,"
                    + "updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status='IN_PROGRESS' "
                    + "AND claim_token IS NULL")
            .param("id", id)
            .param("token", token)
            .update();
    return changed == 1 ? token : null;
  }

  @Override
  public boolean updateTerminal(UUID id, UUID token, Verification v) {
    var e = VerificationEntity.from(v, codec.encode(v.state()), token);
    return jdbc.sql(
                "UPDATE verifications SET status=:status,state=CAST(:state AS jsonb),"
                    + "updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status='IN_PROGRESS' "
                    + "AND claim_token=:token")
            .param("status", e.status())
            .param("state", e.stateJson())
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
        .param("now", now)
        .param("limit", limit)
        .param(
            "state",
            codec.encode(
                new com.incode.verification.domain.type.VerificationState.Failed(
                    new com.incode.verification.domain.type.ProviderFailure.Timeout())))
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
  public Optional<Verification> findTerminalByQuery(NormalizedQuery query) {
    return jdbc.sql(
            "SELECT id,raw_query,normalized_query,started_at,expires_at,state "
                + "FROM verifications WHERE normalized_query=:normalized "
                + "AND status IN ('COMPLETED','FAILED') ORDER BY updated_at DESC LIMIT 1")
        .param("normalized", query.value())
        .query(this::map)
        .optional();
  }

  private Verification map(ResultSet rs, int row) throws java.sql.SQLException {
    return new Verification(
        UUID.fromString(rs.getString("id")),
        rs.getString("raw_query"),
        new NormalizedQuery(rs.getString("normalized_query")),
        rs.getTimestamp("started_at").toInstant(),
        rs.getTimestamp("expires_at").toInstant(),
        codec.decode(rs.getString("state")));
  }
}
