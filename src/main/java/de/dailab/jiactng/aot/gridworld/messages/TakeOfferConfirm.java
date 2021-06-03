package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;

/**
 * Sent from Broker to Worker to reject proposal
 */
public class TakeOfferConfirm extends GameMessage {

    private static final long serialVersionUID = -6765104559599985326L;

    /** the order to be performed */
    public String orderId;
    public Result status;


    @Override
    public String toString() {
        return String.format("AuctionMessage(order=%d)", orderId);
    }
}
