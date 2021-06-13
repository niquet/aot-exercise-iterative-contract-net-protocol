package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Position;

/**
 * Sent from Broker to Worker to start a new Auction
 */
public class StartAuctionMessage extends GameMessage {

    private static final long serialVersionUID = -3394332014571351897L;


    /** the order to be performed */
    public String orderId;
    public Integer deadline;
    public Position orderPosition;



    @Override
    public String toString() {
        return String.format("StartAuctionMessage(order=%s, deadline=%s)", orderId, deadline);
    }
}
