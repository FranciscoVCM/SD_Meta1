package com.googol.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public final class TextUtils {
    private TextUtils() {}

    private static final HashSet<String> STOP = new HashSet<>(Arrays.asList(
            "a","o","os","as","de","da","do","das","dos","e","em","para","por","um","uma","uns","umas",
            "the","and","of","to","in","on","for","is","are","be","with","at","by","an","or","as","it"
    ));

    /** Texto bruto -> lista de termos normalizados (minúsculas, letras/números) sem stopwords. */
    public static List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        if (text == null) return terms;
        String[] raw = text.toLowerCase()
                .replaceAll("[^\\p{L}\\p{Nd}\\s]+", " ")
                .split("\\s+");
        for (String t : raw) {
            if (t.isBlank()) continue;
            if (STOP.contains(t)) continue;
            terms.add(t);
        }
        return terms;
    }
}
