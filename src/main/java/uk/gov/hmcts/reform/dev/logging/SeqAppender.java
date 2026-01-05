package uk.gov.hmcts.reform.dev.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Logback appender that sends logs to Seq via HTTP in CLEF format.
 */
public class SeqAppender extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_INSTANT;
    private static final int QUEUE_SIZE = 1000;
    private static final int BATCH_SIZE = 50;
    private static final long FLUSH_INTERVAL_MS = 1000;

    private String serverUrl = "http://localhost:5341";
    private String apiKey;
    private BlockingQueue<String> queue;
    private Thread senderThread;
    private volatile boolean running;

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void start() {
        queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        running = true;
        senderThread = new Thread(this::sendLoop, "seq-sender");
        senderThread.setDaemon(true);
        senderThread.start();
        super.start();
        System.out.println("[SeqAppender] Started - sending to " + serverUrl);
    }

    @Override
    public void stop() {
        running = false;
        if (senderThread != null) {
            senderThread.interrupt();
        }
        // Flush remaining logs
        flushQueue();
        super.stop();
        System.out.println("[SeqAppender] Stopped");
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }
        String clef = toClef(event);
        if (!queue.offer(clef)) {
            System.err.println("[SeqAppender] Queue full, dropping log event");
        }
    }

    private void sendLoop() {
        StringBuilder batch = new StringBuilder();
        int count = 0;
        long lastFlush = System.currentTimeMillis();

        while (running || !queue.isEmpty()) {
            try {
                String event = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (event != null) {
                    if (batch.length() > 0) {
                        batch.append("\n");
                    }
                    batch.append(event);
                    count++;
                }

                long now = System.currentTimeMillis();
                boolean shouldFlush = count >= BATCH_SIZE
                    || (count > 0 && now - lastFlush >= FLUSH_INTERVAL_MS)
                    || (!running && count > 0);

                if (shouldFlush) {
                    sendToSeq(batch.toString());
                    batch.setLength(0);
                    count = 0;
                    lastFlush = now;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void flushQueue() {
        StringBuilder batch = new StringBuilder();
        String event;
        while ((event = queue.poll()) != null) {
            if (batch.length() > 0) {
                batch.append("\n");
            }
            batch.append(event);
        }
        if (batch.length() > 0) {
            sendToSeq(batch.toString());
        }
    }

    private void sendToSeq(String payload) {
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(serverUrl + "/api/events/raw?clef");
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/vnd.serilog.clef");

            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("X-Seq-ApiKey", apiKey);
            }

            byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(data.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                System.err.println("[SeqAppender] Failed to send logs, HTTP " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("[SeqAppender] Error sending to Seq: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String toClef(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{");

        // Timestamp
        Instant ts = Instant.ofEpochMilli(event.getTimeStamp());
        sb.append("\"@t\":\"").append(ISO_FORMAT.format(ts.atOffset(ZoneOffset.UTC))).append("\"");

        // Message
        sb.append(",\"@m\":").append(jsonString(event.getFormattedMessage()));

        // Level
        sb.append(",\"@l\":\"").append(mapLevel(event.getLevel().toString())).append("\"");

        // Exception
        if (event.getThrowableProxy() != null) {
            sb.append(",\"@x\":").append(jsonString(formatException(event.getThrowableProxy())));
        }

        // Source context (logger name)
        sb.append(",\"SourceContext\":").append(jsonString(event.getLoggerName()));

        // Thread
        sb.append(",\"ThreadName\":").append(jsonString(event.getThreadName()));

        // Application
        sb.append(",\"Application\":\"test-backend\"");

        // MDC properties
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            for (Map.Entry<String, String> entry : mdc.entrySet()) {
                sb.append(",\"").append(entry.getKey()).append("\":").append(jsonString(entry.getValue()));
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private String mapLevel(String level) {
        return switch (level) {
            case "TRACE" -> "Verbose";
            case "DEBUG" -> "Debug";
            case "INFO" -> "Information";
            case "WARN" -> "Warning";
            case "ERROR" -> "Error";
            default -> "Information";
        };
    }

    private String formatException(IThrowableProxy tp) {
        StringBuilder sb = new StringBuilder();
        sb.append(tp.getClassName()).append(": ").append(tp.getMessage());
        for (StackTraceElementProxy step : tp.getStackTraceElementProxyArray()) {
            sb.append("\n\tat ").append(step.getSTEAsString());
        }
        if (tp.getCause() != null) {
            sb.append("\nCaused by: ").append(formatException(tp.getCause()));
        }
        return sb.toString();
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
