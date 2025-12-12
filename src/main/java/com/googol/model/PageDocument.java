package com.googol.model;

import java.io.Serializable;
import java.util.List;

public class PageDocument implements Serializable {
    private static final long serialVersionUID = 1L;

    public String url;              // URL original
    public String normalizedUrl;    // URL normalizada (sem protocolo, sem #anchors, lowercase)
    public String title;
    public String snippet;
    public String text;
    public List<String> outlinks;

    public int inlinks;
    public int outlinksCount;

    public long timestamp;          // usado para ranking determinístico
}
