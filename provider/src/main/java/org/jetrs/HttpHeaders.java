/* Copyright (c) 2022 JetRS
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

public final class HttpHeaders {
  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.accept">Specification</a>
   */
  public static final String ACCEPT = "Accept";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-CH">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc8942#section-3.1">Specification</a>
   */
  public static final String ACCEPT_CH = "Accept-CH";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-CH-Lifetime">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-08">Specification</a>
   */
  public static final String ACCEPT_CH_LIFETIME = "Accept-CH-Lifetime";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Charset">MDN Web Docs</a>
   * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.2">Specification</a>
   */
  public static final String ACCEPT_CHARSET = "Accept-Charset";

  /**
   * Acceptable version in time.
   *
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc7089">RFC 7089</a>
   */
  public static final String ACCEPT_DATETIME = "Accept-Datetime";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Encoding">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.accept-encoding">Specification</a>
   */
  public static final String ACCEPT_ENCODING = "Accept-Encoding";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.accept-language">Specification</a>
   */
  public static final String ACCEPT_LANGUAGE = "Accept-Language";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Patch">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc5789#section-3.1">Specification</a>
   */
  public static final String ACCEPT_PATCH = "Accept-Patch";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Post">MDN Web Docs</a>
   * @see <a href="https://www.w3.org/TR/ldp/#header-accept-post">Specification</a>
   */
  public static final String ACCEPT_POST = "Accept-Post";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Ranges">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#header.accept-ranges">Specification</a>
   */
  public static final String ACCEPT_RANGES = "Accept-Ranges";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Credentials">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-allow-credentials">Specification</a>
   */
  public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-allow-headers">Specification</a>
   */
  public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Methods">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-allow-methods">Specification</a>
   */
  public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-allow-origin">Specification</a>
   */
  public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Expose-Headers">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-expose-headers">Specification</a>
   */
  public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Max-Age">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-max-age">Specification</a>
   */
  public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Request-Headers">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-request-headers">Specification</a>
   */
  public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Request-Method">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-request-method">Specification</a>
   */
  public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Age">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.age">Specification</a>
   */
  public static final String AGE = "Age";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Allow">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#section-7.4.1">Specification</a>
   */
  public static final String ALLOW = "Allow";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Alt-Svc">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7838.html#alt-svc">Specification</a>
   */
  public static final String ALT_SVC = "Alt-Svc";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7235.html#header.authorization">Specification</a>
   */
  public static final String AUTHORIZATION = "Authorization";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.cache-control">Specification</a>
   */
  public static final String CACHE_CONTROL = "Cache-Control";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Clear-Site-Data">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-clear-site-data/#header">Specification</a>
   */
  public static final String CLEAR_SITE_DATA = "Clear-Site-Data";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Connection">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.connection">Specification</a>
   */
  public static final String CONNECTION = "Connection";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc6266.html#header.field.definition">Specification</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc7578#section-4.2">Specification</a>
   */
  public static final String CONTENT_DISPOSITION = "Content-Disposition";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-DPR">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-07">Specification</a>
   */
  public static final String CONTENT_DPR = "Content-DPR";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Encoding">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.content-encoding">Specification</a>
   */
  public static final String CONTENT_ENCODING = "Content-Encoding";

  /**
   * @see <a href="http://tools.ietf.org/html/rfc2392">RFC 2392</a>
   */
  public static final String CONTENT_ID = "Content-ID";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Language">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.content-language">Specification</a>
   */
  public static final String CONTENT_LANGUAGE = "Content-Language";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Length">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.content-length">Specification</a>
   */
  public static final String CONTENT_LENGTH = "Content-Length";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Location">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.content-location">Specification</a>
   */
  public static final String CONTENT_LOCATION = "Content-Location";

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc1544">RFC 1544</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc1864">RFC 1864</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc4021">RFC 4021</a>
   */
  public static final String CONTENT_MD5 = "Content-MD5";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Range">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#header.content-range">Specification</a>
   */
  public static final String CONTENT_RANGE = "Content-Range";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-csp/#csp-header">Specification</a>
   */
  public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy-Report-Only">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-csp/#cspro-header">Specification</a>
   */
  public static final String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#status.206">Specification</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.content-type">Specification</a>
   */
  public static final String CONTENT_TYPE = "Content-Type";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cookie">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc6265.html#cookie">Specification</a>
   */
  public static final String COOKIE = "Cookie";

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-45">List of HTTP header fields</a>
   */
  public static final String CORRELATION_ID = "Correlation-ID";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Embedder-Policy">MDN Web Docs</a>
   * @see <a href="https://html.spec.whatwg.org/multipage/origin.html#coep">Specification</a>
   */
  public static final String CROSS_ORIGIN_EMBEDDER_POLICY = "Cross-Origin-Embedder-Policy";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Opener-Policy">MDN Web Docs</a>
   * @see <a href="https://html.spec.whatwg.org/multipage/origin.html#the-coop-headers">Specification</a>
   */
  public static final String CROSS_ORIGIN_OPENER_POLICY = "Cross-Origin-Opener-Policy";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Resource-Policy">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#cross-origin-resource-policy-header">Specification</a>
   */
  public static final String CROSS_ORIGIN_RESOURCE_POLICY = "Cross-Origin-Resource-Policy";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Date">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.date">Specification</a>
   */
  public static final String DATE = "Date";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Device-Memory">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/device-memory/#sec-device-memory-client-hint-header">Specification</a>
   */
  public static final String DEVICE_MEMORY = "Device-Memory";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Digest">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-digest-headers-05#section-3">Specification</a>
   */
  public static final String DIGEST = "Digest";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/DNT">MDN Web Docs</a>
   * @see <a href="https://www.w3.org/TR/tracking-dnt/#dnt-header-field">Specification</a>
   */
  public static final String DNT = "DNT";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Downlink">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/netinfo/#downlink-request-header-field">Specification</a>
   */
  public static final String DOWNLINK = "Downlink";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/DPR">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-07">Specification</a>
   */
  public static final String DPR = "DPR";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Early-Data">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc8470.html#header">Specification</a>
   */
  public static final String EARLY_DATA = "Early-Data";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ECT">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/netinfo/#ect-request-header-field">Specification</a>
   */
  public static final String ECT = "ECT";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.etag">Specification</a>
   */
  public static final String ETAG = "ETag";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.expect">Specification</a>
   */
  public static final String EXPECT = "Expect";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-expect-ct-08#section-2.1">Specification</a>
   */
  public static final String EXPECT_CT = "Expect-CT";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expires">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.expires">Specification</a>
   */
  public static final String EXPIRES = "Expires";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Feature-Policy">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-permissions-policy/#permissions-policy-http-header-field">Specification</a>
   */
  public static final String FEATURE_POLICY = "Feature-Policy";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc7239#section-4">Specification</a>
   */
  public static final String FORWARDED = "Forwarded";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/From">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.from">Specification</a>
   */
  public static final String FROM = "From";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Host">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.host">Specification</a>
   */
  public static final String HOST = "Host";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Match">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.if-match">Specification</a>
   */
  public static final String IF_MATCH = "If-Match";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Modified-Since">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.if-modified-since">Specification</a>
   */
  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-None-Match">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.if-none-match">Specification</a>
   */
  public static final String IF_NONE_MATCH = "If-None-Match";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Range">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#header.if-range">Specification</a>
   */
  public static final String IF_RANGE = "If-Range";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Unmodified-Since">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.if-unmodified-since">Specification</a>
   */
  public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Keep-Alive">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#compatibility.with.http.1.0.persistent.connections">Specification</a>
   */
  public static final String KEEP_ALIVE = "Keep-Alive";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Large-Allocation">MDN Web Docs</a>
   * @see <a href="https://gist.github.com/mystor/5739e222e398efc6c29108be55eb6fe3">Specification</a>
   */
  public static final String LARGE_ALLOCATION = "Large-Allocation";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.last-modified">Specification</a>
   */
  public static final String LAST_MODIFIED = "Last-Modified";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Link">MDN Web Docs</a>
   */
  public static final String LINK = "Link";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Location">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.location">Specification</a>
   */
  public static final String LOCATION = "Location";

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616">RFC 2616</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc7232">RFC 7232</a>
   */
  public static final String MAX_FORWARDS = "Max-Forwards";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/NEL">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/network-error-logging/#nel-response-header">Specification</a>
   */
  public static final String NEL = "NEL";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Origin">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc6454#section-7">Specification</a>
   * @see <a href="https://fetch.spec.whatwg.org/#origin-header">Specification</a>
   */
  public static final String ORIGIN = "Origin";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Pragma">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.pragma">Specification</a>
   */
  public static final String PRAGMA = "Pragma";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Proxy-Authenticate">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7235.html#header.proxy-authenticate">Specification</a>
   */
  public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Proxy-Authorization">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7235.html#header.proxy-authorization">Specification</a>
   */
  public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#header.range">Specification</a>
   */
  public static final String RANGE = "Range";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referer">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.referer">Specification</a>
   */
  public static final String REFERER = "Referer";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referrer-Policy">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-referrer-policy/#referrer-policy-header">Specification</a>
   */
  public static final String REFERRER_POLICY = "Referrer-Policy";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Retry-After">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.retry-after">Specification</a>
   */
  public static final String RETRY_AFTER = "Retry-After";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/RTT">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/netinfo/#rtt-request-header-field">Specification</a>
   */
  public static final String RTT = "RTT";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Save-Data">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/savedata/#save-data-request-header-field">Specification</a>
   */
  public static final String SAVE_DATA = "Save-Data";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua">Specification</a>
   */
  public static final String SEC_CH_UA = "Sec-CH-UA";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Arch">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-arch">Specification</a>
   */
  public static final String SEC_CH_UA_ARCH = "Sec-CH-UA-Arch";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Bitness">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-bitness">Specification</a>
   */
  public static final String SEC_CH_UA_BITNESS = "Sec-CH-UA-Bitness";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Full-Version">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-full-version">Specification</a>
   */
  public static final String SEC_CH_UA_FULL_VERSION = "Sec-CH-UA-Full-Version";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Full-Version-List">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-full-version-list">Specification</a>
   */
  public static final String SEC_CH_UA_FULL_VERSION_LIST = "Sec-CH-UA-Full-Version-List";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Mobile">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-mobile">Specification</a>
   */
  public static final String SEC_CH_UA_MOBILE = "Sec-CH-UA-Mobile";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Model">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-model">Specification</a>
   */
  public static final String SEC_CH_UA_MODEL = "Sec-CH-UA-Model";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Platform">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-platform">Specification</a>
   */
  public static final String SEC_CH_UA_PLATFORM = "Sec-CH-UA-Platform";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Platform-Version">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-platform-version">Specification</a>
   */
  public static final String SEC_CH_UA_PLATFORM_VERSION = "Sec-CH-UA-Platform-Version";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-Fetch-Dest">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-fetch-metadata/#sec-fetch-dest-header">Specification</a>
   */
  public static final String SEC_FETCH_DEST = "Sec-Fetch-Dest";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-Fetch-Mode">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-fetch-metadata/#sec-fetch-mode-header">Specification</a>
   */
  public static final String SEC_FETCH_MODE = "Sec-Fetch-Mode";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-Fetch-Site">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-fetch-metadata/#sec-fetch-site-header">Specification</a>
   */
  public static final String SEC_FETCH_SITE = "Sec-Fetch-Site";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-Fetch-User">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-fetch-metadata/#sec-fetch-user-header">Specification</a>
   */
  public static final String SEC_FETCH_USER = "Sec-Fetch-User";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-WebSocket-Accept">MDN Web Docs</a>
   */
  public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.server">Specification</a>
   */
  public static final String SERVER = "Server";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing">MDN Web Docs</a>
   * @see <a href="https://www.w3.org/TR/server-timing/">Specification</a>
   */
  public static final String SERVER_TIMING = "Server-Timing";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Service-Worker-Navigation-Preload">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/ServiceWorker/#handle-fetch">Specification</a>
   */
  public static final String SERVICE_WORKER_NAVIGATION_PRELOAD = "Service-Worker-Navigation-Preload";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc6265.html#sane-set-cookie">Specification</a>
   */
  public static final String SET_COOKIE = "Set-Cookie";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/SourceMap">MDN Web Docs</a>
   * @see <a href="https://sourcemaps.info/spec.html#h.lmz475t4mvbx">Specification</a>
   */
  public static final String SOURCEMAP = "SourceMap";

  /**
   * CGI header field specifying the status of the HTTP response. Normal HTTP responses use a separate "Status-Line" instead,
   * defined by RFC 7230.
   *
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   */
  public static final String STATUS = "Status";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc6797#section-6.1">Specification</a>
   */
  public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/TE">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.te">Specification</a>
   */
  public static final String TE = "TE";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Timing-Allow-Origin">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/resource-timing/#sec-timing-allow-origin">Specification</a>
   */
  public static final String TIMING_ALLOW_ORIGIN = "Timing-Allow-Origin";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Tk">MDN Web Docs</a>
   * @see <a href="https://www.w3.org/TR/tracking-dnt/#Tk-header-defn">Specification</a>
   */
  public static final String TK = "Tk";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Trailer">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.trailer">Specification</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#chunked.trailer.part">Specification</a>
   */
  public static final String TRAILER = "Trailer";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.transfer-encoding">Specification</a>
   */
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Upgrade">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7540.html#informational-responses">Specification</a>
   */
  public static final String UPGRADE = "Upgrade";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Upgrade-Insecure-Requests">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-upgrade-insecure-requests/#preference">Specification</a>
   */
  public static final String UPGRADE_INSECURE_REQUESTS = "Upgrade-Insecure-Requests";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.user-agent">Specification</a>
   */
  public static final String USER_AGENT = "User-Agent";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Vary">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.vary">Specification</a>
   */
  public static final String VARY = "Vary";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Via">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.via">Specification</a>
   */
  public static final String VIA = "Via";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Viewport-Width">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-07">Specification</a>
   */
  public static final String VIEWPORT_WIDTH = "Viewport-Width";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Want-Digest">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-digest-headers-05#section-4">Specification</a>
   */
  public static final String WANT_DIGEST = "Want-Digest";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Warning">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.warning">Specification</a>
   */
  public static final String WARNING = "Warning";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Width">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-07">Specification</a>
   */
  public static final String WIDTH = "Width";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/WWW-Authenticate">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7235.html#header.www-authenticate">Specification</a>
   */
  public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

  /**
   * Provide the duration of the audio or video in seconds; only supported by Gecko browsers.
   *
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   */
  public static final String X_CONTENT_DURATION = "X-Content-Duration";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#x-content-type-options-header">Specification</a>
   */
  public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-DNS-Prefetch-Control">MDN Web Docs</a>
   */
  public static final String X_DNS_PREFETCH_CONTROL = "X-DNS-Prefetch-Control";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For">MDN Web Docs</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">Specification</a>
   */
  public static final String X_FORWARDED_FOR = "X-Forwarded-For";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host">MDN Web Docs</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">Specification</a>
   */
  public static final String X_FORWARDED_HOST = "X-Forwarded-Host";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Proto">MDN Web Docs</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">Specification</a>
   */
  public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options">MDN Web Docs</a>
   * @see <a href="https://html.spec.whatwg.org/multipage/browsing-the-web.html#the-x-frame-options-header">Specification</a>
   */
  public static final String X_FRAME_OPTIONS = "X-Frame-Options";

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-32">List of HTTP header fields</a>
   */
  public static final String X_HTTP_METHOD_OVERRIDE = "X-Http-Method-Override";

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-39">List of HTTP header fields</a>
   */
  public static final String X_CSRF_TOKEN = "X-Csrf-Token";

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-43">List of HTTP header fields</a>
   */
  public static final String X_REQUEST_ID = "X-Request-ID";

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-44">List of HTTP header fields</a>
   */
  public static final String X_CORRELATION_ID = "X-Correlation-ID";

  /**
   * Mainly used to identify Ajax requests (most JavaScript frameworks send this field with value of XMLHttpRequest); also
   * identifies Android apps using WebView.
   *
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   * @see <a href="https://www.stoutner.com/the-x-requested-with-header/">Specification</a>
   */
  public static final String X_REQUESTED_WITH = "X-Requested-With";

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection">MDN Web Docs</a>
   */
  public static final String X_XSS_PROTECTION = "X-XSS-Protection";

  private HttpHeaders() {
  }
}