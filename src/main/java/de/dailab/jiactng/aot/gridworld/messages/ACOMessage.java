package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sent from Broker to Worker to indicate where the worker currently is positioned.
 */
public class ACOMessage extends GameMessage {

    private static final long serialVersionUID = 3030312724626177286L;

    /** the AgentID of the order
     public String orderId; */

    /** the ID of the worker */
    public String workerAgentId;

    /** the Id of the worker for the server */
    public String workerId;

    /** the size of the grid (both width and height) */
    public Position size;

    /** the initial position of the worker */
    public Position position;

    /** server for communication */
    public ICommunicationAddress server;

    /** initial list of workers available to the bidder */
    public List<Worker> initialWorkers;

    /** initial list of workerAddressList available to the bidder */
    public ArrayList<ICommunicationAddress> workerAddressList;

    /** optional list of obstacles on the map; can be null if obstacles are only
     *  revealed when bumping into them; empty list means no obstacles on the map */
    public Set<Position> obstacles;

}
