import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

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
        System.out.println("Connected to server...");
        System.out.println("Commands:");
        System.out.println("wget:<filename> - Download file from server");
        System.out.println("ls - List files in server");
        System.out.println("delete:<filename> - Delete file from server");
        System.out.println("mv:<filename>:<newfilename> - Rename file in server");
        System.out.println("upload:<filename> - Upload file to server");
        System.out.println("exit - Exit server");
        while(true) {
            System.out.println(">>");
            Scanner input = new Scanner(System.in);
            String command = input.nextLine();
            //bytes =
            processCommand(command);
            //socketChannel.write(ByteBuffer.wrap(command.getBytes()));
        }
    }

    private static void processCommand(String command) {
        String[] commandParts = command.split(":");
        switch (commandParts[0].toLowerCase()) {
            case ("wget"):
                ClientCommands.downloadFile(commandParts[1]);
                break;
            case ("ls"):
                listFiles();
                break;
            case ("delete"):
                deleteFile(commandParts[1]);
                break;
            case ("mv"):
                renameFile(commandParts[1], commandParts[2]);
                break;
            case ("upload"):
                uploadFile(commandParts[1]);
                break;
            case ("exit"):
                System.exit(0);
                break;
            default:
                System.out.println("Invalid command");
        }
    }
}

