package me.mss1r.siegeworks.client.firearm;

import org.joml.Vector3f;

/** Nearest point on the intersection of the two rigid arms' reach spheres. */
final class FirearmAim {
    private FirearmAim() {}

    static Vector3f position(Vector3f a, Vector3f b, float radiusA, float radiusB, Vector3f preferred) {
        Vector3f axis = new Vector3f(b).sub(a);
        float distance = axis.length();
        if (distance < 1e-5F) {
            Vector3f direction = new Vector3f(preferred).sub(a);
            if (direction.lengthSquared() < 1e-8F) direction.set(0,0,1);
            return direction.normalize(radiusA).add(a);
        }
        axis.div(distance);
        float along = (radiusA*radiusA-radiusB*radiusB+distance*distance)/(2*distance);
        Vector3f center = new Vector3f(axis).mul(along).add(a);
        float height = (float)Math.sqrt(Math.max(0,radiusA*radiusA-along*along));
        Vector3f side = new Vector3f(preferred).sub(center);
        side.sub(new Vector3f(axis).mul(side.dot(axis)));
        if (side.lengthSquared()<1e-8F) {
            side.set(Math.abs(axis.y)<.9F ? new Vector3f(0,1,0) : new Vector3f(0,0,1));
            side.sub(new Vector3f(axis).mul(side.dot(axis)));
        }
        return side.normalize(height).add(center);
    }
}
