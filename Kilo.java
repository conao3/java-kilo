import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Kilo {
    static byte ctrlKey(char key) {
        return (byte)(key & 0x1f);
    }

    static String enableRawMode() throws IOException {
        var currentSettings = exec("/usr/bin/env", "stty", "-g");
        exec("/usr/bin/env", "stty", "-brkint", "-icrnl", "-inpck", "-istrip", "-ixon");
        exec("/usr/bin/env", "stty", "-opost");
        exec("/usr/bin/env", "stty", "cs8");
        exec("/usr/bin/env", "stty", "-echo", "-icanon", "-iexten", "-isig");
        exec("/usr/bin/env", "stty", "min", "0");
        exec("/usr/bin/env", "stty", "time", "1");
        return currentSettings;
    }

    static void disableRawMode(String currentSettings) throws IOException {
        if (currentSettings == null) {
            return;
        }
        exec("/usr/bin/env", "stty", currentSettings);
    }

    static String exec(String... cmd) throws IOException {
        var pb = new ProcessBuilder(cmd);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);

        var process = pb.start();
        var stdoutBuffer = new ByteArrayOutputStream();
        var stdout = process.getInputStream();
        var readByte = stdout.read();
        while (readByte != -1) {
            stdoutBuffer.write(readByte);
            readByte = stdout.read();
        }
        return stdoutBuffer.toString().trim();
    }

    public static void main(String[] args) {
        System.err.println("Kilo, Kilo, Kilo");

        var buf = new byte[1024];
        String currentSettings = null;
        try {
            currentSettings = enableRawMode();
            MAIN:while (true) {
                if (System.in.available() == 0) {
                    Thread.sleep(100);
                    continue;
                }
                var readByte = System.in.read(buf);
                if (readByte == -1) {
                    break;
                }
                for (int i = 0; i < readByte; i++) {
                    var b = buf[i];
                    var bStr = String.format("%d (%c)", b, b);
                    System.out.write(bStr.getBytes());
                    System.out.write(new byte[]{'\r', '\n'});
                    if (b == ctrlKey('q')) {
                        break MAIN;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                disableRawMode(currentSettings);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
