import org.apache.http.ExceptionLogger;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.config.SocketConfig;
import org.apache.http.protocol.HttpContext;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class VulnerableJavaServer {

    public static void main(String[] args) throws Exception {
        
        // 1. Configure the transport layer behavior
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(0)      // 0 = Infinite timeout. Server sleeps indefinitely waiting for packet fragments.
                .setBacklogSize(1)    // Drops secondary network handshakes at OS level if the socket is busy.
                .build();

        // 2. Build the server instance cleanly
        HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(8080)
                .setLocalAddress(java.net.InetAddress.getByName("0.0.0.0")) // Bind to LAN
                .setSocketConfig(socketConfig)
                
                // 3. INTERCEPTOR LOGGING: Intercepts and streams header bytes to stdout in real-time
                .addInterceptorFirst(new HttpRequestInterceptor() {
                    @Override
                    public void process(HttpRequest request, HttpContext context) {
                        System.out.println("\n[LOG] ---> Incoming Request Headers Received!");
                        System.out.println("[LOG] Request Line: " + request.getRequestLine());
                        
                        // Enumerate and print incoming headers to reveal slow stream configurations
                        for (org.apache.http.Header header : request.getAllHeaders()) {
                            System.out.println("[LOG] Header -> " + header.getName() + ": " + header.getValue());
                        }
                    }
                })
                
                // CRITICAL FIX: Use the framework's built-in standard error logger 
                // to eliminate version-specific signature errors.
                .setExceptionLogger(ExceptionLogger.STD_ERR)
                .create();

        System.out.println("=========================================================");
        System.out.println("VULNERABLE APACHE HTTPCORE SERVER ONLINE");
        System.out.println("Address: http://0.0.0");
        System.out.println("Status: Configured for low-rate testing environment");
        System.out.println("=========================================================");
        
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nGracefully terminating the server process...");
            server.shutdown(5, TimeUnit.SECONDS);
        }));
    }
}
