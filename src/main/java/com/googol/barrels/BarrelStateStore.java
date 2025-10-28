package com.googol.barrels;

import java.nio.file.Path;

public class BarrelStateStore {
    private final Path baseDir;
    public BarrelStateStore(Path baseDir) { this.baseDir = baseDir; }
    public void save(Object state) { /* TODO: persistir */ }
    public Object load() { /* TODO: recuperar */ return null; }
}