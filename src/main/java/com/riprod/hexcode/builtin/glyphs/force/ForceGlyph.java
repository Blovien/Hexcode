package com.riprod.hexcode.builtin.glyphs.force;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.utils.VelocityUtil;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class ForceGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Force";
    
    @Override
    public String getId() {
        return ID;
    };

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot(ForceGlyphSlots.TARGET, hexContext);
        EntityVar entityVar = HexVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            LOGGER.atWarning().log("Force: target must be Entity");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Force: target must be Entity");
            return;
        }

        Ref<EntityStore> ref = entityVar.getRef(hexContext.getAccessor());
        if (ref == null || !ref.isValid()) {
            LOGGER.atWarning().log("Force: target ref unresolved");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Force: target ref unresolved");
            return;
        }

        HexVar dirInput = glyph.readSlot(ForceGlyphSlots.DIRECTION, hexContext);
        HexVar magInput = glyph.readSlot(ForceGlyphSlots.MAGNITUDE, hexContext);
        double magnitude = HexVarUtil.numberOrDefault(magInput, 20.0);

        try {
            TransformComponent tc = hexContext.getAccessor().getComponent(ref,
                    TransformComponent.getComponentType());
            if (tc != null) {
                Vector3d targetPos = tc.getPosition();
                Vector3d direction = null;
                if (dirInput != null) {
                    direction = HexDirectionUtil.resolveDirection(dirInput, targetPos,
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
