package com.vipin.lottery.web;

import java.time.LocalDate;
import java.util.*;

public record DrawRecord(LocalDate date, List<Integer> whites, Integer special) {
    public DrawRecord {
        Objects.requireNonNull(date, "Draw date is required");
        if (whites == null || whites.size() != 5) throw new IllegalArgumentException("Exactly five white balls are required");
        whites = whites.stream().sorted().toList();
        if (new HashSet<>(whites).size() != 5 || whites.get(0) < 1 || whites.get(4) > 75)
            throw new IllegalArgumentException("White balls must be distinct values from 1 to 75");
        if (special != null && (special < 1 || special > 99)) throw new IllegalArgumentException("Invalid special ball");
    }
    public int sum() { return whites.stream().mapToInt(Integer::intValue).sum(); }
    public double deviation() {
        double mean = sum() / 5.0;
        return Math.sqrt(whites.stream().mapToDouble(n -> (n - mean) * (n - mean)).sum() / 5);
    }
    public String values() {
        return String.join(" ", whites.stream().map(Object::toString).toList()) + (special == null ? "" : " " + special);
    }
    public static int maximum(String game) {
        return switch (game) { case "PB" -> 69; case "MM" -> 75; default -> throw new IllegalArgumentException("Game must be PB or MM"); };
    }
}
