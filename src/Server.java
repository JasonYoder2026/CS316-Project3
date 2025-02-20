import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server {
    public static void main(String[] args) throws Exception {
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(3001));

        while (true) {
            SocketChannel serverChannel = listenChannel.accept();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = serverChannel.read(buffer);
            buffer.flip();
            byte[] a = new byte[bytesRead];
            buffer.get(a);
            String receivedData = new String(a);
            char commandChar = receivedData.charAt(0);
            String filename = receivedData.substring(1);

            File individualFile = new File("ServerFiles/" + filename);
            File allFiles = new File("ServerFiles/");

            switch (Character.toLowerCase(commandChar)) {
                case ('d'):
                    if (!individualFile.exists()) {
                        serverChannel.write(ByteBuffer.wrap("400".getBytes()));
                    } else {
                        FileInputStream fs = new FileInputStream(individualFile);
                        FileChannel fc = fs.getChannel();
                        ByteBuffer fileContent = ByteBuffer.allocate(1024);
                        int byteRead = 0;
                        do {
                            byteRead = fc.read(fileContent);
                            fileContent.flip();
                            serverChannel.write(fileContent);
                            fileContent.clear();
                        } while (byteRead > -1);
                        fs.close();
                    }
                    serverChannel.shutdownOutput();
                    serverChannel.close();

                    break;
                case ('l'):
                    File[] files = allFiles.listFiles();
                    if (files == null) {
                        serverChannel.write(ByteBuffer.wrap("No files found".getBytes()));
                    } else {
                        for (File f : files) {
                            serverChannel.write(ByteBuffer.wrap(f.getName().getBytes()));
                        }
                    }
                    serverChannel.shutdownOutput();
                    serverChannel.close();
                    break;
                case ('r'):
                    if (individualFile.exists()) {
                        System.out.println("File not found.");
                    } else {
                        individualFile.delete();
                    }
                    break;
                case ('m'):
                    if (!individualFile.exists()) {
                        System.out.println("File not found.");
                    } else {
                        //regex separator; for example "myfile$newMyFile"
                        String[] newFilename = filename.split("\\$");
                        individualFile.renameTo(new File("ServerFiles/" + newFilename[1]));
                    }
                    serverChannel.write(ByteBuffer.wrap("200".getBytes()));
                    serverChannel.shutdownOutput();

                    break;
                case ('u'):
                    System.out.println(filename);
                    try (FileOutputStream fs = new FileOutputStream("ServerFiles/" + filename, true)) {
                        ByteBuffer uploadBuffer = ByteBuffer.allocate(1024);
                        int uploadedBytesRead;
                        while((uploadedBytesRead = serverChannel.read(uploadBuffer)) != -1) {
                            uploadBuffer.flip();
                            byte[] u1 = new byte[uploadedBytesRead];
                            uploadBuffer.get(u1);
                            fs.write(u1);
                            uploadBuffer.clear();
                        }
                        fs.close();
                        serverChannel.write(ByteBuffer.wrap("200".getBytes()));
                    } catch (IOException e) {
                        System.err.print("Error fetching file.\n");
                        serverChannel.write(ByteBuffer.wrap("400".getBytes()));
                    }
                    serverChannel.shutdownOutput();
                    break;
                case ('e'):
                    System.exit(0);
                    serverChannel.write(ByteBuffer.wrap("200".getBytes()));
                    serverChannel.shutdownOutput();

                    break;
                default:
                    serverChannel.write(ByteBuffer.wrap("400".getBytes()));
                    serverChannel.shutdownOutput();

                    System.out.println("Invalid command");
            }
        }
    }
}