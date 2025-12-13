package com.googol.webserver.rest;

import java.util.ArrayList;
import java.util.List;

public class HNSearchResult {
    public String term = "";
    public int page = 0;
    public int totalPages = 0;

    public List<HNItem> items = new ArrayList<>();
}
