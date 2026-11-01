import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * GameLogger - writes the standard game log (Appendix C): one event per line,
 * fields separated by | with no spaces. Lines go to the console (optional), to
 * a log file (optional) and to an in-memory list used by the tests.
 * Only Appendix C lines are written here; prompts and UI text never are.
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class GameLogger {
    private final PrintWriter file;
    private final boolean echo;
    private final ArrayList<String> lines = new ArrayList<>();

    /** path may be null (no file). echo prints each line to the console. */
    public GameLogger(String path, boolean echo) throws IOException {
        this.file = (path == null) ? null : new PrintWriter(new FileWriter(path));
        this.echo = echo;
    }

    public void log(String... fields) {
        String line = String.join("|", fields);
        lines.add(line);
        if (echo) {
            System.out.println(line);
        }
        if (file != null) {
            file.println(line);
            file.flush();
        }
    }

    public ArrayList<String> getLines() {
        return lines;
    }

    public void close() {
        if (file != null) {
            file.close();
        }
    }
}
