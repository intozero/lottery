# Machine learning forecast documentation

This guide describes the implemented Lottery Workbench forecast, including the
actual training samples, calculations, ten classifiers, and retrospective checks.
It describes **Weka 3.8.6**, as configured in this repository. Model scores are
uncalibrated ranking scores; the application has not demonstrated an advantage
in predicting independent lottery draws.

## Reading guide

| Document | Contents |
| --- | --- |
| [Data and calculations](DATA-AND-CALCULATIONS.md) | Draw selection, labels, all 11 feature formulas, sample weights, historical prior, blending, ranking, evaluation, and worked examples. |
| [All ten models](MODELS.md) | Each algorithm's fitting logic, sample usage, exact configuration, output calculation, and limitations. |
| [Sample sets and validation](SAMPLE-SETS-AND-VALIDATION.md) | Production data origins, synthetic fixtures, internal model samples, API contract, reproducibility, and what the tests establish. |
| [Weka options snapshot](weka-3.8.6-options.txt) | Actual `getOptions()` output from the application's classifier factory with Weka 3.8.6. |

## Using the feature

1. Select the game and global Starting draw, Last draw, Row step, and optional
   date bounds. Draw #1 is the oldest stored draw for that game.
2. Open **Next draw candidates → Machine learning · Weka Java**. At least 20
   selected draws are required.
3. Choose one model or all ten, a training window, delta, and ML weight.
4. Run the forecast. Each successful model returns five distinct white balls,
   ranked ball scores, and comparisons with stored draws after Last draw.

| Setting | Default | Allowed values | Meaning |
| --- | --- | --- | --- |
| Model | `all` | `all` or one ID in the model guide | Train independently and display each result. There is no ensemble across these ten choices. |
| Training window | 150 | Integer 20–300 | Maximum labeled selected-history transitions. Earlier selected draws still supply feature context. |
| Delta | 1 | Finite number 0–10 | Exponential recency weighting of training examples and the historical prior. |
| ML weight | 0.7 | Finite number 0–1 | Fraction of final ball score supplied by the classifier; the remainder comes from the historical prior. |

Every run trains fresh Java classifiers in memory. There are ten algorithms in
one library, with no pretrained models, external training service, saved model,
automatic model selection, or automatic tuning. All ten receive the same prepared
training dataset. Some make internal bootstrap/pruning samples, documented below.
Only white balls are predicted; the special ball is excluded throughout.

The separate **Generate candidates** search uses unseen range patterns and modal
sum/deviation constraints. Those constraints do not filter ML guesses. ML uses
sum and deviation as features, and selects the five highest blended scores even
when their joint pattern has previously occurred. The existing
[web guide](../../lottery-web/README.md#next-draw-candidates) documents that
exhaustive candidate search.

## Implementation and authoritative references

- [MachineLearningForecast.java](../../lottery-core/src/main/java/com/vipin/lottery/core/analysis/MachineLearningForecast.java): dataset construction, features, factory settings, score blending, evaluation.
- [ApiController.java](../../lottery-web/src/main/java/com/vipin/lottery/web/api/ApiController.java): `/api/ml-forecast`, selection and original draw numbering.
- [DrawRecord.java](../../lottery-core/src/main/java/com/vipin/lottery/core/model/DrawRecord.java): sorted white balls, sums, population deviation, game universe.
- [Core Maven dependency](../../lottery-core/pom.xml): pinned Weka version.
- [Weka 3.8.6 source archive](https://repo.maven.apache.org/maven2/nz/ac/waikato/cms/weka/weka-stable/3.8.6/weka-stable-3.8.6-sources.jar): source inspected for algorithm defaults and calculations in this guide.
- [Weka Java integration](https://waikato.github.io/weka-wiki/use_weka_in_your_java_code/): library usage.

The application source and pinned Weka source define behavior. Public Weka
`doc.dev` API pages linked in the model guide are supplementary and may describe
a different release. Recheck defaults and the options snapshot when changing the
library or classifier factory.
