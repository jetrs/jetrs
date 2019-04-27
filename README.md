# OpenJAX XRS

> jaX Rest Server

[![Build Status](https://travis-ci.org/openjax/xrs.png)](https://travis-ci.org/openjax/xrs)
[![Coverage Status](https://coveralls.io/repos/github/openjax/xrs/badge.svg)](https://coveralls.io/github/openjax/xrs)
[![Javadocs](https://www.javadoc.io/badge/org.openjax.xrs/xrs.svg)](https://www.javadoc.io/doc/org.openjax.xrs/xrs)
[![Released Version](https://img.shields.io/maven-central/v/org.openjax.xrs/xrs.svg)](https://mvnrepository.com/artifact/org.openjax.xrs/xrs)

### Introduction

**XRS** is an implementation of the [JAX-RS v2.0 Specification][jax-rs-spec] that runs in a [Servlet Container][web-container]. This project was inspired with the goal to create a better, simpler, easier to use, reliable, and debugable JAX-RS implementation using the CohesionFirst approach.

### Why **XRS**?

#### CohesionFirst

Developed with the CohesionFirst approach, **XRS** is reliably designed, consistently implemented, and straightforward to use. Made possible by the rigorous conformance to design patterns and best practices in every line of its implementation, **XRS** is a pure [JAX-RS 2.0][jax-rs-spec] solution that is written with the developer in mind. The **XRS** solution differentiates itself from the rest with its ease of use and debugability of RESTful applications.

#### Simple and Lightweight

**XRS** was built to implement the [JAX-RS v2.0 Specification][jax-rs-spec] while keeping the internal complexity as low as feasable. Static state is used in but one use-case, so it is never a challenge to debug a request or a response.

Existing solutions such as [Jersey][jersey], [JBoss RESTEasy][RESTeasy], [Restlet][restlet], [Apache CXF][apache-cxf], [Apache Wink][apache-wink] and others are challenging to work with, because they are buggy, difficult to debug, and are not pure to the JAX-RS specification. Many people experience unnecessary pains using existing JAX-RS implementations. Debugging of JAX-RS servers is especially difficult, because of the high internal complexities of the implementations.

#### Minimum Dynamic Invocation

A common pattern that is used in JAX-RS implementations is dynamic method invocation. Dynamic method invocation is powerful, but it comes at a cost: debugability. Dynamic method invocation results in stack-traces that lack information of the specific execution path that led to the exception. Instead of clear trace methods and line numbers, a dynamically invoked method call is cluttered with multitudes of `Method.invoke()` in the trace. After much pain and suffering, the idea of a pure JAX-RS server emerged, one that minimizes dynamic invocation, providing clear execution paths, and conforming to the [JAX-RS v2.0 Specification][jax-rs-spec] in its pure form.

#### Conforming to JAX-RS 2.0

**XRS** is a pure implementation of the [JAX-RS v2.0 Specification][jax-rs-spec]. More often than not, JAX-RS implementations introduce their own proprietary APIs, which thus couple you to the implementation. **XRS** is designed to be clear, cohesive, and 100% conformant to the [JAX-RS v2.0 Specification][jax-rs-spec].

### Getting Started

#### Prerequisites

* [Java 8][jdk8-download] - The minimum required JDK version.
* [Maven][maven] - The dependency management system.
* [Servlet Container][web-container] - A Servlet Container is needed to provide the HTTP service functionality. We recommend [Jetty][jetty] as the ideal starting point for any project.

#### Example

1. In your preferred development directory, create a [`maven-archetype-quickstart`][maven-archetype-quickstart] project.

  ```bash
  mvn archetype:generate -DgroupId=com.mycompany.app -DartifactId=my-app -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
  ```

2. Add the `mvn.repo.openjax.org` Maven repositories to the POM.

  ```xml
  <repositories>
    <repository>
      <id>mvn.repo.openjax.org</id>
      <url>http://mvn.repo.openjax.org/m2</url>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>mvn.repo.openjax.org</id>
      <url>http://mvn.repo.openjax.org/m2</url>
    </pluginRepository>
  </pluginRepositories>
  ```

3. Add the `org.openjax.xrs:xrs-server` dependency to the POM.

  ```xml
  <dependency>
    <groupId>org.openjax.xrs</groupId>
    <artifactId>xrs-server</artifactId>
    <version>2.0.3-SNAPSHOT</version>
  </dependency>
  <!-- Optional dependency for MessageBodyReader and MessageBodyWriter classes of JSONX module
  <dependency>
    <groupId>org.openjax.jsonx</groupId>
    <artifactId>jsonx-rs</artifactId>
    <version>0.2.2-SNAPSHOT</version>
  </dependency> -->
  ```

4. Create a `javax.ws.rs.core.Application`.

  ```java
  @javax.ws.rs.ApplicationPath("/*")
  public class Application extends javax.ws.rs.core.Application {
    @Override
    public java.util.Set<Object> getSingletons() {
      java.util.Set<Object> singletons = new java.util.HashSet<Object>();
      singletons.add(new org.openjax.jsonx.rs.JxObjectProvider()); // Optional Provider to parse and marshal JSON messages to Java beans.
      return singletons;
    }
  }
  ```

5. Extend `org.openjax.xrs.server.DefaultRESTServlet`, pointing to `Application`.

  ```java
  @WebServlet(initParams={@WebInitParam(name="javax.ws.rs.Application", value="Application")})
  public class RESTServlet extends org.openjax.xrs.server.DefaultRESTServlet {
  }
  ```

6. Deploy the servlet to a Servlet Container. For an easy embedded servlet container solution, [see here][jetty] for a solution based on [Jetty][jetty]. In the arguments to `new Server(8080, ...)` add `RESTServlet.class` as such:

  ```java
  new Server(8080, null, null, true, null, RESTServlet.class);
  ```

  This will automatically add `RESTServlet` to the application.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

### License

This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) file for details.

[apache-cxf]: http://cxf.apache.org/
[apache-wink]: https://wink.apache.org/
[jax-rs-spec]: http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-eval-spec/jsr339-jaxrs-2.0-final-spec.pdf
[jdk8-download]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[jersey]: https://jersey.java.net/
[jetty]: /../../../../openjax/support-jetty
[jetty]: http://www.eclipse.org/jetty/
[maven-archetype-quickstart]: http://maven.apache.org/archetypes/maven-archetype-quickstart/
[maven]: https://maven.apache.org/
[RESTeasy]: http://resteasy.jboss.org/
[restlet]: https://restlet.com/
[web-container]: https://en.wikipedia.org/wiki/Web_container