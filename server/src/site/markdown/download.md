<h1 style="line-height: 22px;">XRS 2.0.1<br/><span style="font-size: 13px; font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif; font-style: italic; font-weight: 300;">A <b>cohesion first</b> JAX-RS implementation</span></h1> 

**XRS 2.0.1**, which implements [JAX-RS 2.0 API](http://jax-rs-spec.java.net/), is the most recent
release of **XRS**.

Maven is the preferred installation mechanism for **XRS**. For other dependency managers, please
see [dependency information](/dependency-info.html).

To install **XRS** in a maven project, please add the safris.org repository and **XRS** dependency:

1. Add safris.org repository.

        <repository>
          <id>mvn.repo.safris.org</id>
          <url>http://mvn.repo.safris.org/m2</url>
        </repository>

2. Add XRS dependency.

        <dependency>
          <groupId>org.safris.xws</groupId>
          <artifactId>xrs</artifactId>
          <version>2.0.1</version>
        </dependency>

For dependency and license infomation, please see [dependencies](/dependencies.html). 

**NOTE**: **XRS** implements a subset of the JAX-RS 2.0 specification, as facets are implemented on
an as-needed basis. If **XRS** is missing something you need, please let us know!