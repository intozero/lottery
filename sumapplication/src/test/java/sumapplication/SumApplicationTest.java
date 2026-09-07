package sumapplication;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public class SumApplicationTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private List<LineDTO> draws() {
        return Arrays.asList(new LineDTO(LocalDate.of(2026,1,1),new int[]{1,2,3,4,5}),
                new LineDTO(LocalDate.of(2026,1,2),new int[]{1,2,3,4,6}));
    }

    @Test public void sumsAndAveragesKeepFractionalPart() throws Exception {
        String report = report(draws());
        assertTrue(report.contains("15.50"));
        assertTrue(report.matches("(?s).*Total of all draw sums +\\| 31 +\\|.*"));
        assertTrue(report.matches("(?s).*Average draw sum +\\| 15.50 +\\|.*"));
        assertTrue(report.contains("50.00%"));
        assertEquals(15,draws().get(0).sum());
    }

    @Test public void frequencySortIsDeterministicAndStateDoesNotLeak() throws Exception {
        List<LineDTO> input = new ArrayList<>(draws());
        input.add(new LineDTO(LocalDate.of(2026,1,3),new int[]{1,2,3,4,5}));
        String first = report(input);
        String frequencies = first.split("Sum frequencies")[1];
        assertTrue(frequencies.indexOf("|  16") < frequencies.indexOf("|  15"));
        assertTrue(frequencies.contains("66.67%"));
        Collections.reverse(input); assertEquals(first,report(input));
        SumReport writer = new SumReport();
        StringWriter a = new StringWriter(), b = new StringWriter();
        writer.write(draws(),a); writer.write(draws(),b); assertEquals(a.toString(),b.toString());
    }

    @Test public void parserSupportsOptionalSpecialBallAndSortsDates() throws Exception {
        Path input = file("input.txt","\uFEFF1/2/2026\t1 2 3 4 6 99\r\n\r\n1/1/2026  5 4 3 2 1");
        FileRead reader = new FileRead();
        List<LineDTO> rows = reader.read(input,69);
        assertEquals(2,rows.size()); assertEquals(LocalDate.of(2026,1,1),rows.get(0).getDate());
        assertEquals(16,rows.get(1).sum()); assertEquals(2,reader.read(input,69).size());
    }

    @Test public void rejectsInvalidRowsWithLineContext() throws Exception {
        for (String invalid : Arrays.asList("2/30/2026 1 2 3 4 5","1/1/2026 1 2 3 4",
                "1/1/2026 1 1 3 4 5","1/1/2026 1 2 3 4 70","1/1/2026 1 2 3 4 5 0",
                "1/1/2026 1 2 3 4 5\n1/1/2026 1 2 3 4 6")) {
            IOException e = assertThrows(IOException.class, () -> new FileRead().read(file("bad.txt",invalid),69));
            assertTrue(e.getMessage().contains("line "));
        }
        assertThrows(IOException.class, () -> new FileRead().read(file("empty.txt",""),69));
        assertThrows(IOException.class, () -> new FileRead().read(temp.getRoot().toPath().resolve("missing"),69));
    }

    @Test public void drawCannotBeMutatedAndDuplicateDatesAreRejected() throws Exception {
        int[] balls = {1,2,3,4,5}; LineDTO draw = new LineDTO(LocalDate.of(2026,1,1),balls);
        balls[0] = 69; draw.getWhiteBalls()[0] = 69; assertEquals(15,draw.sum());
        assertThrows(IllegalArgumentException.class, () -> report(Arrays.asList(draw,draw)));
        assertThrows(IllegalArgumentException.class, () -> report(Collections.emptyList()));
    }

    @Test public void reportsAlignAndUseLocaleIndependentDecimals() throws Exception {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String report = report(draws()); assertTrue(report.contains("15.50")); assertFalse(report.contains("\t"));
            int width = -1;
            for (String line : report.split("\n")) {
                if (line.startsWith("+")) { if (width == -1) width = line.length(); assertEquals(width,line.length()); }
                else if (line.startsWith("|")) assertEquals(width,line.length());
                else width = -1;
            }
        } finally { Locale.setDefault(old); }
    }

    @Test public void reportReplacementPreservesInputAndFailurePreservesReport() throws Exception {
        Path input = file("input.txt","source"), output = file("report.txt","old");
        SumApplication.writeReport(input,output,draws());
        byte[] bytes = Files.readAllBytes(output);
        SumApplication.writeReport(input,output,draws()); assertArrayEquals(bytes,Files.readAllBytes(output));
        assertThrows(IllegalArgumentException.class, () -> SumApplication.writeReport(input,output,Collections.emptyList()));
        assertArrayEquals(bytes,Files.readAllBytes(output));
        assertThrows(IOException.class, () -> SumApplication.writeReport(input,input,draws()));
        assertEquals("source",new String(Files.readAllBytes(input),StandardCharsets.UTF_8));
    }

    @Test public void consolePromptsAndErrorsAreClear() throws Exception {
        for (String value : Arrays.asList("", "BAD\n", "PB\n", "PB\nmissing-sums-input.txt\n")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream out = new PrintStream(bytes)) {
                assertEquals(1,SumApplication.run(new BufferedReader(new StringReader(value)),out,out));
                assertTrue(bytes.toString("UTF-8").contains("Lottery PB or MM"));
                assertTrue(bytes.toString("UTF-8").contains("sumapplication:"));
            }
        }
    }

    private Path file(String name,String value) throws IOException {
        Path path=temp.getRoot().toPath().resolve(name); Files.write(path,value.getBytes(StandardCharsets.UTF_8));return path;
    }
    private String report(List<LineDTO> draws) throws IOException {
        StringWriter out=new StringWriter();new SumReport().write(draws,out);return out.toString();
    }
}
