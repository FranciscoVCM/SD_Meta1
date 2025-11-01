package com.googol.downloaders;

import com.googol.model.CrawlResult;
import com.googol.util.TextUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawler {

    public static CrawlResult crawl(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            String html = resp.body();
            String title = extractTitle(html);
            String text  = stripHtml(html);
            List<String> terms = TextUtils.tokenize(text);
            String snippet = text.length() > 160 ? text.substring(0, 160) + "..." : text;

            CrawlResult r = new CrawlResult();
            r.url = url;
            r.title = (title != null) ? title : url;
            r.snippet = snippet;
            r.text = text;
            r.terms = terms;
            r.outlinks = List.of(); // fica para exercícios seguintes
            return r;
        } catch (Exception e) {
            // uma falha de download não deve estourar o sistema
            CrawlResult r = new CrawlResult();
            r.url = url;
            r.title = url;
            r.snippet = "download failed: " + e.getClass().getSimpleName();
            r.terms = List.of();
            r.outlinks = List.of();
            return r;
        }
    }

    private static final Pattern TITLE_RE =
            Pattern.compile("(?is)<title\\b[^>]*>(.*?)</title>");

    private static String extractTitle(String html) {
        if (html == null) return null;
        Matcher m = TITLE_RE.matcher(html);
        if (m.find()) return m.group(1).replaceAll("\\s+", " ").trim();
        return null;
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        // remove scripts/styles
        String noScript = html.replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ");
        // remove tags
        String text = noScript.replaceAll("(?is)<[^>]+>", " ");
        // normaliza espaços
        return text.replaceAll("\\s+", " ").trim();
    }
}
