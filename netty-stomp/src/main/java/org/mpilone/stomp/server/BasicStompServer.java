
package org.mpilone.stomp.server;

import org.mpilone.stomp.shared.FrameDebugHandler;
import org.mpilone.stomp.shared.StompFrameDecoder;
import org.mpilone.stomp.shared.StompFrameEncoder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 *
 * @author mpilone
 */
public class BasicStompServer {
  private Channel channel;
  private NioEventLoopGroup bossGroup;
  private NioEventLoopGroup workerGroup;

  public void start(int port) throws InterruptedException {
    bossGroup = new NioEventLoopGroup(); 
    workerGroup = new NioEventLoopGroup();

    ServerBootstrap b = new ServerBootstrap(); 
    b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class) 
        .childHandler(createChildHandler())
        .option(ChannelOption.SO_BACKLOG, 128) 
        .childOption(ChannelOption.SO_KEEPALIVE, true);

    // Bind and start to accept incoming connections.
    ChannelFuture f = b.bind(port).sync(); 
    channel = f.channel();
  }

  public void stop() throws InterruptedException {
    try {
      // Wait until the server socket is closed.
      if (channel.isActive()) {
        channel.close().sync();
      }
    }
    finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();

      workerGroup = null;
      bossGroup = null;
      channel = null;
    }
  }

  protected ChannelHandler createChildHandler() {
    return new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new StompFrameDecoder());
        ch.pipeline().addLast(new StompFrameEncoder());

        ch.pipeline().addLast(new FrameDebugHandler());
        ch.pipeline().addLast(new ConnectFrameHandler());
        ch.pipeline().addLast(new ReceiptWritingHandler());
        ch.pipeline().addLast(new ErrorWritingHandler());
        ch.pipeline().addLast(new DisconnectFrameHandler());
      }
    };
  }

}
