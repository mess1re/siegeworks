package me.mss1r.siegeworks.gameplay.movement.ladder;

import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureGeometry;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Body-sized queries shared by ladder staging, traversal and landing. Never moves the query entity. */
final class LadderTraversalCollision {
    private static final double SKIN = 1.0E-5D;

    private LadderTraversalCollision() {}

    static AABB bodyAt(Entity entity, Vec3 feet) {
        return entity.getBoundingBox().move(feet.subtract(entity.position())).deflate(SKIN);
    }

    static boolean clear(Entity entity, Vec3 feet) {
        return clear(entity, bodyAt(entity, feet));
    }

    private static boolean clear(Entity entity, AABB box) {
        if (!entity.level().getWorldBorder().isWithinBounds(box)
                || entity.level().getBlockCollisions(entity, box).iterator().hasNext()) return false;
        for (var structure : StructureCollisionSystem.structuresNear(entity, box)) {
            if (StructureCollisionResolver.intersects(structure, StructureGeometry.solid(structure), box, 0)) return false;
        }
        return true;
    }

    static boolean passage(Entity entity, Vec3 from, Vec3 to) {
        Vec3 wanted = to.subtract(from);
        AABB box = bodyAt(entity, from);
        AABB swept = box.expandTowards(wanted);
        // Vanilla's movement resolver applies separate axis moves. A long diagonal can be
        // accepted as X-then-Z even though following that diagonal in small steps hits a corner.
        // Sweep the body itself instead: ray vs Minkowski-expanded obstacle is exact for AABBs.
        for (var shape : entity.level().getBlockCollisions(entity, swept)) {
            for (AABB obstacle : shape.toAabbs()) if (sweepHits(box, wanted, obstacle)) return false;
        }
        for (var shape : entity.level().getEntityCollisions(entity, swept)) {
            for (AABB obstacle : shape.toAabbs()) if (sweepHits(box, wanted, obstacle)) return false;
        }
        for (var structure : StructureCollisionSystem.structuresNear(entity, swept)) {
            // Ask for the first impact, not a final displacement that may include sliding.
            if (StructureCollisionResolver.resolve(structure, box, wanted, 0).allowedTime() < 1.0D - 1.0E-7D)
                return false;
        }
        return clear(entity, to);
    }

    private static boolean sweepHits(AABB body, Vec3 movement, AABB obstacle) {
        AABB expanded = obstacle.inflate(body.getXsize() * .5D, body.getYsize() * .5D, body.getZsize() * .5D);
        Vec3 center = body.getCenter();
        return expanded.contains(center) || expanded.clip(center, center.add(movement)).isPresent();
    }

    /** A flat, supported approach can use normal steering without scheduling another path search. */
    static boolean walkable(Entity entity, Vec3 from, Vec3 to) {
        if (Math.abs(from.y - to.y) > .1D || !passage(entity, from, to)) return false;
        int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) / .5D));
        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.lerp(to, (double) i / steps);
            Vec3 floor = standing(entity, point, point.y - .1D);
            if (floor == null || Math.abs(floor.y - point.y) > .1D) return false;
        }
        return true;
    }

    static String describeStanding(Entity entity, Vec3 feet, double width, double height) {
        AABB box = new AABB(feet.x - width / 2, feet.y + .001, feet.z - width / 2,
                feet.x + width / 2, feet.y + height, feet.z + width / 2);
        var floor = net.minecraft.core.BlockPos.containing(feet).below();
        var structures = new java.util.ArrayList<String>();
        for (var structure : StructureCollisionSystem.structuresNear(entity, box)) {
            if (StructureCollisionResolver.intersects(structure, StructureGeometry.solid(structure), box, 0))
                structures.add(structure.getClass().getSimpleName() + ":" + StructureCollisionSystem.worldBounds(structure));
        }
        return "floor=" + entity.level().getBlockState(floor) + " at " + floor
                + " blocks=" + java.util.stream.StreamSupport.stream(entity.level().getBlockCollisions(entity, box).spliterator(), false)
                .flatMap(shape -> shape.toAabbs().stream()).limit(4).toList() + " structures=" + structures;
    }

    /** Finds an actual collision surface, including slabs and structure decks, below a clear body. */
    static Vec3 standing(Entity entity, Vec3 near, double minimumY) {
        return standing(entity, near, minimumY, entity.getBbWidth(), entity.getBbHeight());
    }

    static Vec3 standing(Entity entity, Vec3 near, double minimumY, double width, double height) {
        int centerY = net.minecraft.util.Mth.floor(near.y);
        for (int offset = 2; offset >= -2; offset--) {
            Vec3 from = new Vec3(near.x, centerY + offset + .05D, near.z);
            AABB box = new AABB(from.x - width / 2, from.y, from.z - width / 2,
                    from.x + width / 2, from.y + height, from.z + width / 2).deflate(SKIN);
            if (!clear(entity, box)) continue;
            Vec3 down = new Vec3(0, -1.05D, 0);
            Vec3 allowed = Entity.collideBoundingBox(entity, down, box, entity.level(), java.util.List.of());
            for (var structure : StructureCollisionSystem.structuresNear(entity, box.expandTowards(down))) {
                allowed = StructureCollisionResolver.resolveMovement(structure, box, allowed, 0).allowed();
            }
            if (allowed.y <= down.y + SKIN) continue;
            Vec3 at = from.add(allowed).add(0, SKIN, 0);
            AABB standing = box.move(at.subtract(from));
            if (at.y >= minimumY && at.y <= near.y + .6D && clear(entity, standing)) return at;
        }
        return null;
    }
}
