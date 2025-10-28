package com.googol.model;

import java.io.Serializable;

public class SearchQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String terms;
    public final int page;

    public SearchQuery(String terms, int page) {
        this.terms = terms;
        this.page = page;
    }
}
