import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class RedisServer{
    private static final Map<String, String> cachehe = new HashMap<>();
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
            channel.register(selector, SelectionKey.OP_READ,new StringBuilder());
            System.out.println("Registered client for reading.");
        } catch (IOException e) {
            System.out.println("oopsies");
        }

    }

    private static void handleRead(SelectionKey key) {
        SocketChannel clientchannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesread = - 1;
        try {
            bytesread = clientchannel.read(buffer);
        } catch (IOException e) {
            disconnect(key, clientchannel);
            System.out.println("client disconnected abruptly" + e.getMessage());
            return;
        }
        if (bytesread == - 1) {
            System.out.println("Connection closed");
            disconnect(key, clientchannel);
            return;
        }
        StringBuilder sb = (StringBuilder) key.attachment();
        buffer.flip();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        String content = sb.toString();

        if (content.contains("\n")) {
            List<String> args = RespParser.decode(content);
            sb.setLength(0);
            if (args.isEmpty()) return;
            String response = executeCommand(args);
            try {
                clientchannel.write(ByteBuffer.wrap(response.getBytes()));
            } catch (IOException e) {
                disconnect(key, clientchannel);
            }
        }
    }

    private static String executeCommand(List<String> args) {
        String cmd = args.get(0).toUpperCase();

        switch (cmd) {
            case "PING":
                return "+PONG\r\n";

            case "SET":
                if (args.size() < 3) {
                    return "-ERR wrong number of arguments for 'set' command\r\n";
                }
                String key = args.get(1);
                String value = args.get(2);
                cachehe.put(key, value);
                return "+OK\r\n";

            case "GET":
                if (args.size() < 2) {
                    return "-ERR wrong number of arguments for 'get' command\r\n";
                }
                String getKey = args.get(1);
                String val = cachehe.get(getKey);
                if (val == null) {
                    return "$-1\r\n";
                } else {
                    return "$" + val.length() + "\r\n" + val + "\r\n";
                }
            default:
                return "-ERR unknown command '" + cmd + "'\r\n";
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

