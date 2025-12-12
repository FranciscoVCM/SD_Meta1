package com.googol.webserver.rest;

public record HNItem(
        String title,
        String url,
        String author,
        int points,
        int comments,
        String createdAt
) {}
