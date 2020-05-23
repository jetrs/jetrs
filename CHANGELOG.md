# Changes by Version

## [v2.1.0-alpha-4-SNAPSHOT](https://github.com/jetrs/jetrs/compare/3412e736ddd8975fbb582c486a9fcf2652bfdcb1..HEAD)

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

## v2.1.0-alpha-1 (2019-05-13)
* Initial public release.