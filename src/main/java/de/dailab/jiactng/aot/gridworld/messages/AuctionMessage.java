package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Position;

/**
 * Sent from Broker to Worker to start a new Auction
 */
public class AuctionMessage extends GameMessage {

    private static final long serialVersionUID = 2816632115810497551L;


    /** the order to be performed */
    public String orderId;
    public Integer orderCreated;
    public Integer deadline;
    public Position orderPosition;



    @Override
    public String toString() {
        return String.format("AuctionMessage(order=%s, deadline=%s)", orderId, deadline);
    }
}
