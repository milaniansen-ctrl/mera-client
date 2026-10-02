package net.mera;

import com.google.gson.JsonObject;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

/** Minimal Discord Rich Presence client (talks to the Discord app over IPC, no extra libraries). */
public class DiscordRPC {
    // >>> PUT YOUR DISCORD APPLICATION ID HERE (discord.com/developers/applications) <<<
    public static final String CLIENT_ID = "1499873215315710264";
    // Name of the image you upload under Rich Presence > Art Assets
    public static final String LARGE_IMAGE = "logo";

    private static InputStream in;
    private static OutputStream out;
    private static boolean connected;
    private static String lastDetails = "";
    private static final long START = System.currentTimeMillis() / 1000L;

    public static synchronized void connect() {
        if (connected || CLIENT_ID.startsWith("YOUR_")) return;
        for (int i = 0; i < 10 && !connected; i++) {
            try {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                    in = new FileInputStream(pipe.getFD());
                    out = new FileOutputStream(pipe.getFD());
                } else {
                    String dir = firstNonNull(System.getenv("XDG_RUNTIME_DIR"), System.getenv("TMPDIR"),
                            System.getenv("TMP"), System.getenv("TEMP"), "/tmp");
                    SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                    ch.connect(UnixDomainSocketAddress.of(Path.of(dir, "discord-ipc-" + i)));
                    in = Channels.newInputStream(ch);
                    out = Channels.newOutputStream(ch);
                }
                JsonObject hs = new JsonObject();
                hs.addProperty("v", 1);
                hs.addProperty("client_id", CLIENT_ID);
                send(0, hs.toString());
                read();
                connected = true;
            } catch (Exception ignored) { }
        }
    }

    /** Set the "Idling" / "In game" line. Safe to call every tick; only sends on change. */
    public static synchronized void setDetails(String details) {
        if (!connected || details.equals(lastDetails)) return;
        lastDetails = details;
        try {
            JsonObject ts = new JsonObject();
            ts.addProperty("start", START);

            JsonObject assets = new JsonObject();
            assets.addProperty("large_image", LARGE_IMAGE);
            assets.addProperty("large_text", "MΞRA Client");

            JsonObject activity = new JsonObject();
            activity.addProperty("details", details);
            activity.add("timestamps", ts);
            activity.add("assets", assets);

            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", activity);

            JsonObject cmd = new JsonObject();
            cmd.addProperty("cmd", "SET_ACTIVITY");
            cmd.add("args", args);
            cmd.addProperty("nonce", UUID.randomUUID().toString());

            send(1, cmd.toString());
            read();
        } catch (Exception e) {
            connected = false;
        }
    }

    private static void send(int op, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op).putInt(data.length).put(data);
        out.write(buf.array());
        out.flush();
    }

    private static void read() throws IOException {
        byte[] header = in.readNBytes(8);
        if (header.length < 8) throw new EOFException();
        int len = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        in.readNBytes(len);
    }

    private static String firstNonNull(String... s) {
        for (String x : s) if (x != null) return x;
        return "/tmp";
    }
}
