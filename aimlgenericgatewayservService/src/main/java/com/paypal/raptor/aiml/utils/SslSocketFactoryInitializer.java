package com.paypal.raptor.aiml.utils;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import com.paypal.raptor.aiml.common.exception.HttpUtilsException;


/**
 * @author: qingwu
 **/
public class SslSocketFactoryInitializer {

	private PoolingHttpClientConnectionManager connectionManager;
	private SSLConnectionSocketFactory sslConnectionSocketFactory;

	/**
	 * Constructor.
	 */
	public SslSocketFactoryInitializer() {
		createSslConnectionSocketFactory();
		createPoolingHttpClientConnectionManager();
	}

	/***
	 * Get the connection pool manager.
	 *
	 * @return PoolingHttpClientConnectionManager
	 */
	public PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager() {
		return connectionManager;
	}

	/**
	 * Get SSLConnectionSocketFactory.
	 *
	 * @return SSLConnectionSocketFactory
	 */
	public SSLConnectionSocketFactory getSslConnectionSocketFactory() {
		return sslConnectionSocketFactory;
	}

	private void createSslConnectionSocketFactory() {
		SSLContextBuilder builder = new SSLContextBuilder();
		TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
		try {
			builder.loadTrustMaterial(null, acceptingTrustStrategy);
			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
							builder.build(), new String[]{"TLSv1.2", "TLSv1.3"},
					null, NoopHostnameVerifier.INSTANCE);
			sslConnectionSocketFactory = sslSocketFactory;
		} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
			throw new HttpUtilsException("CreatePoolingHttpClientConnectionManager() failed.", e);
		}
	}

	private void createPoolingHttpClientConnectionManager() {
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
						.register("http", new PlainConnectionSocketFactory())
						.register("https", getSslConnectionSocketFactory())
						.build();

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		connectionManager = cm;
	}
}
