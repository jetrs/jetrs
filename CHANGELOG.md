# Changes by Version

## [v2.2.0-SNAPSHOT](https://github.com/libj/util/compare/e2bc2a31383386a25a6405752383f6af51a3b0e6..HEAD)

## [v2.1.0-alpha-5](https://github.com/jetrs/jetrs/compare/3412e736ddd8975fbb582c486a9fcf2652bfdcb1..e2bc2a31383386a25a6405752383f6af51a3b0e6) (2023-09-20)
* #21 Support standard entity providers
* #20 Support HTTP 30X Follow Redirect
* #19 Support ServerProperties
* #18 Support ClientProperties 1
* #17 Support CommonProperties 1
* #16 Implement ClientDriver
* #15 Support ParamConverterProvider, ParamConverter, and @DefaultValue
* #14 Support @Singleton annotation
* #13 Process @NotNull annotation for entities
* #12 Support MatrixParam
* #11 Implement ContentCodec and GZipCodec
* #10 Upgrade Maven dependencies
* #9 Validate header names in HttpHeadersMap
* #8 Support @Context injection for ContainerRequestContext and ContainerResponseContext
* #7 Implement ServerTestRuntimeDelegate
* #6 Create shared \"provider\" module
* #5 Implement ClientCertificateFilter
* #4 Implement DefaultHeaderDelegate
* #3 Support @Context for ServletConfig and ServletContext
* #2 Implement ParamConverter and ParamConverterProvider
* #1 Transition to GitHub Actions

## [v2.1.0-alpha-3](https://github.com/jetrs/jetrs/compare/81057d19ba20886fece1f820c1d6b72dfe7ec623..3412e736ddd8975fbb582c486a9fcf2652bfdcb1) (2020-05-23)
* Separate `server` module to `common` and `client` modules.
* Implement `JAX-RS Client` APIs.
* Introduce common `Bootstrap` for `server` and `client`.
* Add `ServerBootstrap`.
* Improve `AnnotationInjector`.
* Add common `Configuration` pattern for `server` and `client`.
* Rewrite `HttpHeadersMap`, `MirrorMultivaluedMap`, `HttpHeaderMirrors`, `HttpHeadersImpl`, and `HttpHeadersUtil`.
* Add `MirrorQualityList`, `MultivaluedMaps`,
* Rewrite `ResponseImpl`.
* Revise `ExecutionContext`, `ResourceAnnotationProcessor`, `ResourceManifest`.
* Improve `NewCookieHeaderDelegate`.
* Add `FileProvider`.
* Add `CountingBufferedOutputStream`.
* Revise `RestHttpServlet`.
* Improve tests.
* Improve javadocs and xmldocs.

## [v2.1.0-alpha-2](https://github.com/jetrs/jetrs/compare/f7dde254b8fa906ee4e6b8c3e620690294546cb6..81057d19ba20886fece1f820c1d6b72dfe7ec623) (2019-07-21)
* Upgrade `org.libj:io:0.7.5` to `0.7.6`.
* Upgrade `org.libj:lang:0.7.3` to `0.7.4`.
* Upgrade `org.libj:net:0.5.0` to `0.7.1`.

## [v2.1.0-alpha-1](https://github.com/entinae/pom/compare/648748e15265c4c079e496b2f0caf2337f091cec..f7dde254b8fa906ee4e6b8c3e620690294546cb6) (2019-05-13)
* Initial public release.