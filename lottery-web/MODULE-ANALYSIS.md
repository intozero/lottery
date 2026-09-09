# Repository review and web capability mapping

The root reactor contained eleven analysis/update modules before the web module
was added. They read variations of dated text files. Several older programs use
hard-coded paths, raw collections, static state, or mutable file readers; running
their main methods inside HTTP requests would be unsafe and difficult to control.

The web application therefore uses a database as the canonical working copy,
reuses the already-refined public calculation/report classes, and implements
stateless equivalents of basic workflows from the remaining console scripts.
Original modules remain available and their source files were not rewritten for
this integration.

| Existing module | Original purpose / findings | Web treatment |
| --- | --- | --- |
| as-number | Concatenates unpadded draw values; counts overlapping digit windows and repeated combinations. | Digit patterns with a bounded configurable window; exact repeats on Draw history. Missing special balls produce a clear error. |
| sumapplication | Per-draw sums, running average and total, frequency distribution; recently refined stateless report writer. | Sums & deviation page; reuse SumReport for text export. |
| totsincecombined | Total/since/gaps per number, cumulative range totals, range patterns and occupancy shapes; refined incremental Statistics. | Reuse Statistics for overview/numbers/ranges and existing LAST/SIM/NUM_OCCUR/RAN exporters. Number timeline exposes individual historical snapshots interactively. |
| maxmindiffoccurence | First/last occurrence, min/max gaps, recency; older implementation counts ball positions and carries mutable state. | Correct draw-index-based gaps and since in Number statistics; last appearance date, per-number timeline. |
| occurencestudy | Occurrence points, gaps, first/last sorted white-ball studies, smallest+largest sums. | Number timeline plus smallest/largest/end-sum frequency tables. |
| deviation-mean-sum-eachlot | Draw sum, mean and deviation, distributions; legacy integer truncation loses precision. | Exact mean/population standard deviation and distributions; deviation floor for grouping/search, explicitly documented. |
| groupingrange | Ordered decade counts, occupancy shapes, cumulative range frequency. | Range totals, observed patterns/shapes; exact range keys replace collision-prone numeric weights. |
| groupingrangecomparewithsuperset | Relates observed range patterns to a precomputed universe in text files. | Generate the range universe mathematically and join with observed history. |
| range-occurence-study | Reads two range files, subtracts observed patterns, filters by square score. | Unseen-only range filter with sum-of-squared-counts column and table search/sort. No brittle fixed-width substring parsing. |
| all-combinations | Exhaustive five-ball combinations, sums/deviation filters, range forms and counts. | Bounded sum/deviation combination search; exact per-range combinatorial counts and square scores. Results disclose the 500-row cap. Exhaustive unbounded batch exports are not run in HTTP requests. |
| powerball-sync | Validates file history against official Texas CSV; corrects duplicates/mismatches and missing draws with file backup/audit. | Manual DB synchronization using the same official source and completeness rules; unique dates, transactional updates and stored old/new values. Original files stay untouched; export when wanted. |

## Deliberate behavior changes

- The web database has a unique game/date key; ordinary imports never silently
  overwrite a known conflicting result.
- White-ball analyses exclude the special ball; digit analyses require it.
- Dates are chronological and explicit. Duplicate dates do not inflate frequency.
- Never-seen values refer to the selected history window, not all-time history.
- Exact floating-point means/deviation replace integer-truncated legacy results.
- Frequency/range results are descriptive; neither recency nor absence changes the
  chance of a future independent draw.
- Report downloads preserve familiar plain-text workflows while the UI provides
  filtering, searchable tables and pagination.
- MM retains the historical 1–75 analysis envelope and is not treated as a
  date-specific rules validator. Limit selected history to the desired era.
- The application is local and single-user; it does not place bets, purchase
  tickets, schedule updates, or publish data externally.
