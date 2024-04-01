import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


class KiloArrayList extends ArrayList<Byte> {
    public void extend(byte[] bytes) {
        for (var b : bytes) {
            this.add(b);
        }
    }

    public byte[] toPrimitive() {
        var bytes = new byte[this.size()];
        for (int i = 0; i < this.size(); i++) {
            bytes[i] = this.get(i);
        }
        return bytes;
    }
}

public class Kilo {
    static final String KILO_VERSION = "0.0.1";
    static int screenrows;
    static int screencols;
    static int cx = 0;
    static int cy = 0;


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

    static int read(byte[] buf) throws IOException {
        if (System.in.available() == 0) {
            return 0;
        }
        var readByte = System.in.read(buf);
        if (readByte == -1) {
            return -1;
        }
        return 1;
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
        while (true) {
            var ret = read(buf);
            if (ret == -1) {
                return -1;
            }
            if (ret == 1) {
                break;
            }
        }
        if (buf[0] == 0x1b) {
            var buf0 = new byte[1];
            var buf1 = new byte[1];
            // var buf2 = new byte[1];
            if (read(buf0) != 1) {
                return buf[0];
            }
            if (read(buf1) != 1) {
                return buf[0];
            }
            if (buf0[0] == '[') {
                switch (buf1[0]) {
                    case 'A':
                        return 'w';
                    case 'B':
                        return 's';
                    case 'C':
                        return 'd';
                    case 'D':
                        return 'a';
                }
            }
            return buf[0];
        } else {
            return buf[0];
        }
    }

    static int[] getWindowSize() throws IOException {
        var output = exec("/usr/bin/env", "stty", "size");
        return Arrays.stream(output.split(" ")).mapToInt(Integer::parseInt).toArray();
    }


    /*** output ***/

    static void editorClearScreenDirect() throws IOException {
        System.out.write(new byte[]{0x1b, '[', '2', 'J'});
        System.out.write(new byte[]{0x1b, '[', 'H'});
    }

    static void editorDrawRows(KiloArrayList ab) throws IOException {
        for (int y = 0; y < screenrows; y++) {
            if (y == screenrows / 3) {
                var welcome = String.format("Kilo editor -- version %s", KILO_VERSION);
                int padding = (screencols - welcome.length()) / 2;
                if (padding > 0) {
                    ab.extend("~".getBytes());
                    padding--;
                }
                ab.extend(" ".repeat(padding).getBytes());
                ab.extend(welcome.getBytes());
            } else {
                ab.extend("~".getBytes());
            }

            ab.extend("\u001b[K".getBytes());
            if (y >= screenrows - 1) {
                continue;
            }
            ab.extend("\r\n".getBytes());
        }
    }

    static void editorRefreshScreen() throws IOException {
        var ab = new KiloArrayList();

        ab.extend("\u001b[?25l".getBytes());
        ab.extend("\u001b[H".getBytes());
        editorDrawRows(ab);
        ab.extend(String.format("\u001b[%d;%dH", cy + 1, cx + 1).getBytes());
        ab.extend("\u001b[?25h".getBytes());
        System.out.write(ab.toPrimitive());
    }


    /*** input ***/

    static void editorMoveCursor(byte key) {
        switch (key) {
            case 'w':
                cy--;
                break;
            case 's':
                cy++;
                break;
            case 'a':
                cx--;
                break;
            case 'd':
                cx++;
                break;
        }
    }

    static int editorProcessKeyPress() throws IOException, InterruptedException {
        var b = editorReadKey();
        if (b == 0 || b == -1) {
            return b;
        }
        var bStr = String.format("%d (%c)", b, b);
        System.out.write(bStr.getBytes());
        System.out.write(new byte[]{'\r', '\n'});
        if (b == ctrlKey('q')) {
            editorClearScreenDirect();
            return -1;
        }
        if (b == 'w' || b == 's' || b == 'a' || b == 'd') {
            editorMoveCursor(b);
        }
        return 0;
    }


    /*** init ***/

    static void initEditor() throws IOException {
        var size = getWindowSize();
        screenrows = size[0];
        screencols = size[1];
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
                editorClearScreenDirect();
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
