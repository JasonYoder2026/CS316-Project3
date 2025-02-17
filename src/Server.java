import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server {
    public static void main(String[] args) throws Exception {
        //open server socket
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        //binds serve socket to a port and listens for client activity on that port
        listenChannel.bind(new InetSocketAddress(3001));

        while (true) {
            //accept a client connection
            SocketChannel serveChannel = listenChannel.accept();
            //byte buffer for storing incoming data
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            //reads client data into the buffer
            int bytesRead = serveChannel.read(buffer);
            //flips buffer from write mode into read mode
            buffer.flip();
            //creates byte array of size bytesRead
            byte[] a = new byte[bytesRead];
            //transfer data from byte array into the buffer
            buffer.get(a);

            //TODO:
            // - I am receiving a ByteBuffer of a big string
            // containing command + user data all 'separated' by quotes
            // - Use regex to separate for realsies
            String commandParts = new String(a);
            ByteBuffer commandBytes = processCommand(commandParts);

        }
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


    //ls = return array of all filenames in the db
    public static void listFiles(String[] args) {

    }

    //delete = delete file that user specifies in command
    public static void deleteFile(String[] args) {

    }

    //upload = upload a file into the server db
    public static void uploadFile(String[] args) {

    }

    //download = accept file from server side into the client side
    public static void downloadFile(String[] args) {
        if (!file.exists()) {
            System.out.println("File doesn't exist");
        } else {
            FileInputStream fs = new FileInputStream(file);
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
    }

    //rename = change file name
    public static void remaneFile(String[] args) {

    }

}