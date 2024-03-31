import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Kilo {
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

        var stdin = new BufferedReader(new InputStreamReader(System.in));
        String line;
        String currentSettings = null;
        try {
            currentSettings = exec("/usr/bin/env", "stty", "-g");
            exec("/usr/bin/env", "stty", "-icanon", "-echo");
            System.err.println(currentSettings);
            while ((line = stdin.readLine()) != null) {
                System.err.println(line);
                if (line.equals("q")) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                if (currentSettings != null) {
                    System.err.println(currentSettings);
                    exec("/usr/bin/env", "stty", currentSettings);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
