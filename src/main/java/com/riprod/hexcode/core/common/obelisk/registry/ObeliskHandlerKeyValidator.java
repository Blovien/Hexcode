package com.riprod.hexcode.core.common.obelisk.registry;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;

public final class ObeliskHandlerKeyValidator implements Validator<String> {

    public static final ObeliskHandlerKeyValidator INSTANCE = new ObeliskHandlerKeyValidator();

    private ObeliskHandlerKeyValidator() {
    }

    @Override
    public void accept(String key, @Nonnull ValidationResults results) {
        if (key == null || key.isEmpty()) return;
        if (!ObeliskHandlerRegistry.getAll().containsKey(key)) {
            results.fail("Unknown obelisk handler '" + key + "'. Registered: "
                    + String.join(", ", ObeliskHandlerRegistry.getAll().keySet()));
        }
    }

    @Override
    public void updateSchema(SchemaContext context, @Nonnull Schema target) {
        target.setDescription("Must match a registered hexcode obelisk handler (HexcodeObeliskHandlers).");
    }
}
