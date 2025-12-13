package com.googol.downloaders;

import com.googol.model.CrawlResult;
import com.googol.util.TextUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class WebCrawler {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/122 Safari/537.36";

    /**
     * Download, parse, extract metadata, snippet, outlinks.
     */
    public static CrawlResult crawl(String url) {

        CrawlResult r = new CrawlResult();
        r.url = url;

        try {
            Connection.Response resp = Jsoup
                    .connect(url)
                    .userAgent(UA)
                    .timeout(20000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .maxBodySize(10_000_000)
                    .execute();

            String ct = Optional.ofNullable(resp.contentType()).orElse("").toLowerCase();
            if (!ct.contains("html") && !ct.contains("xml"))
                return emptyResult(r, url);

            Document doc = resp.parse();

            /* ============================
               TITLE
            ============================ */
            r.title = Optional.ofNullable(doc.title()).orElse(url);
            if (r.title.isBlank()) r.title = url;

            /* ============================
               RICH TEXT EXTRACTION
            ============================ */
            StringBuilder rich = new StringBuilder();

            extractText(rich, doc.select("meta[name=description]"), "content");
            extractText(rich, doc.select("h1,h2,h3,h4"));
            extractText(rich, doc.select("p,li"));
            extractText(rich, doc.select("article"));
            extractText(rich, doc.select("section"));
            extractText(rich, doc.select("div"));

            String text = rich.toString().replaceAll("\\s+", " ").trim();
            r.text = text;

            /* ============================
               TERMS + SNIPPET
            ============================ */
            r.terms = TextUtils.tokenize(text);
            r.snippet = TextUtils.makeSnippet(text, r.terms);

            /* ============================
               OUTLINKS
            ============================ */
            List<String> out = new ArrayList<>(100);
            Elements links = doc.select("a[href]");

            for (Element a : links) {
                String abs = a.attr("abs:href");
                if (abs.startsWith("http") && abs.length() < 400)
                    out.add(abs);
                if (out.size() >= 100) break;
            }

            r.outlinks = out;
            return r;

        } catch (Exception e) {
            return emptyResult(r, url);
        }
    }

    /* ==========================================================
                     INTERNAL EXTRACTION HELPERS
       ========================================================== */

    private static void extractText(StringBuilder sb, Elements els) {
        for (Element e : els) {
            String t = e.text();
            if (t.length() > 2)
                sb.append(' ').append(t);
        }
    }

    private static void extractText(StringBuilder sb, Elements els, String attr) {
        for (Element e : els) {
            String t = e.attr(attr);
            if (t.length() > 2)
                sb.append(' ').append(t);
        }
    }

    private static CrawlResult emptyResult(CrawlResult r, String url) {
        r.title = url;
        r.text = "";
        r.terms = List.of();
        r.outlinks = List.of();
        r.snippet = "";
        return r;
    }
}
