package net.chen.buildShowcase.download;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Deprecated(forRemoval = true)
public class DownloadServer {

    private final int port;
    private final File baseDir;

    public DownloadServer(int port, File baseDir) {
        this.port = port;
        this.baseDir = baseDir;
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new DownloadHandler(baseDir));
                        }
                    });

            Channel ch = bootstrap.bind(port).sync().channel();
            System.out.println("Download server started on port " + port);
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // Handler
    private static class DownloadHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final File baseDir;

        public DownloadHandler(File baseDir) {
            this.baseDir = baseDir;
        }


        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
            if (!req.method().equals(HttpMethod.GET)) {
                sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
                return;
            }

            String uri = req.uri(); // /download/filename.schem
            if (!uri.startsWith("/download/")) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            String fileName = java.net.URLDecoder.decode(uri.substring("/download/".length()), StandardCharsets.UTF_8);
            File file = new File(baseDir, fileName);

            if (!file.exists() || !file.isFile()) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();

            // 创建响应头
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpUtil.setContentLength(response, fileLength);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
            response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + file.getName() + "\"");
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            ctx.write(response);

            // 发送文件
            ChannelFuture sendFileFuture = ctx.writeAndFlush(
                    new ChunkedFile(raf, 0, fileLength, 8192),
                    ctx.newProgressivePromise()
            );

            sendFileFuture.addListener(future -> {
                try {
                    raf.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // 浏览器需要最后写 LastHttpContent
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
        }

        private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
