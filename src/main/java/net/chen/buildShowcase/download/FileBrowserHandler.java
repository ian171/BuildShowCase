package net.chen.buildShowcase.download;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FileBrowserHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final File root;

    public FileBrowserHandler(File root) {
        this.root = root;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        String uri = URLDecoder.decode(req.uri(), StandardCharsets.UTF_8);
        if (uri.contains("..")) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }

        File target = new File(root, uri);
        if (!target.exists()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        if (target.isDirectory()) {
            sendDirectory(ctx, target, uri);
        } else {
            sendFile(ctx, target);
        }
    }

    /* ===== 目录 HTML ===== */
    private void sendDirectory(ChannelHandlerContext ctx, File dir, String uri) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='utf-8'><title>Index of ")
                .append(uri)
                .append("</title></head><body>");
        html.append("<h2>Index of ").append(uri).append("</h2><ul>");

        if (!uri.equals("/")) {
            html.append("<li><a href=\"../\">../</a></li>");
        }

        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                String name = f.getName() + (f.isDirectory() ? "/" : "");
                html.append("<li><a href=\"")
                        .append(uri.endsWith("/") ? uri : uri + "/")
                        .append(name)
                        .append("\">")
                        .append(name)
                        .append("</a></li>");
            }
        }

        html.append("</ul></body></html>");

        FullHttpResponse resp = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                ctx.alloc().buffer().writeBytes(html.toString().getBytes(StandardCharsets.UTF_8))
        );

        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        HttpUtil.setContentLength(resp, resp.content().readableBytes());
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }

    /* ===== 文件下载 ===== */
    private void sendFile(ChannelHandlerContext ctx, File file) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(file, "r");

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpUtil.setContentLength(response, raf.length());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getName() + "\"");

        ctx.write(response);
        ctx.write(new ChunkedFile(raf));
        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                .addListener(ChannelFutureListener.CLOSE);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }
}
