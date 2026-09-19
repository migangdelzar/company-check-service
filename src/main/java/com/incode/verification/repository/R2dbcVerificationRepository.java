package com.incode.verification.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.mapper.VerificationStateMapper;
import com.incode.verification.repository.entity.VerificationEntity;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.Verification;
import com.incode.verification.service.model.VerificationState;
import com.incode.verification.util.UuidV7;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcVerificationRepository implements VerificationRepository {
  private final DatabaseClient database;
  private final VerificationStateMapper codec;

  public R2dbcVerificationRepository(DatabaseClient database, ObjectMapper mapper) {
    this.database = database;
    this.codec = new VerificationStateMapper(mapper);
  }

  @Override
  public Mono<Boolean> insertInProgress(Verification verification) {
    var entity =
        VerificationEntity.fromVerification(verification, codec.encode(verification.state()), null);
    return database
        .sql(
            "INSERT INTO verifications(id,raw_query,normalized_query,started_at,expires_at,status,state) "
                + "VALUES (:id,:raw,:normalized,:started,:expires,:status,CAST(:state AS jsonb)) "
                + "ON CONFLICT (id) DO NOTHING")
        .bind("id", entity.id())
        .bind("raw", entity.rawQuery())
        .bind("normalized", entity.normalizedQuery())
        .bind("started", entity.startedAt())
        .bind("expires", entity.expiresAt())
        .bind("status", entity.status())
        .bind("state", entity.stateJson())
        .fetch()
        .rowsUpdated()
        .map(updated -> updated == 1);
  }

  @Override
  public Mono<UUID> claim(UUID id) {
    UUID token = UuidV7.generate();
    return database
        .sql(
            "UPDATE verifications SET claim_token=:token,claimed_at=CURRENT_TIMESTAMP,"
                + "updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status='IN_PROGRESS' "
                + "AND claim_token IS NULL")
        .bind("id", id)
        .bind("token", token)
        .fetch()
        .rowsUpdated()
        .flatMap(updated -> updated == 1 ? Mono.just(token) : Mono.empty());
  }

  @Override
  public Mono<Boolean> complete(UUID id, UUID token, Verification verification) {
    if (!id.equals(verification.id())
        || verification.state() instanceof VerificationState.InProgress) {
      return Mono.just(false);
    }
    var entity =
        VerificationEntity.fromVerification(
            verification, codec.encode(verification.state()), token);
    return database
        .sql(
            "UPDATE verifications SET status=:status,state=CAST(:state AS jsonb),"
                + "claim_token=NULL,claimed_at=NULL,updated_at=CURRENT_TIMESTAMP "
                + "WHERE id=:id AND status='IN_PROGRESS' "
                + "AND claim_token=:token")
        .bind("status", entity.status())
        .bind("state", entity.stateJson())
        .bind("id", id)
        .bind("token", token)
        .fetch()
        .rowsUpdated()
        .map(updated -> updated == 1);
  }

  @Override
  public Mono<Integer> expireBatch(Instant now, int limit) {
    var state =
        codec.encode(
            new VerificationState.Failed(
                new com.incode.verification.service.model.ProviderFailure.Timeout()));
    return database
        .sql(
            "WITH candidates AS (SELECT id FROM verifications WHERE status='IN_PROGRESS' "
                + "AND expires_at<=:now ORDER BY expires_at "
                + "FOR UPDATE SKIP LOCKED LIMIT :limit) UPDATE verifications v SET status='FAILED',"
                + "state=CAST(:state AS jsonb),claim_token=NULL,claimed_at=NULL,updated_at=CURRENT_TIMESTAMP "
                + "FROM candidates c "
                + "WHERE v.id=c.id")
        .bind("now", now)
        .bind("limit", limit)
        .bind("state", state)
        .fetch()
        .rowsUpdated()
        .map(Math::toIntExact);
  }

  @Override
  public Mono<Verification> findById(UUID id) {
    return database
        .sql(
            "SELECT id,raw_query,normalized_query,started_at,expires_at,state "
                + "FROM verifications WHERE id=:id")
        .bind("id", id)
        .map(this::map)
        .one();
  }

  @Override
  public Mono<Verification> findByQuery(NormalizedQuery query) {
    return database
        .sql(
            "SELECT id,raw_query,normalized_query,started_at,expires_at,state "
                + "FROM verifications WHERE normalized_query=:normalized "
                + "AND status IN ('COMPLETED','FAILED') ORDER BY updated_at DESC LIMIT 1")
        .bind("normalized", query.value())
        .map(this::map)
        .one();
  }

  private Verification map(Row row, io.r2dbc.spi.RowMetadata metadata) {
    Object rawState = row.get("state");
    String stateJson = rawState instanceof Json json ? json.asString() : String.valueOf(rawState);
    return new Verification(
        row.get("id", UUID.class),
        row.get("raw_query", String.class),
        new NormalizedQuery(row.get("normalized_query", String.class)),
        instant(row, "started_at"),
        instant(row, "expires_at"),
        codec.decode(stateJson));
  }

  private static Instant instant(Row row, String column) {
    Object value = row.get(column);
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toInstant();
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.toInstant(ZoneOffset.UTC);
    }
    throw new IllegalStateException("Unsupported timestamp value for " + column + ": " + value);
  }
}
