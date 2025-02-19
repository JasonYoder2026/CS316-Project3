import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        System.out.println("get \"<filename>\" - Download file from server");
        System.out.println("ls - List files in server");
        System.out.println("rm \"<filename>\" - Delete file from server");
        System.out.println("mv \"<filename>\" \"<newfilename>\" - Rename file in server");
        System.out.println("ftp \"<filename>\" - Upload file to server");
        System.out.println("exit - Exit server");
        while(true) {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(args[0], serverPort));
            System.out.println(">>");
            String command = input.nextLine().trim();
            String[] commandParts = getCommandParts(command);
            try {
                ByteBuffer cmd = processCommand(commandParts, socketChannel);
                if (cmd.getChar(0) == 'e') {
                    break;
                }
            } catch (IOException e) {
                System.err.println("Error processing command.");
            }
            socketChannel.close();
        }
        input.close();
    }

    private static String[] getCommandParts(String command) {
        Pattern pattern = Pattern.compile("\"([^\"]+)\"|(\\S+)");
        Matcher matcher = pattern.matcher(command);
        String[] commandParts = new String[3];
        int i = 0;
        while (matcher.find() && i < commandParts.length) {
            commandParts[i++] = matcher.group(1) != null ? "\"" + matcher.group(1) + "\"" : matcher.group(2);
        }
        return commandParts;
    }

    private static ByteBuffer processCommand(String[] commandParts, SocketChannel socketChannel) throws IOException {
        ByteBuffer commandBytes = null;
        switch (commandParts[0].toLowerCase()) {
            case ("get"):
                commandBytes = ByteBuffer.wrap(("d" + commandParts[1]).getBytes());
                downloadFile(socketChannel, commandParts[1]);
                authentication(socketChannel, "Download");
                break;
            case ("ls"):
                commandBytes = ByteBuffer.wrap("l".getBytes());
                socketChannel.write(commandBytes);
                authentication(socketChannel, "List");
                socketChannel.shutdownOutput();
                break;
            case ("rm"):
                commandBytes = ByteBuffer.wrap(("r" + commandParts[1]).getBytes());
                socketChannel.write(commandBytes);
                authentication(socketChannel, "Deletion");
                socketChannel.shutdownOutput();
                break;
            case ("mv"):
                commandBytes = ByteBuffer.wrap(("m" + commandParts[1] + commandParts[2]).getBytes());
                socketChannel.write(commandBytes);
                authentication(socketChannel, "Renaming");
                socketChannel.shutdownOutput();
                break;
            case ("ftp"):
                commandBytes = ByteBuffer.wrap(("u" + commandParts[1]).getBytes());
                uploadFile(socketChannel, commandParts[1]);
                break;
            case ("exit"):
                commandBytes = ByteBuffer.wrap("e".getBytes());
                break;
            default:
                System.out.println("Invalid command");
        }
        return commandBytes;
    }

    private static void uploadFile(SocketChannel socket, String filename) {
        File file = new File("ClientFiles/" + filename);
        if(!file.exists()) {
            System.out.println("File not found.");
        } else {
            try {
                socket.write(ByteBuffer.wrap("u".getBytes()));
                FileInputStream fs = new FileInputStream(file);
                FileChannel fc = fs.getChannel();
                ByteBuffer fileContent = ByteBuffer.allocate(1024);
                int byteRead;
                do {
                    byteRead = fc.read(fileContent);
                    fileContent.flip();
                    socket.write(fileContent);
                    fileContent.clear();
                } while(byteRead >= 0);
                fs.close();
                socket.shutdownOutput();
            } catch (IOException e) {
                System.err.print("Error reading file.\n");
            }
        }
    }

    private static void downloadFile(SocketChannel socket, String filename) {
        try {
            socket.write(ByteBuffer.wrap(("d" + filename).getBytes()));
            socket.shutdownOutput();
            FileOutputStream fs = new FileOutputStream("ClientFiles/" + filename, true);
            FileChannel fc = fs.getChannel();
            ByteBuffer fileContent = ByteBuffer.allocate(1024);

            while (socket.read(fileContent) >= 0) {
                fileContent.flip();
                fc.write(fileContent);
                fileContent.clear();
            }
            fs.close();
            System.out.println(filename + " downloaded.");

        } catch (IOException e) {
            System.err.print("Error fetching file.\n");
        }
    }

    private static void authentication(SocketChannel socket, String command) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = socket.read(buffer);
            buffer.flip();
            byte[] a = new byte[bytesRead];
            buffer.get(a);
            String statusCode = new String(a);
            if (command.equals("List")) {
                System.out.println("Files in server:");
                while (socket.read(buffer) >= 0) {
                    buffer.flip();
                    byte[] b = new byte[bytesRead];
                    buffer.get(b);
                    System.out.println(new String(b));
                    buffer.clear();
                }
            }else if (statusCode.equals("400")) {
                System.out.println(command + " unsuccessful. An error occurred.");
                System.exit(1);
            } else if (statusCode.equals("200")) {
                System.out.println(command + " successful.");
            } else {
                System.out.println(statusCode);
            }
        } catch (IOException e) {
            System.err.println("Error reading message.");
        }
    }
}

