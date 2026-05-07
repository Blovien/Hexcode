package com.riprod.hexcode.core.common.imbuement.registry;

import com.riprod.hexcode.core.common.imbuement.asset.ImbuementProfileAsset;

import javax.annotation.Nullable;

public final class ImbuementProfileRegistry {

    private ImbuementProfileRegistry() {
    }

    @Nullable
    public static ImbuementProfileAsset byCategory(String categoryId) {
        if (categoryId == null) return null;
        for (ImbuementProfileAsset profile : ImbuementProfileAsset.getAssetMap().getAssetMap().values()) {
            if (categoryId.equals(profile.getCategoryId())) return profile;
        }
        return null;
    }

    @Nullable
    public static ImbuementProfileAsset first(@Nullable String[] categories) {
        if (categories == null) return null;
        for (String category : categories) {
            ImbuementProfileAsset profile = byCategory(category);
            if (profile != null) return profile;
        }
        return null;
    }
}
