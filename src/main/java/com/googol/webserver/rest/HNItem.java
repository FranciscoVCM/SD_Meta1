package com.googol.webserver.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HNItem(
        Integer id,
        String by,
        Long time,
        String title,
        String url,
        String text,
        Integer descendants,
        Integer score
) {}
