package com.googol.util;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtils {

    private TextUtils() {}

    /** STOP words melhoradas */
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "a","o","os","as","de","da","do","das","dos","e","em","para","por",
            "um","uma","uns","umas","the","and","of","to","in","on","for","is",
            "are","be","with","at","by","an","or","as","if","that","this","it"
    ));

    public static boolean isStopWord(String t) {
        return t == null || t.isBlank() || STOP.contains(t.toLowerCase());
    }

    /** Remove acentos, emojis, normaliza símbolos e espaços */
    public static String normalize(String s) {
        if (s == null) return "";

        // remover acentos
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // separar CamelCase (MegaKnight → Mega Knight)
        s = s.replaceAll("(?<=[a-z])(?=[A-Z])", " ");

        // remover emojis e símbolos estranhos
        s = s.replaceAll("[^\\p{L}\\p{Nd}\\s]+", " ");

        // tudo lowercase
        s = s.toLowerCase();

        // normalizar espaços
        return s.replaceAll("\\s+", " ").trim();
    }

    /** Tokenização inteligente */
    public static List<String> tokenize(String text) {
        if (text == null) return Collections.emptyList();

        String[] raw = normalize(text).split("\\s+");
        List<String> terms = new ArrayList<>(raw.length);

        for (String t : raw) {
            if (t.isBlank()) continue;
            if (STOP.contains(t)) continue;

            // aplicar CamelCase split se necessário
            List<String> split = splitCamelCase(t);
            terms.addAll(split);
        }

        return terms;
    }


    private static final Pattern TITLE_RE = Pattern.compile("(?is)<title>(.*?)</title>");
    public static String extractTitle(String html) {
        if (html == null) return null;
        Matcher m = TITLE_RE.matcher(html);
        return m.find() ? m.group(1).replaceAll("\\s+", " ").trim() : null;
    }
    private static List<String> splitCamelCase(String t) {
        // MegaKnight → [Mega, Knight]
        // DarkX → [Dark, X]
        if (!t.matches(".*[a-z][A-Z].*")) return List.of(t);

        String spaced = t.replaceAll("([a-z])([A-Z])", "$1 $2");
        List<String> out = new ArrayList<>();
        for (String s : spaced.split("\\s+")) {
            s = s.trim().toLowerCase();
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }


    /** Extrator de snippet inteligente */
    public static String makeSnippet(String text, List<String> terms) {
        if (text == null || text.isBlank()) return "";

        String low = text.toLowerCase();
        int pos = Integer.MAX_VALUE;

        for (String t : terms) {
            int i = low.indexOf(t.toLowerCase());
            if (i >= 0) pos = Math.min(pos, i);
        }

        if (pos == Integer.MAX_VALUE) pos = 0;

        int width = 200;
        int start = Math.max(0, pos - width / 3);
        int end = Math.min(text.length(), start + width);

        String sn = text.substring(start, end).replaceAll("\\s+", " ").trim();

        if (start > 0) sn = "… " + sn;
        if (end < text.length()) sn += " …";

        return sn;


    }
}
