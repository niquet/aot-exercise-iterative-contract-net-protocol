package de.dailab.jiactng.aot.gridworld.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * You can use this stub as a starting point for your broker bean.
 */

public class BrokerBean extends AbstractAgentBean {

	/* Here are addresses and descriptions */
	private ICommunicationAddress server = null;
	private List<IAgentDescription> agentDescriptionList = null;
	/* Here are IDs that are needed for future messages */
	private Integer gameId = null;
	/* Here are the flags used to check for certain conditions */
	private Boolean isGameStarted = false;
	/* Here are values that need to be remembered for future decisions */
	private Integer maximumNumberOfAgents = null;
	private Integer time = 0;
	/* Here are data structures that hold complex information */
	private GridworldGame gridworldGame = null;
	private List<Worker> initialWorkers = null;
	private Map<String, WorkerInformation> workerInformationList = new HashMap<>();
	private List<Order> currentOrders = null;
	// OrderId, zugehöriges, bestes Angebot
	private Map<String, AuctionResponse> bestOffers = new HashMap<>();

	@Override
	public void doStart() throws Exception {
		/* TODO */
		log.info("starting broker bean...");
	}

	@Override
	public void execute() {
		/* TODO */
		log.info("running broker bean...");
		IAgentDescription serverAgent = thisAgent.searchAgent(new AgentDescription(
			null,
			"ServerAgent",
			null,
			null,
			null,
			null
		));

		if (serverAgent != null) {
			this.server = serverAgent.getMessageBoxAddress();

			if (!isGameStarted) {
				StartGameMessage startGameMessage = new StartGameMessage();
				startGameMessage.brokerId = thisAgent.getAgentId();
				startGameMessage.gridFile = "/grids/04_1.grid";
				sendMessage(server, startGameMessage);

				this.isGameStarted = true;
			}

		} else {
			System.out.println("SERVER NOT FOUND!");
		}

		/* example of handling incoming messages without listener */
		for (JiacMessage message : memory.removeAll(new JiacMessage())) {
			Object payload = message.getPayload();

			if (payload instanceof StartGameResponse) {

				StartGameResponse startGameResponse = (StartGameResponse) message.getPayload();

				this.maximumNumberOfAgents = startGameResponse.initialWorkers.size();
				this.initialWorkers = startGameResponse.initialWorkers;
				this.agentDescriptionList = getMyWorkerAgents(this.maximumNumberOfAgents);
				this.gameId = startGameResponse.gameId;

				/** Debugging purposes */
				System.out.println("SERVER SENDING " + startGameResponse.toString());

				this.gridworldGame = new GridworldGame();
				this.gridworldGame.size = startGameResponse.size;
				this.gridworldGame.obstacles.addAll(startGameResponse.obstacles);

				/**
				 * Send the Position messages to each Agent for a specific worker
				 * PositionMessages are sent to inform the worker where it is located
				 * additionally put the position of the worker in the positionMap
				 */
				for(Worker worker: startGameResponse.initialWorkers) {

					WorkerInformation workerInformation = new WorkerInformation();
					workerInformation.workerId = worker.id;

					int indexOfWorker = startGameResponse.initialWorkers.indexOf(worker);
					workerInformation.agentDescription = this.agentDescriptionList.get(indexOfWorker);
					workerInformation.agentId = workerInformation.agentDescription.getAid();
					workerInformation.initialWorkerPosition = worker.position;

					workerInformationList.put(workerInformation.agentId, workerInformation);

					// Send each Agent their current position
					ACOMessage acoMessage = new ACOMessage();
					acoMessage.workerAgentId = workerInformation.agentId;
					acoMessage.workerId = worker.id;
					acoMessage.initialWorkers = this.initialWorkers;
					acoMessage.position = worker.position;
					acoMessage.obstacles = this.gridworldGame.obstacles;
					acoMessage.size = this.gridworldGame.size;
					acoMessage.gameId = this.gameId;

					ICommunicationAddress workerAddress = workerInformation.agentDescription.getMessageBoxAddress();
					sendMessage(workerAddress, acoMessage);

				}

				/* TODO iterative Contract Net Protocol call-for-proposals for the orders */

			}

			if (payload instanceof ACOConfirm) {

				ACOConfirm acoConfirm = (ACOConfirm) message.getPayload();

				if (acoConfirm.state == Result.FAIL) {

					WorkerInformation workerInformation = workerInformationList.get(acoConfirm.workerAgentId);

					ACOMessage acoMessage = new ACOMessage();
					acoMessage.workerAgentId = acoConfirm.workerAgentId;
					acoMessage.workerId = workerInformation.workerId;
					acoMessage.initialWorkers = this.initialWorkers;
					acoMessage.position = workerInformation.initialWorkerPosition;
					acoMessage.obstacles = this.gridworldGame.obstacles;
					acoMessage.size = this.gridworldGame.size;
					acoMessage.gameId = this.gameId;

					/* TODO why can't we use message.getSender() here? */
					ICommunicationAddress workerAddress = workerInformation.agentDescription.getMessageBoxAddress();
					sendMessage(workerAddress, acoMessage);

				}

				if (acoConfirm.state == Result.SUCCESS) {

					/* TODO do something useful with this information */

				}

			}

			// if new order arrives
			if(payload instanceof OrderMessage){
				OrderMessage orderMessage = (OrderMessage) message.getPayload();
				currentOrders.add(orderMessage.order);
				// übrige Annahmezeit bis verfall speichern
				// jede Runde: Orders in Liste Kontrollieren, ggf. Deadlines ändern
				// start ggf. neue Auktion -> Turnpenalty, deadline, profit berücksichtigen, evtl. auch andere Orders

				// TODO aktuell senden wir immer eine start auction message, mit der unveränderten deadline
				AuctionMessage startAuction = new AuctionMessage();
				startAuction.orderId = orderMessage.order.id;
				startAuction.deadline =  orderMessage.order.deadline;
				startAuction.orderPosition =  orderMessage.order.position;
				for (Worker worker:initialWorkers) {
					sendMessage(workerInformationList.get(worker.id).agentDescription.getMessageBoxAddress(), startAuction);
				}

			}

			if (payload instanceof AuctionResponse){
				// angebote kommen an

				AuctionResponse response = (AuctionResponse) message.getPayload();

				// wenn order von einem Worker nicht angenommen wird
				if(response.status == Result.FAIL){
					//server deadline, kein anderer kann die Order mehr übernehmen
					// TODO Wenn die Zeit vorbei is wollen wir immer verfallen lassen, oder den Worker mit bestem Angebot zuweisen

					for (Order order : currentOrders) {
						if(response.orderId == order.id){
							if(time - order.created > 2 && bestOffers.get(response.orderId) != null){
								// notify Server
								TakeOrderMessage takeOrder = new TakeOrderMessage();
								takeOrder.orderId = response.orderId;
								takeOrder.brokerId = thisAgent.getAgentId();
								sendMessage(server, takeOrder);
								// TODO Aufgabe an den Worker mit aktuell bestem Offer geben
								// TODO ggf. modifikator an deadline anbringen
								AssignOrderMessage assignOrder = new AssignOrderMessage();
								assignOrder.order = order;
								assignOrder.server = this.server;
								assignOrder.gameId = this.gameId;
								// an worker mit bestem angebot
								sendMessage(bestOffers.get(order.id).sender, assignOrder);

							}
						}
					}

					continue;
				}

				if(bestOffers.get(response.orderId) == null ||response.deadlineOffer < bestOffers.get(response.orderId).deadlineOffer){
					bestOffers.put(response.orderId, response);
				}
				// auswerten:
				// will ich die Auktion fortführen
				// 3 Runden Zeit vom Server, reward  max(value - (turnCompleted - turnCreated) * turnPenalty, 0), wenn reward 0, auktion ende
				// wenn alle ablehnen, takeordermsg mit fail
				// wann wird takeordermsg an server gesendet?
				// nach takeorderconfirm ausgewählten worker benachrichtigen



				if(bestOffers.get(response.orderId) == null){

				}else{

				}
			}

			/* TODO other message formats required for the iterative net protocol */

			if (payload instanceof EndGameMessage) {

				EndGameMessage endGameMessage = (EndGameMessage) message.getPayload();
				// TODO lernen lernen lernen lol
				System.out.println("Reward: " + endGameMessage.totalReward);

			}

		}
	time ++;
	// letzte Klammer vom execute
	}


	/** example function for using getAgentNode() and retrieving a list of all worker agents */
	private List<IAgentDescription> getMyWorkerAgents(int maxNum) {
		String nodeId = thisAgent.getAgentNode().getUUID();
		return thisAgent.searchAllAgents(new AgentDescription(
				null,
				null,
				null,
				null,
				null,
				nodeId)).stream()
						.filter(a -> a.getName().startsWith("WorkerAgent"))
						.limit(maxNum)
						.collect(Collectors.toList());
	}

	/** example function to send messages to other agents */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("BROKER SENDING " + payload);
	}

}
