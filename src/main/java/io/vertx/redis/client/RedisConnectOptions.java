/*
 * Copyright 2019 Red Hat, Inc.
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
package io.vertx.redis.client;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.impl.RedisClientVersion;

import java.util.ArrayList;
import java.util.List;

@DataObject
@JsonGen(publicConverter = false)
public abstract class RedisConnectOptions {

  private String user;
  private String password;
  private List<String> endpoints;
  private int maxNestedArrays;
  private boolean protocolNegotiation;
  private ProtocolVersion preferredProtocolVersion;
  private int maxWaitingHandlers;
  private boolean clientIdentification;
  private List<String> librarySuffixes;

  public RedisConnectOptions() {
    maxNestedArrays = 32;
    protocolNegotiation = true;
    maxWaitingHandlers = 2048;
    clientIdentification = true;
  }

  public RedisConnectOptions(RedisOptions options) {
    this();
    setUser(options.getUser());
    setPassword(options.getPassword());
    setEndpoints(new ArrayList<>(options.getEndpoints()));
    setMaxNestedArrays(options.getMaxNestedArrays());
    setProtocolNegotiation(options.isProtocolNegotiation());
    setPreferredProtocolVersion(options.getPreferredProtocolVersion());
    setMaxWaitingHandlers(options.getMaxWaitingHandlers());
    setClientIdentification(options.isClientIdentification());
    setLibrarySuffixes(options.getLibrarySuffixes() == null ? null : new ArrayList<>(options.getLibrarySuffixes()));
  }

  public RedisConnectOptions(RedisConnectOptions other) {
    this();
    setUser(other.getUser());
    setPassword(other.getPassword());
    setEndpoints(new ArrayList<>(other.getEndpoints()));
    setMaxNestedArrays(other.getMaxNestedArrays());
    setProtocolNegotiation(other.isProtocolNegotiation());
    setPreferredProtocolVersion(other.getPreferredProtocolVersion());
    setMaxWaitingHandlers(other.getMaxWaitingHandlers());
    setClientIdentification(other.isClientIdentification());
    setLibrarySuffixes(other.getLibrarySuffixes() == null ? null : new ArrayList<>(other.getLibrarySuffixes()));
  }

  public RedisConnectOptions(JsonObject json) {
    this();
    RedisConnectOptionsConverter.fromJson(json, this);
  }

  /**
   * Tune how much nested arrays are allowed on a redis response. This affects the parser performance.
   *
   * @return the configured max nested arrays allowance.
   */
  public int getMaxNestedArrays() {
    return maxNestedArrays;
  }

  /**
   * Tune how much nested arrays are allowed on a redis response. This affects the parser performance.
   *
   * @param maxNestedArrays the configured max nested arrays allowance.
   * @return fluent self.
   */
  public RedisConnectOptions setMaxNestedArrays(int maxNestedArrays) {
    this.maxNestedArrays = maxNestedArrays;
    return this;
  }

  /**
   * Should the client perform {@code RESP} protocol negotiation during the connection handshake.
   * By default this is {@code true}, but there are situations when using broken servers it may
   * be useful to skip this and always fallback to {@code RESP2} without using the {@code HELLO}
   * command.
   *
   * @return true to perform negotiation.
   */
  public boolean isProtocolNegotiation() {
    return protocolNegotiation;
  }

  /**
   * Should the client perform {@code REST} protocol negotiation during the connection acquire.
   * By default this is {@code true}, but there are situations when using broken servers it may
   * be useful to skip this and always fallback to {@code RESP2} without using the {@code HELLO}
   * command.
   *
   * @param protocolNegotiation false to disable {@code HELLO} (not recommended) unless reasons...
   * @return fluent self
   */
  public RedisConnectOptions setProtocolNegotiation(boolean protocolNegotiation) {
    this.protocolNegotiation = protocolNegotiation;
    return this;
  }

  /**
   * Returns the preferred protocol version to be used during protocol negotiation. When not set,
   * defaults to RESP 3. When protocol negotiation is disabled, this setting has no effect.
   *
   * @return preferred protocol version
   */
  public ProtocolVersion getPreferredProtocolVersion() {
    return preferredProtocolVersion;
  }

  /**
   * Sets the preferred protocol version to be used during protocol negotiation. When not set,
   * defaults to RESP 3. When protocol negotiation is disabled, this setting has no effect.
   *
   * @param preferredProtocolVersion preferred protocol version
   * @return fluent self
   */
  public RedisConnectOptions setPreferredProtocolVersion(ProtocolVersion preferredProtocolVersion) {
    this.preferredProtocolVersion = preferredProtocolVersion;
    return this;
  }

  /**
   * Get the default username for Redis connections.
   *
   * @return username
   */
  public String getUser() {
    return user;
  }

  /**
   * Set the default username for Redis connections.
   *
   * @param user the default username
   * @return fluent self
   */
  public RedisConnectOptions setUser(String user) {
    this.user = user;
    return this;
  }

  /**
   * Get the default password for Redis connections.
   *
   * @return password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Set the default password for Redis connections.
   *
   * @param password the default password
   * @return fluent self
   */
  public RedisConnectOptions setPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Gets the redis endpoint to use
   *
   * @return the Redis connection string URI
   */
  @GenIgnore
  public String getEndpoint() {
    if (endpoints == null || endpoints.isEmpty()) {
      return RedisOptions.DEFAULT_ENDPOINT;
    }

    return endpoints.get(0);
  }

  /**
   * Adds a connection string (endpoint) to use while connecting to the redis server. Only the cluster mode will
   * consider more than 1 element. If more are provided, they are not considered by the client when in single server mode.
   *
   * @param connectionString a string URI following the scheme: redis://[username:password@][host][:port][/database]
   * @return fluent self.
   *
   * @see <a href="https://www.iana.org/assignments/uri-schemes/prov/redis">Redis scheme on iana.org</a>
   */
  @GenIgnore
  public RedisConnectOptions addConnectionString(String connectionString) {
    if (endpoints == null) {
      endpoints = new ArrayList<>();
    }
    this.endpoints.add(connectionString);
    return this;
  }

  /**
   * Sets a single connection string (endpoint) to use while connecting to the redis server.
   * Will replace the previously configured connection strings.
   *
   * @param connectionString a string following the scheme: redis://[username:password@][host][:port][/[database].
   * @return fluent self.
   * @see <a href="https://www.iana.org/assignments/uri-schemes/prov/redis">Redis scheme on iana.org</a>
   */
  @GenIgnore
  public RedisConnectOptions setConnectionString(String connectionString) {
    if (endpoints == null) {
      endpoints = new ArrayList<>();
    } else {
      endpoints.clear();
    }

    this.endpoints.add(connectionString);
    return this;
  }

  /**
   * Gets the list of redis endpoints to use (mostly used while connecting to a cluster)
   *
   * @return list of socket addresses.
   */
  public List<String> getEndpoints() {
    if (endpoints == null) {
      endpoints = new ArrayList<>();
      endpoints.add(RedisOptions.DEFAULT_ENDPOINT);
    }
    return endpoints;
  }

  /**
   * Set the endpoints to use while connecting to the redis server. Only the cluster mode will consider more than
   * 1 element. If more are provided, they are not considered by the client when in single server mode.
   *
   * @param endpoints list of socket addresses.
   * @return fluent self.
   */
  public RedisConnectOptions setEndpoints(List<String> endpoints) {
    this.endpoints = endpoints;
    return this;
  }

  /**
   * The client will always work on pipeline mode, this means that messages can start queueing. You can control how much
   * backlog you're willing to accept. This methods returns how much handlers is the client willing to queue.
   *
   * @return max allowed queued waiting handlers.
   */
  public int getMaxWaitingHandlers() {
    return maxWaitingHandlers;
  }

  /**
   * The client will always work on pipeline mode, this means that messages can start queueing. You can control how much
   * backlog you're willing to accept. This methods sets how much handlers is the client willing to queue.
   *
   * @param maxWaitingHandlers max allowed queued waiting handlers.
   * @return fluent self.
   */
  public RedisConnectOptions setMaxWaitingHandlers(int maxWaitingHandlers) {
    this.maxWaitingHandlers = maxWaitingHandlers;
    return this;
  }

  /**
   * Whether the client identifies itself to the server after the handshake using {@code CLIENT SETINFO}
   * (Redis 7.2+), so that connections are attributable in {@code CLIENT INFO} / {@code CLIENT LIST}.
   * Defaults to {@code true}.
   *
   * @return true when self-identification is enabled.
   */
  public boolean isClientIdentification() {
    return clientIdentification;
  }

  /**
   * Sets whether the client identifies itself to the server after the handshake using
   * {@code CLIENT SETINFO} (Redis 7.2+). Disabling this skips the identification commands entirely.
   *
   * @param clientIdentification false to disable self-identification.
   * @return fluent self.
   */
  public RedisConnectOptions setClientIdentification(boolean clientIdentification) {
    this.clientIdentification = clientIdentification;
    return this;
  }

  /**
   * Gets the framework suffixes appended to the reported {@code lib-name}. These allow upstream
   * libraries (for example Quarkus) to attribute themselves on top of the base
   * {@code vertx-redis-client} name.
   *
   * @return the configured library suffixes, may be {@code null}.
   */
  public List<String> getLibrarySuffixes() {
    return librarySuffixes;
  }

  /**
   * Sets the framework suffixes appended to the reported {@code lib-name}. The composed name has the
   * form {@code vertx-redis-client(suffix1;suffix2)}. Suffixes must not contain whitespace, {@code (},
   * {@code )} or {@code ;}.
   *
   * @param librarySuffixes the library suffixes, may be {@code null} to report only the base name.
   * @return fluent self.
   * @throws IllegalArgumentException if any suffix contains an illegal character.
   */
  public RedisConnectOptions setLibrarySuffixes(List<String> librarySuffixes) {
    if (librarySuffixes != null) {
      for (String suffix : librarySuffixes) {
        validateSuffix(suffix);
      }
    }
    this.librarySuffixes = librarySuffixes;
    return this;
  }

  /**
   * Adds a single framework suffix appended to the reported {@code lib-name}.
   *
   * @param librarySuffix the library suffix, must not contain whitespace, {@code (}, {@code )} or {@code ;}.
   * @return fluent self.
   * @throws IllegalArgumentException if the suffix contains an illegal character.
   */
  @GenIgnore
  public RedisConnectOptions addLibrarySuffix(String librarySuffix) {
    validateSuffix(librarySuffix);
    if (librarySuffixes == null) {
      librarySuffixes = new ArrayList<>();
    }
    librarySuffixes.add(librarySuffix);
    return this;
  }

  private static void validateSuffix(String suffix) {
    if (suffix != null && !RedisClientVersion.isValidSuffix(suffix)) {
      throw new IllegalArgumentException(
        "Library suffix must not contain whitespace, '(', ')' or ';': " + suffix);
    }
  }

  /**
   * Converts this object to JSON notation.
   *
   * @return JSON
   */
  public JsonObject toJson() {
    final JsonObject json = new JsonObject();
    RedisConnectOptionsConverter.toJson(this, json);
    return json;
  }
}
