package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Sent from Worker to Broker to indicate where the worker currently is positioned.
 */
public class ACOConfirm extends GameMessage {

    private static final long serialVersionUID = -3900206571778413868L;

    /** the ID of the order
     public String orderId; */

    /** the AgentID of the worker */
    public String workerAgentId;

    /** whether the worker accepted taking the order
     public Result state; */

    /** the Position of the worker */
    public Result state;


    @Override
    public String toString() {
        return String.format("ACOConfirm(game=%d, workerId=%s, state=%s)", gameId, workerAgentId, state);
    }

}
