package com.riprod.hexcode.builtin.glyphs.force;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VelocityUtil;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ForceGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Force";
    
    @Override
    public String getId() {
        return ID;
    };

    // force power where mana cost equals the base JSON cost (ManaConsumption: 8)
    // power 16 ≈ a solid shove, costs exactly base mana
    private static final double MANA_REFERENCE_POWER = 16.0;
    // >1 = superlinear: big forces cost disproportionately more
    private static final double MANA_POWER_EXPONENT = 1.5;
    // floor so near-zero forces aren't completely free
    private static final double MANA_MIN_MULTIPLIER = 0.1;

    private double computeForcePower(Glyph glyph, HexContext hexContext) {
        HexVar dirInput = glyph.readSlot(ForceGlyphSlots.DIRECTION, hexContext);
        HexVar magInput = glyph.readSlot(ForceGlyphSlots.MAGNITUDE, hexContext);
        double magnitude = SpellVarUtil.resolveNumberOrDefault(magInput, 20.0);

        Vector3d direction = null;
        if (dirInput != null) {
            direction = SpellVarUtil.resolveDirection(dirInput, null, hexContext.getAccessor());
        }
        if (direction == null) {
            direction = new Vector3d(0, 1, 0);
        }
        Vector3d force = new Vector3d(direction).scale(magnitude);

        return Math.abs(force.getX()) + Math.abs(force.getY()) + Math.abs(force.getZ());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot(ForceGlyphSlots.TARGET, hexContext);
        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        Ref<EntityStore> ref = entityVar.getRef(hexContext.getAccessor());
        if (ref == null || !ref.isValid()) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        HexVar dirInput = glyph.readSlot(ForceGlyphSlots.DIRECTION, hexContext);
        HexVar magInput = glyph.readSlot(ForceGlyphSlots.MAGNITUDE, hexContext);
        double magnitude = SpellVarUtil.resolveNumberOrDefault(magInput, 20.0);

        try {
            TransformComponent tc = hexContext.getAccessor().getComponent(ref,
                    TransformComponent.getComponentType());
            if (tc != null) {
                Vector3d targetPos = tc.getPosition();
                Vector3d direction = null;
                if (dirInput != null) {
                    direction = SpellVarUtil.resolveDirection(dirInput, targetPos,
                            hexContext.getAccessor());
                }
                if (direction == null) {
                    direction = new Vector3d(0, 1, 0);
                }
                Vector3d force = new Vector3d(direction).scale(magnitude);

                VelocityUtil.applyVelocity(ref, force, ChangeVelocityType.Add,
                        new VelocityConfig(), hexContext.getAccessor());

                ForceGlyphStyle.render(targetPos, force, hexContext.getColors(),
                        hexContext.getAccessor());
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("force glyph: could not apply force to entity: %s", e.getMessage());
        }

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
