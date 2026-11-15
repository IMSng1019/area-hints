package areahint.teleport;

import areahint.data.AreaData;

import java.util.List;

public final class ServerAreaGeometry {
    private ServerAreaGeometry() {
    }

    public static boolean contains(AreaData area, double x, double y, double z) {
        if (area == null || !isWithinAltitude(area, y)) {
            return false;
        }
        return isPointInAABB(x, z, area.getSecondVertices()) && isPointInPolygon(x, z, area.getVertices());
    }

    public static boolean isWithinAltitude(AreaData area, double y) {
        AreaData.AltitudeData altitude = area.getAltitude();
        return altitude == null || altitude.isInRange(y);
    }

    public static boolean isPointInAABB(double x, double z, List<AreaData.Vertex> secondVertices) {
        if (secondVertices == null || secondVertices.size() != 4) {
            return false;
        }

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (AreaData.Vertex vertex : secondVertices) {
            minX = Math.min(minX, vertex.getX());
            maxX = Math.max(maxX, vertex.getX());
            minZ = Math.min(minZ, vertex.getZ());
            maxZ = Math.max(maxZ, vertex.getZ());
        }

        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public static boolean isPointInPolygon(double x, double z, List<AreaData.Vertex> vertices) {
        if (vertices == null || vertices.size() < 3) {
            return false;
        }

        int intersections = 0;
        for (int i = 0; i < vertices.size(); i++) {
            AreaData.Vertex current = vertices.get(i);
            AreaData.Vertex next = vertices.get((i + 1) % vertices.size());
            if (intersectsRay(x, z, current, next)) {
                intersections++;
            }
        }
        return intersections % 2 == 1;
    }

    private static boolean intersectsRay(double x, double z, AreaData.Vertex v1, AreaData.Vertex v2) {
        double x1 = v1.getX();
        double z1 = v1.getZ();
        double x2 = v2.getX();
        double z2 = v2.getZ();

        if ((z1 <= z && z2 > z) || (z2 <= z && z1 > z)) {
            double xIntersect = x1 + (z - z1) * (x2 - x1) / (z2 - z1);
            return xIntersect > x;
        }
        return false;
    }
}
