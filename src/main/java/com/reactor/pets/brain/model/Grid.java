package com.reactor.pets.brain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;

/**
 * Grid containing all cells with neighbor topology.
 * Phase 6: Enhanced with cortical layer structure and cell type distribution.
 */
public class Grid {
    @Getter
    private final int width;

    @Getter
    private final int height;

    private final Cell[][] cells;
    private final Random random;

    public Grid(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = new Cell[height][width];
        this.random = new Random(42); // Fixed seed for reproducibility

        // Create all cells with layer-aware distribution
        // Y-position determines cortical layer (creates horizontal bands)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Assign layer based on Y position (top = superficial, bottom = deep)
                CorticalLayer layer = CorticalLayer.fromYPosition(y, height);

                // Assign cell type randomly based on layer-specific distributions
                CellType cellType = CellType.random(layer, random);

                cells[y][x] = new Cell(x, y, layer, cellType);
            }
        }

        // Wire up neighbor connections (Moore neighborhood: 8 neighbors)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                wireNeighbors(x, y);
            }
        }
    }

    /**
     * Connect cell to its 8 neighbors (with wrapping for toroidal topology).
     */
    private void wireNeighbors(int x, int y) {
        Cell cell = cells[y][x];

        // Relative positions for 8 neighbors
        int[][] offsets = {
                {-1, -1}, {0, -1}, {1, -1}, // Top row
                {-1, 0}, {1, 0}, // Middle row (skip self)
                {-1, 1}, {0, 1}, {1, 1} // Bottom row
        };

        for (int[] offset : offsets) {
            int nx = (x + offset[0] + width) % width; // Wrap horizontally
            int ny = (y + offset[1] + height) % height; // Wrap vertically
            cell.addNeighbor(cells[ny][nx]);
        }
    }

    /**
     * Get cell at position.
     */
    public Cell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("Cell coordinates out of bounds");
        }
        return cells[y][x];
    }

    /**
     * Get all cells as a flat list.
     */
    public List<Cell> getAllCells() {
        List<Cell> allCells = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                allCells.add(cells[y][x]);
            }
        }
        return allCells;
    }
}
