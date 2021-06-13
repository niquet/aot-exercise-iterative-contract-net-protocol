package de.dailab.jiactng.aot.gridworld.client;

import astar.src.com.ai.astar.AStar;
import astar.src.com.ai.astar.Node;
import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.WorkerAction;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.*;


/**
 * You can use this stub as a starting point for your worker bean.
 */


public class WorkerBean extends AbstractAgentBean {

    private final Map<String, Order> currentOrders = new HashMap<>();
    private final Map<Order, ICommunicationAddress> orderToAddress = new HashMap<>();
    private final Comparator<Order> compareOrder = new Comparator<Order>() {
        @Override
        public int compare(Order o1, Order o2) {
            int p1 = aStarDistance(position, o1.position);
            int p2 = aStarDistance(position, o2.position);
            //int p1 = position.distance(o1.position);
            //int p2 = position.distance(o2.position);
            if (p1 < p2) return -1;
            if (p1 > p2) return 1;
            return 0;
        }
    };
    private final PriorityQueue<Order> priorityQueue = new PriorityQueue<>(compareOrder);
    private PriorityQueue<Order> priorityQueueForBidding = new PriorityQueue<>(priorityQueue);

    private Order handleOrder = null;
    private Boolean hasArrivedAtTarget = false;
    private Integer gameId = null;
    private Position gridSize = null;
    private Set<Position> obstacles = null;
    private Position estimatedDestination = null;

    private Position position = null;
    private WorkerAction lastMove = null;
    private Boolean lastMoveFailed = false;
    private List<Node> path = null;

    private String workerIdForServer = null;
    private ICommunicationAddress broker = null;
    private int time;

    // TODO remove the obstacle messaging for now
    private ArrayList<ICommunicationAddress> otherWorkers = null;
    private AStar astar = null;
    private Boolean hasOrderChanged = false;
    private Boolean canOrderBeChanged = true;

    @Override
    public void doStart() throws Exception {
        /* TODO */

        memory.attach(new MessageObserver(), new JiacMessage());

    }


    @Override
    public void execute() {

        // if we already have assignments
        if (!priorityQueue.isEmpty()) {
            // Order firstOrder = priorityQueue.peek();

            if (time > handleOrder.deadline) {
                System.out.println("WORKER " + workerIdForServer + " REMOVED ORDER: " + handleOrder.id);

                priorityQueue.remove(handleOrder);

                if (!priorityQueue.isEmpty()) {

                    handleOrder = priorityQueue.peek();
                    hasOrderChanged = true;

                    System.out.println("WORKER " + workerIdForServer + " CURRENTLY PROCESSING ORDER: " + handleOrder.id);

                } else {
                    return;
                }
            }
            if (position == null) {
                return;
            }
            /* Kann durch den ACO Worker alles geändert werden, aber für funktionalität lass ich es erstmal so */

            if (position.equals(priorityQueue.peek().position)) {
                WorkerMessage move = new WorkerMessage();
                move.action = WorkerAction.ORDER;
                move.gameId = gameId;
                move.workerId = workerIdForServer;
                hasArrivedAtTarget = true;
                canOrderBeChanged = false;

                sendMessage(orderToAddress.get(priorityQueue.peek()), move);
            }

            WorkerMessage move = new WorkerMessage();
            if (hasOrderChanged) {

                System.out.println("WORKER " + workerIdForServer + " CURRENTLY PROCESSING ORDER: " + handleOrder.id);

                aStarUpdate(position, handleOrder.position);
            }

            Position nextMove = getNextMove();
            if (nextMove != null) {
                move.action = getMoveAction(position, nextMove);
                lastMove = move.action;
                move.gameId = gameId;
                move.workerId = workerIdForServer;

                sendMessage(orderToAddress.get(handleOrder), move);
            }

        }

    }


    /**
     * This is an example of using the SpaceObeserver for message processing.
     */
    @SuppressWarnings({"serial", "rawtypes"})
    class MessageObserver implements SpaceObserver<IFact> {

        @Override
        public void notify(SpaceEvent<? extends IFact> event) {
            if (event instanceof WriteCallEvent) {
                JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
                Object payload = message.getPayload();

                if (payload instanceof ACOMessage) {
                    /** Initialize position and stuff */
                    ACOMessage acoMessage = (ACOMessage) message.getPayload();
                    workerIdForServer = acoMessage.workerId;
                    position = acoMessage.position;
                    gameId = acoMessage.gameId;
                    broker = message.getSender();
                    gridSize = acoMessage.size;
                    obstacles = acoMessage.obstacles;
                    if (otherWorkers == null) {
                        otherWorkers = acoMessage.workerAddressList;
                    }

                    aStar(new Position(0, 0), new Position(0, 0));

                    /* TODO Spielfeld initialisieren, obstacles und andere Worker speichern? */

                    ACOConfirm acoConfirm = new ACOConfirm();
                    acoConfirm.state = Result.SUCCESS;
                    acoConfirm.workerAgentId = thisAgent.getAgentId();
                    acoConfirm.gameId = gameId;

                    sendMessage(broker, acoConfirm);
                    time = 0;

                    log.info("WORKER RECEIVED " + acoMessage.toString());
                }

                if (payload instanceof AssignOrderMessage) {
                    /** Order to assign to the agent */
                    if (gameId == null) gameId = ((AssignOrderMessage) message.getPayload()).gameId;

                    //ICommunicationAddress broker = message.getSender();

                    AssignOrderMessage assignOrderMessage = (AssignOrderMessage) message.getPayload();

                    Order order = assignOrderMessage.order;
                    ICommunicationAddress server = assignOrderMessage.server;

                    AssignOrderConfirm assignOrderConfirm = new AssignOrderConfirm();
                    assignOrderConfirm.orderId = order.id;
                    assignOrderConfirm.gameId = assignOrderMessage.gameId;
                    assignOrderConfirm.workerId = thisAgent.getAgentId();
                    assignOrderConfirm.state = Result.FAIL;

                    orderToAddress.put(order, server);
                    priorityQueue.add(order);
                    assignOrderConfirm.state = Result.SUCCESS;

                    if (handleOrder == null) {
                        handleOrder = priorityQueue.peek();

                        aStar(position, handleOrder.position);
                    } else {
                        if (!handleOrder.id.equals(priorityQueue.peek().id) && canOrderBeChanged) {
                            hasOrderChanged = true;
                            handleOrder = priorityQueue.peek();
                        }
                    }

                    sendMessage(broker, assignOrderConfirm);
                    return;

                }

                if (payload instanceof StartAuctionMessage) {
                    StartAuctionMessage auctionMessage = (StartAuctionMessage) message.getPayload();
                    broker = message.getSender();

                    Order order = new Order();
                    order.id = auctionMessage.orderId;
                    order.deadline = auctionMessage.deadline;
                    order.position = auctionMessage.orderPosition;
                    priorityQueueForBidding.add(order);

                    if (position != null) {
                        /* TODO Calculated distance, considering already taken Assignments, hier Intelligenz einbauen */
                        AuctionResponse auctionResponse = new AuctionResponse();
                        auctionResponse.orderId = auctionMessage.orderId;
                        auctionResponse.gameId = gameId;
                        auctionResponse.sender = thisAgent.getAgentDescription().getMessageBoxAddress();
                        if (auctionMessage.orderPosition == null || position == null) {
                            auctionResponse.status = Result.FAIL;
                        } else {
                            int isGoodOrder = evaluateOrder(order);
                            auctionResponse.deadlineOffer = isGoodOrder;

                            if (auctionResponse.deadlineOffer >= auctionMessage.deadline || isGoodOrder == -1) {
                                auctionResponse.status = Result.FAIL;
                            } else {
                                auctionResponse.status = Result.SUCCESS;
                            }
                        }
                        sendMessage(broker, auctionResponse);
                    } else {
                        AuctionMessage sendAgain = new AuctionMessage();
                        sendAgain.orderId = auctionMessage.orderId;
                        sendAgain.deadline = auctionMessage.deadline;
                        sendAgain.orderPosition = auctionMessage.orderPosition;
                        sendMessage(broker, sendAgain);
                    }
                }

                if (payload instanceof AuctionMessage) {
                    boolean orderContained = false;

                    AuctionMessage auctionMessage = (AuctionMessage) message.getPayload();
                    broker = message.getSender();
                    if (position != null) {
                        /* TODO Calculated distance, considering already taken Assignments, hier Intelligenz einbauen */
                        for(Order orderInQueue: priorityQueueForBidding) {
                            if (auctionMessage.orderId.equals(orderInQueue.id)) {
                                orderContained = true;
                                break;
                            }
                        }
                        for(Order orderInQueue: priorityQueue) {
                            if (auctionMessage.orderId.equals(orderInQueue.id)) {
                                orderContained = false;
                                break;
                            }
                        }
                        if(!orderContained) return;

                        AuctionResponse auctionResponse = new AuctionResponse();
                        auctionResponse.orderId = auctionMessage.orderId;
                        auctionResponse.gameId = gameId;
                        auctionResponse.sender = thisAgent.getAgentDescription().getMessageBoxAddress();
                        if (auctionMessage.orderPosition == null || position == null) {
                            auctionResponse.status = Result.FAIL;
                        } else {
                            Order order = new Order();
                            order.position = auctionMessage.orderPosition;
                            order.deadline = auctionMessage.deadline;
                            order.id = auctionMessage.orderId;

                            int isGoodOrder = evaluateOrder(order);
                            auctionResponse.deadlineOffer = isGoodOrder;

                            if (auctionResponse.deadlineOffer >= auctionMessage.deadline || isGoodOrder == -1) {
                                auctionResponse.status = Result.FAIL;
                            } else {
                                auctionResponse.status = Result.SUCCESS;
                            }
                        }
                        sendMessage(broker, auctionResponse);
                    } else {
                        AuctionMessage sendAgain = new AuctionMessage();
                        sendAgain.orderId = auctionMessage.orderId;
                        sendAgain.deadline = auctionMessage.deadline;
                        sendAgain.orderPosition = auctionMessage.orderPosition;
                        sendMessage(broker, auctionMessage);
                    }

                }

                if (payload instanceof DefinitivBidMessage) {
                    DefinitivBidMessage bid = (DefinitivBidMessage) message.getPayload();

                    DefinitivBidMessage answer = new DefinitivBidMessage();
                    answer.orderId = bid.orderId;

                    Position goal = position;

                    int zeit = time;
                    int distance = 0;
                    Order bidOrder = new Order();
                    bidOrder.id = bid.orderId;
                    bidOrder.position = bid.orderPosition;
                    bidOrder.deadline = bid.deadlineOffer;

                    for(Order orderInQueue: priorityQueueForBidding) {
                        distance = aStarDistance(goal, orderInQueue.position) + 1;
                        zeit += distance;
                        if (bid.orderId.equals(orderInQueue.id)) {
                            break;
                        }
                        goal = orderInQueue.position;
                    }

                    answer.deadlineOffer = zeit + 1;
                    answer.status = Result.SUCCESS;

                    if (bid.orderPosition == null) {
                        System.out.println("ORDER POSITION NULL");
                    } else {
                        // TODO ???
                        int deadline = aStarDistance(position, bid.orderPosition) + time;
                        if (bid.deadlineOffer < deadline) {
                            answer.status = Result.FAIL;
                        }
                    }

                    sendMessage(broker, answer);
                }

                if (payload instanceof DefinitivRejectMessage) {
                    DefinitivRejectMessage reject = (DefinitivRejectMessage) message.getPayload();
                    // TODO
                    for(Order orderInQueue: priorityQueueForBidding) {
                        if (reject.order.id.equals(orderInQueue.id)) {
                            priorityQueueForBidding.remove(orderInQueue);
                            break;
                        }
                    }
                }

                if (payload instanceof WorkerConfirm) {

                    /* Update time to be aware of expired Offers */
                    time += 1;
                    System.out.println("TIME: " + time);

                    WorkerConfirm workerConfirm = (WorkerConfirm) message.getPayload();
                    Result result = workerConfirm.state;

                    if ((workerConfirm.action == WorkerAction.ORDER) && hasArrivedAtTarget) {
                        priorityQueue.remove(handleOrder);
                        System.out.println("SUCCESS " + handleOrder);
                        handleOrder = priorityQueue.peek();
                        hasOrderChanged = true;
                        canOrderBeChanged = true;
                        hasArrivedAtTarget = false;

                        if(handleOrder != null)
                        for(Order orderInQueue: priorityQueueForBidding) {
                            if (handleOrder.id.equals(orderInQueue.id)) {
                                priorityQueueForBidding.remove(orderInQueue);
                                break;
                            }
                        }
                    }

                    if (result == Result.FAIL) {
                        // TODO unbekannte obstacles
                        if (workerConfirm.action != WorkerAction.ORDER) {
                            lastMoveFailed = true;
                            for (ICommunicationAddress workerAddress : otherWorkers) {
                                ObstacleUpdate obstacleUpdate = new ObstacleUpdate();
                                obstacleUpdate.gameId = gameId;
                                obstacleUpdate.position = denoteObstacle(lastMove);
                                obstacleUpdate.workerAgentId = thisAgent.getAgentId();
                                sendMessage(workerAddress, obstacleUpdate);
                            }

                        }
                        return;
                    }

                    if (!hasArrivedAtTarget) {
                        // Agent hasn't arrived at target, so conduct the planned move
                        doMove(workerConfirm.action);
                        lastMoveFailed = false;
                    }
                }

                if (payload instanceof ObstacleUpdate) {
                    if (((ObstacleUpdate) payload).workerAgentId.equals(thisAgent.getAgentId())) {
                        return;
                    }
                    obstacles.add(((ObstacleUpdate) payload).position);
                    if (handleOrder != null) {
                        aStarUpdate(position, handleOrder.position);
                    }


                }

            }
        }
    }

    private int possibleEnd(Position target) {
        Position goal = this.position;
        int zeit = 0;
        /* TODO wo rein damit gewinn maximiert ?? */

        for (Order order : this.priorityQueue) {
            zeit += aStarDistance(goal, order.position) + 1;
            goal = order.position;
        }
        zeit += aStarDistance(goal, target);
        return zeit;
    }

    /**
     * example function to send messages to other agents
     */
    private void sendMessage(ICommunicationAddress receiver, IFact payload) {
        Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
        JiacMessage message = new JiacMessage(payload);
        invoke(sendAction, new Serializable[]{message, receiver});
        System.out.println("WORKER " + this.workerIdForServer + " SENDING " + payload);
    }

    /**
     * calculate next move
     */
    private WorkerAction getNextMove_old(Position current, Position target, Boolean lastMoveFailed) {
        // TODO
        if (current.equals(target)) {
            hasArrivedAtTarget = true;
            return WorkerAction.ORDER;
        }

        int[] distances = null;

        if (lastMoveFailed) {

            switch (lastMove) {
                case NORTH:
                case SOUTH:
                    Position E = new Position(current.x + 1, current.y);
                    Position W = new Position(current.x - 1, current.y);
                    distances = new int[]{target.distance(E), target.distance(W)};
                    return (distances[0] > distances[1]) ? WorkerAction.EAST : WorkerAction.WEST;
                case EAST:
                case WEST:
                    Position N = new Position(current.x, current.y - 1);
                    Position S = new Position(current.x, current.y + 1);
                    distances = new int[]{target.distance(N), target.distance(S)};
                    return (distances[0] > distances[1]) ? WorkerAction.NORTH : WorkerAction.SOUTH;
            }

        } else {

            // [N, S, E, W]
            Position N = new Position(current.x, current.y - 1);
            Position S = new Position(current.x, current.y + 1);
            Position E = new Position(current.x + 1, current.y);
            Position W = new Position(current.x - 1, current.y);

            distances = new int[]{target.distance(N), target.distance(S), target.distance(E), target.distance(W)};

        }

        WorkerAction workerAction = null;
        int index = -1;
        int min = Integer.MAX_VALUE;

        for (int i = 0; i < distances.length; i++) {

            if (distances[i] < min) {
                min = distances[i];
                index = i;
            }

        }

        switch (index) {
            case 1:
                workerAction = WorkerAction.SOUTH;
                break;
            case 2:
                workerAction = WorkerAction.EAST;
                break;
            case 3:
                workerAction = WorkerAction.WEST;
                break;
            case 0:
                workerAction = WorkerAction.NORTH;
                break;
            default:
                workerAction = WorkerAction.ORDER;
                break;
        }

        return workerAction;

    }

    private Position getNextMove() {
        if (path != null && path.size() > 0) {
            Position move = new Position(path.get(0).getRow(), path.get(0).getCol());

            path.remove(path.get(0));
            return move;
        }
        return null;
    }

    private void doMove(WorkerAction action) {
        if (action == WorkerAction.NORTH) position = new Position(position.x, position.y - 1);
        if (action == WorkerAction.SOUTH) position = new Position(position.x, position.y + 1);
        if (action == WorkerAction.WEST) position = new Position(position.x - 1, position.y);
        if (action == WorkerAction.EAST) position = new Position(position.x + 1, position.y);
    }

    private WorkerAction getMoveAction(Position curr, Position next) {
        System.out.println("CURRENT UND NEXT IN GETMOVEACTION: " + curr.toString() + next.toString());
        int x = next.x - curr.x;
        int y = next.y - curr.y;

        if (x == -1) return WorkerAction.WEST;
        if (x == 1) return WorkerAction.EAST;
        if (y == -1) return WorkerAction.NORTH;
        if (y == 1) return WorkerAction.SOUTH;
        return null;
    }

    /**
     * Wegfindungsfunktion mit A*
     * nutzt außer Argumenten noch gridSize, obstacles
     * QUELLE: https://github.com/marcelo-s/A-Star-Java-Implementation
     */
    private void aStar(Position start, Position target) {
        Node initialNode = new Node(start.x, start.y);
        Node finalNode = new Node(target.x, target.y);
        this.astar = new AStar(gridSize.x, gridSize.y, initialNode, finalNode, 1, Integer.MAX_VALUE);

        int[][] obsts = new int[obstacles.size()][2];
        int i = 0;
        for (Position obs : obstacles) {
            obsts[i][0] = obs.x;
            obsts[i][1] = obs.y;
            i++;
        }
        astar.setBlocks(obsts);

        path = astar.findPath(initialNode, finalNode);
        if (path.size() > 1) {
            path.remove(path.get(0));
        }
        System.out.println("PATH: " + path.toString());
    }

    /**
     * Wegfindungsfunktion mit A*
     *
     */
    private int aStarUpdate(Position start, Position target) {

        Node initialNode = new Node(start.x, start.y);
        Node finalNode = new Node(target.x, target.y);

        this.astar.setInitialNode(initialNode);
        this.astar.setFinalNode(finalNode);

        this.path = astar.findPath(initialNode, finalNode);
        if (path.size() > 0) {
            path.remove(path.get(0));
        }

        return this.path.size();

    }

    /**
     * Wegfindungsfunktion mit A*
     *
     */
    private int aStarDistance(Position start, Position target) {

        Node initialNode = new Node(start.x, start.y);
        Node finalNode = new Node(target.x, target.y);

        int distanceForThisOrder = 0;

        if (this.astar == null) {
            System.out.println("This.astar is null");
            aStar(start, target);
            distanceForThisOrder = path.size();
            return distanceForThisOrder;
        }

        return astar.findPath(initialNode, finalNode).size() - 1;
    }

    private Position denoteObstacle(WorkerAction action) {
        Position obstacle = null;
        switch (action) {
            case NORTH:
                obstacle = new Position(position.x, position.y - 1);
                break;
            case SOUTH:
                obstacle = new Position(position.x, position.y + 1);
                break;
            case WEST:
                obstacle = new Position(position.x - 1, position.y);
                break;
            case EAST:
                obstacle = new Position(position.x + 1, position.y);
                break;
        }
        return obstacle;
    }

    /**
     * sort the orders according to a score and put them into a queue
     */
    private void sortOrders(Position orderPosition) {
        // TODO

    }

    /**
     * evaluate order score
     */
    // TODO create order in execute()
    private Integer evaluateOrder(Order order) {
        // TODO check if enough time to process order
        int distanceForThisOrder = 0;
        Position goal = this.position;

        if (this.astar == null) {
            System.out.println("This.astar is null");
            aStar(goal, order.position);
            distanceForThisOrder = path.size();
            if (distanceForThisOrder > order.deadline || distanceForThisOrder == 0) {
                return -1;
            }
            return distanceForThisOrder;
        }

        int zeit = time;
        int distance = 0;
        /* TODO wo rein damit gewinn maximiert ?? */

        for(Order orderInQueue: priorityQueueForBidding) {
            distance = aStarDistance(goal, orderInQueue.position) + 1;
            zeit += distance;
            if (zeit > orderInQueue.deadline) {
                return -1;
            }
            if (order.id.equals(orderInQueue.id)) {
                distanceForThisOrder = zeit;
            }
            goal = orderInQueue.position;
        }
        if(distanceForThisOrder == 0)
            return -1;

        return distanceForThisOrder;
    }

}
