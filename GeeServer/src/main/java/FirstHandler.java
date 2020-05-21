import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class FirstHandler extends ChannelInboundHandlerAdapter {
    public enum State{
        IDLE, LOGIN_PASSWORD_LENGTH, LOGIN_PASSWORD, AUTHORIZED
    }
    private State currentState;
    private static List<String> currentUsers = new ArrayList<>();
    int lengthInt;

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
                    lengthInt = buf.readByte();
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
                    String[] strings = logPass.split(" ");
                    System.out.println("Login and password are " + logPass);
                    if(currentUsers.contains(strings[0])) {
                        ctx.writeAndFlush(ByteBuffer.allocate(1).put((byte)21));
                        buf.release();
                        return;
                    }
//Сделать так, что бы connection открывался статически для всех клиентов в 1 месте
                    DBHandler db = DBHandler.getInstance();
                    System.out.println("Starting authorization");
                    if(db.authorization(strings[0], strings[1])) {
                        currentState = State.AUTHORIZED;
                        System.out.println("Client authorized");
                        currentUsers.add(strings[0]);
                        ctx.writeAndFlush(ByteBuffer.allocate(1).put((byte)25));
                    } else {
                        ctx.writeAndFlush(ByteBuffer.allocate(1).put((byte)22));
                    }
                }
            }

            if(currentState == State.AUTHORIZED) {
                if(buf.readableBytes() > 0) {
                    ctx.fireChannelRead(buf.copy());
                    buf.release();
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
