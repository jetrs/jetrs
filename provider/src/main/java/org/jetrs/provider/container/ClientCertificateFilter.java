/* Copyright (c) 2021 JetRS
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

package org.jetrs.provider.container;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.libj.lang.Strings;
import org.openjax.security.cert.X509Certificates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ContainerRequestFilter} with functionality to extract X509 client
 * certificates provided in the headers of a {@link ContainerRequestContext}.
 */
public abstract class ClientCertificateFilter implements ContainerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(ClientCertificateFilter.class);

  private static String removeBeginEnd(String pem) {
    final int len = pem.length();
    pem = pem.replace("-----BEGIN CERTIFICATE-----", "");
    if (len == pem.length())
      return pem;

    pem = pem.replace("-----END CERTIFICATE-----", "");
    pem = pem.replace("\r\n", "");
    pem = pem.replace("\n", "");
    return pem.trim();
  }

  /**
   * Read the certificates in the provided {@linkplain KeyStore Trust Store}
   * into the specified {@code trustedRootCerts} and {@code intermediateCerts}
   * {@linkplain Set sets}.
   *
   * @param keyStore The {@link KeyStore} representing the Trust Store.
   * @param trustedRootCerts The {@linkplain Set set} to which root (self-issued
   *          certificates) are to be added.
   * @param intermediateCerts The {@linkplain Set set} to which intermediate
   *          certificates are to be added.
   * @throws KeyStoreException If the keystore has not been initialized
   *           (loaded).
   * @throws NullPointerException If any parameter is null.
   */
  protected static void readTrustStore(final KeyStore keyStore, final Set<X509Certificate> trustedRootCerts, final Set<X509Certificate> intermediateCerts) throws KeyStoreException {
    for (final Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
      final String alias = aliases.nextElement();
      if (keyStore.isCertificateEntry(alias)) {
        final Certificate cert = keyStore.getCertificate(alias);
        if (cert instanceof X509Certificate) {
          final X509Certificate x509Cert = (X509Certificate)cert;
          (X509Certificates.isSelfIssued(x509Cert) ? trustedRootCerts : intermediateCerts).add(x509Cert);
        }
      }
    }
  }

  private static X509Certificate[] convertCertPathtoX509CertArray(final List<? extends Certificate> certs, final int index, final int depth) {
    if (index == certs.size())
      return depth == 0 ? null : new X509Certificate[depth];

    final Certificate cert = certs.get(index);
    final X509Certificate[] x509Certificates = convertCertPathtoX509CertArray(certs, index + 1, cert instanceof X509Certificate ? depth + 1 : depth);
    if (cert instanceof X509Certificate)
      x509Certificates[depth] = (X509Certificate)cert;

    return x509Certificates;
  }

  /**
   * The {@code $ssl_client_escaped_cert} variable of NginX does not provide the
   * CA chain, so it must be rebuilt from the @{@code clientCert},
   * {@code trustedRootCerts} and {@code intermediateCerts}.
   */
  private static X509Certificate[] buildCertificateChain(final X509Certificate clientCert, final Set<X509Certificate> trustedRootCerts, final Set<X509Certificate> intermediateCerts) throws CertPathBuilderException, InvalidAlgorithmParameterException, NoSuchAlgorithmException {
    // Create the selector that specifies the starting certificate
    final X509CertSelector targetConstraints = new X509CertSelector();
    targetConstraints.setCertificate(clientCert);

    // Create the trust anchors (set of root CA certificates)
    final HashSet<TrustAnchor> trustAnchors = new HashSet<>();
    for (final X509Certificate trustedRootCert : trustedRootCerts)
      trustAnchors.add(new TrustAnchor(trustedRootCert, null));

    // Configure the PKIX certificate builder algorithm parameters
    final PKIXBuilderParameters pkixBuilderParameters = new PKIXBuilderParameters(trustAnchors, targetConstraints);

    // Disable CRL checks, as it's possibly done after depending on the settings
    pkixBuilderParameters.setRevocationEnabled(false);
    pkixBuilderParameters.setExplicitPolicyRequired(false);
    pkixBuilderParameters.setAnyPolicyInhibited(false);
    pkixBuilderParameters.setPolicyQualifiersRejected(false);
    pkixBuilderParameters.setMaxPathLength(-1);

    // Adding the list of intermediate certificates + end client certificate
    intermediateCerts.add(clientCert);
    pkixBuilderParameters.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(intermediateCerts)));

    // Build and verify the certification chain (revocation status excluded)
    final CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX");
    final CertPath certPath = certPathBuilder.build(pkixBuilderParameters).getCertPath();
    if (logger.isDebugEnabled())
      logger.debug("Certification path built with " + certPath.getCertificates().size() + " X.509 Certificates");

    final X509Certificate[] clientCertChain = convertCertPathtoX509CertArray(certPath.getCertificates(), 0, 0);
    if (clientCertChain != null && logger.isDebugEnabled())
      logger.debug("Client certificate: Chain DN: " + Arrays.stream(clientCertChain).map(c -> c.getSubjectDN().toString()).collect(Collectors.joining(",", "{", "}")));

    return clientCertChain;
  }

  private X509Certificate getCertificateFromHeader(final ContainerRequestContext requestContext, final String headerName) {
    String value = requestContext.getHeaders().getFirst(Objects.requireNonNull(headerName));
    if (value == null || (value = value.trim()).length() == 0)
      return null;

    value = Strings.trimStartEnd(value, '"', '"').trim();
    try {
      final X509Certificate cert = decodePem(value);
      if (cert == null)
        logger.warn("Invalid X.509 certificate in header \"" + headerName + "\": " + value);
      else if (logger.isDebugEnabled())
        logger.debug("Valid X.509 certificate in header \"" + headerName + "\"");

      return cert;
    }
    catch (final CertificateException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  private X509Certificate[] getCertificateChain(final ContainerRequestContext requestContext, final String clientCertChainHeaderPrefix, final int index, final int depth) {
    final X509Certificate clientCert = getCertChainFromHeader(requestContext, clientCertChainHeaderPrefix, index);
    if (clientCert == null)
      return new X509Certificate[depth];

    final X509Certificate[] clientCertChain = getCertificateChain(requestContext, clientCertChainHeaderPrefix, index + 1, depth + 1);
    clientCertChain[depth] = clientCert;
    return clientCertChain;
  }

  protected X509Certificate getCertChainFromHeader(final ContainerRequestContext requestContext, final String clientCertChainHeaderPrefix, final int index) {
    return getCertificateFromHeader(requestContext, clientCertChainHeaderPrefix + "_" + index);
  }

  /**
   * Get a <b>valid</b> certificate chain from the {@code clientCertHeader}
   * header specifying the client certificate and the
   * {@code clientCertChainHeaderPrefix} header name prefix (to be prepended to
   * {@code "_" + i}, where {@code i} starts at {@code 0} and is incremented
   * until the first {@code null} header chain entry is encountered) specifying
   * the additional chain certificates in the provided
   * {@link ContainerRequestContext}, or {@code null} if the specified header
   * does not exist or the certificate is not valid.
   * <p>
   * <b>Note:</b> The {@code clientCertHeader} header must contain the client
   * certificate, and the header values specified by the
   * {@code clientCertChainHeaderPrefix} header name prefix must provide the
   * additional chain certificates.
   *
   * @param requestContext The {@link ContainerRequestContext} providing the
   *          header values of the request.
   * @param clientCertHeader The header name containing a base64-encoded
   *          (Section 4 of [RFC4648]) DER [ITU.X690] PKIX certificate.
   * @param clientCertChainHeaderPrefix The header name prefix to be prepended
   *          to {@code "_" + i}, where {@code i} starts at {@code 0} and is
   *          incremented until the first {@code null} header chain entry is
   *          encountered.
   * @return A <b>valid</b> certificate chain from the {@code clientCertHeader}
   *         header specifying the client certificate and the
   *         {@code clientCertChainHeaderPrefix} header name prefix (to be
   *         prepended to {@code "_" + i}, where {@code i} starts at {@code 0}
   *         and is incremented until the first {@code null} header chain entry
   *         is encountered) specifying the additional chain certificates in the
   *         provided {@link ContainerRequestContext}, or {@code null} if the
   *         specified header does not exist or the certificate is not valid.
   * @throws NullPointerException If any parameter is null.
   */
  protected X509Certificate[] getCertificateChain(final ContainerRequestContext requestContext, final String clientCertHeader, final String clientCertChainHeaderPrefix) {
    final X509Certificate clientCert = getCertificateFromHeader(requestContext, clientCertHeader);
    if (clientCert == null)
      return null;

    final X509Certificate[] clientCertChain = getCertificateChain(requestContext, Objects.requireNonNull(clientCertChainHeaderPrefix), 0, 1);
    clientCertChain[0] = clientCert;

    if (logger.isDebugEnabled())
      logger.debug("getCertificateChain(): " + Arrays.stream(clientCertChain).map(c -> c.getSubjectDN().toString()).collect(Collectors.joining(",", "{", "}")));

    return clientCertChain;
  }

  /**
   * Get a <b>valid</b> certificate chain from the {@code clientCertHeader}
   * header specifying the client certificate in the provided
   * {@link ContainerRequestContext} and the {@code trustedRootCerts} and
   * {@code intermediateCerts} specifying the {@linkplain KeyStore Trust Store},
   * or {@code null} if the specified header does not exist or the certificate
   * is not valid.
   * <p>
   * <b>Note:</b> The specified header must contain the client certificate, and
   * not the full certificate chain. The full certificate chain is rebuilt from
   * the {@linkplain KeyStore Trust Store} specified {@code trustedRootCerts}
   * and {@code intermediateCerts}.
   *
   * @param requestContext The {@link ContainerRequestContext} providing the
   *          header values of the request.
   * @param clientCertHeader The header name containing a base64-encoded
   *          (Section 4 of [RFC4648]) DER [ITU.X690] PKIX certificate.
   * @param trustedRootCerts The root certificates of the {@linkplain KeyStore
   *          Trust Store} specifying the certificate chain.
   * @param intermediateCerts The intermediate certificates of the
   *          {@linkplain KeyStore Trust Store} specifying the certificate
   *          chain.
   * @return A <b>valid</b> certificate chain from the {@code clientCertHeader}
   *         header specifying the client certificate in the provided
   *         {@link ContainerRequestContext} and the {@code trustedRootCerts}
   *         and {@code intermediateCerts} specifying the {@link KeyStore Trust
   *         Store}, or {@code null} if the specified header does not exist or
   *         the certificate is not valid.
   * @throws NullPointerException If any parameter is null.
   */
  protected X509Certificate[] getCertificateChain(final ContainerRequestContext requestContext, final String clientCertHeader, final Set<X509Certificate> trustedRootCerts, final Set<X509Certificate> intermediateCerts) {
    final X509Certificate clientCert = getCertificateFromHeader(requestContext, clientCertHeader);
    if (clientCert == null)
      return null;

    if (logger.isDebugEnabled())
      logger.debug("Client certificate: SubjectDN=[" + clientCert.getSubjectDN() + "] SerialNumber=[" + clientCert.getSerialNumber() + "]");

    try {
      return buildCertificateChain(clientCert, trustedRootCerts, new HashSet<>(intermediateCerts));
    }
    catch (final CertPathBuilderException e) {
      if ("unable to find valid certification path to requested target".equals(e.getMessage())) {
        if (logger.isDebugEnabled())
          logger.debug("Client certificate: invalid");

        return null;
      }

      throw new UnsupportedOperationException(e);
    }
    catch (final InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  /**
   * Returns a {@link X509Certificate} decoded from the provided URL-encoded and
   * Base64-encoded PEM certificate.
   *
   * @param cert The URL-encoded and Base64-encoded PEM certificate.
   * @return A {@link X509Certificate} decoded from the provided URL-encoded and
   *         Base64-encoded PEM certificate.
   * @throws CertificateException If an exception occurs parsing the provided
   *           {@code cert}.
   */
  protected X509Certificate decodePem(final String cert) throws CertificateException {
    try {
      return X509Certificates.decodeCertificate(removeBeginEnd(URLDecoder.decode(cert, "UTF-8")));
    }
    catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}