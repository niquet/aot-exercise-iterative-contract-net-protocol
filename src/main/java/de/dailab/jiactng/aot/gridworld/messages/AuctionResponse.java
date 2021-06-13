package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;

/**
 * Sent from Worker to Broker as proposal
 */
public class AuctionResponse extends GameMessage {

    private static final long serialVersionUID = -8916633866910177212L;


    /** the order to be performed */
    public String orderId;
    public Integer deadlineOffer;
    public Result status;
    public ICommunicationAddress sender;


    @Override
    public String toString() {
        return String.format("AuctionResponse(order=%s, deadline=%s, status=%s)", orderId, deadlineOffer, status);
    }
}
