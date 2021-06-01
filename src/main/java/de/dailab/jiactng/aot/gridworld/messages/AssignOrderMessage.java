package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.gridworld.model.Order;

/**
 * Sent from Broker to Agent to announce a new Order, which is the main payload.
 */
public class AssignOrderMessage extends GameMessage {

    private static final long serialVersionUID = 813419287179878561L;


    /** the order to be performed */
    public Order order;
    /** the server to be communicated with */
    public ICommunicationAddress server;


    @Override
    public String toString() {
        return String.format("AssignOrderMessage(game=%d, order=%s)", gameId, order);
    }
}
