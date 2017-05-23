/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.safris.commons.util.Collections;

public class ExceptionTruncator {
  private static final StackTraceElement[] reflectElements = new StackTraceElement[] {
    new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke0", null, -1),
    new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke", null, -1),
    new StackTraceElement("sun.reflect.DelegatingMethodAccessorImpl", "invoke", null, -1),
    new StackTraceElement("java.lang.reflect.Method", "invoke", null, -1)
  };

  public static boolean isReflectElement(final StackTraceElement element, final StackTraceElement reflectElement) {
    return element.getClassName().equals(reflectElement.getClassName()) && element.getMethodName().equals(reflectElement.getMethodName());
  }

  public static boolean isInvoked(final Throwable t) {
    return isReflectElement(t.getStackTrace()[0], reflectElements[0]);
  }

  public static void removeInvokeElements(final Collection<StackTraceElement> stackTraceElements) {
    final Iterator<StackTraceElement> iterator = stackTraceElements.iterator();
    while (iterator.hasNext()) {
      final StackTraceElement element = iterator.next();
      for (final StackTraceElement reflectElement : reflectElements)
        if (isReflectElement(element, reflectElement))
          iterator.remove();
    }
  }

  public static Throwable unwind(final Throwable t) {
    if (!isInvoked(t.getCause())) {
      final Collection<StackTraceElement> elements = Collections.asCollection(ArrayList.class, t.getCause().getStackTrace());
      removeInvokeElements(elements);
      t.getCause().setStackTrace(elements.toArray(new StackTraceElement[elements.size()]));
      return t.getCause();
    }

    return t.getCause() != null ? unwind(t.getCause()) : t;
  }

  public static void main(final String[] args) throws Throwable {
    new ExceptionTruncator().testX();
  }

  public static void x(final Throwable t) {
    Throwable cause = t;
    do {
      final Throwable unwound = unwind(cause);
    }
    while ((cause = cause.getCause()) != null);
  }

  public void testX() throws Throwable {
    try {
      ExceptionTruncator.class.getMethod("test0").invoke(this);
    }
    catch (final Throwable e) {
      e.printStackTrace();
      System.err.println();
      throw unwind(e);
    }
  }

  public void test0() throws Exception {
    test1();
  }

  public void test1() throws Exception {
    Method method = ExceptionTruncator.class.getMethod("test2");
    method.invoke(this);
  }

  public void test2() throws Throwable {
    test3();
  }

  public void test3() throws Throwable {
    try {
      test4();
    }
    catch (Exception e) {
      throw new Exception(e);
    }
  }

  public void test4() throws Throwable {
    Method method = ExceptionTruncator.class.getMethod("test5");
    method.invoke(this);
  }

  public void test5() {
    test6();
  }

  public void test6() {
    test7();
  }

  public void test7() {
    System.out.println("yer");
    throw new RuntimeException();
  }
}