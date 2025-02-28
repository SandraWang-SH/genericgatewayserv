package com.paypal.raptor.aiml.utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.paypal.raptor.aiml.common.exception.HttpUtilsException;


/**
 * @author: qingwu
 **/
public class AsyncSslSocketFactoryInitializer {

	private static final Logger logger = LoggerFactory.getLogger(AsyncSslSocketFactoryInitializer.class);

	private PoolingNHttpClientConnectionManager connectionManager;
	private SSLIOSessionStrategy sslConnectionioSessionStrategy;

	/**
	 * Constructor.
	 */
	public AsyncSslSocketFactoryInitializer() {
		createSslConnectionSocketFactory();
		createPoolingNHttpClientConnectionManager();
	}

	/***
	 * Get the connection pool manager.
	 *
	 * @return PoolingHttpClientConnectionManager
	 */
	public PoolingNHttpClientConnectionManager getPoolingNHttpClientConnectionManager() {
		return connectionManager;
	}

	/**
	 * Get SSLConnectionSocketFactory.
	 *
	 * @return SSLConnectionSocketFactory
	 */
	public SSLIOSessionStrategy getSslConnectionioSessionStrategy() {
		return sslConnectionioSessionStrategy;
	}

	private void createSslConnectionSocketFactory() {
		try {
			TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}
						public void checkClientTrusted(X509Certificate[] certs, String authType) {
						}
						public void checkServerTrusted(X509Certificate[] certs, String authType) {
							// don't check
						}
					}
			};
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, null);
			SSLIOSessionStrategy sslioSessionStrategy = new SSLIOSessionStrategy(sslContext, SSLIOSessionStrategy.ALLOW_ALL_HOSTNAME_VERIFIER);
			sslConnectionioSessionStrategy = sslioSessionStrategy;
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new HttpUtilsException("createSslConnectionSocketFactory() failed.", e);
		}
	}

	private void createPoolingNHttpClientConnectionManager() {
		IOReactorConfig ioReactorConfig = IOReactorConfig.custom().
				setIoThreadCount(8 * Runtime.getRuntime().availableProcessors())
				.setSoKeepAlive(true)
				.build();

		Registry<SchemeIOSessionStrategy> registry = RegistryBuilder.<SchemeIOSessionStrategy>create()
				.register("http", NoopIOSessionStrategy.INSTANCE)
				.register("https", getSslConnectionioSessionStrategy())
				.build();

		DefaultConnectingIOReactor ioReactor = null;

		try {
			ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
			ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
				@Override
				public boolean handle(IOException e) {
					logger.error("IoReactor error: {}", e.getMessage());
					return true;
				}
				@Override
				public boolean handle(RuntimeException e) {
					logger.error("IoReactor runtime error: {}", e.getMessage());
					return true;
				}
			});
		} catch (IOReactorException e) {
			logger.error("create connectingIOReactor error");
			throw new HttpUtilsException("createPoolingNHttpClientConnectionManager() failed.", e);
		}
		connectionManager = new PoolingNHttpClientConnectionManager(ioReactor, registry);
	}

}
