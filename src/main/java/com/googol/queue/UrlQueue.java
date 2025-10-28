package com.googol.queue;

import java.util.concurrent.*;

public class UrlQueue implements QueueInterface {
    private final BlockingQueue<String> q = new LinkedBlockingQueue<>();
    @Override public void push(String url) { q.offer(url); }
    @Override public String pop() { try { return q.take(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; } }
}