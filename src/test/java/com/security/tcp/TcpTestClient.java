package com.security.tcp;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP測試客戶端
 */
public class TcpTestClient {
    
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8888;
        
        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        
        System.out.println("TCP Test Client");
        System.out.println("Commands:");
        System.out.println("  1. normal <message>  - Send normal message");
        System.out.println("  2. flood <count>     - Flood connections");
        System.out.println("  3. bigpacket <size>  - Send big packet");
        System.out.println("  4. attack <type>     - Send attack pattern");
        System.out.println("  5. exit              - Exit");
        System.out.println();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            String[] parts = input.split(" ", 2);
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "normal":
                        sendNormalMessage(host, port, parts.length > 1 ? parts[1] : "Hello Server");
                        break;
                        
                    case "flood":
                        int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                        floodConnections(host, port, count);
                        break;
                        
                    case "bigpacket":
                        int size = parts.length > 1 ? Integer.parseInt(parts[1]) : 100000;
                        sendBigPacket(host, port, size);
                        break;
                        
                    case "attack":
                        String type = parts.length > 1 ? parts[1] : "shellcode";
                        sendAttackPattern(host, port, type);
                        break;
                        
                    case "exit":
                        System.out.println("Goodbye!");
                        return;
                        
                    default:
                        System.out.println("Unknown command: " + command);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
    
    private static void sendNormalMessage(String host, int port, String message) throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            System.out.println("Sending: " + message);
            out.println(message);
            
            String response = in.readLine();
            System.out.println("Received: " + response);
        }
    }
    
    private static void floodConnections(String host, int port, int count) {
        System.out.println("Flooding with " + count + " connections...");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        for (int i = 0; i < count; i++) {
            final int id = i;
            executor.submit(() -> {
                try (Socket socket = new Socket(host, port)) {
                    System.out.println("Connection " + id + " established");
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.err.println("Connection " + id + " failed: " + e.getMessage());
                }
            });
        }
        
        executor.shutdown();
    }
    
    private static void sendBigPacket(String host, int port, int size) throws IOException {
        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream()) {
            
            byte[] data = new byte[size];
            for (int i = 0; i < size; i++) {
                data[i] = (byte)(i % 256);
            }
            
            System.out.println("Sending big packet: " + size + " bytes");
            out.write(data);
            out.flush();
            
            Thread.sleep(100);
        } catch (Exception e) {
            System.err.println("Failed to send big packet: " + e.getMessage());
        }
    }
    
    private static void sendAttackPattern(String host, int port, String type) throws IOException {
        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream()) {
            
            byte[] payload = null;
            
            switch (type.toLowerCase()) {
                case "shellcode":
                    // NOP sled pattern
                    payload = new byte[100];
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte)0x90;
                    }
                    System.out.println("Sending shellcode pattern (NOP sled)");
                    break;
                    
                case "overflow":
                    // Buffer overflow pattern (repeated 'A')
                    payload = new byte[1000];
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte)'A';
                    }
                    System.out.println("Sending buffer overflow pattern");
                    break;
                    
                case "sql":
                    // SQL injection in text
                    String sql = "SELECT * FROM users WHERE id='1' OR '1'='1'";
                    payload = sql.getBytes();
                    System.out.println("Sending SQL injection pattern");
                    break;
                    
                case "format":
                    // Format string attack
                    String fmt = "%x%x%x%x%n%n%n%n";
                    payload = fmt.getBytes();
                    System.out.println("Sending format string attack");
                    break;
                    
                default:
                    System.out.println("Unknown attack type: " + type);
                    return;
            }
            
            if (payload != null) {
                out.write(payload);
                out.flush();
                Thread.sleep(100);
                System.out.println("Attack pattern sent");
            }
            
        } catch (Exception e) {
            System.err.println("Failed to send attack pattern: " + e.getMessage());
        }
    }
}