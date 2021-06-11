package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;

/**
 * Sent from Worker to Broker as reject proposal
 */
public class DefinitivRejectMessage extends GameMessage{
        private static final long serialVersionUID = 4591449852002025133L;


        /** the order to be performed */
        public Order order;


        @Override
        public String toString() {
            return String.format("RejectMessage");
        }
    }

