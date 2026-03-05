package com.riprod.hexcode.core.state.crafting.utils;

import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;

public class CraftingGlyphNodeSpawner {

        public static final float NODE_OFFSET_Y = 0f;
        private static final double NODE_SCALE = 0.05;

        public static Ref<EntityStore> spawnNodeForGlyph(CommandBuffer<EntityStore> buffer,
                        Ref<EntityStore> glyphRef, EffectComponent effect, Vector3d glyphPos) {
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

                Vector3d nodePos = new Vector3d(glyphPos.x, glyphPos.y + NODE_OFFSET_Y, glyphPos.z);
                holder.addComponent(TransformComponent.getComponentType(),
                                new TransformComponent(nodePos, new Vector3f(0, 0, 0)));

                NodeComponent node = new NodeComponent();
                node.setParentGlyphRef(glyphRef);
                holder.addComponent(NodeComponent.getComponentType(), node);

                Box nodeBox = new Box(-NODE_SCALE, -NODE_SCALE, -NODE_SCALE,
                                NODE_SCALE, NODE_SCALE, NODE_SCALE);
                holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(nodeBox));

                holder.addComponent(UUIDComponent.getComponentType(),
                                new UUIDComponent(UUID.randomUUID()));
                holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

                int networkId = buffer.getExternalData().takeNextNetworkId();
                holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

                // ModelAsset anchor = ModelAsset.getAssetMap().getAsset("Selection_Anchor");
                // Model model = Model.createScaledModel(anchor, 1.0f);

                // holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                // holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));


                holder.addComponent(DebugComponent.getComponentType(),
                                new DebugComponent(DebugShape.Sphere, new Vector3f(0.8f, 0.8f, 0.2f),
                                                NODE_SCALE * 2.5, 2.0f));
                holder.addComponent(HoverableComponent.getComponentType(),
                                new HoverableComponent(HoverableType.NODE));

                Ref<EntityStore> nodeRef = buffer.addEntity(holder, AddReason.SPAWN);

                effect.setNodeRef(nodeRef);
                return nodeRef;
        }
}
