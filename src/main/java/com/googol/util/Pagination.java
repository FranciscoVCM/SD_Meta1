package com.googol.util;

public class Pagination {
    public static int offset(int page, int size) { return Math.max(0, (page - 1) * size); }
}