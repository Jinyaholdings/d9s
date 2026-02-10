import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
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

    private static String envOrDefault(String key, String def) {
        String val = System.getenv(key);
        return (val == null || val.isEmpty()) ? def : val;
    }

    private static void setupLoggingToFile() {
        String outputFile = envOrDefault("SFDC_OUTPUT_FILE", "salesforce_connectivity.log");
        try {
            PrintStream consoleOut = System.out;
            PrintStream consoleErr = System.err;
            PrintStream fileOut = new PrintStream(new FileOutputStream(outputFile, true), true);
            PrintStream teeOut = new PrintStream(new TeeOutputStream(consoleOut, fileOut), true);
            PrintStream teeErr = new PrintStream(new TeeOutputStream(consoleErr, fileOut), true);
            System.setOut(teeOut);
            System.setErr(teeErr);
            System.out.println("=== Log start: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS").format(new Date()) + " ===");
            String label = envOrDefault("SFDC_CASE_LABEL", "");
            if (!label.isEmpty()) {
                System.out.println("Case: " + label);
            }
            System.out.println("OutputFile: " + outputFile);
        } catch (Exception ex) {
            // Fall back to console only.
            System.err.println("Failed to open log file: " + ex.getMessage());
        }
    }

    private static String normalizeLoginUrl(String base, String apiVersion) {
        if (base == null || base.isEmpty()) {
            return base;
        }
        if (base.contains("/services/Soap/u/")) {
            return base;
        }
        String version = (apiVersion == null || apiVersion.isEmpty()) ? "60.0" : apiVersion;
        return base.replaceAll("/+$", "") + "/services/Soap/u/" + version;
    }

    private static void measureHttpsGet(String label, String url) {
        measure(label, () -> {
            HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
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
    }

    public static void main(String[] args) {
        setupLoggingToFile();

        String loginUrlEnv = envOrDefault("SFDC_LOGINURL", "https://login.salesforce.com");
        String apiVersion = envOrDefault("SFDC_API_VERSION", "60.0");
        String loginUrl = normalizeLoginUrl(loginUrlEnv, apiVersion);
        String clsServiceUrl = envOrDefault("SFDC_CLS_SERVICE_URL", "");
        String namespace = envOrDefault("SFDC_CLS_NAMESPACE", "ps");
        if (clsServiceUrl.isEmpty() && !loginUrl.isEmpty()) {
            try {
                URL login = new URL(loginUrl);
                String base = login.getProtocol() + "://" + login.getHost();
                clsServiceUrl = base + "/services/Soap/class";
            } catch (Exception ignored) {
            }
        }

        String host = args.length > 0 ? args[0] : "login.salesforce.com";
        if (!loginUrl.isEmpty()) {
            try {
                host = new URL(loginUrl).getHost();
            } catch (Exception ignored) {
            }
        }
        int port = 443;
        final String targetHost = host;
        final int targetPort = port;

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

        section("Endpoints");
        if (!loginUrl.isEmpty()) {
            System.out.println("Login URL: " + loginUrl);
        }
        if (!clsServiceUrl.isEmpty()) {
            System.out.println("Apex Service URL: " + clsServiceUrl);
        }
        if (!namespace.isEmpty()) {
            System.out.println("Namespace: " + namespace);
        }

        section("DNS");
        measure("InetAddress.getAllByName", () -> {
            InetAddress[] addresses = InetAddress.getAllByName(targetHost);
            for (InetAddress addr : addresses) {
                System.out.println("  " + targetHost + " -> " + addr.getHostAddress());
            }
        });

        section("TCP Connect");
        measure("Socket.connect", () -> {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(targetHost, targetPort), 10000);
            socket.close();
        });

        section("TLS Handshake");
        measure("SSLSocket.startHandshake", () -> {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(targetHost, targetPort)) {
                socket.setSoTimeout(10000);
                socket.startHandshake();
                System.out.println("Protocol: " + socket.getSession().getProtocol());
                System.out.println("Cipher: " + socket.getSession().getCipherSuite());
            }
        });

        section("HTTPS GET (no auth)");
        measureHttpsGet("HttpsURLConnection", "https://" + targetHost + "/");
        if (!loginUrl.isEmpty()) {
            measureHttpsGet("HTTPS GET login", loginUrl);
        }
        if (!clsServiceUrl.isEmpty() && !namespace.isEmpty()) {
            String apexUrl = clsServiceUrl.replaceAll("/+$", "") + "/" + namespace + "/CallInformationSync";
            measureHttpsGet("HTTPS GET apex", apexUrl);
        }

        section("Done");
        System.out.println("Completed at: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS").format(new Date()));
    }

    private static class TeeOutputStream extends OutputStream {
        private final OutputStream left;
        private final OutputStream right;

        TeeOutputStream(OutputStream left, OutputStream right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public void write(int b) throws java.io.IOException {
            left.write(b);
            right.write(b);
        }

        @Override
        public void write(byte[] b) throws java.io.IOException {
            left.write(b);
            right.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            left.write(b, off, len);
            right.write(b, off, len);
        }

        @Override
        public void flush() throws java.io.IOException {
            left.flush();
            right.flush();
        }
    }

    private interface RunnableEx {
        void run() throws Exception;
    }
}
