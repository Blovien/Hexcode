package com.riprod.hexcode.core.common.hexes.saved;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.util.BsonUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SavedHexWriter {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String DISK_PATH = "Server/Hexcode/SavedHexes";

    private SavedHexWriter() {}

    public static final class Result {
        public final boolean success;
        @Nullable public final String error;
        @Nullable public final Path path;

        private Result(boolean success, @Nullable String error, @Nullable Path path) {
            this.success = success;
            this.error = error;
            this.path = path;
        }

        public static Result ok(Path path) { return new Result(true, null, path); }
        public static Result fail(String error) { return new Result(false, error, null); }
    }

    @Nonnull
    public static Result writeSavedHex(@Nonnull String packName, @Nonnull SavedHexAsset asset) {
        AssetPack pack = AssetModule.get().getAssetPack(packName);
        if (pack == null) {
            logAvailablePacks(packName);
            return Result.fail("Asset pack '" + packName + "' not found (see server log for available packs)");
        }
        if (pack.isImmutable()) {
            return Result.fail("Asset pack '" + packName + "' is immutable (likely a zip/jar) and cannot be written to");
        }

        Path target = pack.getRoot().resolve(DISK_PATH + "/" + asset.getId() + ".json");

        try {
            BsonUtil.writeSync(target, SavedHexAsset.CODEC, asset, LOGGER);
            return Result.ok(target);
        } catch (IOException e) {
            return Result.fail("Failed to write saved hex: " + e.getMessage());
        }
    }

    @Nonnull
    public static Result deleteSavedHex(@Nonnull String packName, @Nonnull String id) {
        AssetPack pack = AssetModule.get().getAssetPack(packName);
        if (pack == null) {
            logAvailablePacks(packName);
            return Result.fail("Asset pack '" + packName + "' not found");
        }
        if (pack.isImmutable()) {
            return Result.fail("Asset pack '" + packName + "' is immutable");
        }

        Path target = pack.getRoot().resolve(DISK_PATH + "/" + id + ".json");
        try {
            if (!Files.deleteIfExists(target)) {
                return Result.fail("Saved hex file not found at " + target);
            }
            return Result.ok(target);
        } catch (IOException e) {
            return Result.fail("Failed to delete saved hex: " + e.getMessage());
        }
    }

    private static void logAvailablePacks(@Nonnull String requested) {
        LOGGER.atWarning().log("Pack must be one of the following (requested: '" + requested + "'):");
        for (AssetPack p : AssetModule.get().getAssetPacks()) {
            LOGGER.atWarning().log("  - '" + p.getName() + "' (immutable=" + p.isImmutable() + ", root=" + p.getRoot() + ")");
        }
    }
}
