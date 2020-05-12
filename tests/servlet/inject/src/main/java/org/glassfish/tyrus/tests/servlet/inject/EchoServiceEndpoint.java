/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.tyrus.tests.servlet.inject;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author Petr Janouch
 */
@ServerEndpoint("/tyrus319Endpoint")
@Stateless
public class EchoServiceEndpoint {

    @EJB(name = "EchoEJB", beanInterface = EchoService.class, mappedName = "corbaname:iiop:localhost:3700#ejb/EchoEJB")
    private EchoService echoService;

    @OnMessage
    public String onMessage(String message, Session session) {
        return echoService.echo(message);
    }
}
