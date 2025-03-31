import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    static class DownloadTask implements Runnable {
        private final SocketChannel socket;
        private final File file;

        public DownloadTask(SocketChannel socket, File file) {
            this.socket = socket;
            this.file = file;
        }

        public void run() {
            try {
                if (!file.exists()) {
                    socket.write(ByteBuffer.wrap("400".getBytes()));
                } else {
                    socket.write(ByteBuffer.wrap("200".getBytes()));
                    FileInputStream fs = new FileInputStream(file);
                    FileChannel fc = fs.getChannel();
                    ByteBuffer fileContent = ByteBuffer.allocate(1024);
                    int byteRead = 0;
                    do {
                        byteRead = fc.read(fileContent);
                        fileContent.flip();
                        socket.write(fileContent);
                        fileContent.clear();
                    } while (byteRead > -1);
                    fs.close();
                }
                socket.shutdownOutput();
                socket.close();
            } catch (Exception e) {
                System.err.print("Error fetching file.\n");
            }
        }
    }

    static class UploadTask implements Runnable{
        private final SocketChannel socket;
        private final String filename;

        public UploadTask(SocketChannel socket, String filename) {
            this.socket = socket;
            this.filename = filename;
        }

        public void run() {
            try {
                socket.write(ByteBuffer.wrap("ready".getBytes()));

                try (FileOutputStream fs = new FileOutputStream("ServerFiles/" + filename, true)) {
                    ByteBuffer uploadBuffer = ByteBuffer.allocate(1024);
                    int uploadedBytesRead;
                    while((uploadedBytesRead = socket.read(uploadBuffer)) != -1) {
                        uploadBuffer.flip();
                        byte[] u1 = new byte[uploadedBytesRead];
                        uploadBuffer.get(u1);
                        fs.write(u1);
                        uploadBuffer.clear();
                    }
                    fs.close();
                    socket.write(ByteBuffer.wrap("200".getBytes()));
                } catch (IOException e) {
                    System.err.print("Error fetching file.\n");
                    socket.write(ByteBuffer.wrap("400".getBytes()));
                }
                socket.shutdownOutput();
            } catch (Exception e) {
                System.err.print("Error uploading file.\n");
            }
        }
    }

    static class AcceptedTask implements Runnable{
        private final ServerSocketChannel socket;
        private final ExecutorService es;

        public AcceptedTask(ServerSocketChannel socket, ExecutorService es) {
            this.socket = socket;
            this.es = es;
        }

        public void run(){
            try {
                while (true) {
                    SocketChannel openSocket = socket.accept();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int bytesRead = openSocket.read(buffer);
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
                            es.submit(new DownloadTask(openSocket, individualFile));
                            break;
                        case ('l'):
                            File[] files = allFiles.listFiles();
                            StringBuilder stringFiles = new StringBuilder();
                            if (files == null) {
                                openSocket.write(ByteBuffer.wrap("No files found".getBytes()));
                            } else {
                                for (File f : files) {
                                    if (!f.getName().equals(".DS_Store")) {
                                        stringFiles.append(f.getName() + "%");
                                    }
                                }
                                String fullString = new String(stringFiles);
                                openSocket.write(ByteBuffer.wrap(fullString.getBytes()));
                            }
                            openSocket.shutdownOutput();
                            openSocket.close();
                            break;
                        case ('r'):
                            if (!individualFile.exists()) {
                                openSocket.write(ByteBuffer.wrap("400".getBytes()));
                            } else {
                                individualFile.delete();
                                openSocket.write(ByteBuffer.wrap("200".getBytes()));
                            }
                            break;
                        case ('m'):
                            String[] splitFiles = filename.split("\\$");
                            String section1 = splitFiles[1];
                            File oldFileName = new File("ServerFiles/" + section1);
                            String newFileName = splitFiles[2];
                            if (!oldFileName.exists()) {
                                openSocket.write(ByteBuffer.wrap("400".getBytes()));
                            } else {
                                oldFileName.renameTo(new File("ServerFiles/" + newFileName));
                                openSocket.write(ByteBuffer.wrap("200".getBytes()));
                            }
                            openSocket.shutdownOutput();

                            break;
                        case ('u'):
                            es.submit(new UploadTask(openSocket, filename));
                            break;
                        case ('e'):
                            openSocket.write(ByteBuffer.wrap("200".getBytes()));
                            System.out.println("Client shutdown connection.\n>>");
                            openSocket.shutdownOutput();
                            openSocket.close();
                            break;
                        default:
                            openSocket.write(ByteBuffer.wrap("400".getBytes()));
                            openSocket.shutdownOutput();
                    }
                }
            } catch (Exception e) {
                System.err.print("Error accepting connection.\n");
            }

        }
    }

    public static void main(String[] args) throws Exception {
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(3001));
        ExecutorService es = Executors.newFixedThreadPool(4);
        System.out.println(">>Server is running on port 3001");
        Scanner sc = new Scanner(System.in);

        while (true) {
            es.submit(new AcceptedTask(listenChannel, es));

            System.out.print(">>");
            String command = sc.nextLine();
            if (command.equals("exit")) {
                break;
            } else {
                System.out.println("Invalid command");
            }
        }
        es.shutdown();
        sc.close();
        System.exit(0);
    }
}