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
            SocketChannel serveChannel = listenChannel.accept();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = serveChannel.read(buffer);
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
                        System.out.println("File doesn't exist");
                    } else {
                        FileInputStream fs = new FileInputStream(individualFile);
                        FileChannel fc = fs.getChannel();
                        ByteBuffer fileContent = ByteBuffer.allocate(1024);
                        int byteRead = 0;
                        do {
                            byteRead = fc.read(fileContent);
                            fileContent.flip();
                            serveChannel.write(fileContent);
                            fileContent.clear();
                        } while (byteRead >= 0);
                        fs.close();

                        fc.read(fileContent);
                        fileContent.flip();
                        serveChannel.write(fileContent);
                        serveChannel.close();
                    }
                    serveChannel.write(ByteBuffer.wrap("200".getBytes()));
                    break;
                case ('l'):
                    File[] files = allFiles.listFiles();
                    if (files == null) {
                        System.out.println("No files found.");
                    } else {
                        for (File f : files) {
                            System.out.println(f.getName());
                        }
                    }
                    serveChannel.write(ByteBuffer.wrap("200".getBytes()));
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
                    serveChannel.write(ByteBuffer.wrap("200".getBytes()));
                    break;
                case ('u'):
                    try {
                        FileOutputStream fs = new FileOutputStream("ServerFiles/" + filename, true);
                        FileChannel fc = fs.getChannel();
                        ByteBuffer fileContent = ByteBuffer.allocate(1024);

                        while (serveChannel.read(fileContent) >= 0) {
                            fileContent.flip();
                            fc.write(fileContent);
                            fileContent.clear();
                        }
                        fs.close();

                    } catch (IOException e) {
                        System.err.print("Error fetching file.\n");
                    }
                    serveChannel.write(ByteBuffer.wrap("200".getBytes()));
                    break;
                case ('e'):
                    System.exit(0);
                    serveChannel.write(ByteBuffer.wrap("200".getBytes()));
                    break;
                default:
                    serveChannel.write(ByteBuffer.wrap("400".getBytes()));
                    System.out.println("Invalid command");
            }
        }
    }
}