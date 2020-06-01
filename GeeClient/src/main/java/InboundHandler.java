import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

public class InboundHandler extends ChannelInboundHandlerAdapter {
    private enum State{
        IDLE, FILELIST, AUTHORIZED, DOWNLOAD_FILE_SIZE, DOWNLOAD_FILE
    }
    private State currentState;

    private byte code;
    private int dataSize = 1024 * 1024 * 2;
    public static String fileName = "";
    private long fileSize = 0L;
    private long numberOfParts;

    public InboundHandler() {
        currentState = State.IDLE;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf)msg;
        if(currentState == State.IDLE) {
            if(byteBuf.readableBytes() > 0) {
                byte readByte = byteBuf.readByte();
                if(readByte == (byte)25) {
                    currentState = State.FILELIST;
//                    System.out.println("Client is authorized");
//                    Client.isAuthorized = true;
                }
            }
        }

        if(currentState == State.FILELIST) {
            if(byteBuf.readableBytes() > 0) {
                System.out.println("currentState == State.FILELIST");
                int stringSize;
                byte[] bytes;
                String fileNameString;
                while (byteBuf.readableBytes() > 0) {
                    stringSize = byteBuf.readInt();
                    bytes = new byte[stringSize];
                    byteBuf.readBytes(bytes);
                    fileNameString = new String(bytes);
                    System.out.println(fileNameString);
                    Client.setListViewFilesServer(fileNameString);
                }
                currentState = State.AUTHORIZED;
                Client.isAuthorized = true;
            }
        }
        if(currentState == State.AUTHORIZED) {
            System.out.println("currentState == State.AUTHORIZED");
            switch (Operations.operationCode) {
                case (byte)30: {
                    currentState = State.DOWNLOAD_FILE_SIZE;
                    break;
                }
            }
        }

        if(currentState == State.DOWNLOAD_FILE_SIZE) {
            System.out.println("State.DOWNLOAD_FILE_SIZE");
            if (byteBuf.readableBytes() == 8) {
                fileSize = byteBuf.readLong();
                numberOfParts = fileSize / dataSize;
                if (fileSize % dataSize != 0) numberOfParts++;
                currentState = State.DOWNLOAD_FILE;
                System.out.println("File size " + fileSize);
            }
        }

        if(currentState == State.DOWNLOAD_FILE) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(dataSize);
            byte[] bytes = new byte[dataSize];
            if(byteBuf.readableBytes() > 0) {
                try(BufferedOutputStream bufferedOutputStream =
                            new BufferedOutputStream(new FileOutputStream("clientStorage/" + fileName), dataSize)){
                    while(numberOfParts > 0) {
                        byteBuf.readBytes(bytes);
                        bufferedOutputStream.write(bytes);
                        bufferedOutputStream.flush();
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

            



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    }
}
