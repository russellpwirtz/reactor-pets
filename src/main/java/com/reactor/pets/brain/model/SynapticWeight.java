package com.reactor.pets.brain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SynapticWeight {
    private String fromCellId;
    private String toCellId;
    private String direction; // N, S, E, W, NE, NW, SE, SW
    private double weight;    // Connection strength (0.0 to 2.0)

    public static String getDirection(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;

        // Handle wrapping (toroidal topology)
        if (Math.abs(dx) > 1) {
            dx = -Integer.signum(dx);
        }
        if (Math.abs(dy) > 1) {
            dy = -Integer.signum(dy);
        }

        if (dx == 0 && dy == -1) {
            return "N";
        }
        if (dx == 0 && dy == 1) {
            return "S";
        }
        if (dx == 1 && dy == 0) {
            return "E";
        }
        if (dx == -1 && dy == 0) {
            return "W";
        }
        if (dx == 1 && dy == -1) {
            return "NE";
        }
        if (dx == -1 && dy == -1) {
            return "NW";
        }
        if (dx == 1 && dy == 1) {
            return "SE";
        }
        if (dx == -1 && dy == 1) {
            return "SW";
        }

        return "UNKNOWN";
    }
}
