import org.apache.http.ExceptionLogger;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.config.SocketConfig;
import org.apache.http.protocol.HttpContext;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VulnerableJavaServer {

    public static void main(String[] args) throws Exception {
        
        // 1. Force infinite socket read timeout and minimize the OS connection queue
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(0)      // 0 = Infinite timeout. Server waits forever for partial data.
                .setBacklogSize(1)    // Drops secondary connection requests instantly if the server is busy.
                .build();

        HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(8080)
                .setLocalAddress(java.net.InetAddress.getByName("0.0.0.0")) // LAN binding
                .setSocketConfig(socketConfig)
                
                // 2. CRITICAL FOR LDDoS: Force a single-threaded execution pool.
                // Without this, Apache will scale threads to handle simultaneous slow requests.
                .setExecutorService(Executors.newSingleThreadExecutor())
                
                // 3. INTERCEPTOR LOGGING: Logs exactly when the server reads HTTP headers
                .addInterceptorFirst(new HttpRequestInterceptor() {
                    @Override
                    public void process(HttpRequest request, HttpContext context) {
                        System.out.println("\n[LOG] ---> Incoming Request Headers Received!");
                        System.out.println("[LOG] Request Line: " + request.getRequestLine());
                        // Print headers to show what the client sent
                        for (org.apache.http.Header header : request.getAllHeaders()) {
                            System.out.println("[LOG] Header -> " + header.getName() + ": " + header.getValue());
                        }
                    }
                })
                
                // 4. EXCEPTION LOGGING: Captures dropped sockets or interrupted streams
                .setExceptionLogger(new ExceptionLogger() {
                    @Override
                    public void log(Exception ex) {
                        System.err.println("\n[SERVER EXCEPTION] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    }
                })
                .create();

        System.out.println("=========================================================");
        System.out.println("VULNERABLE JAVA LAB SERVER RUNNING");
        System.out.println("Address: http://0.0.0");
        System.out.println("Flaws: Single-Threaded, Infinite Timeout, Backlog = 1");
        System.out.println("=========================================================");
        
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down Java server...");
            server.shutdown(5, TimeUnit.SECONDS);
        }));
    }
}

