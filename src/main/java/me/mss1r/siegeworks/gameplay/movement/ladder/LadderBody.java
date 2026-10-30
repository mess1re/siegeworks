package me.mss1r.siegeworks.gameplay.movement.ladder;

import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.StructureGeometry;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A siege ladder as a rigid pole in its own upright plane: its two ends, joined at the ladder's length, fall under
 * gravity and are pushed out of blocks and other structures' solid parts, with friction. A push turns and moves the
 * pole as a uniform rod would, its weight spread along it rather than held in its ends. So a ladder tips around its
 * foot, comes to rest against what it meets, pivots over a ledge and slides off it, and turns end over end if it must.
 * Plane coordinates are {@code s} along the ladder's forward direction and {@code y} up, from the foot at tick start.
 */
public final class LadderBody {
    private static final int SUBSTEPS = 4;
    private static final int ITERATIONS = 6;
    /** Vanilla entity gravity, in blocks per tick squared. */
    private static final double GRAVITY = 0.08D;
    private static final double AIR_DRAG = 0.98D;
    private static final double STATIC_FRICTION = 1.0D;
    private static final double KINETIC_FRICTION = 0.9D;
    private static final double SAMPLE_SPACING = 0.25D;
    private static final double MAX_SPEED = 1.0D;
    private static final double REST_SPEED = 0.004D;
    private static final int REST_TICKS = 8;
    /** How often a resting ladder checks that what holds it is still there. */
    private static final int WAKE_PROBE_INTERVAL = 20;
    /** Ticks a resting ladder is tried forward to tell lost support from the jitter of its contacts. */
    private static final int WAKE_PROBE_TICKS = 4;
    /** How far a tried-forward resting ladder must move to wake: falling, not settling into its contacts. */
    private static final double WAKE_DISTANCE = 0.1D;
    private static final int UNSCHEDULED = Integer.MIN_VALUE;
    private static final double EPSILON = 1.0E-6D;
    /** How far past a face a pushed point is checked, to tell open air from the next obstacle along. */
    private static final double SEAM_STEP = 1.0E-4D;

    /** The ladder's foot in the world and its lean from vertical, positive toward its forward direction. */
    public record Pose(Vec3 foot, double leanRadians) {
    }

    private record Rect(double s0, double s1, double y0, double y1, LadderContactPolygon polygon) {
        Rect(double s0, double s1, double y0, double y1) { this(s0, s1, y0, y1, null); }
    }

    private final double halfWidth;
    private final double halfThickness;
    private double footVs;
    private double footVy;
    private double topVs;
    private double topVy;
    private int stillTicks;
    private boolean settled;
    private int nextProbeTick;

    public LadderBody(double halfWidth, double halfThickness) {
        this.halfWidth = halfWidth;
        this.halfThickness = halfThickness;
    }

    /** True once the ladder has come to rest. */
    public boolean settled() {
        return settled;
    }

    /** Restores a ladder saved at rest, so it does not have to settle again. */
    public void restore(boolean settled) {
        stop();
        this.settled = settled;
        nextProbeTick = UNSCHEDULED;
    }

    /** Stops all motion and lets the ladder settle again from where it is. */
    public void stop() {
        footVs = footVy = topVs = topVy = 0.0D;
        stillTicks = 0;
        settled = false;
    }

    /** Sets both ends moving, in blocks per tick along the plane. */
    public void launch(double footS, double footY, double topS, double topY) {
        footVs = footS;
        footVy = footY;
        topVs = topS;
        topVy = topY;
        stillTicks = 0;
        settled = false;
    }

    /** Pushes the top across the ladder, turning it toward forward for positive {@code speed}. */
    public void pushTop(double leanRadians, double speed) {
        topVs += Math.cos(leanRadians) * speed;
        topVy -= Math.sin(leanRadians) * speed;
        stillTicks = 0;
        settled = false;
    }

    /**
     * Moves the ladder by one tick and returns its new pose, or null while it rests. {@code heldLean}, unless NaN, keeps
     * the top at that lean over the foot, in radians, as when someone stands the ladder up or holds it off a wall.
     */
    public Pose tick(Level level, Entity ladder, Vec3 foot, double leanRadians, double length,
                     Vec3 forward, Vec3 right, double heldLean, int gameTick) {
        boolean held = !Double.isNaN(heldLean);
        if (held) {
            // Someone holds it: never asleep
            settled = false;
        }
        if (settled) {
            if (nextProbeTick == UNSCHEDULED) {
                // Just loaded at rest: the first check waits like any other.
                nextProbeTick = gameTick + 1 + Math.floorMod(ladder.getId() - gameTick, WAKE_PROBE_INTERVAL);
            }
            if (gameTick < nextProbeTick) {
                return null;
            }
            nextProbeTick = gameTick + WAKE_PROBE_INTERVAL;
        }

        double ts = length * Math.sin(leanRadians);
        double ty = length * Math.cos(leanRadians);
        List<Rect> rects = obstacles(level, ladder, foot, forward, right, ts, ty);
        if (settled) {
            if (probeDrift(new double[]{0.0D, 0.0D, ts, ty}, rects, length) < WAKE_DISTANCE) {
                // Still held where it lies: keep it exactly there.
                footVs = footVy = topVs = topVy = 0.0D;
                return null;
            }
            settled = false;
            stillTicks = 0;
        }
        double[] end = simulate(new double[]{0.0D, 0.0D, ts, ty}, rects, length, heldLean);
        double fs = end[0];
        double fy = end[1];

        double moved = Math.max(Math.hypot(fs, fy), Math.hypot(end[2] - ts, end[3] - ty));
        if (moved < REST_SPEED && !held) {
            if (++stillTicks >= REST_TICKS) {
                footVs = footVy = topVs = topVy = 0.0D;
                settled = true;
                nextProbeTick = gameTick + 1 + Math.floorMod(ladder.getId() - gameTick, WAKE_PROBE_INTERVAL);
            }
        } else {
            stillTicks = 0;
        }

        Vec3 newFoot = foot.add(forward.scale(fs)).add(0.0D, fy, 0.0D);
        return new Pose(newFoot, Math.atan2(end[2] - fs, end[3] - fy));
    }

    /** How far the ends of a ladder resting at {@code start} move when let go for a few ticks; its state is kept. */
    private double probeDrift(double[] start, List<Rect> rects, double length) {
        footVs = footVy = topVs = topVy = 0.0D;
        double[] p = start;
        double drift = 0.0D;
        for (int tick = 0; tick < WAKE_PROBE_TICKS; tick++) {
            p = simulate(p, rects, length, Double.NaN);
            drift = Math.max(drift, Math.max(Math.hypot(p[0] - start[0], p[1] - start[1]),
                    Math.hypot(p[2] - start[2], p[3] - start[3])));
        }
        footVs = footVy = topVs = topVy = 0.0D;
        return drift;
    }

    /** One tick of motion from the ends at {@code {foot s, foot y, top s, top y}}; returns where they end up. */
    private double[] simulate(double[] start, List<Rect> rects, double length, double heldLean) {
        double fs = start[0];
        double fy = start[1];
        double ts = start[2];
        double ty = start[3];
        footVs *= AIR_DRAG;
        footVy *= AIR_DRAG;
        topVs *= AIR_DRAG;
        topVy *= AIR_DRAG;
        double h = 1.0D / SUBSTEPS;
        for (int step = 0; step < SUBSTEPS; step++) {
            footVy -= GRAVITY * h;
            topVy -= GRAVITY * h;
            double[] previous = {fs, fy, ts, ty};
            double[] p = {fs + footVs * h, fy + footVy * h, ts + topVs * h, ty + topVy * h};
            List<double[]> contacts = new ArrayList<>();
            for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                collide(p, previous, rects, length, contacts);
                keepLength(p, length, heldLean);
            }
            // Friction once the pushes are done, so no later push undoes it.
            for (double[] contact : contacts) {
                rub(p, previous, contact, length);
            }
            // Holding a contact back can turn another part of the ladder into a block: put it out once more.
            collide(p, previous, rects, length, new ArrayList<>());
            keepLength(p, length, heldLean);
            footVs = clamp((p[0] - previous[0]) / h);
            footVy = clamp((p[1] - previous[1]) / h);
            topVs = clamp((p[2] - previous[2]) / h);
            topVy = clamp((p[3] - previous[3]) / h);
            fs = p[0];
            fy = p[1];
            ts = p[2];
            ty = p[3];
        }
        return new double[]{fs, fy, ts, ty};
    }

    private static double clamp(double speed) {
        return Math.max(-MAX_SPEED, Math.min(MAX_SPEED, speed));
    }

    /** Pushes the front and back faces of the ladder out of every obstacle, noting each push as a contact. */
    private void collide(double[] p, double[] previous, List<Rect> rects, double length, List<double[]> contacts) {
        int samples = Math.max(2, (int) Math.ceil(length / SAMPLE_SPACING) + 1);
        for (int face = -1; face <= 1; face += 2) {
            for (int i = 0; i < samples; i++) {
                double u = (double) i / (samples - 1);
                double[] point = point(p, u, face, length);
                double[] before = point(previous, u, face, length);
                double[] push = pushOut(point[0], point[1], before[0], before[1], rects);
                if (push == null) {
                    continue;
                }
                double ds = push[0];
                double dy = push[1];
                double depth = Math.hypot(ds, dy);
                move(p, u, ds, dy);
                contacts.add(new double[]{u, face, ds / depth, dy / depth, depth});
            }
        }
        for (Rect rect : rects) {
            if (rect.polygon != null) {
                for (var vertex : rect.polygon.vertices) corner(p, previous, vertex.s(), vertex.y(), contacts);
                continue;
            }
            corner(p, previous, rect.s0, rect.y0, contacts);
            corner(p, previous, rect.s0, rect.y1, contacts);
            corner(p, previous, rect.s1, rect.y0, contacts);
            corner(p, previous, rect.s1, rect.y1, contacts);
        }
    }

    /**
     * Keeps an obstacle's corner out of the ladder's body between sample points, so the ladder slides smoothly over
     * an edge: the ladder is moved across itself to the side of the corner it was on.
     */
    private void corner(double[] p, double[] previous, double cs, double cy, List<double[]> contacts) {
        if (Double.isInfinite(cs)) {
            return;
        }
        double ds = p[2] - p[0];
        double dy = p[3] - p[1];
        double norm = Math.hypot(ds, dy);
        if (norm < EPSILON) {
            return;
        }
        double u = ((cs - p[0]) * ds + (cy - p[1]) * dy) / (norm * norm);
        if (u <= 0.0D || u >= 1.0D) {
            return;
        }
        double ns = -dy / norm;
        double ny = ds / norm;
        double distance = (cs - p[0]) * ns + (cy - p[1]) * ny;
        if (Math.abs(distance) >= 3.0D * halfThickness) {
            return;
        }
        double side = Math.signum((cs - previous[0]) * -(previous[3] - previous[1])
                + (cy - previous[1]) * (previous[2] - previous[0]));
        if (side == 0.0D) {
            side = Math.signum(distance);
        }
        if (side * distance >= halfThickness) {
            return;
        }
        double shift = distance - side * halfThickness;
        move(p, u, ns * shift, ny * shift);
        contacts.add(new double[]{u, 0, ns, ny, Math.abs(shift)});
    }

    /**
     * Friction: holds a contact, {@code {u, face, normal s, normal y, depth}}, where it was at the start of the
     * substep, as far as how hard it was pushed allows; past that it slides, held back by less.
     */
    private void rub(double[] p, double[] previous, double[] contact, double length) {
        double u = contact[0];
        int face = (int) contact[1];
        double ns = contact[2];
        double ny = contact[3];
        double depth = contact[4];
        double[] before = point(previous, u, face, length);
        double[] after = point(p, u, face, length);
        double slideS = after[0] - before[0];
        double slideY = after[1] - before[1];
        double normal = slideS * ns + slideY * ny;
        slideS -= normal * ns;
        slideY -= normal * ny;
        double slide = Math.hypot(slideS, slideY);
        if (slide > EPSILON) {
            double share = slide <= STATIC_FRICTION * depth ? 1.0D : Math.min(1.0D, KINETIC_FRICTION * depth / slide);
            move(p, u, -slideS * share, -slideY * share);
        }
    }

    /**
     * How to put a point that moved from {@code (fromS, fromY)} into an obstacle back out: through the face it came
     * in by, or, if it was already inside, the shortest way. Null if it is in none. The way out runs on through
     * whatever obstacle it enters next, so a point is never pushed into a neighbour.
     */
    private static double[] pushOut(double s, double y, double fromS, double fromY, List<Rect> rects) {
        Rect rect = inside(s, y, rects);
        if (rect == null) {
            return null;
        }
        if (rect.polygon != null) {
            var entered = rect.polygon.entry(fromS, fromY, s, y);
            if (entered != null) return escape(s, y, entered.ns(), entered.ny(), rects, Double.POSITIVE_INFINITY);
            double[] best = null;
            double distance = Double.POSITIVE_INFINITY;
            for (var edge : rect.polygon.edges) {
                double[] way = escape(s, y, edge.ns(), edge.ny(), rects, distance);
                double length = Math.hypot(way[0], way[1]);
                if (length < distance) { distance = length; best = way; }
            }
            return best;
        }
        int entered = enteredFace(rect, s, y, fromS, fromY);
        if (entered >= 0) {
            return escape(s, y, entered, rects, Double.POSITIVE_INFINITY);
        }
        double[] best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int direction = 0; direction < 4; direction++) {
            double[] way = escape(s, y, direction, rects, bestDistance);
            double distance = Math.abs(way[0]) + Math.abs(way[1]);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = way;
            }
        }
        return best;
    }

    /** Faces: 0 toward +s, 1 toward -s, 2 up, 3 down. The one crossed last on the way in, or -1 if none was. */
    private static int enteredFace(Rect rect, double s, double y, double fromS, double fromY) {
        double alongS = -1.0D;
        int faceS = -1;
        if (fromS >= rect.s1 && fromS > s) {
            alongS = (fromS - rect.s1) / (fromS - s);
            faceS = 0;
        } else if (fromS <= rect.s0 && fromS < s) {
            alongS = (rect.s0 - fromS) / (s - fromS);
            faceS = 1;
        }
        double alongY = -1.0D;
        int faceY = -1;
        if (fromY >= rect.y1 && fromY > y) {
            alongY = (fromY - rect.y1) / (fromY - y);
            faceY = 2;
        } else if (fromY <= rect.y0 && fromY < y) {
            alongY = (rect.y0 - fromY) / (y - fromY);
            faceY = 3;
        }
        return alongY >= alongS ? faceY : faceS;
    }

    private static double[] escape(double s, double y, int face, List<Rect> rects, double limit) {
        double ds = face == 0 ? 1.0D : face == 1 ? -1.0D : 0.0D;
        double dy = face == 2 ? 1.0D : face == 3 ? -1.0D : 0.0D;
        return escape(s, y, ds, dy, rects, limit);
    }

    private static double[] escape(double s, double y, double ds, double dy, List<Rect> rects, double limit) {
        double cs = s;
        double cy = y;
        double distance = 0.0D;
        Rect rect;
        while ((rect = inside(cs, cy, rects)) != null && distance < limit) {
            double exit;
            if (rect.polygon != null) exit = rect.polygon.exitDistance(cs, cy, ds, dy);
            else {
                double sx = ds > EPSILON ? (rect.s1 - cs) / ds : ds < -EPSILON ? (rect.s0 - cs) / ds : Double.POSITIVE_INFINITY;
                double sy = dy > EPSILON ? (rect.y1 - cy) / dy : dy < -EPSILON ? (rect.y0 - cy) / dy : Double.POSITIVE_INFINITY;
                exit = Math.min(sx, sy);
            }
            exit += SEAM_STEP;
            distance += exit;
            cs += ds * exit;
            cy += dy * exit;
        }
        return new double[]{ds * distance, dy * distance};
    }

    private static Rect inside(double s, double y, List<Rect> rects) {
        for (Rect rect : rects) {
            if (s > rect.s0 && s < rect.s1 && y > rect.y0 && y < rect.y1
                    && (rect.polygon == null || rect.polygon.contains(s, y))) {
                return rect;
            }
        }
        return null;
    }

    /** A point on a face of the ladder, {@code u} of the way from foot to top. */
    private double[] point(double[] p, double u, int face, double length) {
        double ds = p[2] - p[0];
        double dy = p[3] - p[1];
        double norm = Math.max(EPSILON, Math.hypot(ds, dy));
        double offset = face * halfThickness / norm;
        return new double[]{p[0] + ds * u - dy * offset, p[1] + dy * u + ds * offset};
    }

    /**
     * Moves the point {@code u} of the way along by the given amount, as a rigid ladder would: along itself the whole
     * ladder slides, across itself it moves and turns as a uniform rod pushed at that point, about a centre of mass
     * in its middle with a moment of inertia of a twelfth of its mass times its length squared. So a push at one end
     * swings the other end back by half as much, and a ladder tipping over its foot falls as fast as a real one.
     */
    private static void move(double[] p, double u, double ds, double dy) {
        move(p, p, u, ds, dy);
    }

    /** As {@link #move(double[], double, double, double)}, on {@code target}: the ends' positions or velocities. */
    private static void move(double[] target, double[] p, double u, double ds, double dy) {
        double rs = p[2] - p[0];
        double ry = p[3] - p[1];
        double norm = Math.hypot(rs, ry);
        if (norm > EPSILON) {
            rs /= norm;
            ry /= norm;
            double along = ds * rs + dy * ry;
            target[0] += rs * along;
            target[1] += ry * along;
            target[2] += rs * along;
            target[3] += ry * along;
            ds -= rs * along;
            dy -= ry * along;
        }
        double offset = u - 0.5D;
        double compliance = 1.0D + 12.0D * offset * offset;
        double footShare = (1.0D - 6.0D * offset) / compliance;
        double topShare = (1.0D + 6.0D * offset) / compliance;
        target[0] += ds * footShare;
        target[1] += dy * footShare;
        target[2] += ds * topShare;
        target[3] += dy * topShare;
    }

    /** Puts the ends back at the ladder's length apart, or the top at its held lean over the foot. */
    private static void keepLength(double[] p, double length, double heldLean) {
        double ds = p[2] - p[0];
        double dy = p[3] - p[1];
        double distance = Math.hypot(ds, dy);
        if (!Double.isNaN(heldLean) || distance < EPSILON) {
            double lean = Double.isNaN(heldLean) ? 0.0D : heldLean;
            p[2] = p[0] + length * Math.sin(lean);
            p[3] = p[1] + length * Math.cos(lean);
            return;
        }
        double correction = (distance - length) / distance * 0.5D;
        p[0] += ds * correction;
        p[1] += dy * correction;
        p[2] -= ds * correction;
        p[3] -= dy * correction;
    }

    /** Blocks and exact oriented structure parts clipped to the ladder's width and projected into its motion plane. */
    private List<Rect> obstacles(Level level, Entity ladder, Vec3 foot, Vec3 forward, Vec3 right,
                                 double topS, double topY) {
        Vec3 top = foot.add(forward.scale(topS)).add(0.0D, topY, 0.0D);
        double sideX = Math.abs(right.x) * halfWidth;
        double sideZ = Math.abs(right.z) * halfWidth;
        // Neither end moves more than MAX_SPEED in a tick.
        AABB region = new AABB(foot, top).inflate(sideX, 0.0D, sideZ).inflate(MAX_SPEED + halfThickness + 0.5D);

        List<Rect> rects = new ArrayList<>();
        // Walk a narrow swept band along the pole, not every block in the empty volume
        // between a diagonal ladder and its world bounding box.
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        int bands = Math.max(1, (int) Math.ceil(foot.distanceTo(top)));
        for (int band = 0; band < bands; band++) {
            AABB sweep = new AABB(foot.lerp(top, (double) band / bands), foot.lerp(top, (double) (band + 1) / bands))
                    .inflate(sideX, 0, sideZ).inflate(MAX_SPEED + halfThickness + .5D);
            for (BlockPos cursor : BlockPos.betweenClosed(BlockPos.containing(sweep.minX, sweep.minY, sweep.minZ),
                    BlockPos.containing(sweep.maxX, sweep.maxY, sweep.maxZ))) {
                if (!visited.add(cursor.immutable())) continue;
                var state = level.getBlockState(cursor);
                if (state.isAir()) continue;
                for (AABB box : state.getCollisionShape(level, cursor).toAabbs()) {
                    cut(box.move(cursor), foot, forward, right, rects);
                }
            }
        }
        for (CollidableStructure structure : StructureCollisionSystem.structuresNear(ladder, region)) {
            if (structure == ladder) {
                continue;
            }
            for (CollisionGroup group : StructureGeometry.solid(structure)) {
                for (CollisionPart part : group.parts()) {
                    if (!StructureCollisionResolver.worldPartBounds(structure.collisionTransform(), group, part).intersects(region)) continue;
                    Vec3[] vertices = vertices(part.box());
                    for (int i = 0; i < vertices.length; i++) vertices[i] = structure.collisionTransform().toWorld(group.fromPart(part, vertices[i]));
                    LadderContactPolygon polygon = LadderContactPolygon.project(vertices, foot, forward, right, halfWidth);
                    if (polygon != null) {
                        double minS = Double.POSITIVE_INFINITY, maxS = Double.NEGATIVE_INFINITY;
                        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
                        for (var vertex : polygon.vertices) {
                            minS = Math.min(minS, vertex.s()); maxS = Math.max(maxS, vertex.s());
                            minY = Math.min(minY, vertex.y()); maxY = Math.max(maxY, vertex.y());
                        }
                        rects.add(new Rect(minS, maxS, minY, maxY, polygon));
                    }
                }
            }
        }
        return merge(rects);
    }

    /** Full-width block projection; a thin obstruction between the rails must still make contact. */
    private void cut(AABB box, Vec3 foot, Vec3 forward, Vec3 right, List<Rect> rects) {
        LadderContactPolygon projection = LadderContactPolygon.project(vertices(box), foot, forward, right, halfWidth);
        if (projection == null) return;
        double s0 = Double.POSITIVE_INFINITY, s1 = Double.NEGATIVE_INFINITY;
        for (var point : projection.vertices) { s0 = Math.min(s0, point.s()); s1 = Math.max(s1, point.s()); }
        // World block boxes stay vertical, so this projection is an axis-aligned rectangle.
        rects.add(new Rect(s0, s1, box.minY - foot.y, box.maxY - foot.y));
    }

    private static Vec3[] vertices(AABB box) {
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) corners[i] = new Vec3((i & 1) == 0 ? box.minX : box.maxX,
                (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
        return corners;
    }

    /**
     * Joins rectangles that share a side, so a floor or a wall is one solid piece and a ladder end is never pushed
     * sideways into the next block along it.
     */
    private static List<Rect> merge(List<Rect> rects) {
        List<Rect> rows = mergeRuns(rects,
                Comparator.comparingDouble(Rect::y0).thenComparingDouble(Rect::y1).thenComparingDouble(Rect::s0),
                true);
        return mergeRuns(rows,
                Comparator.comparingDouble(Rect::s0).thenComparingDouble(Rect::s1).thenComparingDouble(Rect::y0),
                false);
    }

    private static List<Rect> mergeRuns(List<Rect> rects, Comparator<Rect> order, boolean alongS) {
        List<Rect> sorted = new ArrayList<>(rects);
        sorted.sort(order);
        List<Rect> merged = new ArrayList<>(sorted.size());
        Rect current = null;
        for (Rect rect : sorted) {
            if (current != null && current.polygon == null && rect.polygon == null && alongS
                    && Math.abs(rect.y0 - current.y0) < EPSILON && Math.abs(rect.y1 - current.y1) < EPSILON
                    && rect.s0 <= current.s1 + EPSILON) {
                current = new Rect(current.s0, Math.max(current.s1, rect.s1), current.y0, current.y1);
            } else if (current != null && current.polygon == null && rect.polygon == null && !alongS
                    && Math.abs(rect.s0 - current.s0) < EPSILON && Math.abs(rect.s1 - current.s1) < EPSILON
                    && rect.y0 <= current.y1 + EPSILON) {
                current = new Rect(current.s0, current.s1, current.y0, Math.max(current.y1, rect.y1));
            } else {
                if (current != null) {
                    merged.add(current);
                }
                current = rect;
            }
        }
        if (current != null) {
            merged.add(current);
        }
        return merged;
    }
}
