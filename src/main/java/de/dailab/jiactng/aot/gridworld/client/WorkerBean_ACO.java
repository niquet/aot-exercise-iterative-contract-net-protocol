package de.dailab.jiactng.aot.gridworld.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.*;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * You can use this stub as a starting point for your worker bean.
 */



public class WorkerBean_ACO extends AbstractAgentBean {

    /* Here are addresses and descriptions */
    private ICommunicationAddress broker = null;
    /* Here are IDs that are needed for future messages */
    private Integer gameId = null;
    private String workerId = null;
    /* Here are the flags used to check for certain conditions */

    /* Here are values that need to be remembered for future decisions */
    private Integer time = 0;
    /* Here are data structures that hold complex information */
    private Position position = null;
    private ConstructionGraph constructionGraph = null;

    /*

    In ACO, a set of software agents called artificial ants search for good solutions to a given optimization problem.
    To apply ACO, the optimization problem is transformed into the problem of finding the best path on a weighted graph.
    The artificial ants (hereafter ants) incrementally build solutions by moving on the graph. The solution construction
    process is stochastic and is biased by a pheromone model, that is, a set of parameters associated with graph
    components (either nodes or edges) whose values are modified at runtime by the ants.

    TODO
    - Search space defined over a finite set of discrete decision variables;
    - Set of constraints among the variables; and
    - Objective function to be minimized (as maximizing over f is the same as minimizing over −f , every Combinatorial
      Optimization Problem can be described as a minimization problem)

     */

    @Override
    public void doStart() throws Exception {
        /* TODO */
        memory.attach(new MessageObserver(), new JiacMessage());
        log.info("starting...");

    }



    @Override
    public void execute() {
        /* TODO */
        this.time += 1;
        if (this.constructionGraph != null) {

            /* TODO get updated ant solutions */

        }

    }

    /** This is an example of using the SpaceObeserver for message processing. */
    @SuppressWarnings({"serial", "rawtypes"})
    class MessageObserver implements SpaceObserver<IFact> {

        @Override
        public void notify(SpaceEvent<? extends IFact> event) {
            if (event instanceof WriteCallEvent) {
                JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
                Object payload = message.getPayload();

                if (payload instanceof ACOMessage) {
                    /** Order to assign to the agent */

                    ACOMessage acoMessage = (ACOMessage) message.getPayload();

                    ICommunicationAddress brokerAddress = message.getSender();
                    broker = brokerAddress;

                    ACOConfirm acoConfirm = new ACOConfirm();
                    acoConfirm.workerAgentId = thisAgent.getAgentId();
                    acoConfirm.gameId = acoMessage.gameId;

                    /**
                     * Send Position confirm with FAIL if the message is not for us
                     */
                    if (!acoMessage.workerAgentId.equals(thisAgent.getAgentId()) && position == null) {

                        acoConfirm.state = Result.FAIL;
                        sendMessage(brokerAddress, acoConfirm);
                        return;

                    }

                    acoConfirm.state = Result.SUCCESS;
                    sendMessage(brokerAddress, acoConfirm);
                    time = 1;

                    /**
                     * Only set position if it is for us
                     */
                    if (position == null || workerId == null) {

                        position = acoMessage.position;
                        workerId = acoMessage.workerId;

                        constructionGraph = new ConstructionGraph(acoMessage.size, acoMessage.initialWorkers, acoMessage.obstacles);
                        constructionGraph.initializeVertices();
                        constructionGraph.initializeConstructionGraph();

                    }

                    /**
                     *
                     * DEBUGGING
                     *
                     */
                    //System.out.println("WORKER RECEIVED " + acoMessage.toString());
                    log.info("WORKER RECEIVED " + acoMessage.toString());

                }

                if (payload instanceof WorkerConfirm) {

                    /* TODO */

                }

            }
        }
    }

    /** example function to send messages to other agents */
    private void sendMessage(ICommunicationAddress receiver, IFact payload) {
        Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
        JiacMessage message = new JiacMessage(payload);
        invoke(sendAction, new Serializable[] {message, receiver});
        System.out.println("WORKER SENDING " + payload);
    }

}
