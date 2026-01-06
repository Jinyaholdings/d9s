import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SalesforceConnectivity {
    private static void section(String title) {
        System.out.println();
        System.out.println("==== " + title + " ====");
    }

    private static long measure(String label, RunnableEx action) {
        long start = System.currentTimeMillis();
        try {
            action.run();
            long elapsed = System.currentTimeMillis() - start;
            System.out.println(label + ": " + elapsed + " ms");
            return elapsed;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println(label + ": " + elapsed + " ms (error)");
            System.out.println("Error: " + ex.getClass().getName() + ": " + ex.getMessage());
            return elapsed;
        }
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "login.salesforce.com";
        int port = 443;

        section("System");
        System.out.println("Time: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS").format(new Date()));
        System.out.println("Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")");
        System.out.println("User: " + System.getProperty("user.name"));

        section("Proxy (System properties + ProxySelector)");
        System.out.println("http.proxyHost: " + System.getProperty("http.proxyHost", ""));
        System.out.println("http.proxyPort: " + System.getProperty("http.proxyPort", ""));
        System.out.println("https.proxyHost: " + System.getProperty("https.proxyHost", ""));
        System.out.println("https.proxyPort: " + System.getProperty("https.proxyPort", ""));
        System.out.println("java.net.useSystemProxies: " + System.getProperty("java.net.useSystemProxies", ""));
        try {
            List<Proxy> proxies = ProxySelector.getDefault().select(new URI("https://" + host + "/"));
            for (Proxy proxy : proxies) {
                System.out.println("ProxySelector: " + proxy);
            }
        } catch (Exception ex) {
            System.out.println("ProxySelector error: " + ex.getMessage());
        }

        section("DNS");
        measure("InetAddress.getAllByName", () -> {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                System.out.println("  " + host + " -> " + addr.getHostAddress());
            }
        });

        section("TCP Connect");
        measure("Socket.connect", () -> {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 10000);
            socket.close();
        });

        section("TLS Handshake");
        measure("SSLSocket.startHandshake", () -> {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                socket.setSoTimeout(10000);
                socket.startHandshake();
                System.out.println("Protocol: " + socket.getSession().getProtocol());
                System.out.println("Cipher: " + socket.getSession().getCipherSuite());
            }
        });

        section("HTTPS GET (no auth)");
        measure("HttpsURLConnection", () -> {
            URI uri = new URI("https://" + host + "/");
            HttpsURLConnection conn = (HttpsURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            System.out.println("StatusCode: " + code);
            try (InputStream in = conn.getInputStream()) {
                byte[] buf = new byte[1];
                in.read(buf);
            }
            conn.disconnect();
        });

        section("Done");
        System.out.println("Completed at: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS").format(new Date()));
    }

    private interface RunnableEx {
        void run() throws Exception;
    }
}
