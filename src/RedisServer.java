import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class RedisServer{
    public static void main(String[] args) {
        System.out.println("starting server on port 6767");
        try {
            Selector selector = Selector.open();
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(6767));

            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isAcceptable()) {
                        handleAccept(serverSocket, selector);
                    } else if(key.isReadable()) {
                        handleRead(key);
                    }
                    iter.remove();
                }

            }
        } catch (IOException e) {
            System.out.println("dont know"); //fix
        }

    }

    private static void handleAccept(ServerSocketChannel serverSocket, Selector selector) {
        try {
            SocketChannel channel = serverSocket.accept();
            System.out.println("Accepted connection from " + channel.getRemoteAddress());
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            System.out.println("Registered client for reading.");
        } catch (IOException e) {
            System.out.println("oopsies");//fix
        }

    }

    private static void handleRead(SelectionKey key){
        SocketChannel clientchannel = (SocketChannel)  key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesread=-1;
        try{
            bytesread= clientchannel.read(buffer);
        } catch (IOException e) {
            disconnect(key,clientchannel);
            System.out.println("client disconnected abruptly"+ e.getMessage());
            return;
        }
        if (bytesread==-1){
            System.out.println("Connection closed");
            disconnect(key,clientchannel);
            return;
        }
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        String message = new String(data).trim();
        System.out.println("Received: " + message);
        String response = "+OK\r\n";

        ByteBuffer responseBuff= ByteBuffer.wrap(response.getBytes());
        try {
            clientchannel.write(responseBuff);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void disconnect(SelectionKey key, SocketChannel clientchannel){
        try {
            key.cancel();
            clientchannel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}

