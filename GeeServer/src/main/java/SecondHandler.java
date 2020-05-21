import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;


public class SecondHandler extends ChannelInboundHandlerAdapter {
    private enum State{
        IDLE, LOGIN_LENGTH, LOGIN, FILE_NAME_LENGTH, FILE_NAME, READY
    }

    private final int dataSize;
    private State currentState;
    String login;
    String fileName;
    byte code;
    int loginLength;
    int fileNameLength;
    long fileSize;
    long numberOfParts;

    public SecondHandler() {
        dataSize = 1024 * 1024 * 2;
        currentState = State.IDLE;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf)msg;

        while(buf.readableBytes() > 0) {
            if(currentState == State.IDLE) {
                code = buf.readByte();
                if(code == (byte)30) {
                    currentState = State.LOGIN_LENGTH;
                    System.out.println("Code " + code);
                }else{
                    System.out.println("Invalid byte: " + code);
                }
            }
            if(currentState == State.LOGIN_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    loginLength = buf.readByte();
                    currentState = State.LOGIN;
                    System.out.println("login length is " + login);
                }
            }

            if(currentState == State.LOGIN) {
                if (buf.readableBytes() >= loginLength) {
                    byte[] bytes = new byte[loginLength];
                    buf.readBytes(bytes);
                    login = new String(bytes);
                    System.out.println("Login is " + login);
                    currentState = State.FILE_NAME_LENGTH;
                }
            }

            if(currentState == State.FILE_NAME_LENGTH){
                if(buf.readableBytes() >= 4) {
                    fileNameLength = buf.readByte();
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
                    if(code == (byte) 30) currentState = State.READY;
                }
            }


            if (currentState == State.READY) {
                Path path = Paths.get("clients/" + login + "/" + fileName);
                fileSize = Files.size(path);

                byte[] bytes = new byte[1];
                bytes[0] = new Long(fileSize).byteValue();
                ctx.writeAndFlush(bytes);
                numberOfParts = fileSize/dataSize;

                bytes = new byte[dataSize];

                ByteBuffer byteBuf = ByteBuffer.allocate(dataSize);
                try(FileChannel fChan = (FileChannel) Files.newByteChannel(path)){
                    while(numberOfParts > 0) {
                        fChan.read(byteBuf);
                        byteBuf.rewind();
                        byteBuf.get(bytes);
                        ctx.writeAndFlush(bytes);
                        numberOfParts--;
                    }
                    if(fileSize % dataSize != 0) {
                        byteBuf = ByteBuffer.allocate(new Long(fileSize % dataSize).intValue());
                        fChan.read(byteBuf);
                        byteBuf.rewind();
                        bytes = new byte[byteBuf.capacity()];
                        byteBuf.get(bytes);
                        ctx.writeAndFlush(bytes);
                    }
                    System.out.println("File is sent");
                    currentState = State.IDLE;
                }catch (InvalidPathException e) {
                    System.out.println("Path error");
                    e.printStackTrace();
                }catch (IOException e) {
                    System.out.println("I/O error");
                    e.printStackTrace();
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
