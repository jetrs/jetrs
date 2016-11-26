<img src="http://safris.org/logo.png" align="right" />
# XRS [![CohesionFirst](http://safris.org/cf2.svg)](https://cohesionfirst.com/)
> jaX REST Server

## Introduction

XRS is an implementation of the [JAX-RS v2.0 Specification][jax-rs-spec] that runs in a Servlet Container (such as [Jetty][jetty]. This project was inspired to create a better, simpler, easier to use, reliable, and debugable JAX-RS implementation using the CohesionFirst™ approach.

## Why XRS?

### CohesionFirst™

Developed with the CohesionFirst™ approach, XRS is reliable, consistent, and straightforward to use. Made possible by the rigorous conformance to design patterns and best practices in every line of its implementation, XRS is a pure JAX-RS 2.0 solution that is written with the developer in mind. The XRS solution differentiates itself from the rest with its ease of use and debugability of RESTful applications.

### Simple and Lightweight

XRS was built to implement the [JAX-RS v2.0 Specification][jax-rs-spec] while keeping the internal complexity as low as feasable. Static state is used for but one use-case, so it is never a challenge to debug the path of a request or a response.

Existing solutions such as [Jersey][jersey], [JBoss RESTEasy][RESTeasy], [Restlet][restlet], [Apache CXF][apache-cxf], [Apache Wink][apache-wink] and others are challenging to work with, because they are bloated, buggy, and are not pure to the JAX-RS specification. Many people experience unnecessary pains using existing JAX-RS implementations, specifically related to debugging. Debugging of JAX-RS servers is difficult because of the high internal complexity of the implementation.

### Minimum Dynamic Invocation

A common pattern that is used in JAX-RS implementations is dynamic method invocation, which is mainly due to the nature of the specification itself. Dynamic method invocation is powerful, but it comes at a cost: debugability. Dynamic method invocation results in stack traces that lack information of the specific execution path that led to the exception. Instead of a clear trace methods and line numbers, a dynamically invoked method call is overwhelmed with multitudes of `Method.invoke()` in the trace. After much pain and suffering, the idea of a pure JAX-RS server emerged, one that minimizes dynamic invocation, providing clear execution paths, and conforming to the JAX-RS specification.

### Conforming to JAX-RS 2.0

XRS is a pure implementation of the JAX-RS 2.0 specification. More often than not, JAX-RS implementations introduce their own proprietary APIs, which thus couple you to the implementation, making it unnecessarily difficult to migrate to another provider in the future. XRS is designed to be clear, cohesive, and 100% conformant to the JAX-RS v2.0 specification.

## Getting Started

### Prerequisites

* [Maven][maven] - The dependency management system used to install XRS.
* [Servlet Container][web-container] - A Servlet Container is needed to provide the HTTP service functionality. We recommend [Jetty][jetty] as the ideal starting point for any project.

### Example

1. In your preferred development directory, create a [`maven-archetype-quickstart`](http://maven.apache.org/archetypes/maven-archetype-quickstart/) project.

  ```tcsh
  mvn archetype:generate -DgroupId=com.mycompany.app -DartifactId=my-app -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
  ```

2. Add the `mvn.repo.safris.org` Maven repositories to the POM.

  ```xml
  <repositories>
    <repository>
      <id>mvn.repo.safris.org</id>
      <url>http://mvn.repo.safris.org/m2</url>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>mvn.repo.safris.org</id>
      <url>http://mvn.repo.safris.org/m2</url>
    </pluginRepository>
  </pluginRepositories>
  ```

3. Add the `org.safris.cf`:`xrs` dependency to the POM.

  ```xml
  <dependency>
    <groupId>org.safris.cf</groupId>
    <artifactId>xrs</artifactId>
    <version>2.0.1</version>
  </dependency>
  ```
  
4. Create a `javax.ws.rs.core.Application`.

  ```java
  @javax.ws.rs.ApplicationPath("/*")
  public class Application extends javax.ws.rs.core.Application {
    @Override
    public java.util.Set<Object> getSingletons() {
      final java.util.Set<Object> singletons = new java.util.HashSet<Object>();
      singletons.add(new org.safris.xws.xjb.rs.JSObjectBodyReader()); // Optional MessageBodyReader to parse JSON messages to beans.
      singletons.add(new org.safris.xws.xjb.rs.JSObjectBodyWriter()); // Optional MessageBodyWriter to marshal beans to JSON messages.
      return singletons;
    }
  }
  ```

5. Extend `org.safris.cf.xrs.DefaultRESTServlet`, pointing to `Application`.

  ```java
  @WebServlet(initParams={@WebInitParam(name="javax.ws.rs.Application", value="Application")})
  public class RESTServlet extends org.safris.cf.xrs.DefaultRESTServlet {
  }
  ```

6. Deploy the servlet to a Servlet Container. For an easy embedded servlet container solution, [see here](https://github.com/SevaSafris/java/tree/master/commons/jetty/) for a solution based on [Jetty][jetty]. In the arguments to `new Server(8080, ...)` add `RESTServlet.class` as such:

  ```java
  new Server(8080, null, null, true, null, RESTServlet.class);
  ```

  This will automatically add `RESTServlet` to the application.
  
## License

This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) file for details.

[apache-cxf]: http://cxf.apache.org/
[apache-wink]: https://wink.apache.org/
[jax-rs-spec]: http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-eval-spec/jsr339-jaxrs-2.0-final-spec.pdf
[jersey]: https://jersey.java.net/
[jetty]: http://www.eclipse.org/jetty/
[RESTeasy]: http://resteasy.jboss.org/
[restlet]: https://restlet.com/
[maven]: https://maven.apache.org/
[web-container]: https://en.wikipedia.org/wiki/Web_container