package io.vertx.redis.client.impl;

import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisCluster;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.RequestGrouping;
import io.vertx.redis.client.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class RedisClusterImpl implements RedisCluster {
  private static final Logger LOG = LoggerFactory.getLogger(RedisClusterImpl.class);

  private final Redis client;
  private final RedisConnection connection;

  public RedisClusterImpl(Redis client) {
    if (!(client instanceof RedisClusterClient)) {
      throw new IllegalArgumentException("Given Redis client is not a Redis cluster client: " + client);
    }

    this.client = client;
    this.connection = null;
  }

  public RedisClusterImpl(RedisConnection connection) {
    if (!(connection instanceof RedisClusterConnection)) {
      throw new IllegalArgumentException("Given Redis connection is not a Redis cluster connection: " + connection);
    }

    this.client = null;
    this.connection = connection;
  }

  @Override
  public Future<List<Response>> onAllNodes(Request request) {
    return onAllNodes(request, false);
  }

  @Override
  public Future<List<Response>> onAllMasterNodes(Request request) {
    return onAllNodes(request, true);
  }

  private Future<List<Response>> onAllNodes(Request request, boolean mastersOnly) {
    if (connection != null) {
      return onAllNodes(request, mastersOnly, (RedisClusterConnection) connection);
    } else /* client != null */ {
      return client.connect()
        .compose(conn -> onAllNodes(request, mastersOnly, (RedisClusterConnection) conn)
          .andThen(ignored -> {
            conn.close().onFailure(LOG::warn);
          }));
    }
  }

  private Future<List<Response>> onAllNodes(Request request, boolean mastersOnly, RedisClusterConnection conn) {
    return conn.sharedSlots.get()
      .compose(slots -> {
        String[] endpoints = mastersOnly ? slots.masterEndpoints() : slots.endpoints();
        HashSet<String> endpointsSet = new HashSet<>(endpoints.length);
        Collections.addAll(endpointsSet, endpoints);
        String[] uniqueEndpoints = endpointsSet.toArray(new String[0]);

        Promise<List<Response>> promise = conn.vertx.promise();
        onAllNodes(uniqueEndpoints, 0, request, new ArrayList<>(uniqueEndpoints.length), conn, promise);
        return promise.future();
      });
  }

  private void onAllNodes(String[] endpoints, int index, Request request, List<Response> result, RedisClusterConnection conn, Completable<List<Response>> promise) {
    if (index >= endpoints.length) {
      promise.succeed(result);
      return;
    }

    conn.send(endpoints[index], RedisClusterConnection.RETRIES, request, (res, err) -> {
      if (err == null) {
        result.add(res);
        onAllNodes(endpoints, index + 1, request, result, conn, promise);
      } else {
        promise.fail(err);
      }
    });
  }

  @Override
  public Future<RequestGrouping> groupByNodes(List<Request> requests) {
    if (connection != null) {
      return groupByNodes(requests, (RedisClusterConnection) connection);
    } else /* client != null */ {
      return client.connect()
        .compose(conn -> groupByNodes(requests, (RedisClusterConnection) conn)
          .andThen(ignored -> {
            conn.close().onFailure(LOG::warn);
          }));
    }
  }

  private Future<RequestGrouping> groupByNodes(List<Request> requests, RedisClusterConnection conn) {
    return conn.sharedSlots.get()
      .compose(slots -> {
        Map<String, List<Request>> grouping = new HashMap<>();
        List<Request> ambiguous = null;

        for (Request request : requests) {
          RequestImpl req = (RequestImpl) request;
          CommandImpl cmd = (CommandImpl) req.command();
          List<byte[]> keys = req.keys();

          if (cmd.needsGetKeys() || keys.isEmpty()) {
            if (ambiguous == null) {
              ambiguous = new ArrayList<>();
            }
            ambiguous.add(request);
          } else if (keys.size() == 1) {
            int slot = ZModem.generate(keys.get(0));
            String endpoint = slots.endpointsForKey(slot)[0];
            grouping.computeIfAbsent(endpoint, ignored -> new ArrayList<>()).add(request);
          } else {
            String endpoint = null;
            for (byte[] key : keys) {
              int slot = ZModem.generate(key);
              String endpointForSlot = slots.endpointsForKey(slot)[0];
              if (endpoint == null) {
                endpoint = endpointForSlot;
              } else if (!endpointForSlot.equals(endpoint)) {
                return Future.failedFuture(conn.buildCrossslotFailureMsg(req));
              }
            }

            grouping.computeIfAbsent(endpoint, ignored -> new ArrayList<>()).add(request);
          }
        }

        return Future.succeededFuture(new RequestGrouping(grouping.values(), ambiguous != null ? ambiguous : Collections.emptyList()));
      });
  }
}
