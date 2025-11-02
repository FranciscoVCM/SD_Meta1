package com.googol.downloaders;

import com.googol.model.CrawlResult;
import com.googol.util.TextUtils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Crawler baseado em Jsoup.
 * - Respeita followRedirects
 * - Timeout e maxBodySize razoáveis
 * - Extrai título, texto limpo, termos, snippet e até 50 outlinks absolutos (http/https)
 */
public class WebCrawler {

    private static final String UA =
            "GoogolBot/1.0 (+https://example.edu/SD_Meta1; student crawler)";

    private static final int TIMEOUT_MS     = 8_000;         // 8s
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024; // 2MB
    private static final int MAX_OUTLINKS   = 50;

    public static CrawlResult crawl(String url) {
        CrawlResult r = new CrawlResult();
        r.url = url;

        try {
            Connection.Response resp = Jsoup
                    .connect(url)
                    .userAgent(UA)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)   // queremos ver códigos != 200 também
                    .maxBodySize(MAX_BODY_BYTES)
                    .execute();

            // Só processamos HTML
            String ct = resp.contentType();
            if (ct == null || !ct.toLowerCase().contains("text/html")) {
                r.title    = url;
                r.text     = "";
                r.snippet  = "";
                r.terms    = List.of();
                r.outlinks = List.of();
                return r;
            }

            Document doc = resp.parse();

            String title = safe(doc.title());
            String text  = (doc.body() != null) ? doc.body().text() : "";

            List<String> terms   = TextUtils.tokenize(text);
            String       snippet = TextUtils.makeSnippet(text, terms);

            // Outlinks absolutos (Jsoup resolve "abs:href" usando <base> ou URL da resposta)
            List<String> out = new ArrayList<>();
            Elements links = doc.select("a[href]");
            for (Element a : links) {
                String href = a.attr("abs:href");
                if (isHttp(href)) {
                    out.add(href);
                    if (out.size() >= MAX_OUTLINKS) break;
                }
            }

            r.title    = title.isBlank() ? url : title;
            r.text     = text;
            r.snippet  = snippet;
            r.terms    = terms;
            r.outlinks = out;

        } catch (Exception e) {
            r.title    = url;
            r.text     = "";
            r.snippet  = "download failed: " + e.getClass().getSimpleName();
            r.terms    = List.of();
            r.outlinks = List.of();
        }
        return r;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static boolean isHttp(String href) {
        if (href == null || href.isBlank()) return false;
        String h = href.toLowerCase();
        return h.startsWith("http://") || h.startsWith("https://");
    }
}
