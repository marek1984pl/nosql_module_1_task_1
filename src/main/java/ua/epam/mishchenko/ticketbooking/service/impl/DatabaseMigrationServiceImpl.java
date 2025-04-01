package ua.epam.mishchenko.ticketbooking.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.epam.mishchenko.ticketbooking.model.Event;
import ua.epam.mishchenko.ticketbooking.model.Ticket;
import ua.epam.mishchenko.ticketbooking.model.User;
import ua.epam.mishchenko.ticketbooking.model.UserAccount;
import ua.epam.mishchenko.ticketbooking.model.mongo.EventMongo;
import ua.epam.mishchenko.ticketbooking.model.mongo.TicketMongo;
import ua.epam.mishchenko.ticketbooking.model.mongo.UserAccountMongo;
import ua.epam.mishchenko.ticketbooking.model.mongo.UserMongo;
import ua.epam.mishchenko.ticketbooking.repository.EventRepository;
import ua.epam.mishchenko.ticketbooking.service.DatabaseMigrationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DatabaseMigrationServiceImpl implements DatabaseMigrationService {

    private final EventRepository eventRepository;
    private final MongoTemplate mongoTemplate;

    @Value("${spring.properties.migration.enabled}")
    private boolean migrationEnabled;

    @Override
    @Transactional
    public void migrate() {
        if (Boolean.FALSE.equals(migrationEnabled)) {
            return;
        }

        Iterable<Event> events = eventRepository.findAll();

        List<EventMongo> mongoEvents = new ArrayList<>();

        events.forEach(event -> {
            EventMongo mongoEvent = createMongoEvent(event);
            mongoEvents.add(mongoEvent);
            mongoTemplate.save(mongoEvent, "events");
        });
    }

    private EventMongo createMongoEvent(Event sqlEvent) {
        EventMongo mongoEvent = new EventMongo();
        mongoEvent.setTitle(sqlEvent.getTitle());
        mongoEvent.setTicketPrice(sqlEvent.getTicketPrice());
        mongoEvent.setDate(sqlEvent.getDate());
        mongoEvent.setTicketPrice(sqlEvent.getTicketPrice());
        mongoTemplate.insert(mongoEvent, "events");

        List<TicketMongo> mongoTickets = sqlEvent.getTickets().stream()
                .map(sqlTicket -> createMongoTicket(sqlTicket, mongoEvent))
                .toList();

        mongoEvent.setTickets(mongoTickets);
        return mongoEvent;
    }

    private TicketMongo createMongoTicket(Ticket sqlTicket, EventMongo event) {
        TicketMongo mongoTicket = new TicketMongo();
        mongoTicket.setEvent(event);
        mongoTicket.setPlace(sqlTicket.getPlace());
        mongoTicket.setCategory(sqlTicket.getCategory());

        User user = sqlTicket.getUser();
        UserMongo mongoUser = new UserMongo();
        mongoUser.setName(user.getName());
        mongoUser.setEmail(user.getEmail());

        mongoTicket.setUser(mongoUser);

        Optional<UserAccount> userAccount = Optional.ofNullable(user.getUserAccount());
        if (userAccount.isPresent()) {
            UserAccountMongo mongoUserAccount = new UserAccountMongo();
            mongoUserAccount.setMoney(userAccount.get().getMoney());
            mongoUser.setUserAccount(mongoUserAccount);
        }
        return mongoTicket;
    }
}
