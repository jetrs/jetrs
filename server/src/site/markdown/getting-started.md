<h1 style="line-height: 22px;">XRS 2.0.1<br/><span style="font-size: 13px; font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif; font-style: italic; font-weight: 300;">A <b>cohesion first</b> JAX-RS implementation</span></h1> 

  **XRS** is the server-only implementation of the [JAX-RS 2.0 API](http://jax-rs-spec.java.net/) specification, designed to run in a servlet container. When deciding on a servlet containers to host **XRS**, one can choose amongst, but is not limited to: [GlassFish](https://glassfish.java.net/), [JBoss](http://www.jboss.org/products/eap/overview/), [Wildfly](http://wildfly.org/), [Apache Tomcat](http://tomcat.apache.org/), [Apache TomEE](http://tomee.apache.org/), [Apache Geronimo](http://geronimo.apache.org/). For the lightest-weight and embedded sevlet container host, I recommend [Jetty](http://eclipse.org/jetty/).

  Deplying XSR in Jetty

1. Create a Maven project, and include the mvn.repo.safris.org repository, as well as the XRS dependency in the POM [as described here](/download.html).

2. Add the EmbeddedServletContainer dependency:

        <dependency>
          <groupId>org.safris.jetty</groupId>
          <artifactId>servlet</artifactId>
          <version>1.1.2</version>
        </dependency>

3. Create Server.java as the executable entrypoint:

        package com.example;

        import javax.ws.rs.ext.RuntimeDelegate;

        import org.safris.jetty.servlet.EmbeddedServletContainer;
        import org.safris.xws.xrs.ext.RuntimeDelegateImpl;

        public class Server extends EmbeddedServletContainer {
          static {
            System.setProperty(RuntimeDelegate.JAXRS_RUNTIME_DELEGATE_PROPERTY, RuntimeDelegateImpl.class.getName());
          }

          public static void main(final String[] args) throws Exception {
            final Server instance = new Server();
            instance.start();
            instance.join();
          }

          private Server() {
            super(8080, null, null, false, null, RESTServlet.class);
          }
        }

4. Create a Hello World REST Service:

        package com.example.service;

        import javax.ws.rs.GET;
        import javax.ws.rs.Path;
        import javax.ws.rs.PathParam;
        import javax.ws.rs.core.Response;

        @Path("/hello")
        public class HelloWorldService {
          @GET
          @Path("/{param}")
          public Response getMsg(@PathParam("param") final String msg) {
            final String output = "XSR says: " + msg;
            return Response.status(200).entity(output).build();
          }
        }

5. Build and run com.example.Server. The server should initialize and start listening on port 8080, as specified in Server.java.

6. Browse to http://localhost:8080/hello/world
