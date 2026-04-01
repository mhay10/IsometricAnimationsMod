package com.isoanimations.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class SodiumMixinPlugin implements IMixinConfigPlugin {
    private static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Vanilla Chunk Mixin: skip if Sodium installed
        if (mixinClassName.endsWith("RenderSectionRegionMixin")) {
            LOGGER.info("RegionSectionRegionMixin --> Sodium detected: {}", SODIUM_LOADED);
            return !SODIUM_LOADED;
        }

        // Sodium Chunk Mixin: skip if Sodium not installed
        if (mixinClassName.endsWith("LevelSliceMixin")) {
            LOGGER.info("LevelSliceMixin --> Sodium detected: {}", SODIUM_LOADED);
            return SODIUM_LOADED;
        }

        // Apply every other mixins
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
