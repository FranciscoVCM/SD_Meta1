package com.googol.util;

import java.util.*;

public final class TextUtils {
    private TextUtils() {}

    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "a","o","os","as","de","da","do","das","dos","e","em","para","por",
            "um","uma","uns","umas","the","and","of","to","in","on","for","is",
            "are","be","with","at","by","an","or","as"
    ));

    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^\\p{L}\\p{Nd}\\s]+"," ").trim();
    }

    public static List<String> tokenize(String text) {
        if (text == null) return Collections.emptyList();
        String[] raw = normalize(text).split("\\s+");
        List<String> terms = new ArrayList<>(raw.length);
        for (String t : raw) {
            if (t.isBlank()) continue;
            if (STOP.contains(t)) continue;
            terms.add(t);
        }
        return terms;
    }

    public static boolean isStopWord(String t) {
        return t == null || t.isBlank() || STOP.contains(t);
    }

    /** Snippet tosco: devolve ~160 chars centrados na 1ª ocorrência de algum termo. */
    public static String makeSnippet(String text, List<String> terms) {
        if (text == null || text.isBlank()) return "";
        String low = text.toLowerCase();
        int pos = Integer.MAX_VALUE;
        for (String t : terms) {
            String k = t.toLowerCase();
            int i = low.indexOf(k);
            if (i >= 0) pos = Math.min(pos, i);
        }
        if (pos == Integer.MAX_VALUE) pos = 0;
        int width = 160;
        int start = Math.max(0, pos - width/3);
        int end   = Math.min(text.length(), start + width);
        String sn = text.substring(start, end).replaceAll("\\s+"," ").trim();
        if (start > 0) sn = "… " + sn;
        if (end < text.length()) sn = sn + " …";
        return sn;
    }
}
