package com.googol.barrels;

import com.googol.index.InvertedIndex;
import com.googol.model.CrawlResult;
import com.googol.model.PageDocument;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BarrelReplica extends UnicastRemoteObject implements Barrel {

    private final String name;
    private InvertedIndex index = new InvertedIndex();
    private final Path snapshotPath;

    /** Só salva snapshot a cada N páginas */
    private static final int SAVE_EVERY = 1000;
    private int appendedSinceSave = 0;

    public BarrelReplica(String name, String snapshotFile) throws RemoteException {
        super();
        this.name = name;
        this.snapshotPath = Path.of(snapshotFile);

        loadIfExists();

        // Snapshot final ao encerrar
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                synchronized (this) { persist(); }
            } catch (Exception ignored) {}
        }));
    }

    @Override
    public String getName() {
        return name;
    }

    /** ============================================
     * APPEND DE DOCUMENTOS
     * ============================================ */
    @Override
    public synchronized void append(CrawlResult r) {
        if (r == null || r.url == null) return;

        PageDocument doc = new PageDocument();
        doc.url      = r.url;
        doc.title    = (r.title == null || r.title.isBlank()) ? r.url : r.title;
        doc.snippet  = r.snippet;
        doc.text     = r.text;
        doc.outlinks = r.outlinks;

        index.add(doc);

        if (++appendedSinceSave >= SAVE_EVERY) {
            appendedSinceSave = 0;
            persist();
        }
    }

    /** ============================================
     * PESQUISA (com medição de latência)
     * ============================================ */
    @Override
    public synchronized SearchResult search(SearchQuery q) throws RemoteException {
        long start = System.currentTimeMillis();
        SearchResult r = index.search(q);
        long end = System.currentTimeMillis();

        r.lastSearchMs = end - start;
        return r;
    }

    /** ============================================
     * INLINKS / BACKLINKS
     * ============================================ */
    @Override
    public synchronized int inlinks(String url) throws RemoteException {
        return index.inlinks(url);
    }

    @Override
    public synchronized List<String> backlinks(String url) throws RemoteException {
        return index.backlinks(url);
    }

    /** ============================================
     * ESTATÍSTICAS DO BARREL
     * ============================================ */
    @Override
    public synchronized StatsSnapshot barrelStats() throws RemoteException {
        return index.stats();
    }

    /** ============================================
     * SNAPSHOT (persistência)
     * ============================================ */
    private void persist() {
        try {
            if (snapshotPath.getParent() != null)
                Files.createDirectories(snapshotPath.getParent());

            // Escrita temporária para garantir atomicidade
            Path tmp = snapshotPath.resolveSibling(snapshotPath.getFileName() + ".tmp");

            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(index);
            }

            Files.move(tmp, snapshotPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            System.out.println("[Barrel " + name + "] Snapshot saved.");

        } catch (Exception e) {
            System.err.println("[Barrel " + name + "] Snapshot failed: " + e);
        }
    }

    /** ============================================
     * RECUPERAR SNAPSHOT
     * ============================================ */
    private void loadIfExists() {
        try {
            if (!Files.exists(snapshotPath)) return;

            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(snapshotPath))) {
                Object obj = in.readObject();
                if (obj instanceof InvertedIndex loaded) {
                    index = loaded;
                    System.out.println("[Barrel " + name + "] Snapshot restored.");
                }
            }

        } catch (Exception e) {
            System.err.println("[Barrel " + name + "] Snapshot load failed: " + e);
        }
    }
}
