package io.vertx.tests.redis.internal;

import io.vertx.redis.client.impl.RedisClientVersion;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedisClientVersionTest {

  @Test
  public void testLibraryName() {
    assertEquals("vertx-redis-client", RedisClientVersion.NAME);
  }

  @Test
  public void testLibraryVersionIsNeverAPlaceholder() {
    // VERSION is null when running from class files (no JAR manifest); it must never fall back to a
    // placeholder such as "unknown", and when present it must be a non-empty string
    String version = RedisClientVersion.VERSION;
    assertNotEquals("unknown", version);
    if (version != null) {
      assertFalse(version.isEmpty());
    }
  }

  @Test
  public void testFormatLibraryNameWithoutSuffixes() {
    assertEquals("vertx-redis-client", RedisClientVersion.formatLibraryName(null));
    assertEquals("vertx-redis-client", RedisClientVersion.formatLibraryName(Collections.emptyList()));
  }

  @Test
  public void testFormatLibraryNameWithSingleSuffix() {
    assertEquals("vertx-redis-client(quarkus-redis_v3.x)",
      RedisClientVersion.formatLibraryName(Collections.singletonList("quarkus-redis_v3.x")));
  }

  @Test
  public void testFormatLibraryNameWithMultipleSuffixes() {
    assertEquals("vertx-redis-client(quarkus-redis_v3.x;spring-data_v3.2)",
      RedisClientVersion.formatLibraryName(Arrays.asList("quarkus-redis_v3.x", "spring-data_v3.2")));
  }

  @Test
  public void testFormatLibraryNameIgnoresBlankAndInvalidSuffixes() {
    assertEquals("vertx-redis-client(valid)",
      RedisClientVersion.formatLibraryName(Arrays.asList(null, "", "   ", "has space", "has(paren", "has;semi", "valid")));
  }

  @Test
  public void testIsValidSuffix() {
    assertTrue(RedisClientVersion.isValidSuffix("quarkus-redis_v3.x"));
    assertFalse(RedisClientVersion.isValidSuffix("has space"));
    assertFalse(RedisClientVersion.isValidSuffix("has(paren"));
    assertFalse(RedisClientVersion.isValidSuffix("has)paren"));
    assertFalse(RedisClientVersion.isValidSuffix("has;semi"));
    assertFalse(RedisClientVersion.isValidSuffix(null));
  }

  @Test
  public void testIsValidSuffixRejectsNonAscii() {
    // control characters and DEL
    assertFalse(RedisClientVersion.isValidSuffix("has\ttab"));
    assertFalse(RedisClientVersion.isValidSuffix("hasdel"));
    // non-ASCII BMP characters
    assertFalse(RedisClientVersion.isValidSuffix("café"));
    // characters outside the Basic Multilingual Plane (encoded as a surrogate pair)
    assertFalse(RedisClientVersion.isValidSuffix("emoji😀"));
  }

  @Test
  public void testFormatLibraryNameIgnoresNonAsciiSuffixes() {
    assertEquals("vertx-redis-client(valid)",
      RedisClientVersion.formatLibraryName(Arrays.asList("café", "emoji😀", "valid")));
  }
}
