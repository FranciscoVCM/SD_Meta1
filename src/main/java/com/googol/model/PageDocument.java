package com.googol.model;

import java.io.Serializable;
import java.util.List;

public class PageDocument implements Serializable {
    private static final long serialVersionUID = 1L;

    public String url;
    public String title;
    public String snippet;
    public List<String> outlinks;
}
