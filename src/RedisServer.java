import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RedisServer{
    public static void main(String[] args) {
        System.out.println("starting server on port 6767");
        Selector selector=null;
        ServerSocketChannel serverSocket=null;
        try {
            selector = Selector.open();
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(6767));

            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        }catch (IOException e) {
            System.out.println("cant start port might be busy");
        }
        try{
            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if(!key.isValid()) continue;
                    if (key.isAcceptable()) {
                        handleAccept(serverSocket, selector);
                    } else if(key.isReadable()) {
                        handleRead(key);
                    }

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
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
            System.out.println("oopsies");
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
        StringBuilder sb= (StringBuilder) key.attachment();
        buffer.flip();
        while(buffer.hasRemaining()){
            sb.append((char)buffer.get());
        }
        String content = sb.toString();

        if(content.contains("\n")){
            List<String> args= RespParser.decode(content);
            sb.setLength(0);
            if(args.isEmpty()) return;
            String command = args.get(0).toUpperCase();
            System.out.println("Command"+ command);
            System.out.println("args: "+args);
        }
        String response = "+OK\r\n";

        ByteBuffer responseBuff= ByteBuffer.wrap(response.getBytes());

        try {
            clientchannel.write(responseBuff);
        } catch (IOException e) {
            disconnect(key,clientchannel);
            System.out.println("failed to write ,disconnected");
        }
    }

    private static void disconnect(SelectionKey key, SocketChannel clientchannel){
        try {
            key.cancel();
            clientchannel.close();
        } catch (IOException e) {
            System.out.println("couldnt close properly");
        }


    }
}

