package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Position;

/**
 * Sent from Broker to Worker to indicate where the worker currently is positioned.
 */
public class PositionUpdate extends GameMessage {

    private static final long serialVersionUID = -842304050698432227L;

    /** the ID of the order
     public String orderId; */

    /** the ID of the worker */
    public String workerId;

    /** whether the worker accepted taking the order
     public Result state; */

    /** the Position of the worker */
    public Position position;

    @Override
    public String toString() {
        return String.format("PositionMessage(game=%d, workerId=%s, position=%s)", gameId, workerId, position);
    }

}
