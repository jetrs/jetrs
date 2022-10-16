package org.jetrs;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.ssl.SslContextFactory;

// Copied from the JavaDoc for org.eclipse.jetty.http2.client.HTTP2Client
public class JettyHttp2Example {
  public static void main(final String[] args) throws Exception {
    final URL url = new URL("https://catfact.ninja/fact");
    final long startTime = System.currentTimeMillis();

    // Create and start HTTP2Client.
    final HTTP2Client client = new HTTP2Client();
    final SslContextFactory sslContextFactory = new SslContextFactory(true);
    client.addBean(sslContextFactory);
    client.start();

    // Connect to host.
    final String host = "catfact.ninja";
    final int port = 443;

    final FuturePromise<Session> sessionPromise = new FuturePromise<>();
    client.connect(sslContextFactory, new InetSocketAddress(host, port), new ServerSessionListener.Adapter(), sessionPromise);

    // Obtain the client Session object.
    final Session session = sessionPromise.get(5, TimeUnit.SECONDS);

    // Prepare the HTTP request headers.
    final HttpFields requestFields = new HttpFields();
    requestFields.put("User-Agent", client.getClass().getName() + "/" + Jetty.VERSION);
    // Prepare the HTTP request object.
    final MetaData.Request request = new MetaData.Request("GET", new HttpURI(url.toURI()), HttpVersion.HTTP_2, requestFields);
    // Create the HTTP/2 HEADERS frame representing the HTTP request.
    final HeadersFrame headersFrame = new HeadersFrame(request, null, true);

    // Prepare the listener to receive the HTTP response frames.
    final Stream.Listener responseListener = new Stream.Listener.Adapter() {
      @Override
      public void onData(final Stream stream, final DataFrame frame, final Callback callback) {
        final byte[] bytes = new byte[frame.getData().remaining()];
        frame.getData().get(bytes);
        final long duration = System.currentTimeMillis() - startTime;
        System.out.println("After " + duration + " ms: " + new String(bytes));
        callback.succeeded();
      }
    };

    session.newStream(headersFrame, new FuturePromise<>(), responseListener);
    session.newStream(headersFrame, new FuturePromise<>(), responseListener);
    session.newStream(headersFrame, new FuturePromise<>(), responseListener);

    Thread.sleep(TimeUnit.SECONDS.toMillis(20));

    client.stop();
  }
}