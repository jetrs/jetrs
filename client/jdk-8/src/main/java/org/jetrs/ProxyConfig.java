/* Copyright (c) 2023 JetRS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.jetrs;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.concurrent.locks.ReentrantLock;

import org.libj.net.ProxyURI;

class ProxyConfig extends ProxyURI {
  private static class LockingAuthenticator extends Authenticator {
    final ReentrantLock lock = new ReentrantLock();
    private String username;
    private char[] password;

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(username, password);
    }
  }

  private static LockingAuthenticator authenticator;

  ProxyConfig(final String proxyUri) {
    super(proxyUri);
    if (username != null) {
      if (authenticator == null) {
        synchronized (this) {
          if (authenticator == null) {
            authenticator = new LockingAuthenticator();
            Authenticator.setDefault(authenticator);
          }
        }
      }
    }
  }

  void acquire() {
    if (username == null)
      return;

    if (authenticator == null)
      return;

    synchronized (authenticator.lock) {
      authenticator.lock.lock();
    }

    authenticator.username = username;
    authenticator.password = password;
  }

  void release() {
    if (username == null)
      return;

    if (authenticator == null)
      return;

    synchronized (authenticator.lock) {
      authenticator.lock.unlock();
    }
  }

  @Override
  public String toString() {
    return proxyUri;
  }
}