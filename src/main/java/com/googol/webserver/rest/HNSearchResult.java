package com.googol.webserver.rest;

import java.util.ArrayList;
import java.util.List;

public class HNSearchResult {
    public String term;
    public int page;
    public int totalPages;

    public List<HNItem> items = new ArrayList<>();
}
