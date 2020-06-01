import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

import java.io.IOException;
import java.nio.file.Path;

public class Operations {
    public static byte operationCode;
    public static String fileName = "";
    public static void authorization(String login, String password, Channel channel,
                                     ChannelFutureListener finishListener){
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(1);
        byteBuf.writeByte((byte)10);
        channel.writeAndFlush(byteBuf);

        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        String logAndPas = login + " " + password;
        byteBuf.writeInt(logAndPas.getBytes().length);
        channel.writeAndFlush(byteBuf);

        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(logAndPas.getBytes().length);
        byteBuf.writeBytes(logAndPas.getBytes());
        channel.writeAndFlush(byteBuf);
        System.out.println(byteBuf.capacity() + " " + byteBuf.readableBytes());



    }
    public static void download(String fileName, String login, Channel channel, ChannelFutureListener finishListener) throws IOException {
        Operations.fileName = fileName;
        operationCode = (byte)30;
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(1);
        byteBuf.writeByte(operationCode);
        channel.writeAndFlush(byteBuf);

        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        byteBuf.writeInt(login.getBytes().length);
        channel.writeAndFlush(byteBuf);

        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(login.getBytes().length);
        byteBuf.writeBytes(login.getBytes());
        channel.writeAndFlush(byteBuf);

        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        byteBuf.writeInt(fileName.getBytes().length);
        channel.writeAndFlush(byteBuf);

        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(fileName.getBytes().length);
        byteBuf.writeBytes(fileName.getBytes());
        channel.writeAndFlush(byteBuf);

    }

    public static void upload(Path path, Channel channel, ChannelFutureListener finishListener){

    }

}
