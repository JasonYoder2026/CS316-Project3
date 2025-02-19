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
            // - u"filename"filebytes
            // - mv"filename""newname"

            String byteString = new String(a);
            String[] byteStringArray = {byteString};
            //processCommand() receives a string array of [u"filename"filebytes]
//            ByteBuffer pieces = processCommand(byteStringArray);

            //TODO:
            // - do something with the bytebuffer after the methods have been performed on it? (maybe)
        }
    }

    //may or may not need to return a bytebuffer with this command
    // - it would be sent back to the main command for something
    // - maybe a status message to client
    private static void processCommand(String[] commandParts) {
        switch (commandParts[0].toLowerCase()) {
            case ("d"):
                downloadFile(commandParts[1], commandParts[2]); //pass filename and/or filebytes
                break;
            case ("l"):
                listFiles();
                break;
            case ("r"):
                deleteFile();
                break;
            case ("m"):
                renameFile();
                break;
            case ("u"):
                uploadFile();
                break;
            case ("exit"):
                System.exit(0);
                break;
            default:
                System.out.println("Invalid command");
        }
    }

    //download = accept file from server side into the client side
    public static void downloadFile(String filename, String fileBytes) {
        File file = new File("ServerFiles/" + fileBytes); //access files in
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

    //ls = return array of all filenames in the db
    public static void listFiles() {
        File file = new File("ServerFiles/");
        File[] files = file.listFiles();
        if(files == null) {
            System.out.println("No files found.");
        } else {
            for (File f : files) {
                System.out.println(f.getName());
            }
        }
    }

    //delete = delete file that user specifies in command
    public static void deleteFile(String filename) {
        File file = new File("ServerFiles/" + filename);
        if(file.exists()) {
            System.out.println("File not found.");
        } else {
            file.delete();
        }
    }

    //rename = change file name
    public static void renameFile(String filename, String newFilename) {
        File file = new File("ServerFiles/" + filename);
        if(!file.exists()) {
            System.out.println("File not found.");
        } else {
            file.renameTo(new File("ServerFiles/" + newFilename));
        }
    }

    //upload = upload a file into the server db
    public static void uploadFile(String filename, SocketChannel socket) {
        try {
            FileOutputStream fs = new FileOutputStream("ServerFiles/" + filename, true);
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

}