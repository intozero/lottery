package sumapplication;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Bordered plain-text tables sized to their contents, suitable for files and consoles. */
final class TextTable {
    private final String[] headers;
    private final boolean[] numeric;
    private final List<String[]> rows = new ArrayList<>();

    TextTable(String[] headers, boolean... numeric) {
        if (headers.length != numeric.length) throw new IllegalArgumentException("Column alignment count mismatch");
        this.headers = headers.clone();
        this.numeric = numeric.clone();
    }

    TextTable row(Object... values) {
        if (values.length != headers.length) throw new IllegalArgumentException("Column count mismatch");
        rows.add(Arrays.stream(values).map(String::valueOf).toArray(String[]::new));
        return this;
    }

    void write(Writer out) throws IOException {
        int[] widths = Arrays.stream(headers).mapToInt(String::length).toArray();
        for (String[] row : rows)
            for (int i = 0; i < widths.length; i++) widths[i] = Math.max(widths[i], row[i].length());
        border(out, widths);
        line(out, headers, widths, false);
        border(out, widths);
        for (String[] row : rows) line(out, row, widths, true);
        border(out, widths);
        out.write("\n");
    }

    private void line(Writer out, String[] values, int[] widths, boolean alignNumbers) throws IOException {
        out.write("|");
        for (int i = 0; i < widths.length; i++) {
            String padding = repeat(' ', widths[i] - values[i].length());
            out.write(" " + (alignNumbers && numeric[i] ? padding + values[i] : values[i] + padding) + " |");
        }
        out.write("\n");
    }

    private void border(Writer out, int[] widths) throws IOException {
        out.write("+");
        for (int width : widths) out.write(repeat('-', width + 2) + "+");
        out.write("\n");
    }

    private String repeat(char value, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }
}
