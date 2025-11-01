package com.googol.model;

import java.io.Serializable;

public class StatsSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public int pagesIndexed;
    public int urlsInQueue;
    public int activeDownloaders;
    public int numDocs;
    public int numTerms;
    public int numPostings;
}
