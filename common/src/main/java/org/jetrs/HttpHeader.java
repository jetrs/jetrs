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

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.jetrs.HeaderDelegateImpl.HeaderDelegateComposite;

final class HttpHeader<T> {
  static {
    HeaderDelegateImpl.values();
  }

  static final HashMap<String,AbstractMap.SimpleEntry<HttpHeader<?>,HeaderDelegateImpl<?>>> headerNameToDelegate = new HashMap<>();

  static final char[] none = {};
  static final char[] comma = {','};
  static final char[] semi = {';'};
  static final char[] semiComma = {';', ','};

  private final String name;
  private final char[] delimiters;
  private final boolean forbidden;

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.accept">Specification</a>
   */
  static final HttpHeader<MediaType> ACCEPT = new HttpHeader<>(HttpHeaders.ACCEPT, comma, false, HeaderDelegateImpl.MEDIA_TYPE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-CH">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc8942#section-3.1">Specification</a>
   */
  static final HttpHeader<String> ACCEPT_CH = new HttpHeader<>(HttpHeaders.ACCEPT_CH, comma, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-CH-Lifetime">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-08">Specification</a>
   */
  static final HttpHeader<Number> ACCEPT_CH_LIFETIME = new HttpHeader<>(HttpHeaders.ACCEPT_CH_LIFETIME, comma, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Charset">MDN Web Docs</a>
   * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.2">Specification</a>
   */
  static final HttpHeader<Charset> ACCEPT_CHARSET = new HttpHeader<>(HttpHeaders.ACCEPT_CHARSET, comma, true, HeaderDelegateImpl.CHARSET);

  /**
   * Acceptable version in time.
   *
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc7089">RFC 7089</a>
   */
  static final HttpHeader<Date> ACCEPT_DATETIME = new HttpHeader<>(HttpHeaders.ACCEPT_DATETIME, none, true, HeaderDelegateImpl.DATE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Encoding">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.accept-encoding">Specification</a>
   */
  static final HttpHeader<String> ACCEPT_ENCODING = new HttpHeader<>(HttpHeaders.ACCEPT_ENCODING, comma, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.accept-language">Specification</a>
   */
  static final HttpHeader<Locale> ACCEPT_LANGUAGE = new HttpHeader<>(HttpHeaders.ACCEPT_LANGUAGE, comma, false, HeaderDelegateImpl.LOCALE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Patch">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc5789#section-3.1">Specification</a>
   */
  static final HttpHeader<MediaType> ACCEPT_PATCH = new HttpHeader<>(HttpHeaders.ACCEPT_PATCH, comma, true, HeaderDelegateImpl.MEDIA_TYPE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Post">MDN Web Docs</a>
   * @see <a href="https://www.w3.org/TR/ldp/#header-accept-post">Specification</a>
   */
  static final HttpHeader<MediaType> ACCEPT_POST = new HttpHeader<>(HttpHeaders.ACCEPT_POST, comma, true, HeaderDelegateImpl.MEDIA_TYPE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Ranges">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#header.accept-ranges">Specification</a>
   */
  static final HttpHeader<String> ACCEPT_RANGES = new HttpHeader<>(HttpHeaders.ACCEPT_RANGES, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Credentials">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-allow-credentials">Specification</a>
   */
  static final HttpHeader<Boolean> ACCESS_CONTROL_ALLOW_CREDENTIALS = new HttpHeader<>(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, none, false, HeaderDelegateImpl.BOOLEAN_TRUE_FALSE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-allow-headers">Specification</a>
   */
  static final HttpHeader<String> ACCESS_CONTROL_ALLOW_HEADERS = new HttpHeader<>(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, comma, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Methods">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-allow-methods">Specification</a>
   */
  static final HttpHeader<String> ACCESS_CONTROL_ALLOW_METHODS = new HttpHeader<>(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-allow-origin">Specification</a>
   */
  static final HttpHeader<String> ACCESS_CONTROL_ALLOW_ORIGIN = new HttpHeader<>(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Expose-Headers">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-expose-headers">Specification</a>
   */
  static final HttpHeader<String> ACCESS_CONTROL_EXPOSE_HEADERS = new HttpHeader<>(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, comma, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Max-Age">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-max-age">Specification</a>
   */
  static final HttpHeader<Number> ACCESS_CONTROL_MAX_AGE = new HttpHeader<>(HttpHeaders.ACCESS_CONTROL_MAX_AGE, none, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Request-Headers">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-request-headers">Specification</a>
   */
  static final HttpHeader<String> ACCESS_CONTROL_REQUEST_HEADERS = new HttpHeader<>(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, comma, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Request-Method">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#http-access-control-request-method">Specification</a>
   */
  static final HttpHeader<String> ACCESS_CONTROL_REQUEST_METHOD = new HttpHeader<>(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Age">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.age">Specification</a>
   */
  static final HttpHeader<Number> AGE = new HttpHeader<>(HttpHeaders.AGE, none, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Allow">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#section-7.4.1">Specification</a>
   */
  static final HttpHeader<String> ALLOW = new HttpHeader<>(HttpHeaders.ALLOW, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Alt-Svc">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7838.html#alt-svc">Specification</a>
   */
  static final HttpHeader<String> ALT_SVC = new HttpHeader<>(HttpHeaders.ALT_SVC, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7235.html#header.authorization">Specification</a>
   */
  static final HttpHeader<String> AUTHORIZATION = new HttpHeader<>(HttpHeaders.AUTHORIZATION, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.cache-control">Specification</a>
   */
  static final HttpHeader<CacheControl> CACHE_CONTROL = new HttpHeader<>(HttpHeaders.CACHE_CONTROL, none, false, HeaderDelegateImpl.CACHE_CONTROL);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Clear-Site-Data">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-clear-site-data/#header">Specification</a>
   */
  static final HttpHeader<String> CLEAR_SITE_DATA = new HttpHeader<>(HttpHeaders.CLEAR_SITE_DATA, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Connection">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.connection">Specification</a>
   */
  static final HttpHeader<String> CONNECTION = new HttpHeader<>(HttpHeaders.CONNECTION, comma, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc6266.html#header.field.definition">Specification</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc7578#section-4.2">Specification</a>
   */
  static final HttpHeader<String> CONTENT_DISPOSITION = new HttpHeader<>(HttpHeaders.CONTENT_DISPOSITION, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-DPR">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-07">Specification</a>
   */
  static final HttpHeader<Number> CONTENT_DPR = new HttpHeader<>(HttpHeaders.CONTENT_DPR, none, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Encoding">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.content-encoding">Specification</a>
   */
  static final HttpHeader<String> CONTENT_ENCODING = new HttpHeader<>(HttpHeaders.CONTENT_ENCODING, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="http://tools.ietf.org/html/rfc2392">RFC 2392</a>
   */
  static final HttpHeader<String> CONTENT_ID = new HttpHeader<>(HttpHeaders.CONTENT_ID, comma, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Language">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.content-language">Specification</a>
   */
  static final HttpHeader<Locale> CONTENT_LANGUAGE = new HttpHeader<>(HttpHeaders.CONTENT_LANGUAGE, comma, false, HeaderDelegateImpl.LOCALE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Length">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.content-length">Specification</a>
   */
  static final HttpHeader<Number> CONTENT_LENGTH = new HttpHeader<>(HttpHeaders.CONTENT_LENGTH, none, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Location">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.content-location">Specification</a>
   */
  static final HttpHeader<URI> CONTENT_LOCATION = new HttpHeader<>(HttpHeaders.CONTENT_LOCATION, comma, false, HeaderDelegateImpl.URI);

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc1544">RFC 1544</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc1864">RFC 1864</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc4021">RFC 4021</a>
   */
  static final HttpHeader<String> CONTENT_MD5 = new HttpHeader<>("Content-MD5", none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Range">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#header.content-range">Specification</a>
   */
  static final HttpHeader<String> CONTENT_RANGE = new HttpHeader<>(HttpHeaders.CONTENT_RANGE, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-csp/#csp-header">Specification</a>
   */
  static final HttpHeader<String> CONTENT_SECURITY_POLICY = new HttpHeader<>(HttpHeaders.CONTENT_SECURITY_POLICY, semi, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy-Report-Only">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-csp/#cspro-header">Specification</a>
   */
  static final HttpHeader<String> CONTENT_SECURITY_POLICY_REPORT_ONLY = new HttpHeader<>(HttpHeaders.CONTENT_SECURITY_POLICY_REPORT_ONLY, semi, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#status.206">Specification</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.content-type">Specification</a>
   */
  static final HttpHeader<MediaType> CONTENT_TYPE = new HttpHeader<>(HttpHeaders.CONTENT_TYPE, none, false, HeaderDelegateImpl.MEDIA_TYPE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cookie">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc6265.html#cookie">Specification</a>
   */
  static final HttpHeader<Cookie> COOKIE = new HttpHeader<>(HttpHeaders.COOKIE, semiComma, false, HeaderDelegateImpl.COOKIE);
  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-45">List of HTTP header fields</a>
   */
  static final HttpHeader<String> CORRELATION_ID = new HttpHeader<>(HttpHeaders.CORRELATION_ID, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Embedder-Policy">MDN Web Docs</a>
   * @see <a href="https://html.spec.whatwg.org/multipage/origin.html#coep">Specification</a>
   */
  static final HttpHeader<String> CROSS_ORIGIN_EMBEDDER_POLICY = new HttpHeader<>(HttpHeaders.CROSS_ORIGIN_EMBEDDER_POLICY, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Opener-Policy">MDN Web Docs</a>
   * @see <a href="https://html.spec.whatwg.org/multipage/origin.html#the-coop-headers">Specification</a>
   */
  static final HttpHeader<String> CROSS_ORIGIN_OPENER_POLICY = new HttpHeader<>(HttpHeaders.CROSS_ORIGIN_OPENER_POLICY, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Resource-Policy">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#cross-origin-resource-policy-header">Specification</a>
   */
  static final HttpHeader<String> CROSS_ORIGIN_RESOURCE_POLICY = new HttpHeader<>(HttpHeaders.CROSS_ORIGIN_RESOURCE_POLICY, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Date">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.date">Specification</a>
   */
  static final HttpHeader<Date> DATE = new HttpHeader<>(HttpHeaders.DATE, none, true, HeaderDelegateImpl.DATE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Device-Memory">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/device-memory/#sec-device-memory-client-hint-header">Specification</a>
   */
  static final HttpHeader<BigDecimal> DEVICE_MEMORY = new HttpHeader<>(HttpHeaders.DEVICE_MEMORY, none, false, HeaderDelegateImpl.BIG_DECIMAL);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Digest">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-digest-headers-05#section-3">Specification</a>
   */
  static final HttpHeader<String> DIGEST = new HttpHeader<>(HttpHeaders.DIGEST, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/DNT">MDN Web Docs</a>
   * @see <a href="https://www.w3.org/TR/tracking-dnt/#dnt-header-field">Specification</a>
   */
  static final HttpHeader<Boolean> DNT = new HttpHeader<>(HttpHeaders.DNT, comma, true, HeaderDelegateImpl.BOOLEAN_1_0);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Downlink">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/netinfo/#downlink-request-header-field">Specification</a>
   */
  static final HttpHeader<BigDecimal> DOWNLINK = new HttpHeader<>(HttpHeaders.DOWNLINK, none, false, HeaderDelegateImpl.BIG_DECIMAL);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/DPR">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-07">Specification</a>
   */
  static final HttpHeader<BigDecimal> DPR = new HttpHeader<>(HttpHeaders.DPR, none, false, HeaderDelegateImpl.BIG_DECIMAL);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Early-Data">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc8470.html#header">Specification</a>
   */
  static final HttpHeader<String> EARLY_DATA = new HttpHeader<>(HttpHeaders.EARLY_DATA, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ECT">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/netinfo/#ect-request-header-field">Specification</a>
   */
  static final HttpHeader<String> ECT = new HttpHeader<>(HttpHeaders.ECT, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.etag">Specification</a>
   */
  static final HttpHeader<EntityTag> ETAG = new HttpHeader<>(HttpHeaders.ETAG, none, false, HeaderDelegateImpl.ENTITY_TAG);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.expect">Specification</a>
   */
  static final HttpHeader<String> EXPECT = new HttpHeader<>(HttpHeaders.EXPECT, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-expect-ct-08#section-2.1">Specification</a>
   */
  static final HttpHeader<String> EXPECT_CT = new HttpHeader<>(HttpHeaders.EXPECT_CT, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expires">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.expires">Specification</a>
   */
  static final HttpHeader<Date> EXPIRES = new HttpHeader<>(HttpHeaders.EXPIRES, none, false, HeaderDelegateImpl.DATE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Feature-Policy">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-permissions-policy/#permissions-policy-http-header-field">Specification</a>
   */
  static final HttpHeader<String> FEATURE_POLICY = new HttpHeader<>(HttpHeaders.FEATURE_POLICY, none, true, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc7239#section-4">Specification</a>
   */
  static final HttpHeader<String> FORWARDED = new HttpHeader<>(HttpHeaders.FORWARDED, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/From">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.from">Specification</a>
   */
  static final HttpHeader<String> FROM = new HttpHeader<>(HttpHeaders.FROM, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Host">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.host">Specification</a>
   */
  static final HttpHeader<String> HOST = new HttpHeader<>(HttpHeaders.HOST, none, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Match">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.if-match">Specification</a>
   */
  static final HttpHeader<EntityTag> IF_MATCH = new HttpHeader<>(HttpHeaders.IF_MATCH, comma, false, HeaderDelegateImpl.ENTITY_TAG);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Modified-Since">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.if-modified-since">Specification</a>
   */
  static final HttpHeader<Date> IF_MODIFIED_SINCE = new HttpHeader<>(HttpHeaders.IF_MODIFIED_SINCE, none, false, HeaderDelegateImpl.DATE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-None-Match">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.if-none-match">Specification</a>
   */
  static final HttpHeader<EntityTag> IF_NONE_MATCH = new HttpHeader<>(HttpHeaders.IF_NONE_MATCH, comma, false, HeaderDelegateImpl.ENTITY_TAG);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Range">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#header.if-range">Specification</a>
   */
  static final HttpHeader<Object> IF_RANGE = new HttpHeader<>(HttpHeaders.IF_RANGE, none, false, HeaderDelegateImpl.DATE, HeaderDelegateImpl.ENTITY_TAG);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Unmodified-Since">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.if-unmodified-since">Specification</a>
   */
  static final HttpHeader<Date> IF_UNMODIFIED_SINCE = new HttpHeader<>(HttpHeaders.IF_UNMODIFIED_SINCE, none, false, HeaderDelegateImpl.DATE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Keep-Alive">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#compatibility.with.http.1.0.persistent.connections">Specification</a>
   */
  static final HttpHeader<String> KEEP_ALIVE = new HttpHeader<>(HttpHeaders.KEEP_ALIVE, none, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Large-Allocation">MDN Web Docs</a>
   * @see <a href="https://gist.github.com/mystor/5739e222e398efc6c29108be55eb6fe3">Specification</a>
   */
  static final HttpHeader<Number> LARGE_ALLOCATION = new HttpHeader<>(HttpHeaders.LARGE_ALLOCATION, none, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7232.html#header.last-modified">Specification</a>
   */
  static final HttpHeader<Date> LAST_MODIFIED = new HttpHeader<>(HttpHeaders.LAST_MODIFIED, none, false, HeaderDelegateImpl.DATE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Link">MDN Web Docs</a>
   */
  static final HttpHeader<String> LINK = new HttpHeader<>(HttpHeaders.LINK, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Location">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.location">Specification</a>
   */
  static final HttpHeader<URI> LOCATION = new HttpHeader<>(HttpHeaders.LOCATION, none, false, HeaderDelegateImpl.URI);

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616">RFC 2616</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc7232">RFC 7232</a>
   */
  static final HttpHeader<Number> MAX_FORWARDS = new HttpHeader<>(HttpHeaders.MAX_FORWARDS, none, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/NEL">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/network-error-logging/#nel-response-header">Specification</a>
   */
  static final HttpHeader<String> NEL = new HttpHeader<>(HttpHeaders.NEL, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Origin">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc6454#section-7">Specification</a>
   * @see <a href="https://fetch.spec.whatwg.org/#origin-header">Specification</a>
   */
  static final HttpHeader<String> ORIGIN = new HttpHeader<>(HttpHeaders.ORIGIN, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Pragma">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.pragma">Specification</a>
   */
  static final HttpHeader<String> PRAGMA = new HttpHeader<>(HttpHeaders.PRAGMA, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Proxy-Authenticate">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7235.html#header.proxy-authenticate">Specification</a>
   */
  static final HttpHeader<String> PROXY_AUTHENTICATE = new HttpHeader<>(HttpHeaders.PROXY_AUTHENTICATE, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Proxy-Authorization">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7235.html#header.proxy-authorization">Specification</a>
   */
  static final HttpHeader<String> PROXY_AUTHORIZATION = new HttpHeader<>(HttpHeaders.PROXY_AUTHORIZATION, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7233.html#header.range">Specification</a>
   */
  static final HttpHeader<String> RANGE = new HttpHeader<>(HttpHeaders.RANGE, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referer">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.referer">Specification</a>
   */
  static final HttpHeader<URI> REFERER = new HttpHeader<>(HttpHeaders.REFERER, none, false, HeaderDelegateImpl.URI);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referrer-Policy">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-referrer-policy/#referrer-policy-header">Specification</a>
   */
  static final HttpHeader<String> REFERRER_POLICY = new HttpHeader<>(HttpHeaders.REFERRER_POLICY, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Retry-After">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.retry-after">Specification</a>
   */
  static final HttpHeader<Object> RETRY_AFTER = new HttpHeader<>(HttpHeaders.RETRY_AFTER, none, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER, HeaderDelegateImpl.DATE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/RTT">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/netinfo/#rtt-request-header-field">Specification</a>
   */
  static final HttpHeader<Number> RTT = new HttpHeader<>(HttpHeaders.RTT, none, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Save-Data">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/savedata/#save-data-request-header-field">Specification</a>
   */
  static final HttpHeader<String> SAVE_DATA = new HttpHeader<>(HttpHeaders.SAVE_DATA, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua">Specification</a>
   */
  static final HttpHeader<String> SEC_CH_UA = new HttpHeader<>(HttpHeaders.SEC_CH_UA, comma, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Arch">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-arch">Specification</a>
   */
  static final HttpHeader<String> SEC_CH_UA_ARCH = new HttpHeader<>(HttpHeaders.SEC_CH_UA_ARCH, none, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Bitness">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-bitness">Specification</a>
   */
  static final HttpHeader<Number> SEC_CH_UA_BITNESS = new HttpHeader<>(HttpHeaders.SEC_CH_UA_BITNESS, none, true, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Full-Version">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-full-version">Specification</a>
   */
  static final HttpHeader<String> SEC_CH_UA_FULL_VERSION = new HttpHeader<>(HttpHeaders.SEC_CH_UA_FULL_VERSION, none, true, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Full-Version-List">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-full-version-list">Specification</a>
   */
  static final HttpHeader<String> SEC_CH_UA_FULL_VERSION_LIST = new HttpHeader<>(HttpHeaders.SEC_CH_UA_FULL_VERSION_LIST, comma, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Mobile">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-mobile">Specification</a>
   */
  static final HttpHeader<Boolean> SEC_CH_UA_MOBILE = new HttpHeader<>(HttpHeaders.SEC_CH_UA_MOBILE, none, true, HeaderDelegateImpl.BOOLEAN_TRUE_FALSE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Model">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-model">Specification</a>
   */
  static final HttpHeader<String> SEC_CH_UA_MODEL = new HttpHeader<>(HttpHeaders.SEC_CH_UA_MODEL, none, true, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Platform">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-platform">Specification</a>
   */
  static final HttpHeader<String> SEC_CH_UA_PLATFORM = new HttpHeader<>(HttpHeaders.SEC_CH_UA_PLATFORM, none, true, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA-Platform-Version">MDN Web Docs</a>
   * @see <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-platform-version">Specification</a>
   */
  static final HttpHeader<String> SEC_CH_UA_PLATFORM_VERSION = new HttpHeader<>(HttpHeaders.SEC_CH_UA_PLATFORM_VERSION, none, true, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-Fetch-Dest">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-fetch-metadata/#sec-fetch-dest-header">Specification</a>
   */
  static final HttpHeader<String> SEC_FETCH_DEST = new HttpHeader<>(HttpHeaders.SEC_FETCH_DEST, none, true, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-Fetch-Mode">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-fetch-metadata/#sec-fetch-mode-header">Specification</a>
   */
  static final HttpHeader<String> SEC_FETCH_MODE = new HttpHeader<>(HttpHeaders.SEC_FETCH_MODE, none, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-Fetch-Site">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-fetch-metadata/#sec-fetch-site-header">Specification</a>
   */
  static final HttpHeader<String> SEC_FETCH_SITE = new HttpHeader<>(HttpHeaders.SEC_FETCH_SITE, none, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-Fetch-User">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-fetch-metadata/#sec-fetch-user-header">Specification</a>
   */
  static final HttpHeader<String> SEC_FETCH_USER = new HttpHeader<>(HttpHeaders.SEC_FETCH_USER, none, true, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-WebSocket-Accept">MDN Web Docs</a>
   */
  static final HttpHeader<String> SEC_WEBSOCKET_ACCEPT = new HttpHeader<>(HttpHeaders.SEC_WEBSOCKET_ACCEPT, none, true, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.server">Specification</a>
   */
  static final HttpHeader<String> SERVER = new HttpHeader<>(HttpHeaders.SERVER, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing">MDN Web Docs</a>
   * @see <a href="XXX">Specification</a>
   */
  static final HttpHeader<String> SERVER_TIMING = new HttpHeader<>(HttpHeaders.SERVER_TIMING, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Service-Worker-Navigation-Preload">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/ServiceWorker/#handle-fetch">Specification</a>
   */
  static final HttpHeader<String> SERVICE_WORKER_NAVIGATION_PRELOAD = new HttpHeader<>(HttpHeaders.SERVICE_WORKER_NAVIGATION_PRELOAD, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc6265.html#sane-set-cookie">Specification</a>
   */
  static final HttpHeader<NewCookie> SET_COOKIE = new HttpHeader<>(HttpHeaders.SET_COOKIE, none, false, HeaderDelegateImpl.NEW_COOKIE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/SourceMap">MDN Web Docs</a>
   * @see <a href="https://sourcemaps.info/spec.html#h.lmz475t4mvbx">Specification</a>
   */
  static final HttpHeader<String> SOURCEMAP = new HttpHeader<>(HttpHeaders.SOURCEMAP, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * CGI header field specifying the status of the HTTP response. Normal HTTP responses use a separate "Status-Line" instead,
   * defined by RFC 7230.
   *
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   */
  static final HttpHeader<Response.StatusType> STATUS = new HttpHeader<>(HttpHeaders.STATUS, none, false, HeaderDelegateImpl.STATUS_TYPE);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security">MDN Web Docs</a>
   * @see <a href="https://www.rfc-editor.org/rfc/rfc6797#section-6.1">Specification</a>
   */
  static final HttpHeader<String> STRICT_TRANSPORT_SECURITY = new HttpHeader<>(HttpHeaders.STRICT_TRANSPORT_SECURITY, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/TE">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.te">Specification</a>
   */
  static final HttpHeader<String> TE = new HttpHeader<>(HttpHeaders.TE, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Timing-Allow-Origin">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/resource-timing/#sec-timing-allow-origin">Specification</a>
   */
  static final HttpHeader<String> TIMING_ALLOW_ORIGIN = new HttpHeader<>(HttpHeaders.TIMING_ALLOW_ORIGIN, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Tk">MDN Web Docs</a>
   * @see <a href="https://www.w3.org/TR/tracking-dnt/#Tk-header-defn">Specification</a>
   */
  static final HttpHeader<Tk> TK = new HttpHeader<>(HttpHeaders.TK, none, false, HeaderDelegateImpl.TK);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Trailer">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.trailer">Specification</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#chunked.trailer.part">Specification</a>
   */
  static final HttpHeader<String> TRAILER = new HttpHeader<>(HttpHeaders.TRAILER, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.transfer-encoding">Specification</a>
   */
  static final HttpHeader<String> TRANSFER_ENCODING = new HttpHeader<>(HttpHeaders.TRANSFER_ENCODING, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Upgrade">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7540.html#informational-responses">Specification</a>
   */
  static final HttpHeader<String> UPGRADE = new HttpHeader<>(HttpHeaders.UPGRADE, comma, true, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Upgrade-Insecure-Requests">MDN Web Docs</a>
   * @see <a href="https://w3c.github.io/webappsec-upgrade-insecure-requests/#preference">Specification</a>
   */
  static final HttpHeader<Boolean> UPGRADE_INSECURE_REQUESTS = new HttpHeader<>(HttpHeaders.UPGRADE_INSECURE_REQUESTS, none, false, HeaderDelegateImpl.BOOLEAN_1);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.user-agent">Specification</a>
   */
  static final HttpHeader<String> USER_AGENT = new HttpHeader<>(HttpHeaders.USER_AGENT, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Vary">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7231.html#header.vary">Specification</a>
   */
  static final HttpHeader<String> VARY = new HttpHeader<>(HttpHeaders.VARY, comma, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Via">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7230.html#header.via">Specification</a>
   */
  static final HttpHeader<String> VIA = new HttpHeader<>(HttpHeaders.VIA, comma, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Viewport-Width">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-07">Specification</a>
   */
  static final HttpHeader<Number> VIEWPORT_WIDTH = new HttpHeader<>(HttpHeaders.VIEWPORT_WIDTH, comma, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Want-Digest">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-digest-headers-05#section-4">Specification</a>
   */
  static final HttpHeader<String> WANT_DIGEST = new HttpHeader<>(HttpHeaders.WANT_DIGEST, comma, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Warning">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7234.html#header.warning">Specification</a>
   */
  static final HttpHeader<String> WARNING = new HttpHeader<>(HttpHeaders.WARNING, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Width">MDN Web Docs</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-client-hints-07">Specification</a>
   */
  static final HttpHeader<Number> WIDTH = new HttpHeader<>(HttpHeaders.WIDTH, comma, false, HeaderDelegateImpl.BYTE, HeaderDelegateImpl.SHORT, HeaderDelegateImpl.INTEGER, HeaderDelegateImpl.LONG, HeaderDelegateImpl.BIG_INTEGER);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/WWW-Authenticate">MDN Web Docs</a>
   * @see <a href="https://httpwg.org/specs/rfc7235.html#header.www-authenticate">Specification</a>
   */
  static final HttpHeader<String> WWW_AUTHENTICATE = new HttpHeader<>(HttpHeaders.WWW_AUTHENTICATE, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * Provide the duration of the audio or video in seconds; only supported by Gecko browsers.
   *
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   */
  static final HttpHeader<BigDecimal> X_CONTENT_DURATION = new HttpHeader<>(HttpHeaders.X_CONTENT_DURATION, none, false, HeaderDelegateImpl.BIG_DECIMAL);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options">MDN Web Docs</a>
   * @see <a href="https://fetch.spec.whatwg.org/#x-content-type-options-header">Specification</a>
   */
  static final HttpHeader<String> X_CONTENT_TYPE_OPTIONS = new HttpHeader<>(HttpHeaders.X_CONTENT_TYPE_OPTIONS, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-DNS-Prefetch-Control">MDN Web Docs</a>
   */
  static final HttpHeader<Boolean> X_DNS_PREFETCH_CONTROL = new HttpHeader<>(HttpHeaders.X_DNS_PREFETCH_CONTROL, comma, false, HeaderDelegateImpl.BOOLEAN_ON_OFF);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For">MDN Web Docs</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">Specification</a>
   */
  static final HttpHeader<String> X_FORWARDED_FOR = new HttpHeader<>(HttpHeaders.X_FORWARDED_FOR, comma, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host">MDN Web Docs</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">Specification</a>
   */
  static final HttpHeader<String> X_FORWARDED_HOST = new HttpHeader<>(HttpHeaders.X_FORWARDED_HOST, comma, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Proto">MDN Web Docs</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">Specification</a>
   */
  static final HttpHeader<String> X_FORWARDED_PROTO = new HttpHeader<>(HttpHeaders.X_FORWARDED_PROTO, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options">MDN Web Docs</a>
   * @see <a href="https://html.spec.whatwg.org/multipage/browsing-the-web.html#the-x-frame-options-header">Specification</a>
   */
  static final HttpHeader<String> X_FRAME_OPTIONS = new HttpHeader<>(HttpHeaders.X_FRAME_OPTIONS, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-32">List of HTTP header fields</a>
   */
  static final HttpHeader<String> X_HTTP_METHOD_OVERRIDE = new HttpHeader<>(HttpHeaders.X_HTTP_METHOD_OVERRIDE, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-39">List of HTTP header fields</a>
   */
  static final HttpHeader<String> X_CSRF_TOKEN = new HttpHeader<>(HttpHeaders.X_CSRF_TOKEN, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-43">List of HTTP header fields</a>
   */
  static final HttpHeader<String> X_REQUEST_ID = new HttpHeader<>(HttpHeaders.X_REQUEST_ID, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#cite_ref-44">List of HTTP header fields</a>
   */
  static final HttpHeader<String> X_CORRELATION_ID = new HttpHeader<>(HttpHeaders.X_CORRELATION_ID, none, false, HeaderDelegateImpl.STRING);

  /**
   * Mainly used to identify Ajax requests (most JavaScript frameworks send this field with value of XMLHttpRequest); also
   * identifies Android apps using WebView.
   *
   * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">List of HTTP header fields</a>
   * @see <a href="https://www.stoutner.com/the-x-requested-with-header/">Specification</a>
   */
  static final HttpHeader<String> X_REQUESTED_WITH = new HttpHeader<>(HttpHeaders.X_REQUESTED_WITH, none, false, HeaderDelegateImpl.STRING);

  /**
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection">MDN Web Docs</a>
   */
  static final HttpHeader<String> X_XSS_PROTECTION = new HttpHeader<>(HttpHeaders.X_XSS_PROTECTION, none, false, HeaderDelegateImpl.STRING); // FIXME: Strong Type Candidate

  @SafeVarargs
  private HttpHeader(final String name, final char[] delimiters, final boolean forbidden, final HeaderDelegateImpl<? extends T> ... headerDelegates) {
    this.name = name;
    this.delimiters = delimiters;
    this.forbidden = forbidden;
    if (headerDelegates.length == 1)
      headerNameToDelegate.put(name.toLowerCase(), new AbstractMap.SimpleEntry<>(this, headerDelegates[0]));
    else
      headerNameToDelegate.put(name.toLowerCase(), new AbstractMap.SimpleEntry<>(this, new HeaderDelegateComposite(headerDelegates[0].getType(), false, headerDelegates)));
  }

  String getName() {
    return name;
  }

  char[] getDelimiters() {
    return delimiters;
  }

  boolean isForbidden() {
    return forbidden;
  }
}