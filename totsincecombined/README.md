# Combined total, since, and range analysis

This module analyzes **white balls only**: how often each number has appeared,
how many draws have passed since its last appearance, and how appearances are
spread across number ranges. It reads local history and produces descriptive
reports; it does not download official results or predict future draws.

## Build and run

From the repository root, using JDK 17 and Maven:

```bash
mvn -pl totsincecombined -am clean install
java -jar totsincecombined/target/totsincecombined-1.0-SNAPSHOT.jar
```

On this Mac, if Maven is not on your PATH:

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -pl totsincecombined -am clean install
```

Use Maven's **Lifecycle → install**, which compiles and packages before installing.
The JAR needs no external runtime libraries and targets Java 8.

The program prompts in the console:

```text
Lottery PB or MM [PB]:
Action LAST, SIM, NUM_OCCUR, RAN [LAST]:
Input file [files/pb/pb-sorted.txt]:
```

Press Enter to accept each default, or type another value. Game and mode are case
insensitive. For MM the default input is `files/archive/mm-sorted.txt`; you can
enter another path. The user-updated Powerball path `files/pb/pb-sorted.txt` is
preserved without a hard-coded home directory. Run from the repository root.

In IntelliJ, run `totsincecombined.AllNumbers.main()` with the `totsincecombined`
module classpath and the repository root as working directory. Answer the prompts
in the Run console. The program always prompts; any saved program arguments in
IntelliJ are ignored. F5 was an unimplemented placeholder and is explicitly
rejected; this rewrite supports PB and MM.

## Modes and reports

| Mode | Report |
| --- | --- |
| `LAST` | Final history snapshot: all numbers sorted by total ascending, then separately by since descending, plus range totals. |
| `SIM` | After every draw: each number's total/since/gaps and cumulative range totals. |
| `NUM_OCCUR` | After every draw: cumulative appearance totals and sums of since for each range. Preserves the old mode's per-prefix range summary purpose. |
| `RAN` | After every draw: cumulative range-pattern, occupancy-shape, and occupied-range/count frequencies. |

Reports are written to `totsincecombined/target/<game>-<mode>.txt`, for example
`pb-last.txt`. They are replaced on successful reruns, never appended. The console
prints the complete report and its saved path. Both outputs use bordered,
automatically sized plain-text tables: labels and dates align left, numeric values
align right, and percentages show two decimal places and a percent sign. Use a
monospaced font when viewing the text files. `SIM` and `RAN` can
produce large reports and substantial console output because they include every
historical snapshot. Reports in
`target/` are removed by `mvn clean`; copy a report elsewhere to retain it.

## Definitions

- **Total:** number of draws containing that white ball.
- **Since:** completed draws after its last appearance. Every white ball in the
  latest draw has since 0. Never-seen numbers have since equal to the number of
  analyzed draws and `NEVER` as last date. This measures only the supplied history.
- **Min/max gap:** smallest/largest distance between consecutive appearance draw
  indices. Consecutive appearances have gap 1. The first appearance does not create
  a gap; fewer than two appearances displays `NA`.
- **Range total:** sum of individual appearance totals within a range. Across all
  ranges this is exactly five times the snapshot's draw count.
- **Sum of since:** sum of each number's since within a range, including never-seen
  numbers. This is not the time since any number in that range appeared.
- **Pattern:** counts in ordered ranges `1–9`, `10–19`, and so on. PB ends at `60–69`;
  the legacy MM analysis universe ends at `70–75`. Pattern counts sum to five.
- **Shape:** nonzero range counts sorted descending, for example `3+2` or `2+1+1+1`.
- **Occupied range/count category:** explicit pair such as `1-9: 5 balls` or
  `60-69: 1 balls`. These remain distinct; the old weighted numeric encoding could
  merge them. Only occupied ranges contribute.

Pattern and shape percentages use the number of draws as denominator. Occupied
category percentages use the number of occupied range observations across draws.
Every frequency table states its denominator and sorts by count descending, then
category text for ties. Number sorting ties use the number ascending.

PB uses the analysis universe 1–69. MM retains the legacy module's 1–75 universe
to accommodate historical files; this is **not** validation of the current game's
rules or date-specific eligibility. Numbers outside a game's eligible set for part
of a mixed-era history still appear in this fixed-universe analysis. For comparisons
within one ruleset, supply a history limited to that era. Special balls, when present,
are structurally checked as 1–99 and excluded from all statistics.

## Input and failure handling

UTF-8 input accepts blank lines, a leading BOM, spaces/tabs, CRLF/LF, and a final
line without a newline. Each nonblank row contains a strict `M/d/yyyy` date, five
unique in-range white balls, and optionally one special ball. Five-ball-only rows
are supported because this analysis does not require special-ball values. Missing
white balls are rejected; values are never fabricated as 99.

Draws are sorted chronologically before analysis. Duplicate dates, malformed rows,
invalid calendar dates, repeated white balls, and empty or missing files produce
an error with file/line context where applicable. These checks do not verify draw
schedules or official results; use `powerball-sync` for that task. Archived MM data
may need corrections before it passes these stricter checks.

Successful runs return 0; invalid console input, EOF, input errors, and write errors
return 1. No source files are rewritten. Output is streamed to a temporary file and
atomically replaced only on success; failures leave an existing report untouched.

## Implementation and verification

- `AllNumbers`: console interaction and atomic report replacement.
- `DrawReader` / `Draw`: strict file parsing and immutable draw values.
- `Statistics` / `NumberStats`: incremental counters and immutable snapshots.
- `ReportWriter`: deterministic tables for the four modes.

The old module-local DTOs, delegates, duplicated grouping/recency classes, and raw
sorting utilities were replaced. No other module imports these old classes. There
is no shared mutable static state, busy-wait thread, or repeated file read per
snapshot. Statistics are updated once per draw; report work scales with the output
requested, particularly the cumulative pattern tables in RAN.

The rewrite fixes final-line counting, the broken LAST initialization, off-by-one
recency behavior, fabricated values, range-key collisions, and success exits that
previously returned 1. Reports intentionally differ from those buggy outputs.

```bash
mvn -pl totsincecombined -am test
```

Ten JUnit tests cover totals, recency, gaps, unseen numbers, snapshots, range-key
collisions, parsing, all modes, deterministic output, console errors, and atomic
replacement. They use temporary fixtures and do not modify lottery history.
