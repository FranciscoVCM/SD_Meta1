package com.googol.downloaders;

import com.googol.model.CrawlResult;
import com.googol.util.TextUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class WebCrawler {

    public static CrawlResult crawl(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String html = resp.body();
            String title = extractTitle(html);
            String text = stripHtml(html);
            List<String> terms = TextUtils.tokenize(text);
            String snippet = text.length() > 160 ? text.substring(0,160) + "..." : text;

            CrawlResult r = new CrawlResult();
            r.url = url;
            r.title = title != null ? title : url;
            r.snippet = snippet;
            r.terms = terms;
            r.outlinks = List.of(); // fica para exercícios seguintes
            return r;
        } catch (Exception e) {
            // falha ao descarregar — ignora neste exercício
            return null;
        }
    }

    private static String extractTitle(String html) {
        if (html == null) return null;
        var m = java.util.regex.Pattern.compile("(?is)<title>(.*?)</title>").matcher(html);
        return m.find() ? m.group(1).replaceAll("\\s+"," ").trim() : null;
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        // remove scripts/styles
        html = html.replaceAll("(?is)<script.*?>.*?</script>", " ");
        html = html.replaceAll("(?is)<style.*?>.*?</style>", " ");
        // remove tags
        html = html.replaceAll("(?is)<[^>]+>", " ");
        // compactar espaços
        return html.replaceAll("\\s+", " ").trim();
    }
}
