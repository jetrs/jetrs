/* Copyright (c) 2019 JetRS
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

package org.jetrs.server.core;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;

import org.jetrs.common.core.ConfigurationImpl;

public class ServerConfiguration extends ConfigurationImpl {
  private static final long serialVersionUID = 6312216374129609540L;

  public ServerConfiguration(final Application application) {
    super(application);
  }

  @Override
  public RuntimeType getRuntimeType() {
    return RuntimeType.SERVER;
  }

  @Override
  public ServerConfiguration clone() {
    return (ServerConfiguration)super.clone();
  }
}