# Lottery Core

Plain Java 17 library shared by the Lottery Workbench web application. It has
no production dependencies and does not start a server, read console input,
or open a database.

| Package under `com.vipin.lottery.core` | Responsibility |
| --- | --- |
| `model` | Immutable, sorted `DrawRecord` and draw-derived values. |
| `io` | `HistoryParser`: date/value validation and normalization. |
| `source` | `PowerballSource`: official CSV retrieval and completeness validation. |
| `analysis` | `AnalysisService` facade, incremental `Statistics`, number snapshots, digit windows, range universe and bounded combinations. |
| `report` | `SumReport`, `CombinedReport`, and one shared text-table formatter. |

The web module provides these objects as Spring beans. Other Java callers can
instantiate them directly. For example:

```java
var draws = new HistoryParser().parse("1/3/2026  1 2 3 4 5  6", "PB");
var result = new AnalysisService().analyze(draws, "PB");
```

Import the classes from `com.vipin.lottery.core.io` and
`com.vipin.lottery.core.analysis`. The parse result is chronological. Analysis
requests use their own mutable accumulators; shared service instances hold no
per-request calculation state.

From the repository root:

```bash
mvn -pl lottery-core -am clean install
```

Tests verify calculation definitions, immutable snapshots, parser and official
source failures, and report output. `src/test/resources/golden/` contains outputs
captured from the prior report writers; these intentionally protect whitespace
and tabular layouts as well as values.

See [the web guide](../lottery-web/README.md) for calculation definitions and
[the consolidation mapping](../lottery-web/MODULE-ANALYSIS.md) for retired tools.
