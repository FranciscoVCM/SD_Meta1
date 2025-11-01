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
        CrawlResult r = new CrawlResult();
        r.url = url;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            // ignora conteúdos não-HTML
            String ct = resp.headers().firstValue("content-type").orElse("");
            if (!ct.contains("text/html")) {
                r.title    = url;
                r.text     = "";
                r.snippet  = "";
                r.terms    = List.of();
                r.outlinks = List.of();
                return r;
            }

            // --- HTML: extrair título, texto limpo, termos, snippet e outlinks
            String html    = resp.body();
            String title   = TextUtils.extractTitle(html);
            String text    = TextUtils.stripHtml(html);
            List<String> terms   = TextUtils.tokenize(text);
            String snippet = TextUtils.makeSnippet(text, terms);

            r.title    = (title != null && !title.isBlank()) ? title : url;
            r.text     = text;
            r.snippet  = snippet;
            r.terms    = terms;
            r.outlinks = TextUtils.extractLinks(url, html, 50);

        } catch (Exception e) {
            r.title    = url;
            r.text     = "";
            r.snippet  = "download failed: " + e.getClass().getSimpleName();
            r.terms    = List.of();
            r.outlinks = List.of();
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();

        HttpRequest req = HttpRequest.newBuilder(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(8))
                .GET()
                .build();
        return r;
    }
}
