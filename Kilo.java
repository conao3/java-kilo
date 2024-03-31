import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Kilo {
    static int screenrows;
    static int screencols;


    /*** utils ***/

    static byte ctrlKey(char key) {
        return (byte)(key & 0x1f);
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


    /*** terminal ***/

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

    static byte editorReadKey() throws IOException, InterruptedException {
        var buf = new byte[1];
        if (System.in.available() == 0) {
            Thread.sleep(100);
            return 0;
        }
        var readByte = System.in.read(buf);
        if (readByte == -1) {
            return -1;
        }
        return buf[0];
    }


    /*** output ***/

    static void editorClearScreen() throws IOException {
        System.out.write(new byte[]{0x1b, '[', '2', 'J'});
        System.out.write(new byte[]{0x1b, '[', 'H'});
    }

    static void editorDrawRows() throws IOException {
        for (int y = 0; y < screenrows; y++) {
            System.out.write("~".getBytes());
            System.out.write(new byte[]{'\r', '\n'});
        }
    }

    static void editorRefreshScreen() throws IOException {
        editorClearScreen();
        editorDrawRows();
        System.out.write(new byte[]{0x1b, '[', 'H'});
    }


    /*** input ***/

    static int editorProcessKeyPress() throws IOException, InterruptedException {
        var b = editorReadKey();
        if (b == 0 || b == -1) {
            return b;
        }
        var bStr = String.format("%d (%c)", b, b);
        System.out.write(bStr.getBytes());
        System.out.write(new byte[]{'\r', '\n'});
        if (b == ctrlKey('q')) {
            editorClearScreen();
            return -1;
        }
        return 0;
    }


    /*** init ***/

    static void initEditor() {
        screenrows = 24;
        screencols = 80;
    }

    public static void main(String[] args) {
        System.err.println("Kilo, Kilo, Kilo");

        String currentSettings = null;
        try {
            currentSettings = enableRawMode();
            initEditor();
            while (true) {
                editorRefreshScreen();
                if (editorProcessKeyPress() == -1) {
                    break;
                }
            }
        } catch (Exception e) {
            try {
                editorClearScreen();
            } catch (IOException _e) {}
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
