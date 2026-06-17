package io.vertx.tests.redis.client;

import io.vertx.junit5.RunTestOnContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import io.vertx.tests.redis.containers.RedisStandalone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.vertx.redis.client.Command.GET;
import static io.vertx.redis.client.Command.PING;
import static io.vertx.redis.client.Command.SET;
import static io.vertx.redis.client.Request.cmd;
import static io.vertx.tests.redis.client.TestUtils.randomKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
@Testcontainers
public class RedisClientSelfIdentificationToleranceTest {

  // CLIENT SETINFO does not exist before Redis 7.2; 6.2 must still connect and work
  @Container
  public static final RedisStandalone redis = RedisStandalone.builder().setVersion("6.2").build();

  @RegisterExtension
  public final RunTestOnContext context = new RunTestOnContext();

  private Redis client;

  @AfterEach
  public void after() {
    if (client != null) {
      client.close();
    }
  }

  // L2 tolerance: connecting to a server that rejects CLIENT SETINFO must still succeed
  @Test
  public void testConnectSucceedsOnOlderServer(VertxTestContext test) {
    client = Redis.createClient(context.vertx(), new RedisOptions().setConnectionString(redis.getRedisUri()));
    client.connect()
      .compose(conn -> conn.send(cmd(PING)))
      .onComplete(test.succeeding(pong -> {
        assertEquals("PONG", pong.toString());
        test.completeNow();
      }));
  }

  // L2 tolerance: the connection remains fully usable despite the unsupported identification command
  @Test
  public void testConnectionUsableOnOlderServer(VertxTestContext test) {
    final String key = randomKey();
    client = Redis.createClient(context.vertx(), new RedisOptions().setConnectionString(redis.getRedisUri()));
    client.connect()
      .compose(conn -> conn.send(cmd(SET).arg(key).arg("value"))
        .compose(set -> conn.send(cmd(GET).arg(key))))
      .onComplete(test.succeeding(value -> {
        assertEquals("value", value.toString());
        test.completeNow();
      }));
  }
}
