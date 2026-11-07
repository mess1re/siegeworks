package me.mss1r.siegeworks.client.firearm;

import org.joml.Vector3f;

/** Visual fit in player-model pixels; it does not change ammunition or projectile physics. */
public final class FirearmLoadingFit {
    private FirearmLoadingFit() {}

    private static float smooth(float value) {
        float t = Math.max(0, Math.min(1, value));
        return t*t*(3-2*t);
    }

    public static Vector3f position(Vector3f held, Vector3f muzzle, Vector3f axis) {
        Vector3f delta = new Vector3f(held).sub(muzzle);
        float alignment = smooth((1.25F-delta.length())/.8F);
        return new Vector3f(held).lerp(new Vector3f(axis).mul(delta.dot(axis)).add(muzzle), alignment);
    }

    public static float diameter(float distance, float heldDiameter, float boreDiameter) {
        float fit = smooth((2.5F-distance)/1.75F);
        float fittedDiameter = Math.min(heldDiameter, boreDiameter*.8F);
        return heldDiameter + (fittedDiameter-heldDiameter)*fit;
    }
}
