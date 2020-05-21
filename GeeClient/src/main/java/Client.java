

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.Socket;


public class Client {
    public static void main(String[] args) {
        final int dataSize = 1024 * 1024 * 2;
        try (Socket socket = new Socket("localhost", 8080);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.write((byte)10);
            byte[] logPasBytes = "client1 12345".getBytes();
            out.write(logPasBytes.length);
            System.out.println("Sent Log and pas length " + logPasBytes.length);
            out.write(logPasBytes);

            out.write((byte)30);
            byte[] loginBytes = "client1".getBytes();
            out.write(loginBytes.length);
            out.write(loginBytes);
            byte[] fileNameBytes = "text.txt".getBytes();
            out.write(fileNameBytes.length);
            out.write(fileNameBytes);

            byte[] bytes = new byte[1];
            byte[] frame = new byte[dataSize];
            in.read(bytes);
            System.out.println("Files size is: " + bytes[0]);

            BufferedOutputStream write = new BufferedOutputStream(new FileOutputStream("clients\\client1\\_" + new String(fileNameBytes), true));

            int parts = bytes[0]/dataSize;
            while(parts > 0) {
                in.read(frame);
                write.write(frame);
                parts--;
            }
            if(bytes[0]%dataSize != 0) {
                frame = new byte[bytes[0]%dataSize];
                in.read(frame);
                write.write(frame);
                System.out.println(new String(frame));
            }
            System.out.println("File received");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
