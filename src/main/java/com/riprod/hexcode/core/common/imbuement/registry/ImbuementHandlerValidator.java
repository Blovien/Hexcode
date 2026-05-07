package com.riprod.hexcode.core.common.imbuement.registry;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;

public final class ImbuementHandlerValidator implements Validator<String> {

    public static final ImbuementHandlerValidator INSTANCE = new ImbuementHandlerValidator();

    private ImbuementHandlerValidator() {
    }

    @Override
    public void accept(String key, @Nonnull ValidationResults results) {
        if (key == null || key.isEmpty()) return;
        if (!ImbuementHandlerRegistry.keys().contains(key)) {
            results.fail("Unknown imbuement handler '" + key + "'. Registered: "
                    + String.join(", ", ImbuementHandlerRegistry.keys()));
        }
    }

    @Override
    public void updateSchema(SchemaContext context, @Nonnull Schema target) {
        target.setDescription("Must match a registered imbuement handler (HexcodeImbuementHandlers).");
    }
}
