package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;

/**
 * Sent from Worker to Broker as proposal
 */
public class DefinitivBidMessage extends GameMessage {

    private static final long serialVersionUID = -8369345089638296407L;

    /** the order to be performed */
    public String orderId;
    public Integer deadlineOffer;
    public Result status;
    public Position orderPosition;
    public ICommunicationAddress sender;


    @Override
    public String toString() {
        return String.format("AuctionMessage(order=%s, deadline=%s)", orderId, deadlineOffer);
    }
}
