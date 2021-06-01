package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Position;

/**
 * Sent from Broker to Worker to indicate where the worker currently is positioned.
 */
public class PositionMessage extends GameMessage {

    private static final long serialVersionUID = -2183024630466982177L;

    /** the ID of the order
    public String orderId; */

    /** the ID of the worker */
    public String workerId;

    /** whether the worker accepted taking the order
    public Result state; */

    /** the Position of the worker */
    public Position position;

    /** the Id of the worker */
    public String workerIdForServer;

    @Override
    public String toString() {
        return String.format("PositionMessage(game=%d, workerId=%s, position=%s)", gameId, workerId, position, workerIdForServer);
    }

}
