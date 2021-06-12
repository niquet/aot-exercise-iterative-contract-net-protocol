package de.dailab.jiactng.aot.gridworld.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.Worker;
import de.dailab.jiactng.aot.gridworld.model.WorkerAction;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.*;


/**
 * You can use this stub as a starting point for your worker bean.
 */



public class WorkerBean extends AbstractAgentBean {

	private final Map<String, Order> currentOrders = new HashMap<>();
	private final Map<Order, ICommunicationAddress> orderToAddress = new HashMap<>();
	private final Comparator<Order> compareOrder = new Comparator<Order>() {
		@Override
		public int compare(Order o1, Order o2) {
			int p1 = position.distance(o1.position);
			int p2 = position.distance(o2.position);
			if(p1 < p2) return -1;
			if(p1 > p2) return 1;
			return 0;
		}
	};
	private final PriorityQueue<Order> priorityQueue = new PriorityQueue<>(compareOrder);
	private Order handleOrder = null;
	private Boolean hasArrivedAtTarget = false;
	private Integer gameId = null;

	private Position position = null;
	private WorkerAction lastMove = null;
	private Boolean lastMoveFailed = false;

	private String workerIdForServer = null;
	private ICommunicationAddress broker = null;
	private int time;

	private ArrayList<ICommunicationAddress> otherWorkers = null;

	@Override
	public void doStart() throws Exception {
		/* TODO */

		memory.attach(new MessageObserver(), new JiacMessage());

	}


	@Override
	public void execute() {
		/* Update time to be aware of expired Offers */
		time += 1;

		// if we already have assignments
		if(!priorityQueue.isEmpty()) {

			Order firstOrder = priorityQueue.peek();

			if(time > firstOrder.deadline) {
				priorityQueue.poll();
				if(!priorityQueue.isEmpty())
					firstOrder = priorityQueue.peek();
				else return;
			}

			/**
			 * We handle the order
			 * send message to server
			 */

			if(position == null)
				return;

			/* Kann durch den ACO Worker alles geändert werden, aber für funktionalität lass ich es erstmal so */

			WorkerMessage move = new WorkerMessage();
			move.action = getNextMove(position, firstOrder.position, lastMoveFailed);
			lastMove = move.action;
			move.gameId = gameId;
			move.workerId = workerIdForServer;

			sendMessage(orderToAddress.get(firstOrder), move);

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

				if (payload instanceof ACOMessage){
					/** Initialize position and stuff */
					ACOMessage acoMessage = (ACOMessage) message.getPayload();
					workerIdForServer = acoMessage.workerId;
					position = acoMessage.position;
					gameId = acoMessage.gameId;
					broker = message.getSender();
					if (otherWorkers == null) {
						otherWorkers = acoMessage.workerAddressList;
					}

					/* TODO Spielfeld initialisieren, obstacles und andere Worker speichern? */

					ACOConfirm acoConfirm = new ACOConfirm();
					acoConfirm.state = Result.SUCCESS;
					acoConfirm.workerAgentId = thisAgent.getAgentId();
					acoConfirm.gameId = gameId;

					sendMessage(broker, acoConfirm);

					log.info("WORKER RECEIVED " + acoMessage.toString());
				}

				if (payload instanceof AssignOrderMessage) {
					/** Order to assign to the agent */
					if (gameId == null) gameId = ((AssignOrderMessage) message.getPayload()).gameId;

					//ICommunicationAddress broker = message.getSender();

					AssignOrderMessage assignOrderMessage = (AssignOrderMessage) message.getPayload();

					Order order = assignOrderMessage.order;
					ICommunicationAddress server = assignOrderMessage.server;

					AssignOrderConfirm assignOrderConfirm = new AssignOrderConfirm();
					assignOrderConfirm.orderId = order.id;
					assignOrderConfirm.gameId = assignOrderMessage.gameId;
					assignOrderConfirm.workerId = thisAgent.getAgentId();
					assignOrderConfirm.state = Result.FAIL;

					orderToAddress.put(order, server);
					priorityQueue.add(order);
					assignOrderConfirm.state = Result.SUCCESS;

					if (priorityQueue.contains(order) && handleOrder == null)
						handleOrder = order;
					sendMessage(broker, assignOrderConfirm);
				}

				if(payload instanceof AuctionMessage) {

					AuctionMessage auctionMessage = (AuctionMessage) message.getPayload();
					broker = message.getSender();
					if (position != null) {
						/* Calculated distance, considering already taken Assignments, hier Intelligenz einbauen */
						AuctionResponse auctionResponse = new AuctionResponse();
						auctionResponse.orderId = auctionMessage.orderId;
						auctionResponse.gameId = gameId;
						auctionResponse.sender = thisAgent.getAgentDescription().getMessageBoxAddress();
						if (auctionMessage.orderPosition == null || position == null){
							auctionResponse.status = Result.FAIL;
					} else {
							auctionResponse.deadlineOffer = possibleEnd(auctionMessage.orderPosition) + time;
							if (auctionResponse.deadlineOffer >= auctionMessage.deadline)
								auctionResponse.status = Result.FAIL;
							else auctionResponse.status = Result.SUCCESS;
						}
						sendMessage(broker, auctionResponse);
					}
				}

				if(payload instanceof DefinitivBidMessage){
					DefinitivBidMessage bid = (DefinitivBidMessage) message.getPayload();

					DefinitivBidMessage answer = new DefinitivBidMessage();
					answer.orderId = bid.orderId;
					answer.deadlineOffer = bid.deadlineOffer;
					answer.status = Result.SUCCESS;
					if(bid.deadlineOffer < possibleEnd(bid.orderPosition)) answer.status = Result.FAIL;

					sendMessage(broker, answer);
				}

				if(payload instanceof DefinitivRejectMessage){
					DefinitivRejectMessage reject = (DefinitivRejectMessage) message.getPayload();
					if(priorityQueue.contains(reject.order)) priorityQueue.remove(reject.order);
				}

				if (payload instanceof WorkerConfirm) {

					WorkerConfirm workerConfirm = (WorkerConfirm) message.getPayload();
					Result result = workerConfirm.state;

					if(workerConfirm.action == WorkerAction.ORDER){
						priorityQueue.poll();
						System.out.println("SUCCESS " + handleOrder);
						handleOrder = priorityQueue.peek();
						hasArrivedAtTarget = false;
					}

					if (result == Result.FAIL) {
						// TODO unbekannte obstacles
						if(workerConfirm.action != WorkerAction.ORDER) {
							lastMoveFailed = true;
							for(ICommunicationAddress workerAddress: otherWorkers) {
								PositionUpdate obstacleUpdate = new PositionUpdate();
								obstacleUpdate.gameId = gameId;
								obstacleUpdate.position = denoteObstacle(lastMove);
								obstacleUpdate.workerAgentId = thisAgent.getAgentId();
								sendMessage(workerAddress, obstacleUpdate);
							}

						}

						return;
					}

					if (!hasArrivedAtTarget) {
						// Agent hasn't arrived at target, so conduct the planned move
						doMove(workerConfirm.action);
						lastMoveFailed = false;
					}
				}

				if (payload instanceof PositionUpdate) {

					if (((PositionUpdate) payload).workerAgentId.equals(thisAgent.getAgentId())) {
						// TODO Ignore
						return;
					}

					// TODO let the obstacle magic happen

				}

			}
		}
	}

	private int possibleEnd(Position target){
		Position goal = this.position;
		int zeit = 0;
		/* TODO wo rein damit gewinn maximiert ?? */
		for (Order order: this.priorityQueue) {
			zeit += order.position.distance(goal) + 1;
			goal = order.position;
		}
		zeit += target.distance(goal);
		return zeit;
	}

	/** example function to send messages to other agents */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("WORKER SENDING " + payload);
	}

	/** sort the orders according to a score and put them into a queue
	 *
	 */
	private void sortOrders() {
		// TODO
	}

	/** evaluate order score */
	private void evaluateOrder(Order order) {
		// TODO
	}

	/** calculate next move */
	private WorkerAction getNextMove(Position current, Position target, Boolean lastMoveFailed) {
		// TODO
		if (current.equals(target)) {
			hasArrivedAtTarget = true;
			return WorkerAction.ORDER;
		}

		int[] distances = null;

		if (lastMoveFailed) {

			switch (lastMove) {
				case NORTH:
				case SOUTH:
					Position E = new Position(current.x + 1, current.y);
					Position W = new Position(current.x - 1, current.y);
					distances = new int[]{target.distance(E), target.distance(W)};
					return (distances[0] > distances[1]) ? WorkerAction.EAST:WorkerAction.WEST;
				case EAST:
				case WEST:
					Position N = new Position(current.x, current.y - 1);
					Position S = new Position(current.x, current.y + 1);
					distances = new int[]{target.distance(N), target.distance(S)};
					return (distances[0] > distances[1]) ? WorkerAction.NORTH:WorkerAction.SOUTH;
			}

		} else {

			// [N, S, E, W]
			Position N = new Position(current.x, current.y - 1);
			Position S = new Position(current.x, current.y + 1);
			Position E = new Position(current.x + 1, current.y);
			Position W = new Position(current.x - 1, current.y);

			distances = new int[]{target.distance(N), target.distance(S), target.distance(E), target.distance(W)};

		}

		WorkerAction workerAction = null;
		int index = -1;
		int min = Integer.MAX_VALUE;

		for (int i = 0; i < distances.length; i++) {

			if (distances[i] < min) {
				min = distances[i];
				index = i;
			}

		}

		switch(index) {
			case 1:
				workerAction = WorkerAction.SOUTH;
				break;
			case 2:
				workerAction = WorkerAction.EAST;
				break;
			case 3:
				workerAction = WorkerAction.WEST;
				break;
			case 0:
				workerAction = WorkerAction.NORTH;
				break;
			default:
				workerAction = WorkerAction.ORDER;
				break;
		}

		return workerAction;

	}

	private void doMove(WorkerAction action) {
		if (action == WorkerAction.NORTH) position = new Position(position.x, position.y - 1);
		if (action == WorkerAction.SOUTH) position = new Position(position.x, position.y + 1);
		if (action == WorkerAction.WEST)  position = new Position(position.x - 1, position.y);
		if (action == WorkerAction.EAST)  position = new Position(position.x + 1, position.y);
	}

	private Position denoteObstacle(WorkerAction action) {
		Position obstacle = null;
		switch(action) {
			case NORTH:
				obstacle = new Position(position.x, position.y - 1);
				break;
			case SOUTH:
				obstacle = new Position(position.x, position.y + 1);
				break;
			case WEST:
				obstacle = new Position(position.x - 1, position.y);
				break;
			case EAST:
				obstacle = new new Position(position.x + 1, position.y);
				break;
		}
		return obstacle;
	}
}
