package org.dromara.neutrinoproxy.server.proxy.core;

import io.netty.handler.logging.LoggingHandler;
import org.dromara.neutrinoproxy.core.ProxyMessageDecoder;
import org.dromara.neutrinoproxy.core.ProxyMessageEncoder;
import org.dromara.neutrinoproxy.core.util.FileUtil;
import org.dromara.neutrinoproxy.server.base.proxy.ProxyConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.event.AppLoadEndEvent;
import org.noear.solon.core.event.EventListener;
import org.noear.solon.core.runtime.NativeDetector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * 代理隧道服务
 * @author: aoshiguchen
 * @date: 2022/6/16
 */
@Slf4j
@Component
public class ProxyTunnelServer implements EventListener<AppLoadEndEvent> {
	@Inject
	private ProxyConfig proxyConfig;
	@Inject("tunnelBossGroup")
	private NioEventLoopGroup serverBossGroup;
	@Inject("tunnelWorkerGroup")
	private NioEventLoopGroup serverWorkerGroup;
	@Override
	public void onEvent(AppLoadEndEvent appLoadEndEvent) throws Throwable {
        // aot 阶段，不启动代理服务
        if (NativeDetector.isNotAotRuntime()) {
            startProxyServer();
            startProxyServerForSSL();
        }
	}
	/**
	 * 启动代理服务
	 */
	private void startProxyServer() {
        // 启动代理服务、9000端口
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				proxyServerCommonInitHandler(ch);
			}
		});
		try {
			bootstrap.bind(proxyConfig.getTunnel().getPort()).sync();
			log.info("proxy server started，port：{}", proxyConfig.getTunnel().getPort());
		} catch (Exception e) {
			log.error("proxy server error", e);
		}
	}

	private void startProxyServerForSSL() {
        // 启动代理服务、9002端口
		if (null == proxyConfig.getTunnel().getSslPort()) {
			return;
		}
 		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(serverBossGroup, serverWorkerGroup)
			.channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(createSslHandler());
				proxyServerCommonInitHandler(ch);
			}
		});
		try {
			bootstrap.bind(proxyConfig.getTunnel().getSslPort()).sync();
			log.info("proxy server started，SSL port： {}", proxyConfig.getTunnel().getSslPort());
		} catch (Exception e) {
			log.error("proxy server error", e);
		}
	}

	private ChannelHandler createSslHandler() {
		try {
			InputStream jksInputStream = FileUtil.getInputStream(proxyConfig.getTunnel().getJksPath());
			SSLContext serverContext = SSLContext.getInstance("TLS");
			final KeyStore ks = KeyStore.getInstance("JKS");

			ks.load(jksInputStream, proxyConfig.getTunnel().getKeyStorePassword().toCharArray());
			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, proxyConfig.getTunnel().getKeyManagerPassword().toCharArray());
			TrustManager[] trustManagers = null;

			serverContext.init(kmf.getKeyManagers(), trustManagers, null);

			SSLEngine sslEngine = serverContext.createSSLEngine();
			sslEngine.setUseClientMode(false);
			sslEngine.setNeedClientAuth(false);

			return new SslHandler(sslEngine);
		} catch (Exception e) {
			log.error("create SSL handler error", e);
			e.printStackTrace();
		}
		return null;
	}

	private void proxyServerCommonInitHandler(SocketChannel ch) {
		if (null != proxyConfig.getTunnel().getTransferLogEnable() && proxyConfig.getTunnel().getTransferLogEnable()) {
			ch.pipeline().addFirst(new LoggingHandler(ProxyTunnelServer.class));
		}
		ch.pipeline().addLast(new ProxyMessageDecoder(proxyConfig.getProtocol().getMaxFrameLength(),
			proxyConfig.getProtocol().getLengthFieldOffset(), proxyConfig.getProtocol().getLengthFieldLength(),
			proxyConfig.getProtocol().getLengthAdjustment(), proxyConfig.getProtocol().getInitialBytesToStrip()));
		ch.pipeline().addLast(new ProxyMessageEncoder());
		ch.pipeline().addLast(new IdleStateHandler(proxyConfig.getProtocol().getReadIdleTime(), proxyConfig.getProtocol().getWriteIdleTime(), proxyConfig.getProtocol().getAllIdleTimeSeconds()));
		ch.pipeline().addLast(new ProxyTunnelChannelHandler());
	}
}
