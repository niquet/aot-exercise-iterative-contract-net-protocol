package de.dailab.jiactng.aot.gridworld.model;

import java.util.ArrayList;
import java.util.List;

public class ConstructionGraph {

    // Grid information
    private Position size;              /** the size of the grid (both width and height) */
    public List<Worker> initialWorkers; /** initial list of workers available to the bidder */
    public List<Position> obstacles;    /** optional list of obstacles on the map; can be null if obstacles are only
     *  revealed when bumping into them; empty list means no obstacles on the map */

    // Graph representation using adjacency list
    private ArrayList<Integer> adjacencyList;
    private Integer numberOfVertices;

    public ConstructionGraph() {

        // TODO

    }

}
