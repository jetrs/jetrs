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

package org.jetrs;

import static org.objectweb.asm.Opcodes.*;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.libj.lang.Strings;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

final class EndpointFactory implements BiFunction<Application,Class<?>,HttpServlet> {
  private static final String packageName = RestApplicationServlet.class.getPackage().getName().replace('.', '/');
  private static final String superClassName = RestApplicationServlet.class.getName().replace('.', '/');
  private static final boolean isJdk178 = System.getProperty("java.version").startsWith("1.");
  private static final AtomicInteger serial = new AtomicInteger(1);

  EndpointFactory() {
  }

  private static Class<?> defineClass(final byte[] bytes, final String className) {
    try {
      if (isJdk178) {
        final Method method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        method.setAccessible(true);
        return (Class<?>)method.invoke(EndpointFactory.class.getClassLoader(), className, bytes, 0, bytes.length);
      }

      final Method method = Lookup.class.getMethod("defineClass", byte[].class);
      return (Class<?>)method.invoke(MethodHandles.lookup(), bytes);
    }
    catch (final IllegalAccessException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException)
        throw (RuntimeException)cause;

      throw new RuntimeException(cause);
    }
  }

  private static void addAnnotationValue(final AnnotationVisitor annotationVisitor, final String name, final Class<?> annotationMethodType, final String typeDesc, final Object value) throws IllegalAccessException, InvocationTargetException {
    if (Enum.class.isAssignableFrom(annotationMethodType)) {
      annotationVisitor.visitEnum(name, typeDesc, value.toString());
    }
    else if (annotationMethodType.isAnnotation()) {
      @SuppressWarnings("unchecked")
      final Class<? extends Annotation> annotationType = (Class<? extends Annotation>)annotationMethodType;
      final AnnotationVisitor visitor = annotationVisitor.visitAnnotation(name, typeDesc);
      addAnnotation(visitor, (Annotation)value, annotationType);
      visitor.visitEnd();
    }
    else {
      annotationVisitor.visit(name, value);
    }
  }

  private static void addAnnotation(final AnnotationVisitor annotationVisitor, final Annotation annotation, final Class<? extends Annotation> annotationType) throws IllegalAccessException, InvocationTargetException {
    for (final Method method : annotationType.getDeclaredMethods()) { // [A]
      final Class<?> returnType = method.getReturnType();
      if (returnType.isArray()) {
        final Class<?> componentType = returnType.getComponentType();
        final String componentDesc = Type.getDescriptor(componentType);
        final AnnotationVisitor visitor = annotationVisitor.visitArray(method.getName());
        final Object[] array = (Object[])method.invoke(annotation);
        for (final Object member : array) // [A]
          addAnnotationValue(visitor, null, componentType, componentDesc, member);

        visitor.visitEnd();
      }
      else {
        addAnnotationValue(annotationVisitor, method.getName(), returnType, Type.getDescriptor(returnType), method.invoke(annotation));
      }
    }
  }

  private static String toUrlPattern(final ApplicationPath applicationPath) {
    // FIXME: URL-Encode baseUri, but don't double-encode %-encoded values
    String baseUri = applicationPath.value();
    if (baseUri.length() == 0)
      return "/*";

    if (baseUri.charAt(0) != '/')
      baseUri = "/" + baseUri;

    if (baseUri.charAt(baseUri.length() - 1) != '/')
      baseUri += "/";

    return baseUri + "*";
  }

  @Override
  public RestApplicationServlet apply(final Application application, final Class<?> endpointType) {
    if (!HttpServlet.class.equals(endpointType))
      throw new IllegalArgumentException("Only " + HttpServlet.class.getName() + " endpoint type is supported");

    final Class<? extends Application> applicationClass = application.getClass();
    final ApplicationPath applicationPath = AnnotationUtil.getAnnotation(applicationClass, ApplicationPath.class);
    if (applicationPath == null)
      throw new IllegalArgumentException("Application is missing @ApplicationPath annotation");

    final String urlPattern = toUrlPattern(applicationPath);
    try {
      final int index = serial.getAndIncrement();
      final String className = packageName + "/Endpoint" + index + "Servlet";
      final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
      classWriter.visit(V1_7,                  // Java 1.7
        ACC_PUBLIC,                            // public class
        className,                             // package and name
        null,                                  // signature (null means not generic)
        superClassName,                        // superclass
        null);                                 // interfaces

      final AnnotationVisitor annotationVisitor = classWriter.visitAnnotation(Type.getDescriptor(WebServlet.class), true);
      addAnnotation(annotationVisitor, new WebServlet() {
        @Override
        public Class<? extends Annotation> annotationType() {
          return WebServlet.class;
        }

        @Override
        public String[] value() {
          return Strings.EMPTY_ARRAY;
        }

        @Override
        public String[] urlPatterns() {
          return new String[] {urlPattern};
        }

        @Override
        public String smallIcon() {
          return "";
        }

        @Override
        public String name() {
          return className;
        }

        @Override
        public int loadOnStartup() {
          return 0;
        }

        @Override
        public String largeIcon() {
          return "";
        }

        @Override
        public WebInitParam[] initParams() {
          return new WebInitParam[0];
        }

        @Override
        public String displayName() {
          return "JetRS Endpoint " + index + ": " + applicationClass.getName();
        }

        @Override
        public String description() {
          return "";
        }

        @Override
        public boolean asyncSupported() {
          return true;
        }
      }, WebServlet.class);
      annotationVisitor.visitEnd();

      /* Build constructor */
      final MethodVisitor constructorVisitor = classWriter.visitMethod(
        ACC_PUBLIC,                                       // public method
        "<init>",                                         // method name
        "(Ljavax/ws/rs/core/Application;)V",              // descriptor
        null,                                             // signature (null means not generic)
        null);                                            // exceptions (array of strings)

      constructorVisitor.visitCode();                     // Start the code for this method
      constructorVisitor.visitVarInsn(ALOAD, 0);          // Load "this" onto the stack
      constructorVisitor.visitVarInsn(ALOAD, 1);          // Load "arg" onto the stack
      constructorVisitor.visitMethodInsn(INVOKESPECIAL,   // Invoke an instance method (non-virtual)
        superClassName,                                   // Class on which the method is defined
        "<init>",                                         // Name of the method
        "(Ljavax/ws/rs/core/Application;)V",              // Descriptor
        false);                                           // Is this class an interface?

      constructorVisitor.visitInsn(RETURN);               // End the constructor method
      constructorVisitor.visitMaxs(2, 2);                 // Specify max stack and local vars

      final MethodVisitor initVisitor = classWriter.visitMethod(
        ACC_PUBLIC,                                       // public method
        "init",                                           // method name
        "(Ljavax/servlet/ServletConfig;)V",               // Descriptor
        null,                                             // signature (null means not generic)
        new String[] {"javax/servlet/ServletException"}); // exceptions (array of strings)

      initVisitor.visitCode();                            // Start the code for this method
      initVisitor.visitVarInsn(ALOAD, 0);                 // Load "this" onto the stack
      initVisitor.visitVarInsn(ALOAD, 1);                 // Load "arg" onto the stack
      initVisitor.visitMethodInsn(INVOKESPECIAL,          // Invoke an instance method (non-virtual)
        superClassName,                                   // Class on which the method is defined
        "init",                                           // Name of the method
        "(Ljavax/servlet/ServletConfig;)V",               // Descriptor
        false);                                           // Is this class an interface?

      initVisitor.visitInsn(RETURN);                      // End the constructor method
      initVisitor.visitMaxs(2, 2);                        // Specify max stack and local vars

      final Class<?> cls = defineClass(classWriter.toByteArray(), className.replace('/', '.'));
      final Constructor<?> constructor = cls.getDeclaredConstructor(Application.class);
      return (RestApplicationServlet)constructor.newInstance(application);
    }
    catch (final IllegalAccessException | InstantiationException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException)
        throw (RuntimeException)cause;

      throw new RuntimeException(cause);
    }
  }
}