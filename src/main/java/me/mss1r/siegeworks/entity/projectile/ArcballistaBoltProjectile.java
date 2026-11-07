package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ArcballistaBoltProjectile extends AbstractBoltProjectile {
    public ArcballistaBoltProjectile(EntityType<? extends ArcballistaBoltProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ArcballistaBoltProjectile(EntityType<? extends ArcballistaBoltProjectile> entityType, LivingEntity shooter, Level level) {
        super(entityType, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return SiegeworksItems.ARCBALLISTA_BOLT.get();
    }

}
