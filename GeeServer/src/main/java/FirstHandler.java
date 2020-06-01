import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class FirstHandler extends ChannelInboundHandlerAdapter {
    public enum State{
        IDLE, LOGIN_PASSWORD_LENGTH, LOGIN_PASSWORD, AUTHORIZED
    }
    private State currentState;
    private static List<String> currentUsers = new ArrayList<>();
    private int lengthInt;
    private String[] strings;
    private ByteBuf outboundByteBuf;

    public FirstHandler() {
        currentState = State.IDLE;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf)msg;
        while(buf.readableBytes() > 0) {
            if(currentState == State.IDLE) {
                byte code = buf.readByte();
                if(code == (byte)10) {
                    currentState = State.LOGIN_PASSWORD_LENGTH;
                    System.out.println("Received code: " + code);
                } else{
                    System.out.println("Invalid first byte: " + code);
                }
            }

            if(currentState == State.LOGIN_PASSWORD_LENGTH) {
                if(buf.readableBytes() >= 4) {
                    lengthInt = buf.readInt();
                    currentState = State.LOGIN_PASSWORD;
                    System.out.println("Received LogPas length: " + lengthInt);
                }
            }

            if(currentState == State.LOGIN_PASSWORD) {
                if(buf.readableBytes() >= lengthInt) {
                    System.out.println("Next step is Login and password");
                    byte[] logPassBytes = new byte[lengthInt];
                    buf.readBytes(logPassBytes);
                    String logPass = new String(logPassBytes);
                    strings = logPass.split(" ");
                    System.out.println("Login and password are " + logPass);
                    if(currentUsers.contains(strings[0])) {
                        ctx.writeAndFlush((byte)21);
                        buf.release();
                        return;
                    }
//Сделать так, что бы connection открывался статически для всех клиентов в 1 месте
                    DBHandler db = DBHandler.getInstance();
                    System.out.println("Starting authorization");
                    if(db.authorization(strings[0], strings[1])) {
                        System.out.println("Client authorized");
                        currentUsers.add(strings[0]);
                        ctx.writeAndFlush((byte)25);
                        try {
                            System.out.println("Making List");
                            List<String> fileList = Files.list(Paths.get("serverStorage/" + strings[0]))
                                    .filter(p -> !Files.isDirectory(p))
                                    .map(p -> p.getFileName().toString())
                                    .collect(Collectors.toList());
                            outboundByteBuf = ctx.alloc().buffer(1024 * 1024);
                            for(String s : fileList) {
                                System.out.println(s);
                                outboundByteBuf.writeInt(s.getBytes().length);
                                outboundByteBuf.writeBytes(s.getBytes());
                            }
                            ctx.writeAndFlush(outboundByteBuf);
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                        currentState = State.AUTHORIZED;
                    } else {
                        ctx.writeAndFlush((byte)22);
                    }
                }
            }

            if(currentState == State.AUTHORIZED) {
                System.out.println("currentState == State.AUTHORIZED");
                if(buf.readableBytes() > 0) {
                    ctx.fireChannelRead(buf);
                    return;
                }
            }
        }
        if(buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
