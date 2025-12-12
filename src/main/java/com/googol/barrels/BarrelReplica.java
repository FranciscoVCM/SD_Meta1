package com.googol.barrels;

import com.googol.index.InvertedIndex;
import com.googol.model.CrawlResult;
import com.googol.model.PageDocument;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;
import com.googol.util.TextUtils;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BarrelReplica extends UnicastRemoteObject implements Barrel {

    private String name;
    private InvertedIndex index = new InvertedIndex();

    private final Path snapshotPath;
    private static final int SAVE_EVERY = 100;
    private int appendedSinceSave = 0;

    public BarrelReplica(String name, String snapshotFile) throws RemoteException {
        super();
        this.name = name;
        this.snapshotPath = Path.of(snapshotFile);
        loadIfExists();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                synchronized (this) { persist(); }
            } catch (Exception ignored) {}
        }));
    }

    @Override
    public synchronized String getName() throws RemoteException {
        return name;
    }

    @Override
    public synchronized void append(CrawlResult r) throws RemoteException {
        if (r == null || r.url == null) return;

        PageDocument doc = new PageDocument();
        doc.url = r.url;
        doc.normalizedUrl = TextUtils.normalizeUrl(r.url);
        doc.title = (r.title == null || r.title.isBlank()) ? r.url : r.title;
        doc.text = (r.text == null) ? "" : r.text;
        doc.snippet = r.snippet;
        doc.outlinks = r.outlinks;

        index.add(doc);

        if (++appendedSinceSave >= SAVE_EVERY) {
            appendedSinceSave = 0;
            persist();
        }
    }

    @Override
    public synchronized SearchResult search(SearchQuery q) throws RemoteException {
        long start = System.currentTimeMillis();
        SearchResult r = index.search(q);
        long end = System.currentTimeMillis();
        r.lastSearchMs = end - start;
        return r;
    }

    @Override
    public synchronized int inlinks(String url) throws RemoteException {
        return index.inlinks(url);
    }

    @Override
    public synchronized List<String> backlinks(String url) throws RemoteException {
        return index.backlinks(url);
    }

    @Override
    public synchronized StatsSnapshot barrelStats() throws RemoteException {
        StatsSnapshot s = index.stats();
        return s;
    }

    private void persist() {
        try {
            Files.createDirectories(snapshotPath.getParent() == null ?
                    Path.of(".") : snapshotPath.getParent());

            try (ObjectOutputStream out =
                         new ObjectOutputStream(Files.newOutputStream(snapshotPath))) {
                out.writeObject(index);
            }

            System.out.println("[Barrel " + name + "] Saved snapshot → " + snapshotPath);

        } catch (Exception e) {
            System.err.println("[Barrel] snapshot failed: " + e);
        }
    }

    private void loadIfExists() {
        try {
            if (Files.exists(snapshotPath)) {
                try (ObjectInputStream in =
                             new ObjectInputStream(Files.newInputStream(snapshotPath))) {
                    Object obj = in.readObject();
                    if (obj instanceof InvertedIndex loaded) {
                        this.index = loaded;
                        System.out.println("[Barrel " + name + "] Restored snapshot: " +
                                snapshotPath.toAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Barrel] snapshot load failed: " + e);
        }
    }
}
