import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
            String filename = new String(a);
        }
    }
}
