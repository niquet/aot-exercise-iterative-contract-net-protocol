package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Sent from Broker to Worker to indicate where the worker currently is positioned.
 */
public class PositionConfirm extends GameMessage {

    private static final long serialVersionUID = 2829240620247919469L;

    /** the ID of the order
     public String orderId; */

    /** the ID of the worker */
    public String workerId;

    /** whether the worker accepted taking the order
     public Result state; */

    /** the Position of the worker */
    public Result state;


    @Override
    public String toString() {
        return String.format("PositionMessage(game=%d, workerId=%s, state=%s)", gameId, workerId, state);
    }

}
