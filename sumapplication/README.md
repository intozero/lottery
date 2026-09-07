# White-ball sum analysis

`sumapplication` calculates the sum of the five white balls in each draw, the
running total and average draw sum, and the frequency of each sum. Special balls
are excluded. Results describe the supplied history; they are not forecasts or
verification against official winning numbers.

## Build and run

From the `lottery` repository root, using JDK 17 and Maven:

```bash
mvn -pl sumapplication -am clean install
java -jar sumapplication/target/sumapplication-1.0-SNAPSHOT.jar
```

On this Mac, if Maven is not on your PATH:

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -pl sumapplication -am clean install
```

Use Maven **Lifecycle → install**, which creates the JAR before installing it.
The executable JAR targets Java 8 and requires no additional runtime libraries.

The program always reads user input from console prompts. Saved IDE program
arguments are ignored:

```text
Lottery PB or MM [PB]:
Input file [files/pb/pb-sorted.txt]:
```

Type `PB` or `MM` (case insensitive), then a file path, or press Enter at each
prompt to accept its default. Powerball defaults to `files/pb/pb-sorted.txt`;
Mega Millions defaults to `files/archive/mm-sorted.txt`. Relative paths resolve
from the working directory, so run from the repository root.

In IntelliJ, run `sumapplication.SumApplication.main()` with the `sumapplication`
module classpath and the repository root as the working directory. Answer the
prompts in the Run console. No program arguments are required.

## Output

The same bordered, aligned tables are printed to the console and saved to:

- PB: `sumapplication/target/pb-sums.txt`
- MM: `sumapplication/target/mm-sums.txt`

The report includes:

1. Draw count, date range, grand total, average/minimum/maximum draw sum, and number
   of distinct sums.
2. Every draw in chronological order: date, sorted white balls, sum, running total,
   and running average.
3. Sum frequencies ordered by count ascending, then sum ascending for ties,
   preserving the original module's frequency ordering. Percentages use all draws
   as the denominator.

For sums 15 and 16, the running averages are 15.00 and 15.50. The average is per
**draw**, not per individual ball. Totals use `long`; averages use floating-point
division and display two decimal places. Numeric columns align right. Use a
monospaced font when viewing saved text reports.

A successful run replaces the previous report instead of appending. It never
rewrites the input. The new report is written to a temporary file and atomically
replaces the destination only after completion. Errors leave an existing report
untouched. Reports under `target/` are removed by `mvn clean`; copy them elsewhere
if you need to preserve them.

## Input validation

Each nonblank UTF-8 line must contain a valid `M/d/yyyy` date, five distinct white
balls, and optionally one special ball. Spaces/tabs, a leading BOM, CRLF/LF, and a
final line without a newline are supported. Rows are sorted chronologically.

White-ball bounds are 1–69 for PB and the historical analysis bound 1–75 for MM.
These are broad input bounds, not date-specific lottery rule validation. A special
ball, if present, must be 1–99 and is ignored in the sums. A missing special ball is
allowed; missing white balls are rejected and never fabricated. Duplicate dates,
invalid calendar dates, repeated white balls, malformed rows, and missing/empty
files produce clear errors with line context where applicable.

Invalid console selections or end-of-input exit with status 1, as do file/write
errors. Success returns 0. Use `powerball-sync` separately when you need to verify
or update Powerball history against official results.

## Structure and checks

- `SumApplication`: console prompts, file/report orchestration.
- `FileRead`: strict, stateless reader.
- `LineDTO`: immutable date and five white-ball values.
- `SumReport`: stateless sums, averages, frequencies, and report generation.
- `TextTable`: shared formatting within this module.

The rewrite removes hard-coded home directories, the double-read lottery prompt
bug, mutable static counters, accumulated state between runs, raw collections,
redundant Lombok annotations, and integer-truncated averages. The old mutable DTO
and `sumStart/getSum/arrangeSum` APIs were replaced; no other module called them.

```bash
mvn -pl sumapplication -am test
```

Eight JUnit tests cover fractional averages, deterministic frequencies, parsing,
immutable data, malformed input, table alignment, atomic output, and console
errors. Fixtures use temporary files and do not modify lottery data.
