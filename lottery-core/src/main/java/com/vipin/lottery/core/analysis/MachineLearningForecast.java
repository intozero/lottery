package com.vipin.lottery.core.analysis;

import com.vipin.lottery.core.model.DrawRecord;
import java.time.LocalDate;
import java.util.*;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.meta.LogitBoost;
import weka.classifiers.trees.*;
import weka.core.*;

/** Learns binary ball-in-next-selected-draw labels from strictly earlier features. */
public final class MachineLearningForecast {
    public record ModelInfo(String id, String name) {}

    public static final List<ModelInfo> MODELS =
            List.of(
                    new ModelInfo("random-forest", "Random forest"),
                            new ModelInfo("logistic", "Logistic regression"),
                    new ModelInfo("naive-bayes", "Naive Bayes"),
                            new ModelInfo("j48", "J48 decision tree"),
                    new ModelInfo("rep-tree", "Reduced-error pruning tree"),
                            new ModelInfo("random-tree", "Random tree"),
                    new ModelInfo("knn", "k-nearest neighbors"),
                            new ModelInfo("adaboost", "AdaBoost"),
                    new ModelInfo("logitboost", "LogitBoost"),
                            new ModelInfo("neural-network", "Multilayer perceptron"));

    public record Settings(String model, double delta, double mlWeight, int trainingWindow) {
        public Settings {
            if (model == null
                    || !model.equals("all") && MODELS.stream().noneMatch(m -> m.id().equals(model)))
                throw new IllegalArgumentException("Unknown ML model");
            if (!Double.isFinite(delta) || delta < 0 || delta > 10)
                throw new IllegalArgumentException("Delta must be between 0 and 10");
            if (!Double.isFinite(mlWeight) || mlWeight < 0 || mlWeight > 1)
                throw new IllegalArgumentException("ML weight must be between 0 and 1");
            if (trainingWindow < 20 || trainingWindow > 300)
                throw new IllegalArgumentException(
                        "Training window must be 20–300 selected transitions");
        }
    }

    public record Score(int number, double learned, double historical, double blended) {}

    public record Evaluation(
            int drawNumber,
            LocalDate date,
            int drawsAfterCutoff,
            List<Integer> actual,
            List<Integer> shared,
            int matchedBalls) {}

    public record ModelResult(
            String id,
            String name,
            String error,
            List<Integer> whites,
            List<Score> scores,
            Integer nextDrawMatches,
            Double averageMatches,
            int exactMatches,
            int closestMatchedBalls,
            Integer closestDraw,
            List<Evaluation> later) {}

    public record Result(
            String library,
            Settings settings,
            int selectedDraws,
            int trainingTransitions,
            int trainingExamples,
            LocalDate trainingFrom,
            LocalDate trainingThrough,
            double randomExpectedMatches,
            List<ModelResult> models) {}

    public Result forecast(
            List<DrawRecord> history,
            String game,
            List<NextDrawCandidates.LaterDraw> later,
            int cutoff,
            Settings settings) {
        int maximum = DrawRecord.maximum(game);
        if (history.size() < 20)
            throw new IllegalArgumentException("Select at least 20 training draws for ML");
        if (later.stream().anyMatch(row -> row.drawNumber() <= cutoff))
            throw new IllegalArgumentException(
                    "Evaluation draws must be after the training cutoff");
        int start = Math.max(1, history.size() - settings.trainingWindow());
        Instances data = dataset();
        int[] totals = new int[maximum + 1], last = new int[maximum + 1];
        Arrays.fill(last, -1);
        double runningSum = 0;
        double[][] target = null;
        for (int i = 0; i <= history.size(); i++) {
            if (i >= start) {
                double[][] features = features(history, i, maximum, totals, last, runningSum);
                if (i == history.size()) target = features;
                else {
                    double weight =
                            Math.exp(
                                    -settings.delta()
                                            * (history.size() - 1 - i)
                                            / Math.max(1.0, history.size() - start - 1));
                    for (int number = 1; number <= maximum; number++) {
                        double[] row = Arrays.copyOf(features[number], features[number].length + 1);
                        row[row.length - 1] = history.get(i).whites().contains(number) ? 1 : 0;
                        data.add(new DenseInstance(weight, row));
                    }
                }
            }
            if (i < history.size()) {
                runningSum += history.get(i).sum();
                for (int n : history.get(i).whites()) {
                    totals[n]++;
                    last[n] = i;
                }
            }
        }
        double[] prior = prior(history, maximum, settings.delta());
        List<ModelResult> results = new ArrayList<>();
        for (ModelInfo info : MODELS) {
            if (!settings.model().equals("all") && !settings.model().equals(info.id())) continue;
            try {
                Classifier model = classifier(info.id());
                model.buildClassifier(new Instances(data));
                List<Score> scores = new ArrayList<>();
                for (int n = 1; n <= maximum; n++) {
                    double[] row = Arrays.copyOf(target[n], target[n].length + 1);
                    row[row.length - 1] = Utils.missingValue();
                    var instance = new DenseInstance(1, row);
                    instance.setDataset(data);
                    double learned = model.distributionForInstance(instance)[1];
                    if (!Double.isFinite(learned))
                        throw new IllegalStateException("Non-finite model score");
                    learned = Math.max(0, Math.min(1, learned));
                    scores.add(
                            new Score(
                                    n,
                                    learned,
                                    prior[n],
                                    settings.mlWeight() * learned
                                            + (1 - settings.mlWeight()) * prior[n]));
                }
                results.add(evaluate(info, scores, later, cutoff));
            } catch (Exception e) {
                results.add(
                        new ModelResult(
                                info.id(),
                                info.name(),
                                "Training failed: " + e.getClass().getSimpleName(),
                                List.of(),
                                List.of(),
                                null,
                                null,
                                0,
                                0,
                                null,
                                List.of()));
            }
        }
        return new Result(
                "Weka 3.8.6",
                settings,
                history.size(),
                history.size() - start,
                data.size(),
                history.get(start).date(),
                history.get(history.size() - 1).date(),
                25.0 / maximum,
                List.copyOf(results));
    }

    private static Instances dataset() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (String name :
                List.of(
                        "number",
                        "frequency",
                        "recent10",
                        "recent30",
                        "since",
                        "previousHit",
                        "rangeFrequency",
                        "meanSum",
                        "previousSum",
                        "previousDeviation",
                        "odd")) attributes.add(new Attribute(name));
        attributes.add(new Attribute("nextHit", new ArrayList<>(List.of("no", "yes"))));
        Instances data = new Instances("selected-draw-transitions", attributes, 0);
        data.setClassIndex(attributes.size() - 1);
        return data;
    }

    private static double[][] features(
            List<DrawRecord> history, int at, int maximum, int[] totals, int[] last, double sum) {
        int[] recent10 = new int[maximum + 1],
                recent30 = new int[maximum + 1],
                ranges = new int[maximum / 10 + 1];
        for (int n = 1; n <= maximum; n++) ranges[n / 10] += totals[n];
        for (int i = Math.max(0, at - 30); i < at; i++)
            for (int n : history.get(i).whites()) {
                recent30[n]++;
                if (i >= at - 10) recent10[n]++;
            }
        DrawRecord previous = history.get(at - 1);
        double[][] result = new double[maximum + 1][];
        for (int n = 1; n <= maximum; n++)
            result[n] =
                    new double[] {
                        n / (double) maximum,
                        totals[n] / (double) at,
                        recent10[n] / (double) Math.min(10, at),
                        recent30[n] / (double) Math.min(30, at),
                        (at - 1 - last[n]) / (double) at,
                        previous.whites().contains(n) ? 1 : 0,
                        ranges[n / 10] / (5.0 * at),
                        sum / (at * 5.0 * maximum),
                        previous.sum() / (5.0 * maximum),
                        previous.deviation() / 40.0,
                        n % 2
                    };
        return result;
    }

    private static double[] prior(List<DrawRecord> history, int maximum, double delta) {
        double[] counts = new double[maximum + 1];
        double total = 0;
        for (int i = 0; i < history.size(); i++) {
            double weight =
                    Math.exp(-delta * (history.size() - 1 - i) / Math.max(1.0, history.size() - 1));
            total += weight;
            for (int n : history.get(i).whites()) counts[n] += weight;
        }
        for (int n = 1; n <= maximum; n++) counts[n] = (counts[n] + 5.0 / maximum) / (total + 1);
        return counts;
    }

    private static ModelResult evaluate(
            ModelInfo info,
            List<Score> scores,
            List<NextDrawCandidates.LaterDraw> later,
            int cutoff) {
        var ranking =
                scores.stream()
                        .sorted(
                                Comparator.comparingDouble(Score::blended)
                                        .reversed()
                                        .thenComparingInt(Score::number))
                        .toList();
        var whites = ranking.stream().limit(5).map(Score::number).sorted().toList();
        List<Evaluation> matches = new ArrayList<>();
        int exact = 0, best = -1, total = 0;
        Integer next = null, closest = null;
        for (var row : later) {
            var shared = row.draw().whites().stream().filter(whites::contains).toList();
            matches.add(
                    new Evaluation(
                            row.drawNumber(),
                            row.draw().date(),
                            row.drawNumber() - cutoff,
                            row.draw().whites(),
                            shared,
                            shared.size()));
            total += shared.size();
            if (shared.size() == 5) exact++;
            if (row.drawNumber() == cutoff + 1) next = shared.size();
            if (shared.size() > best) {
                best = shared.size();
                closest = row.drawNumber();
            }
        }
        return new ModelResult(
                info.id(),
                info.name(),
                null,
                whites,
                List.copyOf(ranking),
                next,
                later.isEmpty() ? null : total / (double) later.size(),
                exact,
                Math.max(0, best),
                closest,
                List.copyOf(matches));
    }

    private static Classifier classifier(String id) throws Exception {
        switch (id) {
            case "random-forest":
                {
                    var c = new RandomForest();
                    c.setNumIterations(30);
                    c.setMaxDepth(8);
                    c.setSeed(1);
                    c.setNumExecutionSlots(1);
                    return c;
                }
            case "logistic":
                {
                    var c = new Logistic();
                    c.setMaxIts(100);
                    c.setRidge(1e-4);
                    return c;
                }
            case "naive-bayes":
                return new NaiveBayes();
            case "j48":
                {
                    var c = new J48();
                    c.setMinNumObj(10);
                    return c;
                }
            case "rep-tree":
                {
                    var c = new REPTree();
                    c.setMaxDepth(8);
                    c.setMinNum(10);
                    c.setSeed(1);
                    return c;
                }
            case "random-tree":
                {
                    var c = new RandomTree();
                    c.setMaxDepth(8);
                    c.setMinNum(10);
                    c.setSeed(1);
                    return c;
                }
            case "knn":
                return new IBk(15);
            case "adaboost":
                {
                    var c = new AdaBoostM1();
                    c.setNumIterations(20);
                    c.setSeed(1);
                    return c;
                }
            case "logitboost":
                {
                    var c = new LogitBoost();
                    c.setNumIterations(20);
                    c.setSeed(1);
                    return c;
                }
            case "neural-network":
                {
                    var c = new MultilayerPerceptron();
                    c.setHiddenLayers("8");
                    c.setTrainingTime(30);
                    c.setSeed(1);
                    c.setGUI(false);
                    return c;
                }
            default:
                throw new IllegalArgumentException("Unknown model");
        }
    }
}
