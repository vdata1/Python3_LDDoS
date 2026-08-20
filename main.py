import http.server
import socket

class HyperVulnerableHandler(http.server.SimpleHTTPRequestHandler):
    def handle(self):
        """
        By default, Python sets no timeout on accepted connections.
        We explicitly force an infinite blocking state on the socket.
        """
        self.connection.settimeout(None) # Infinite timeout
        try:
            # Let the native handler attempt to read the slow incoming request
            super().handle()
        except Exception as e:
            pass

class VulnerableHTTPServer(http.server.HTTPServer):
    def server_bind(self):
        """
        Override the socket binding to artificially minimize the backlog.
        A backlog of 1 means the OS queue will drop other legitimate connections instantly.
        """
        super().server_bind()
        # Restrict the OS listening backlog queue to just 1 request
        self.socket.listen(1)

if __name__ == '__main__':
    # Bind to localhost on port 8080
    server_address = ('127.0.0.1', 8080)
    
    # Intentionally do NOT use socketserver.ThreadingMixIn or http.server.ThreadingHTTPServer
    vulnerable_server = VulnerableHTTPServer(server_address, HyperVulnerableHandler)
    
    print("VULNERABLE LAB SERVER: Listening on http://127.0.0.1:8080")
    print("WARNING: This server is single-threaded and will freeze if an exploit holds a connection open.")
    
    try:
        vulnerable_server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server.")
        vulnerable_server.server_close()

