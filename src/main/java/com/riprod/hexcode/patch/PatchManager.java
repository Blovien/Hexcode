package com.riprod.hexcode.patch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.monitor.AssetMonitor;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PatchManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String DEFER_TO_PACK_NAME = "Riprod:patch";
    private static final String OVERRIDE_PACK_GROUP = "Riprod";
    private static final String OVERRIDE_PACK_NAME = "HexcodePatchOverrides";
    public static final String OVERRIDE_PACK_FULL_NAME = OVERRIDE_PACK_GROUP + ":" + OVERRIDE_PACK_NAME;
    public static final String HYTALOR_OVERRIDES_PACK = "com.hypersonicsharkz:Hytalor-Overrides";
    public static final Path OVERRIDES_TEMP_PATH = PluginManager.MODS_PATH.resolve("Riprod_HexcodePatchOverrides");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private final String ownPackName;

    private final Map<Path, String> patchToTarget = new ConcurrentHashMap<>();
    private volatile boolean monitorInstalled = false;

    public PatchManager(@Nonnull String ownPackName) {
        this.ownPackName = ownPackName;
    }

    public void preLoad() {
        Path manifestPath = OVERRIDES_TEMP_PATH.resolve("manifest.json");
        try {
            Files.deleteIfExists(manifestPath);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[%s] Failed to delete stale override manifest.", ownPackName);
        }
    }

    public synchronized void rebuildAndApply(@Nonnull String reason) {
        if (isDeferredToExternalPlugin()) {
            if (AssetModule.get().getAssetPack(OVERRIDE_PACK_FULL_NAME) != null) {
                try {
                    AssetModule.get().unregisterPack(OVERRIDE_PACK_FULL_NAME);
                    clearOverrideDirectory(false);
                    LOGGER.at(Level.INFO).log("[%s] '%s' appeared; tore down own override pack (%s).", ownPackName, DEFER_TO_PACK_NAME, reason);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).withCause(e).log("[%s] Failed to tear down override pack on defer.", ownPackName);
                }
            } else {
                LOGGER.at(Level.INFO).log("[%s] Detected '%s', deferring patch handling (%s).", ownPackName, DEFER_TO_PACK_NAME, reason);
            }
            return;
        }

        AssetPack ownPack = findPack(ownPackName);
        if (ownPack == null) {
            LOGGER.at(Level.WARNING).log("[%s] Could not locate own AssetPack; skipping patch pass (%s).", ownPackName, reason);
            return;
        }

        boolean alreadyRegistered = AssetModule.get().getAssetPack(OVERRIDE_PACK_FULL_NAME) != null;
        if (alreadyRegistered) {
            try {
                AssetModule.get().unregisterPack(OVERRIDE_PACK_FULL_NAME);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).withCause(e).log("[%s] Failed to unregister existing override pack; continuing.", ownPackName);
            }
        }

        clearOverrideDirectory(false);

        int merged = applyAllPatches(ownPack);
        if (merged == 0) {
            LOGGER.at(Level.INFO).log("[%s] No patches resolved; skipping override pack registration (%s).", ownPackName, reason);
            return;
        }

        registerOverridePack();
        topoSortedReload();
        if (!monitorInstalled) {
            installFileMonitor(ownPack);
            monitorInstalled = true;
        }
        LOGGER.at(Level.INFO).log("[%s] Registered patch override pack '%s' with %d merged asset(s) at %s (%s)",
                ownPackName, OVERRIDE_PACK_FULL_NAME, merged, OVERRIDES_TEMP_PATH, reason);
    }

    // engine bulk-load races on parent->child cascade because documents Set iteration is undefined
    // (AssetStore.java:354-362 + 1492-1511). re-decode each merged file individually in topo order
    // (leaves first) so children's currently-registered path is OVERRIDE by the time parent re-loads.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void topoSortedReload() {
        // Re-load each merged file individually to overwrite any vanilla pollution introduced
        // by AssetStore.loadAssetsFromPaths' loadAllChildren auto-expansion during the bulk
        // AssetPackRegisterEvent path. Skip files whose key has children registered in the
        // store - reloading a parent re-triggers expansion and re-pollutes children. Parent
        // assets get their correct content from the bulk load (no parent => no race for them).
        Path serverRoot = OVERRIDES_TEMP_PATH.resolve("Server");
        if (!Files.isDirectory(serverRoot)) return;
        int reloaded = 0;
        int skippedParent = 0;
        for (String relTarget : patchToTarget.values()) {
            Path mergedFile = OVERRIDES_TEMP_PATH.resolve(relTarget);
            if (!Files.isRegularFile(mergedFile)) continue;
            for (AssetStore store : AssetRegistry.getStoreMap().values()) {
                String storePath = store.getPath();
                if (storePath == null) continue;
                Path storeDir = serverRoot.resolve(storePath);
                if (!mergedFile.startsWith(storeDir)) continue;
                if (!mergedFile.getFileName().toString().endsWith(store.getExtension())) continue;
                Object key = store.decodeFilePathKey(mergedFile);
                if (key != null && hasChildrenInStore(store, key)) {
                    skippedParent++;
                    break;
                }
                try {
                    store.loadAssetsFromPaths(OVERRIDE_PACK_FULL_NAME, List.of(mergedFile), AssetUpdateQuery.DEFAULT, true);
                    reloaded++;
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).withCause(e).log("[%s] Serial reload failed for %s", ownPackName, mergedFile);
                }
                break;
            }
        }
        LOGGER.at(Level.INFO).log("[%s] Serial-reloaded %d leaf asset(s); skipped %d parent asset(s) to avoid re-pollution.",
                ownPackName, reloaded, skippedParent);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean hasChildrenInStore(AssetStore store, Object key) {
        try {
            java.util.Set children = store.getAssetMap().getChildren(key);
            return children != null && !children.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void shutdown() {
        clearOverrideDirectory(true);
    }

    private void clearOverrideDirectory(boolean includingDirectories) {
        if (!Files.isDirectory(OVERRIDES_TEMP_PATH)) return;
        try {
            Files.walkFileTree(OVERRIDES_TEMP_PATH, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (includingDirectories) Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[%s] Failed to clear override pack directory.", ownPackName);
        }
    }

    private int applyAllPatches(@Nonnull AssetPack ownPack) {
        List<Path> patchFiles = collectPatchFiles(ownPack);
        if (patchFiles.isEmpty()) {
            LOGGER.at(Level.INFO).log("[%s] No .patch files found.", ownPackName);
            return 0;
        }
        int merged = 0;
        for (Path patchFile : patchFiles) {
            if (mergeOne(ownPack, patchFile)) merged++;
        }
        return merged;
    }

    private boolean isDeferredToExternalPlugin() {
        return findPack(DEFER_TO_PACK_NAME) != null;
    }

    @Nullable
    private AssetPack findPack(@Nonnull String name) {
        for (AssetPack p : AssetModule.get().getAssetPacks()) {
            if (name.equals(p.getName())) return p;
        }
        return null;
    }

    @Nonnull
    private List<Path> collectPatchFiles(@Nonnull AssetPack pack) {
        List<Path> out = new ArrayList<>();
        Path root = pack.getRoot();
        try {
            Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (PathUtil.isPatchFile(file)) out.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[%s] Failed walking pack root for patches.", ownPackName);
        }
        return out;
    }

    private boolean mergeOne(@Nonnull AssetPack ownPack, @Nonnull Path patchFile) {
        String relPatch = PathUtil.normalizeRelative(ownPack.getRoot(), patchFile);
        String relTarget = PathUtil.patchToTargetRelative(relPatch);
        if (relTarget == null) return false;

        Path basePath = resolveBase(relTarget);
        if (basePath == null) {
            LOGGER.at(Level.WARNING).log("[%s] No base asset for patch %s (looking for %s).",
                    ownPackName, relPatch, relTarget);
            return false;
        }

        JsonObject baseJson = readJson(basePath);
        JsonObject patchJson = readJson(patchFile);
        if (baseJson == null || patchJson == null) return false;

        JsonObject merged = JsonDeepMerge.merge(baseJson, patchJson);
        JsonDeepMerge.stripMergeKey(merged);

        Path outPath = OVERRIDES_TEMP_PATH.resolve(relTarget);
        try {
            Files.createDirectories(outPath.getParent());
            try (Writer w = Files.newBufferedWriter(outPath)) {
                gson.toJson(merged, w);
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("[%s] Failed writing merged asset %s.", ownPackName, relTarget);
            return false;
        }
        patchToTarget.put(patchFile, relTarget);
        return true;
    }

    @Nullable
    private Path resolveBase(@Nonnull String relativeTarget) {
        Path winning = null;
        for (AssetPack p : AssetModule.get().getAssetPacks()) {
            if (p.getName().equals(OVERRIDE_PACK_FULL_NAME)) continue;
            Path candidate = p.getRoot().resolve(relativeTarget);
            if (Files.isRegularFile(candidate)) winning = candidate;
        }
        return winning;
    }

    @Nullable
    private JsonObject readJson(@Nonnull Path file) {
        try {
            String content = Files.readString(file);
            try (JsonReader reader = new JsonReader(new StringReader(content))) {
                reader.setStrictness(Strictness.LENIENT);
                return gson.fromJson(reader, JsonObject.class);
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[%s] Failed to parse JSON: %s", ownPackName, file);
            return null;
        }
    }

    private void registerOverridePack() {
        PluginManifest manifest = new PluginManifest(
                OVERRIDE_PACK_GROUP, OVERRIDE_PACK_NAME,
                Semver.fromString("1.0.0"),
                "Synthesized merged patches for " + ownPackName,
                new ArrayList<>(),
                "",
                null,
                null,
                new HashMap<>(), new HashMap<>(), new HashMap<>(),
                new ArrayList<>(),
                false
        );
        AssetModule.get().registerPack(OVERRIDE_PACK_FULL_NAME, OVERRIDES_TEMP_PATH, manifest, false);
    }

    private void installFileMonitor(@Nonnull AssetPack ownPack) {
        AssetMonitor monitor = AssetModule.get().getAssetMonitor();
        if (monitor == null) {
            LOGGER.at(Level.INFO).log("[%s] AssetMonitor unavailable; skipping hot-reload setup.", ownPackName);
            return;
        }
        Path serverDir = ownPack.getRoot().resolve("Server");
        if (!Files.isDirectory(serverDir)) {
            LOGGER.at(Level.WARNING).log("[%s] Server/ not found in pack root; skipping hot-reload.", ownPackName);
            return;
        }
        monitor.monitorDirectoryFiles(serverDir, new PatchMonitorHandler(this, ownPack));
        LOGGER.at(Level.INFO).log("[%s] Watching for .patch changes in %s", ownPackName, serverDir);
    }

    void onPatchEvent(@Nonnull AssetPack ownPack, @Nonnull Path patchFile) {
        if (!PathUtil.isPatchFile(patchFile)) return;
        if (Files.exists(patchFile)) {
            mergeOne(ownPack, patchFile);
        } else {
            String target = patchToTarget.remove(patchFile);
            if (target != null) deleteOverrideFile(target);
        }
    }

    void onBaseEvent(@Nonnull AssetPack ownPack, @Nonnull Path changedJson) {
        for (Map.Entry<Path, String> entry : patchToTarget.entrySet()) {
            Path patchFile = entry.getKey();
            String target = entry.getValue();
            for (AssetPack p : AssetModule.get().getAssetPacks()) {
                if (p.getName().equals(OVERRIDE_PACK_FULL_NAME)) continue;
                if (p.getRoot().resolve(target).equals(changedJson)) {
                    mergeOne(ownPack, patchFile);
                    break;
                }
            }
        }
    }

    private void deleteOverrideFile(@Nonnull String relTarget) {
        try {
            Files.deleteIfExists(OVERRIDES_TEMP_PATH.resolve(relTarget));
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[%s] Failed to delete override %s.", ownPackName, relTarget);
        }
    }
}
