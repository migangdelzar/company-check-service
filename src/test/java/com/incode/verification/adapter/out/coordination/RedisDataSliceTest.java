package com.incode.verification.adapter.out.coordination;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataRedisTest(properties = {"spring.data.redis.host=localhost", "spring.data.redis.port=6379"})
@ActiveProfiles("distributed")
class RedisDataSliceTest {
  @Autowired private RedisConnectionFactory connectionFactory;

  @Autowired private StringRedisTemplate redis;

  @Test
  void configuresRedisTemplateAndConnectionFactory() {
    assertNotNull(connectionFactory);
    assertNotNull(redis);
  }
}
