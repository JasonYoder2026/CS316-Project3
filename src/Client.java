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

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(args[0], serverPort));

        menu(socketChannel);
        socketChannel.close();
    }

    private static void menu(SocketChannel socketChannel) {
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
            System.out.println(">>");
            String command = input.nextLine().trim();
            String[] commandParts = getCommandParts(command);
            ByteBuffer commandBytes = processCommand(commandParts);
            if(commandBytes.getChar(0) == 'u') {
                uploadfile(socketChannel, commandParts[1]);
            } else if(commandBytes.getChar(0) == 'd') {
                downloadFile(socketChannel, commandParts[1]);
            } else {
                try {
                    socketChannel.write(commandBytes);
                } catch (Exception e) {
                    System.err.print("Error sending command to server.\n");
                    break;
                }
            }
        }
        input.close();
    }

    private static String[] getCommandParts(String command) {
        Pattern pattern = Pattern.compile("\"([^\"]+)\"|(\\S+)");
        Matcher matcher = pattern.matcher(command);
        String[] commandParts = new String[3];
        int i = 0;
        while (matcher.find() && i < commandParts.length) {
            commandParts[i++] = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return commandParts;
    }

    private static ByteBuffer processCommand(String[] commandParts) {
        ByteBuffer commandBytes =null;
        switch (commandParts[0].toLowerCase()) {
            case ("get"):
                commandBytes = ByteBuffer.wrap(("d" + commandParts[1]).getBytes());
                break;
            case ("ls"):
                commandBytes = ByteBuffer.wrap("l".getBytes());
                break;
            case ("rm"):
                commandBytes = ByteBuffer.wrap(("r" + commandParts[1]).getBytes());
                break;
            case ("mv"):
                commandBytes = ByteBuffer.wrap(("m" + commandParts[1] + commandParts[2]).getBytes());
                break;
            case ("ftp"):
                commandBytes = ByteBuffer.wrap(("u" + commandParts[1]).getBytes());
                break;
            case ("exit"):
                System.exit(0);
                break;
            default:
                System.out.println("Invalid command");
        }
        return commandBytes;
    }

    private static void uploadfile(SocketChannel socket, String filename) {
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
            } catch (IOException e) {
                System.err.print("Error reading file.\n");
            }
        }
    }

    private static void downloadFile(SocketChannel socket, String filename) {
        try {
            socket.write(ByteBuffer.wrap(("d" + filename).getBytes()));
            FileOutputStream fs = new FileOutputStream("ClientFiles/" + filename, true);
            FileChannel fc = fs.getChannel();
            ByteBuffer fileContent = ByteBuffer.allocate(1024);

            while (socket.read(fileContent) >= 0) {
                fileContent.flip();
                fc.write(fileContent);
                fileContent.clear();
            }
            fs.close();

        } catch (IOException e) {
            System.err.print("Error fetching file.\n");
        }
    }
}

