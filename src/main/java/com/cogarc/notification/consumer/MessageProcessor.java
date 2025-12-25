package com.cogarc.notification.consumer;

import com.cogarc.notification.avro.OrderStatus;
import com.cogarc.notification.avro.TourAppointmentConfirmation;
import com.cogarc.notification.avro.TruckloadConfirmation;
import com.cogarc.notification.avro.UCC;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    @Component("orderStatusProcessor")
    public static class OrderStatusProcessor implements Processor {
        @Autowired
        private MessageStorage messageStorage;

        @Override
        public void process(Exchange exchange) throws Exception {
            OrderStatus orderStatus = exchange.getIn().getBody(OrderStatus.class);
            String identifier = orderStatus.getId();
            messageStorage.storeMessage("order-status", identifier);
            logger.info("Stored order status message: {}", identifier);
        }
    }

    @Component("uccProcessor")
    public static class UCCProcessor implements Processor {
        @Autowired
        private MessageStorage messageStorage;

        @Override
        public void process(Exchange exchange) throws Exception {
            UCC ucc = exchange.getIn().getBody(UCC.class);
            String identifier = ucc.getId();
            messageStorage.storeMessage("ucc", identifier);
            logger.info("Stored UCC message: {}", identifier);
        }
    }

    @Component("tourAppointmentProcessor")
    public static class TourAppointmentProcessor implements Processor {
        @Autowired
        private MessageStorage messageStorage;

        @Override
        public void process(Exchange exchange) throws Exception {
            TourAppointmentConfirmation tourAppointment = exchange.getIn().getBody(TourAppointmentConfirmation.class);
            String identifier = tourAppointment.getId();
            messageStorage.storeMessage("tour-appointment", identifier);
            logger.info("Stored tour appointment message: {}", identifier);
        }
    }

    @Component("truckloadProcessor")
    public static class TruckloadProcessor implements Processor {
        @Autowired
        private MessageStorage messageStorage;

        @Override
        public void process(Exchange exchange) throws Exception {
            TruckloadConfirmation truckload = exchange.getIn().getBody(TruckloadConfirmation.class);
            String identifier = truckload.getId();
            messageStorage.storeMessage("truckload", identifier);
            logger.info("Stored truckload message: {}", identifier);
        }
    }
}

