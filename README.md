# JetRS

**NOTE: This README it outdated!**

[![Build Status](https://travis-ci.org/jetrs/jetrs.png)](https://travis-ci.org/jetrs/jetrs)
[![Coverage Status](https://coveralls.io/repos/github/jetrs/jetrs/badge.svg)](https://coveralls.io/github/jetrs/jetrs)
[![Javadocs](https://www.javadoc.io/badge/org.jetrs/jetrs.svg)](https://www.javadoc.io/doc/org.jetrs/jetrs)
[![Released Version](https://img.shields.io/maven-central/v/org.jetrs/jetrs.svg)](https://mvnrepository.com/artifact/org.jetrs/jetrs)

## Introduction

<ins>JetRS</ins> is an implementation of the [JAX-RS v2.0 Specification][jax-rs-spec] that runs in a [Servlet Container][web-container]. This project was inspired with the goal to create a better, simpler, easier to use, reliable, and debugable JAX-RS implementation using the CohesionFirst approach.

## Getting Started

### Prerequisites

* [Java 8][jdk8-download] - The minimum required JDK version.
* [Maven][maven] - The dependency management system.
* [Servlet Container][web-container] - A Servlet Container is needed to provide the HTTP service functionality. We recommend [Jetty][jetty] as the ideal starting point for any project.

### Example

1. Add the `org.jetrs:server` dependency to the POM.

   ```xml
   <dependency>
     <groupId>org.jetrs</groupId>
     <artifactId>server</artifactId>
     <version>2.1.0</version>
   </dependency>
   ```

1. Optionally, you can add the dependency for the `MessageBodyReader` and `MessageBodyWriter` provider to integrate the [JSONx RS module](https://github.com/jsonxorg/jsonx/tree/master/rs).

   ```xml
   <dependency>
     <groupId>org.jsonx</groupId>
     <artifactId>jsonx-rs</artifactId>
     <version>0.2.2</version>
   </dependency>
   ```

1. Create a `javax.ws.rs.core.Application`.

   ```java
   @javax.ws.rs.ApplicationPath("/*")
   public class Application extends javax.ws.rs.core.Application {
     @Override
     public java.util.Set<Object> getSingletons() {
       java.util.Set<Object> singletons = new java.util.HashSet<Object>();
       singletons.add(new org.jsonx.rs.JxObjectProvider()); // Optional Provider to parse and marshal JSON messages to Java beans.
       return singletons;
     }
   }
   ```

1. Extend `org.jetrs.server.DefaultRESTServlet`, pointing to `Application`.

   ```java
   @WebServlet(initParams={@WebInitParam(name="javax.ws.rs.Application", value="Application")})
   public class RESTServlet extends org.jetrs.server.DefaultRESTServlet {
   }
   ```

1. Deploy the servlet to a Servlet Container. For an easy embedded servlet container solution, [see here][jetty] for a solution based on [Jetty][jetty]. In the arguments to `new Server(8080, ...)` add `RESTServlet.class` as such:

   ```java
   new Server(8080, RESTServlet.class);
   ```

   This will automatically add `RESTServlet` to the application.

## Contributing

Pull requests are welcome. For major changes, please [open an issue](../../issues) first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) file for details.

[apache-cxf]: http://cxf.apache.org/
[apache-wink]: https://wink.apache.org/
[jax-rs-spec]: http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-eval-spec/jsr339-jaxrs-2.0-final-spec.pdf
[jdk8-download]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[jersey]: https://jersey.java.net/
[jetty]: /../../../../jetrs
[jetty]: http://www.eclipse.org/jetty/
[maven-archetype-quickstart]: http://maven.apache.org/archetypes/maven-archetype-quickstart/
[maven]: https://maven.apache.org/
[RESTeasy]: http://resteasy.jboss.org/
[restlet]: https://restlet.com/
[web-container]: https://en.wikipedia.org/wiki/Web_container