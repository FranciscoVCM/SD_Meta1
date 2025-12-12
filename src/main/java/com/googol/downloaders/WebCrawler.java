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

/** Downloader simples usando jsoup. */
public class WebCrawler {

    // identifica-te como crawler académico
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    public static CrawlResult crawl(String url) {
        CrawlResult r = new CrawlResult();
        r.url = url;

        try {
            // 1) Fetch com limites sensatos
            Connection.Response resp = Jsoup
                    .connect(url)
                    .userAgent(UA)
                    .timeout(8000)               // 8s
                    .maxBodySize(2_000_000)      // ~2MB
                    .ignoreContentType(true)     // vamos verificar nós o tipo
                    .followRedirects(true)
                    .execute();

            // 2) Ignorar conteúdos não-HTML
            String ct = resp.contentType() == null ? "" : resp.contentType().toLowerCase();
            if (!ct.contains("text/html")) {
                r.title    = url;
                r.text     = "";
                r.snippet  = "";
                r.terms    = List.of();
                r.outlinks = List.of();
                return r;
            }

            // 3) Parse HTML
            Document doc = resp.parse();

            String title = doc.title();
            String text  = doc.text(); // jsoup já remove tags/scripts/estilos

            // termos/snippet com as tuas utilidades
            List<String> terms   = TextUtils.tokenize(text);
            String snippet       = TextUtils.makeSnippet(text, terms);

            // 4) Extrair ligações absolutas (até 50)
            List<String> out = new ArrayList<>(50);
            Elements links = doc.select("a[href]");
            for (Element a : links) {
                String abs = a.attr("abs:href");
                if (abs == null || abs.isBlank()) continue;
                if (!abs.startsWith("http://") && !abs.startsWith("https://")) continue;
                out.add(abs);
                if (out.size() >= 50) break;
            }

            // 5) Preencher resultado
            r.title    = (title != null && !title.isBlank()) ? title : url;
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
}
