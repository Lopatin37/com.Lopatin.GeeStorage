import io.netty.buffer.ByteBuf;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;


public class SecondHandler extends ChannelInboundHandlerAdapter {
    private enum State{
        IDLE, LOGIN_LENGTH, LOGIN, FILE_NAME_LENGTH, FILE_NAME, READY
    }

    private final int dataSize;
    private State currentState;
    private String login;
    private String fileName;
    private byte code;
    private int loginLength;
    private int fileNameLength;
    private long fileSize;
    private long numberOfParts;

    public SecondHandler() {
        dataSize = 1024 * 1024 * 2;
        currentState = State.IDLE;
        fileSize = 0L;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf)msg;

        while(buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                code = buf.readByte();
                currentState = State.LOGIN_LENGTH;
                System.out.println("Code " + code);
            }
            if (currentState == State.LOGIN_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    loginLength = buf.readInt();
                    currentState = State.LOGIN;
                    System.out.println("login length is " + loginLength);
                }
            }

            if (currentState == State.LOGIN) {
                if (buf.readableBytes() >= loginLength) {
                    byte[] bytes = new byte[loginLength];
                    buf.readBytes(bytes);
                    login = new String(bytes);
                    System.out.println("Login is " + login);
                    currentState = State.FILE_NAME_LENGTH;
                }
            }

            if (currentState == State.FILE_NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    fileNameLength = buf.readInt();
                    System.out.println("File name length is " + fileNameLength);
                    currentState = State.FILE_NAME;
                }
            }

            if (currentState == State.FILE_NAME) {
                if (buf.readableBytes() >= fileNameLength) {
                    byte[] bytes = new byte[fileNameLength];
                    buf.readBytes(bytes);
                    fileName = new String(bytes);
                    System.out.println("File name is " + fileName);
                    currentState = State.READY;
                }
            }


            if (currentState == State.READY) {

                switch (code) {
                    case (byte)30: {
                        System.out.println("currentState == State.READY");
                        Path path = Paths.get("serverStorage/" + login + "/" + fileName);
                        fileSize = Files.size(path);
                        byte[] bytes = new byte[]{
                                (byte) (fileSize >>> 56),
                                (byte) (fileSize >>> 48),
                                (byte) (fileSize >>> 40),
                                (byte) (fileSize >>> 32),
                                (byte) (fileSize >>> 24),
                                (byte) (fileSize >>> 16),
                                (byte) (fileSize >>> 8),
                                (byte) fileSize
                        };
                        ByteBuf byteBuf1 = ByteBufAllocator.DEFAULT.buffer(8);
                        byteBuf1.writeBytes(bytes);
                        ctx.writeAndFlush(byteBuf1);
                        System.out.println("File size " + fileSize);
                        byteBuf1 = ByteBufAllocator.DEFAULT.buffer(dataSize);
                        numberOfParts = fileSize / dataSize;
                        if (fileSize % dataSize != 0) numberOfParts++;
                        bytes = new byte[dataSize];
                        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(
                                "serverStorage\\" + login + "\\" + fileName), dataSize)){
                            while(numberOfParts > 0) {
                                bufferedInputStream.read(bytes);
                                byteBuf1.writeBytes(bytes);
                                ctx.writeAndFlush(byteBuf1);
                                numberOfParts--;
                            }
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                        if(numberOfParts == 0) {
                            currentState = State.IDLE;
                        }
                    }
                }
            }
        }
        if(buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }
}
