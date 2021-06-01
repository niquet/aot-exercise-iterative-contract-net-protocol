package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Sent from Worker back to Broker to indicate whether the Order could be
 * assigned. Orders may only be accepted up to a certain number of turns after
 * their creation (see property in ServerBean), otherwise a Worker could
 * wait until the last minute before accepting an Order.
 */
public class AssignOrderConfirm extends GameMessage {

    private static final long serialVersionUID = -6277806124200494579L;

    /** the ID of the order */
    public String orderId;

    /** the ID of the worker */
    public String workerId;

    /** whether the worker accepted taking the order */
    public Result state;

    @Override
    public String toString() {
        return String.format("AssignOrderConfirm(game=%d, order=%s, broker=%s, %s)", gameId, orderId, workerId, state);
    }

}
