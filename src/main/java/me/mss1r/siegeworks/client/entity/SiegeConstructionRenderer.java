package me.mss1r.siegeworks.client.entity;

import me.mss1r.axiomata.blueprint.client.renderer.ConstructionGeoRenderer;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? if forge {
/*import software.bernie.geckolib.core.animatable.GeoAnimatable;
*///?} else {
import software.bernie.geckolib.animatable.GeoAnimatable;
//?}
import software.bernie.geckolib.model.GeoModel;

/** Siege machines draw their unfinished builds through Axiomata's construction renderer. */
public abstract class SiegeConstructionRenderer<T extends AbstractSiegeEntity & GeoAnimatable>
        extends ConstructionGeoRenderer<T> {
    protected SiegeConstructionRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }
}
