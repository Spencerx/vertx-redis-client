package io.vertx.tests.redis.client;

import io.vertx.junit5.RunTestOnContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.impl.RedisClientVersion;
import io.vertx.tests.redis.containers.RedisStandalone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.vertx.redis.client.Command.CLIENT;
import static io.vertx.redis.client.Command.GET;
import static io.vertx.redis.client.Command.PING;
import static io.vertx.redis.client.Command.SET;
import static io.vertx.redis.client.Request.cmd;
import static io.vertx.tests.redis.client.TestUtils.randomKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@Testcontainers
public class RedisClientSelfIdentificationTest {

  // CLIENT SETINFO is available since Redis 7.2
  @Container
  public static final RedisStandalone redis = RedisStandalone.builder().setVersion("7.2").build();

  @RegisterExtension
  public final RunTestOnContext context = new RunTestOnContext();

  private Redis client;

  @AfterEach
  public void after() {
    if (client != null) {
      client.close();
    }
  }

  // L3 native endpoint: assert the server reports our lib-name/lib-ver via CLIENT INFO
  @Test
  public void testClientInfoReportsLibNameAndVersion(VertxTestContext test) {
    client = Redis.createClient(context.vertx(), new RedisOptions().setConnectionString(redis.getRedisUri()));
    client.connect()
      .compose(conn -> conn.send(cmd(CLIENT).arg("INFO")))
      .onComplete(test.succeeding(info -> {
        String reply = info.toString();
        assertTrue(reply.contains("lib-name=" + RedisClientVersion.NAME),
          "CLIENT INFO should contain lib-name, was: " + reply);
        assertTrue(reply.contains("lib-ver=" + RedisClientVersion.VERSION),
          "CLIENT INFO should contain lib-ver, was: " + reply);
        test.completeNow();
      }));
  }

  // L3 native endpoint: a framework suffix is appended to lib-name and visible via CLIENT INFO
  @Test
  public void testClientInfoReportsLibNameWithSuffix(VertxTestContext test) {
    client = Redis.createClient(context.vertx(), new RedisOptions()
      .setConnectionString(redis.getRedisUri())
      .addLibrarySuffix("quarkus-redis_v3.x"));
    client.connect()
      .compose(conn -> conn.send(cmd(CLIENT).arg("INFO")))
      .onComplete(test.succeeding(info -> {
        String reply = info.toString();
        assertTrue(reply.contains("lib-name=" + RedisClientVersion.NAME + "(quarkus-redis_v3.x)"),
          "CLIENT INFO should contain the composed lib-name, was: " + reply);
        test.completeNow();
      }));
  }

  // L3 native endpoint: disabling identification means no lib-name is reported
  @Test
  public void testClientInfoReportsNoLibNameWhenDisabled(VertxTestContext test) {
    client = Redis.createClient(context.vertx(), new RedisOptions()
      .setConnectionString(redis.getRedisUri())
      .setClientIdentification(false));
    client.connect()
      .compose(conn -> conn.send(cmd(CLIENT).arg("INFO")))
      .onComplete(test.succeeding(info -> {
        String reply = info.toString();
        assertFalse(reply.contains("lib-name=" + RedisClientVersion.NAME),
          "CLIENT INFO should not report a lib-name when identification is disabled, was: " + reply);
        test.completeNow();
      }));
  }

  // L2 integration: the connection remains fully usable once self-identification has run
  @Test
  public void testConnectionUsableAfterSelfIdentification(VertxTestContext test) {
    final String key = randomKey();
    client = Redis.createClient(context.vertx(), new RedisOptions().setConnectionString(redis.getRedisUri()));
    client.connect()
      .compose(conn -> conn.send(cmd(PING))
        .compose(pong -> {
          assertEquals("PONG", pong.toString());
          return conn.send(cmd(SET).arg(key).arg("value"));
        })
        .compose(set -> conn.send(cmd(GET).arg(key))))
      .onComplete(test.succeeding(value -> {
        assertEquals("value", value.toString());
        test.completeNow();
      }));
  }
}
