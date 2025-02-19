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
                        serveChannel.write(ByteBuffer.wrap("400".getBytes()));
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
                        } while (byteRead > -1);
                        fs.close();
                    }
                    serveChannel.shutdownOutput();
                    serveChannel.close();

                    break;
                case ('l'):
                    File[] files = allFiles.listFiles();
                    if (files == null) {
                        serveChannel.write(ByteBuffer.wrap("No files found".getBytes()));
                    } else {
                        for (File f : files) {
                            serveChannel.write(ByteBuffer.wrap(f.getName().getBytes()));
                        }
                    }
                    serveChannel.shutdownOutput();

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
                    serveChannel.shutdownOutput();

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
                    serveChannel.shutdownOutput();

                    break;
                case ('e'):
                    System.exit(0);
                    serveChannel.write(ByteBuffer.wrap("200".getBytes()));
                    serveChannel.shutdownOutput();

                    break;
                default:
                    serveChannel.write(ByteBuffer.wrap("400".getBytes()));
                    serveChannel.shutdownOutput();

                    System.out.println("Invalid command");
            }
        }
    }
}