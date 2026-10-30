package me.mss1r.siegeworks.fabric.mixin;

import me.mss1r.siegeworks.registry.LegacyIdAliases;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Resolve historical names without registering duplicate values or changing registry IDs. */
@Mixin(MappedRegistry.class)
public abstract class RegistryAliasesMixin<T> {
    @ModifyVariable(method={
            "get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;",
            "containsKey(Lnet/minecraft/resources/ResourceLocation;)Z"
    }, at=@At("HEAD"), argsOnly=true)
    private ResourceLocation siegeworks$resolveName(ResourceLocation name) {
        return LegacyIdAliases.resolve(((Registry<?>)(Object)this).key(), name);
    }

    @ModifyVariable(method={
            "get(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
            "getHolder(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
            "containsKey(Lnet/minecraft/resources/ResourceKey;)Z"
    }, at=@At("HEAD"), argsOnly=true)
    private ResourceKey<T> siegeworks$resolveKey(ResourceKey<T> key) {
        if (key == null) return null;
        var registry = ((Registry<T>)(Object)this).key();
        ResourceLocation resolved = LegacyIdAliases.resolve(registry, key.location());
        return resolved.equals(key.location()) ? key : ResourceKey.create(registry, resolved);
    }
}
