package com.googol.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;

        public String url;
        public String title;
        public String snippet;

        public int inlinks;
        public int outlinks;
    }

    public List<Item> items = new ArrayList<>();
    public int total = 0;
    public int page = 1;
}
