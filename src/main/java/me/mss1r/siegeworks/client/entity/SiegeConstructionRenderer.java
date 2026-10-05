package me.mss1r.siegeworks.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import me.mss1r.axiomata.blueprint.client.renderer.ConstructionHighlightRenderType;
import me.mss1r.axiomata.structure.StructureSections;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
//? if forge {
/*import software.bernie.geckolib.core.animatable.GeoAnimatable;
*///?} else {
import software.bernie.geckolib.animatable.GeoAnimatable;
//?}
//? if forge {
/*import software.bernie.geckolib.core.object.Color;
*///?} else {
import software.bernie.geckolib.util.Color;
//?}
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Set;

public abstract class SiegeConstructionRenderer<T extends AbstractSiegeEntity & GeoAnimatable>
        extends GeoEntityRenderer<T> {
    private static final float GHOST_ALPHA = 0.20F;
    private static final Color GHOST_TINT = Color.ofRGBA(0.62F, 0.74F, 1.0F, 1.0F);

    private static final float ACTIVE_BREATH_TICKS = 40.0F;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> warnedAbout = ConcurrentHashMap.newKeySet();

    private StructureSections sections;
    private Set<String> builtSections = Set.of();
    private MultiBufferSource bufferSource;
    private RenderType builtType;
    private RenderType ghostType;
    private RenderType activeType;
    private String activeSection = "";
    private float activePulseHigh;
    private float activePulseLow;
    private boolean building;

    protected SiegeConstructionRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        building = !entity.isFullyBuilt();
        sections = building ? sectionsFor(entity) : null;
        building = building && sections != null;
        bufferSource = buffer;
        builtType = building ? getRenderType(entity, getTextureLocation(entity), buffer, partialTick) : null;
        ghostType = building ? RenderType.entityTranslucent(getTextureLocation(entity)) : null;
        activeType = building ? ConstructionHighlightRenderType.of(getTextureLocation(entity)) : null;
        BlueprintConstructionVisuals.State visualState = building
                ? BlueprintConstructionVisuals.state(entity.buildBlueprintId(), entity.builtSections())
                : new BlueprintConstructionVisuals.State(Set.of(), "", 0, 0);
        builtSections = visualState.builtSections();
        activeSection = visualState.activeSection();
        double phase = (entity.tickCount + partialTick) * Math.PI * 2.0D / ACTIVE_BREATH_TICKS;
        float breath = (float) (0.5D - 0.5D * Math.cos(phase));
        int packedPulse = Math.round(breath * 65535.0F);
        activePulseHigh = ((packedPulse >>> 8) & 0xFF) / 255.0F;
        activePulseLow = (packedPulse & 0xFF) / 255.0F;

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        if (building) {
            renderStageSign(entity, poseStack, buffer, packedLight);
        }

        building = false;
        builtType = null;
        ghostType = null;
        activeType = null;
        bufferSource = null;
    }

    @Override
    //? if forge {
    /*public void renderRecursively(PoseStack poseStack, T entity, GeoBone bone, RenderType renderType,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
    *///?} else {
    public void renderRecursively(PoseStack poseStack, T entity, GeoBone bone, RenderType renderType,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int packedColor) {
    //?}
        var previous = building ? bone.saveSnapshot() : null;
        if (building) {
            // Construction hit cubes are exported in the authored pose, not a loading animation.
            var initial = bone.getInitialSnapshot();
            bone.updateRotation(initial.getRotX(), initial.getRotY(), initial.getRotZ());
            bone.updatePosition(initial.getOffsetX(), initial.getOffsetY(), initial.getOffsetZ());
            bone.updateScale(initial.getScaleX(), initial.getScaleY(), initial.getScaleZ());
        }
        try {
            //? if forge {
            /*super.renderRecursively(poseStack, entity, bone, renderType, buffers, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            *///?} else {
            super.renderRecursively(poseStack, entity, bone, renderType, buffers, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, packedColor);
            //?}
        } finally {
            if (previous != null) {
                bone.updateRotation(previous.getRotX(), previous.getRotY(), previous.getRotZ());
                bone.updatePosition(previous.getOffsetX(), previous.getOffsetY(), previous.getOffsetZ());
                bone.updateScale(previous.getScaleX(), previous.getScaleY(), previous.getScaleZ());
            }
        }
    }

    @Override
    //? if forge {
    /*public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
    *///?} else {
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                  int packedLight, int packedOverlay, int packedColor) {
    //?}
        if (!building) {
            //? if forge {
            /*super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            *///?} else {
            super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, packedColor);
            //?}
            return;
        }

        if (bone.isHidden()) {
            return;
        }

        List<GeoCube> cubes = bone.getCubes();
        for (int index = 0; index < cubes.size(); index++) {
            if (sectionOf(bone.getName(), index) == null) {
                continue;
            }
            boolean built = isBuilt(bone.getName(), index);
            poseStack.pushPose();
            if (built) {
                VertexConsumer builtBuffer = bufferSource.getBuffer(builtType);
                //? if forge {
                /*renderCube(poseStack, cubes.get(index), builtBuffer, packedLight, packedOverlay,
                        red, green, blue, alpha);
                *///?} else {
                renderCube(poseStack, cubes.get(index), builtBuffer, packedLight, packedOverlay, packedColor);
                //?}
            } else if (ghostType != null) {
                boolean active = isActive(bone.getName(), index);
                if (active) {
                    VertexConsumer activeBuffer = bufferSource.getBuffer(activeType);
                    //? if forge {
                    /*renderCube(poseStack, cubes.get(index), activeBuffer, packedLight, packedOverlay,
                            activePulseHigh, activePulseLow, 0.0F, 1.0F);
                    *///?} else {
                    renderCube(poseStack, cubes.get(index), activeBuffer, packedLight, packedOverlay,
                            packColor(activePulseHigh, activePulseLow, 0.0F, 1.0F));
                    //?}
                } else {
                    VertexConsumer ghost = bufferSource.getBuffer(ghostType);
                    //? if forge {
                    /*renderCube(poseStack, cubes.get(index), ghost, packedLight, packedOverlay,
                            GHOST_TINT.getRedFloat(), GHOST_TINT.getGreenFloat(),
                            GHOST_TINT.getBlueFloat(), GHOST_ALPHA);
                    *///?} else {
                    renderCube(poseStack, cubes.get(index), ghost, packedLight, packedOverlay,
                            packColor(GHOST_TINT.getRedFloat(), GHOST_TINT.getGreenFloat(),
                                    GHOST_TINT.getBlueFloat(), GHOST_ALPHA));
                    //?}
                }
            }
            poseStack.popPose();
        }
    }

    private static int packColor(float red, float green, float blue, float alpha) {
        int a = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        int r = Math.round(Mth.clamp(red, 0.0F, 1.0F) * 255.0F);
        int g = Math.round(Mth.clamp(green, 0.0F, 1.0F) * 255.0F);
        int b = Math.round(Mth.clamp(blue, 0.0F, 1.0F) * 255.0F);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private void renderStageSign(T entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlueprintDefinition recipe = BlueprintDefinitions.get(entity.buildBlueprintId());
        if (recipe == null || recipe.stages().isEmpty()) {
            return;
        }
        int stageIndex = Math.min(entity.builtSections(), recipe.stageCount() - 1);
        BlueprintDefinition.Stage stage = recipe.stages().get(stageIndex);

        Component line = Component.empty()
                .append(BlueprintConstructionVisuals.stageName(entity.buildBlueprintId(), stage.section()))
                .append(Component.literal(String.format("  %d/%d  ·  %d/%d",
                        entity.buildHits(), Math.max(1, entity.buildStageHits()),
                        stageIndex + 1, recipe.stageCount())));

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.7D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        Font font = getFont();
        float x = -font.width(line) / 2.0F;
        font.drawInBatch(line, x, 0.0F, 0xFFE8DFC8, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0x40000000, packedLight);
        poseStack.popPose();
    }

    private boolean isActive(String bone, int cube) {
        if (activeSection.isEmpty()) {
            return false;
        }
        StructureSections.Section section = sectionOf(bone, cube);
        return section != null && section.name().equals(activeSection);
    }

    private StructureSections.Section sectionOf(String bone, int cube) {
        for (StructureSections.Section section : sections.sections()) {
            if (section.contains(bone, cube)) {
                return section;
            }
        }
        return null;
    }

    private boolean isBuilt(String bone, int cube) {
        StructureSections.Section section = sectionOf(bone, cube);
        if (section != null) {
            return builtSections.contains(section.name());
        }
        return false;
    }

    private StructureSections sectionsFor(T entity) {
        ResourceLocation model = getGeoModel().getModelResource(entity);
        String path = model.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash < 0 ? path : path.substring(slash + 1);
        if (name.endsWith(".geo.json")) {
            name = name.substring(0, name.length() - ".geo.json".length());
        }
        StructureSections sections = BlueprintConstructionVisuals.sections(
                MinecraftVersionCompat.id(model.getNamespace(), name));
        if (sections == null || matchesLoadedModel(entity, sections, model)) {
            return sections;
        }
        if (warnedAbout.add(model)) {
            LOGGER.warn("Build sections for {} do not match the loaded model; drawing it finished", model);
        }
        return null;
    }

    private boolean matchesLoadedModel(T entity, StructureSections sections, ResourceLocation model) {
        Map<String, Integer> loaded = new HashMap<>();
        for (GeoBone bone : getGeoModel().getBakedModel(model).topLevelBones()) {
            countCubes(bone, loaded);
        }
        return sections.matches(loaded);
    }

    private static void countCubes(GeoBone bone, Map<String, Integer> counts) {
        if (!bone.getCubes().isEmpty()) {
            counts.put(bone.getName(), bone.getCubes().size());
        }
        for (GeoBone child : bone.getChildBones()) {
            countCubes(child, counts);
        }
    }
}
