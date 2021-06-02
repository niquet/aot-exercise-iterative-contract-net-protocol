package de.dailab.jiactng.aot.gridworld.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConstructionGraph {

    // Graph representation using adjacency list
    private ArrayList<Node> vertices = new ArrayList<>();
    private ArrayList<Integer> excludedVertices = null;
    private ArrayList<ArrayList<Node>> adjacencyList = new ArrayList();
    private Integer numberOfVertices = null;
    // Grid information
    public Position size = null;                /** the size of the grid (both width and height) */
    public List<Worker> initialWorkers = null;  /** initial list of workers available to the bidder */
    public Set<Position> obstacles = null;      /** optional list of obstacles on the map; can be null if obstacles are
     only revealed when bumping into them; empty list means no obstacles on the map */
    // Information about the orders
    public ArrayList<Order> acceptedOrders = new ArrayList<>();

    public ConstructionGraph(Position size, List<Worker> initialWorkers, Set<Position> obstacles) {

        // TODO
        this.size = size;
        this.initialWorkers = initialWorkers;
        this.obstacles = obstacles;
        this.numberOfVertices = size.x * size.y;

    }

    /** Gets a position object and returns the corresponding node value */
    public Integer positionToNodeValue(Position position) {

        int width = size.x + 1;
        int nodeValue = position.y * width + position.x;
        return nodeValue;

    }

    public Node containsVertex(Integer Id) {

        for(Node node: this.vertices) {
            if (node.Id == Id) {
                return node;
            }
        }

        return null;

    }

    /** Initialize nodes of the graph */
    public void initializeVertices() {

        int nodeValue = 0;
        for(Worker worker: this.initialWorkers) {
            nodeValue = positionToNodeValue(worker.position);
            excludedVertices.add(nodeValue);
        }

        for(Position obstaclePosition: this.obstacles) {
            nodeValue = positionToNodeValue(obstaclePosition);
            excludedVertices.add(nodeValue);
        }

        /* TODO */
        for (int i = 0; i < numberOfVertices; i++) {

            boolean isNodeExcluded = excludedVertices.contains(i);
            if(!isNodeExcluded) {
                Node node = new Node(i);
                vertices.add(i, node);
            }

        }

    }

    /** Initialize gridworld as partially connected graph */
    public void initializeConstructionGraph() {

        /* TODO */
        int width = size.x + 1;
        int height = size.y + 1;
        int currentIndex = 0;

        /** i represents the rows in the grid construction graph */
        for (int i = 0; i < height; i++) {
            /** j represents the columns in the grid construction graph */
            for (int j = 0; j < width; j++) {

                currentIndex = i * width + j;

                /** Check for edges */
                // Upper edge
                if (i == 0) {
                    /** Check for corners */
                    // Upper left corner
                    if(j == 0) {
                        ArrayList arrayList = new ArrayList();
                        /* TODO check if vertices contains these nodes */
                        Node nodeToTheRight = vertices.get(j + 1);
                        Node nodeToTheBottom = vertices.get(i + 1);
                        arrayList.add(nodeToTheRight);
                        arrayList.add(nodeToTheBottom);
                        arrayList.stream()
                                .filter(node -> node != null);
                        adjacencyList.add(currentIndex, arrayList);
                        continue;
                    }
                    // Upper right corner
                    if(j == width - 1) {
                        ArrayList arrayList = new ArrayList();
                        /* TODO check if vertices contains these nodes */
                        Node nodeToTheLeft = vertices.get(j - 1);
                        Node nodeToTheBottom = vertices.get(i + 1);
                        arrayList.add(nodeToTheLeft);
                        arrayList.add(nodeToTheBottom);
                        arrayList.stream()
                                .filter(node -> node != null);
                        adjacencyList.add(currentIndex, arrayList);
                        continue;
                    }

                }
                // Bottom edge
                if (i == height - 1) {
                    /** Check for corners */
                    // Bottom left corner
                    if(j == 0) {
                        ArrayList arrayList = new ArrayList();
                        /* TODO check if vertices contains these nodes */
                        Node nodeToTheTop = vertices.get(i - 1);
                        Node nodeToTheRight = vertices.get(j + 1);
                        arrayList.add(nodeToTheTop);
                        arrayList.add(nodeToTheRight);
                        arrayList.stream()
                                .filter(node -> node != null);
                        adjacencyList.add(currentIndex, arrayList);
                        continue;
                    }
                    // Bottom right corner
                    if(j == width - 1) {
                        ArrayList arrayList = new ArrayList();
                        /* TODO check if vertices contains these nodes */
                        Node nodeToTheTop = vertices.get(i - 1);
                        Node nodeToTheLeft = vertices.get(j - 1);
                        arrayList.add(nodeToTheTop);
                        arrayList.add(nodeToTheLeft);
                        arrayList.stream()
                                .filter(node -> node != null);
                        adjacencyList.add(currentIndex, arrayList);
                        continue;
                    }

                }
                // Left edge
                if (j == 0) {

                }
                // Right edge
                if (j == width - 1) {

                }

            }
        }

    }

    /** Get the next move as per the calculated ant solution path */
    private WorkerAction nextMove(Position workerPosition) {

        /* TODO */
        return WorkerAction.NORTH;  /** Dummy value */

    }

    class Node {

        public Integer Id;

        public Node(Integer Id) {

            /* TODO */
            this.Id = Id;

        }

    }

}
