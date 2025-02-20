import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Please provide <serverIP> and <serverPort>");
            return;
        }

        int serverPort = Integer.parseInt(args[1]);

        Scanner input = new Scanner(System.in);
        System.out.println("Connected to server...");
        System.out.println("Commands:");
        System.out.println("get - Download file from server");
        System.out.println("ls - List files in server");
        System.out.println("rm - Delete file from server");
        System.out.println("mv - Rename file in server");
        System.out.println("ftp - Upload file to server");
        System.out.println("exit - Exit server");

        while (true) {
            System.out.println(">>");
            String command = input.nextLine().trim();

            switch (command) {
                case ("get"):
                    SocketChannel downloadChannel = SocketChannel.open();
                    downloadChannel.connect(new InetSocketAddress(args[0], serverPort));
                    System.out.println("Enter filename: ");
                    String filename = input.nextLine().trim();
                    downloadChannel.write(ByteBuffer.wrap(("d" + filename).getBytes()));
                    downloadChannel.shutdownOutput();

                    ByteBuffer status = ByteBuffer.allocate(1024);
                    downloadChannel.read(status);
                    status.flip();
                    byte[] statusBytes = new byte[status.remaining()];
                    status.get(statusBytes);
                    String statusCode = new String(statusBytes);

                    if(statusCode.equals("400")) {
                        System.out.println("File doesn't exist");
                        break;
                    } else {
                        try {
                            FileOutputStream fs = new FileOutputStream("ClientFiles/" + filename, true);
                            FileChannel fc = fs.getChannel();
                            ByteBuffer fileContent = ByteBuffer.allocate(1024);

                            while (downloadChannel.read(fileContent) >= 0) {
                                fileContent.flip();
                                fc.write(fileContent);
                                fileContent.clear();
                            }
                            fs.close();
                        } catch (IOException e) {
                            System.err.print("Error fetching file.\n");
                        }
                    }
                    downloadChannel.close();

                    break;
                case ("ls"):
                    SocketChannel listChannel = SocketChannel.open();
                    listChannel.connect(new InetSocketAddress(args[0], serverPort));
                    listChannel.write(ByteBuffer.wrap("l".getBytes()));
                    listChannel.shutdownOutput();
                    System.out.println("Files: ");
                    ByteBuffer files = ByteBuffer.allocate(1024);
                    StringBuilder fileList = new StringBuilder();
                    while (listChannel.read(files) != -1) {
                        files.flip();
                        byte[] b = new byte[files.remaining()];
                        files.get(b);
                        String file = new String(b);
                        fileList.append(file);
                        files.clear();
                    }
                    listChannel.close();
                    String[] fileArray = fileList.toString().split("%");
                    for (String f : fileArray) {
                        System.out.println(f);
                    }
                    break;
                case ("rm"):
                    SocketChannel deleteChannel = SocketChannel.open();
                    deleteChannel.connect(new InetSocketAddress(args[0], serverPort));
                    System.out.println("Enter filename: ");
                    String filenameToDelete = input.nextLine().trim();
                    deleteChannel.write(ByteBuffer.wrap(("r" + filenameToDelete).getBytes()));
                    authentication("Deletion", deleteChannel);
                    deleteChannel.shutdownOutput();
                    deleteChannel.close();
                    break;
                case ("mv"):
                    SocketChannel renameChannel = SocketChannel.open();
                    renameChannel.connect(new InetSocketAddress(args[0], serverPort));
                    System.out.println("Enter filename: ");
                    String filenameToRename = input.nextLine().trim();
                    System.out.println("Enter new filename: ");
                    String newFilename = input.nextLine().trim();
                    renameChannel.write(ByteBuffer.wrap(("m$" + filenameToRename + "$" + newFilename).getBytes()));
                    renameChannel.shutdownOutput();
                    authentication("Rename", renameChannel);
                    renameChannel.close();
                    break;
                case ("ftp"):
                    SocketChannel uploadChannel = SocketChannel.open();
                    uploadChannel.connect(new InetSocketAddress(args[0], serverPort));
                    System.out.println("Enter filename: ");
                    String filenameToUpload = input.nextLine().trim();
                    File file = new File("ClientFiles/" + filenameToUpload);
                    if (!file.exists()) {
                        System.out.println("File not found");
                    } else {
                        uploadChannel.write(ByteBuffer.wrap(("u" + filenameToUpload).getBytes()));
                        ByteBuffer reply = ByteBuffer.allocate(1024);
                        uploadChannel.read(reply);
                        reply.flip();
                        byte[] b = new byte[reply.remaining()];
                        reply.get(b);
                        reply.clear();
                        String replyString = new String(b);
                        if(replyString.equals("ready")) {
                            FileInputStream fs = new FileInputStream(file);
                            byte[] fileContent = new byte[1024];
                            int byteRead;
                            while ((byteRead = fs.read(fileContent)) != -1) {
                                ByteBuffer buffer = ByteBuffer.wrap(fileContent, 0, byteRead);
                                uploadChannel.write(buffer);
                            }
                            fs.close();
                        }
                        uploadChannel.shutdownOutput();
                        authentication("Upload", uploadChannel);
                    }
                    uploadChannel.close();
                    break;
                case ("exit"):
                    SocketChannel exitChannel = SocketChannel.open();
                    exitChannel.connect(new InetSocketAddress(args[0], serverPort));
                    exitChannel.write(ByteBuffer.wrap("e".getBytes()));
                    authentication("Exit", exitChannel);
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid command");
            }
        }
    }

    private static void authentication(String command, SocketChannel uploadChannel) throws IOException {
        ByteBuffer auth = ByteBuffer.allocate(1024);
        int bytesRead = uploadChannel.read(auth);
        auth.flip();
        byte[] a = new byte[bytesRead];
        auth.get(a);
        String statusCode = new String(a);
        if (statusCode.equals("400")) {
            System.out.println(command + " unsuccessful. An error occurred.");
        } else if (statusCode.equals("200")) {
            System.out.println(command + " successful.");
        }
    }
}

