
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.nio.ByteBuffer;

public class SendingHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf byteBuf;
        if(msg instanceof ByteBuf) {
            byteBuf = (ByteBuf) msg;
        }
//        if(msg instanceof byte[]){
//            byte[] b = (byte[])msg;
//            byteBuf = ctx.alloc().buffer(b.length);
//            byteBuf.writeBytes(b);
//        }
        else {
            byte b = (byte) msg;
            byteBuf = ctx.alloc().buffer(1);
            byteBuf.writeByte(b);
        }
        ctx.writeAndFlush(byteBuf);
        byteBuf.release();
    }
}
