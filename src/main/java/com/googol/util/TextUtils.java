package com.googol.util;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern TITLE_RE = Pattern.compile("(?is)<title>(.*?)</title>");
    public static String extractTitle(String html) {
        if (html == null) return null;
        Matcher m = TITLE_RE.matcher(html);
        return m.find() ? m.group(1).replaceAll("\\s+", " ").trim() : null;
    }

    public static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // links relativos -> absolutos, e filtros simples
    private static final Pattern HREF_RE = Pattern.compile("(?is)href\\s*=\\s*\"([^\"]+)\"");
    public static List<String> extractLinks(String baseUrl, String html, int max) {
        List<String> out = new ArrayList<>();
        if (html == null) return out;

        URI base;
        try { base = URI.create(baseUrl); } catch (Exception e) { return out; }

        Matcher m = HREF_RE.matcher(html);
        while (m.find() && out.size() < max) {
            String href = m.group(1).trim();
            if (href.startsWith("javascript:") || href.startsWith("#")) continue;
            try {
                URI u = base.resolve(href);
                String s = u.normalize().toString();
                // filtro básico: http/https, sem fragmentos mailto etc.
                if (s.startsWith("http://") || s.startsWith("https://")) {
                    out.add(s);
                }
            } catch (Exception ignored) {}
        }
        return out;
    }

    public static boolean isStopWord(String t) {
        return t == null || t.isBlank() || STOP.contains(t);
    }

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
