package me.mss1r.siegeworks.gameplay.movement.ladder;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Exact projection of an oriented solid clipped to the ladder's width, into its upright motion plane. */
final class LadderContactPolygon {
    private static final double EPS = 1.0E-8D;
    record Point(double s, double y) {}
    record Edge(double ns, double ny, double limit) {
        double gap(double s, double y) { return ns * s + ny * y - limit; }
    }
    final List<Point> vertices;
    final List<Edge> edges;

    private LadderContactPolygon(List<Point> vertices) {
        this.vertices = List.copyOf(vertices);
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            Point a = vertices.get(i), b = vertices.get((i + 1) % vertices.size());
            double length = Math.hypot(b.s - a.s, b.y - a.y);
            double ns = (b.y - a.y) / length, ny = (a.s - b.s) / length;
            edges.add(new Edge(ns, ny, ns * a.s + ny * a.y));
        }
        this.edges = List.copyOf(edges);
    }

    boolean contains(double s, double y) {
        for (Edge edge : edges) if (edge.gap(s, y) >= -EPS) return false;
        return true;
    }

    double exitDistance(double s, double y, double ds, double dy) {
        double distance = Double.POSITIVE_INFINITY;
        for (Edge edge : edges) {
            double speed = edge.ns * ds + edge.ny * dy;
            if (speed > EPS) distance = Math.min(distance, -edge.gap(s, y) / speed);
        }
        return Math.max(0, distance);
    }

    /** Latest entry face on the swept segment, or null when it already started inside. */
    Edge entry(double fromS, double fromY, double s, double y) {
        Edge face = null;
        double latest = -1;
        for (Edge edge : edges) {
            double before = edge.gap(fromS, fromY), after = edge.gap(s, y);
            if (before >= -EPS && after < -EPS) {
                double at = before / (before - after);
                if (at > latest) { latest = at; face = edge; }
            }
        }
        return face;
    }

    /** Vertices use the box bit order: X=1, Y=2, Z=4; all twelve edges are retained when clipping. */
    static LadderContactPolygon project(Vec3[] world, Vec3 foot, Vec3 forward, Vec3 right, double halfWidth) {
        Vec3[] local = new Vec3[8];
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Vec3 delta = world[i].subtract(foot);
            local[i] = new Vec3(delta.dot(forward), delta.y, delta.dot(right));
            if (Math.abs(local[i].z) <= halfWidth + EPS) points.add(new Point(local[i].x, local[i].y));
        }
        for (int i = 0; i < 8; i++) for (int axis = 1; axis <= 4; axis <<= 1) {
            if ((i & axis) != 0) continue;
            Vec3 a = local[i], b = local[i | axis];
            if (Math.abs(b.z - a.z) < EPS) continue;
            for (double rail : new double[] {-halfWidth, halfWidth}) {
                double t = (rail - a.z) / (b.z - a.z);
                if (t >= 0 && t <= 1) {
                    Vec3 at = a.lerp(b, t);
                    points.add(new Point(at.x, at.y));
                }
            }
        }
        points.sort(Comparator.comparingDouble(Point::s).thenComparingDouble(Point::y));
        List<Point> unique = new ArrayList<>();
        for (Point point : points) {
            if (unique.isEmpty() || Math.hypot(point.s - unique.get(unique.size() - 1).s,
                    point.y - unique.get(unique.size() - 1).y) > EPS) unique.add(point);
        }
        if (unique.size() < 3) return null;
        List<Point> hull = new ArrayList<>();
        for (Point point : unique) appendHull(hull, point, 0);
        int lower = hull.size() - 1;
        for (int i = unique.size() - 2; i >= 0; i--) appendHull(hull, unique.get(i), lower);
        hull.remove(hull.size() - 1);
        return hull.size() < 3 ? null : new LadderContactPolygon(hull);
    }

    private static void appendHull(List<Point> hull, Point p, int floor) {
        while (hull.size() > floor + 1) {
            Point a = hull.get(hull.size() - 2), b = hull.get(hull.size() - 1);
            if ((b.s - a.s) * (p.y - a.y) - (b.y - a.y) * (p.s - a.s) > EPS) break;
            hull.remove(hull.size() - 1);
        }
        hull.add(p);
    }
}
