package desktop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

/** Local desktop child entry point. Each UTF-8 input line receives one flushed JSON reply. */
public final class DesktopMain {
    private DesktopMain() { }

    public static void main(String[] args) throws IOException {
        Path directory = Path.of(System.getProperty("cs380.profileDirectory",
                Path.of(System.getProperty("user.home"), ".cs380-holdem").toString()));
        DesktopBridge bridge = new DesktopBridge(directory);
        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
                BufferedWriter output = new BufferedWriter(
                        new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {
            serve(bridge, input, output);
        }
    }

    static void serve(DesktopBridge bridge, BufferedReader input, BufferedWriter output)
            throws IOException {
        Optional<String> line = Optional.ofNullable(input.readLine());
        while (line.isPresent()) {
            output.write(bridge.respond(line.orElseThrow()));
            output.newLine();
            output.flush();
            line = Optional.ofNullable(input.readLine());
        }
    }
}
