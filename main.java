import org.apache.http.ExceptionLogger;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.config.SocketConfig;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class VulnerableJavaServer {

    public static void main(String[] args) throws Exception {
        
        // 1. Force the underlying network socket to wait indefinitely for incoming packets
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(0) // 0 means INFINITE timeout (Disabled reading deadline)
                .setBacklogSize(1) // Drop any queueing connections immediately if the thread is busy
                .build();

        // 2. Configure the Apache bootstrap server
        HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(8080)
                .setLocalAddress(java.net.InetAddress.getByName("0.0.0.0")) // Bind to local network
                .setSocketConfig(socketConfig)
                
                // 3. Intentionally starve execution resources
                // Setting the thread pool executor to use exactly 1 worker thread 
                // ensures a single slow request blocks all subsequent LAN traffic.
                .setExceptionLogger(new ExceptionLogger() {
                    @Override
                    public void log(Exception ex) {
                        if (ex instanceof java.net.SocketTimeoutException) {
                            System.out.println("Connection stalled by low-rate behavior.");
                        }
                    }
                })
                .create();

        System.out.println("VULNERABLE JAVA LAB SERVER: Active on http://0.0.0");
        System.out.println("Warning: Configured with 1 worker thread and infinite timeouts.");
        
        server.start();

        // Keep the main thread alive to let the background single thread serve requests
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Java server...");
            server.shutdown(5, TimeUnit.SECONDS);
        }));
    }
}

