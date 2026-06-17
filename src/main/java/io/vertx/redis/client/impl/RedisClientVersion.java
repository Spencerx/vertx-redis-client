/*
 * Copyright 2024 Red Hat, Inc.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * <p>
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p>
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 * <p>
 * You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.redis.client.impl;

import java.util.List;

/**
 * Identification of this client library, reported to the server via {@code CLIENT SETINFO}
 * (Redis 7.2+) so that connections are attributable in {@code CLIENT INFO} / {@code CLIENT LIST}.
 */
public final class RedisClientVersion {

  /**
   * The library name reported to the server as {@code lib-name}.
   */
  public static final String NAME = "vertx-redis-client";

  /**
   * The library version reported to the server as {@code lib-ver}. Resolved from the JAR manifest
   * at runtime, falling back to {@code "unknown"} when the implementation version is not available
   * (for example, when running from class files rather than a packaged JAR).
   */
  public static final String VERSION = resolveVersion();

  private RedisClientVersion() {
  }

  private static String resolveVersion() {
    String version = RedisClientVersion.class.getPackage().getImplementationVersion();
    return version != null ? version : "unknown";
  }

  /**
   * Composes the {@code lib-name} reported to the server from the base library name and the optional
   * framework suffixes provided by upstream libraries (for example Quarkus).
   * <p>
   * With no suffixes the result is just {@link #NAME}. When suffixes are present they are joined with
   * {@code ;} and wrapped in parentheses, for example {@code vertx-redis-client(quarkus-redis_v3.x)}.
   * Blank suffixes and suffixes containing characters rejected by {@code CLIENT SETINFO}
   * (whitespace, {@code (}, {@code )} or {@code ;}) are ignored.
   * <p>
   * Each suffix is treated as an opaque string; callers are expected to follow the {@code name_vversion}
   * convention recommended by the Redis {@code CLIENT SETINFO} documentation, but the convention is
   * not enforced.
   *
   * @param suffixes the framework suffixes, may be {@code null} or empty
   * @return the composed {@code lib-name}
   */
  public static String formatLibraryName(List<String> suffixes) {
    if (suffixes == null || suffixes.isEmpty()) {
      return NAME;
    }
    StringBuilder joined = new StringBuilder();
    for (String suffix : suffixes) {
      if (suffix == null || suffix.trim().isEmpty() || !isValidSuffix(suffix)) {
        continue;
      }
      if (joined.length() > 0) {
        joined.append(';');
      }
      joined.append(suffix.trim());
    }
    if (joined.length() == 0) {
      return NAME;
    }
    return NAME + "(" + joined + ")";
  }

  /**
   * A suffix is valid when it contains no characters that {@code CLIENT SETINFO} rejects or that would
   * break the composed {@code lib-name} grammar: whitespace, {@code (}, {@code )} and {@code ;}.
   *
   * @param suffix the suffix to validate
   * @return {@code true} if the suffix is safe to include in the {@code lib-name}
   */
  public static boolean isValidSuffix(String suffix) {
    if (suffix == null) {
      return false;
    }
    for (int i = 0; i < suffix.length(); i++) {
      char c = suffix.charAt(i);
      if (Character.isWhitespace(c) || c == '(' || c == ')' || c == ';') {
        return false;
      }
    }
    return true;
  }
}
