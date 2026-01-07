package net.chen.buildShowcase.download;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import net.chen.buildShowcase.BuildShowcasePlugin;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.RandomAccessFile;

public class HttpsFileServer {

    private final int port;
    private final File rootDir;
    private final File cert;
    private final File key;

    public HttpsFileServer(int port, File rootDir, File cert, File key) {
        this.port = port;
        this.rootDir = rootDir;
        this.cert = cert;
        this.key = key;
    }

    public void start() throws InterruptedException {

        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(65536));
                        ch.pipeline().addLast(new ChunkedWriteHandler());
                        ch.pipeline().addLast(new FileBrowserHandler(new File(BuildShowcasePlugin.getInstance().getDataFolder(), "structures/builds")));
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
                                File file = rootDir;
                                if (!file.exists()) {
                                    return;
                                }

                                RandomAccessFile raf = new RandomAccessFile(file, "r");
                                long fileLength = raf.length();

                                HttpResponse response = new DefaultHttpResponse(
                                        HttpVersion.HTTP_1_1,
                                        HttpResponseStatus.OK
                                );

                                HttpUtil.setContentLength(response, fileLength);
                                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
                                response.headers().set(
                                        HttpHeaderNames.CONTENT_DISPOSITION,
                                        "attachment; filename=\"" + file.getName() + "\""
                                );
                                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

                                ctx.write(response);

                                ChannelFuture sendFileFuture =
                                        ctx.write(new ChunkedFile(raf, 0, fileLength, 8192));

                                sendFileFuture.addListener(ChannelFutureListener.CLOSE);
                                ctx.flush();
                            }
                        });

                    }
                });

        bootstrap.bind(port).sync();
        System.out.println("[BuildShowcase] HTTPS Download Server started on " + port);
    }
}
