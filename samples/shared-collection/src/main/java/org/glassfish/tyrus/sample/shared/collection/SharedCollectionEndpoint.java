/*
 * Copyright (c) 2014, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.shared.collection;

import java.io.IOException;
import java.io.Reader;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * @author Pavel Bucek
 */
@ServerEndpoint("/ws/collection")
public class SharedCollectionEndpoint {

    private static final Deque<Tuple<Session, JsonObject>> broadcastQueue =
            new ConcurrentLinkedDeque<Tuple<Session, JsonObject>>();
    private static volatile Session session;
    private static volatile boolean broadcasting;

    @OnOpen
    public void onOpen(Session s) {
        SharedCollectionEndpoint.session = s;

        final JsonObjectBuilder mapRepresentation = Json.createObjectBuilder();

        for (Map.Entry<String, String> entry : SharedCollection.map.entrySet()) {
            mapRepresentation.add(entry.getKey(), entry.getValue());
        }

        final JsonObjectBuilder event = Json.createObjectBuilder();
        event.add("event", "init");
        event.add("map", mapRepresentation.build());

        try {
            s.getBasicRemote().sendText(event.build().toString());
        } catch (IOException e) {
            // we don't care about that for now.
        }
    }

    @OnMessage
    public void onMessage(Reader message) {
        final JsonObject jsonObject = Json.createReader(message).readObject();
        final String event = jsonObject.getString("event");

        switch (event) {
            case "put":
                SharedCollection.map.put(jsonObject.getString("key"), jsonObject.getString("value"));
                SharedCollection.broadcast(jsonObject);
                break;
            case "remove":
                SharedCollection.map.remove(jsonObject.getString("key"));
                SharedCollection.broadcast(jsonObject);
                break;
            case "clear":
                SharedCollection.map.clear();
                SharedCollection.broadcast(jsonObject);
                break;
        }
    }

    static void broadcast(JsonObject object) {
        broadcastQueue.add(new Tuple<Session, JsonObject>(null, object));

        processQueue();
    }

    private static void processQueue() {

        if (broadcasting) {
            return;
        }

        try {
            synchronized (broadcastQueue) {
                broadcasting = true;

                if (!broadcastQueue.isEmpty()) {
                    while (!broadcastQueue.isEmpty()) {
                        final Tuple<Session, JsonObject> t = broadcastQueue.remove();

                        final Session s = SharedCollectionEndpoint.session;
                        final String message = t.second.toString();

                        for (Session session : s.getOpenSessions()) {
                            // if (!session.getId().equals(s.getId())) {
                            try {
                                session.getBasicRemote().sendText(message);
                            } catch (IOException e) {
                                // we don't care about that for now.
                            }
                            // }
                        }
                    }
                }
            }
        } finally {
            broadcasting = false;
            if (!broadcastQueue.isEmpty()) {
                processQueue();
            }
        }
    }

    @OnError
    public void onError(Throwable t) {
        System.out.println("# onError");
        t.printStackTrace();
    }

    private static class Tuple<T, U> {
        public final T first;
        public final U second;

        private Tuple(T first, U second) {
            this.first = first;
            this.second = second;
        }
    }
}
