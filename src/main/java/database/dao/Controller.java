package database.dao;

import database.DALException;
import database.DataSource;
import database.collections.Event;
import database.collections.Message;
import database.collections.Playground;
import database.collections.User;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

import java.util.List;
import java.util.Set;

import static org.jongo.Oid.withOid;

public class Controller implements IController {
    private static Controller controller;
    private PlaygroundDAO playgroundDAO;
    private UserDAO userDAO;
    private MessageDAO messageDAO;
    private EventDAO eventDAO;

    private Controller() {
        this.playgroundDAO = new PlaygroundDAO();
        this.userDAO = new UserDAO();
        this.messageDAO = new MessageDAO();
        this.eventDAO = new EventDAO();
    }

    public static Controller getController() {
        if (controller == null) {
            controller = new Controller();
        }
        return controller;
    }

    public List<Playground> getAllPlaygrounds() {
        List<Playground> playgrounds = null;
        try {
            playgrounds = playgroundDAO.getPlaygroundList();
        } catch (DALException e) {
            e.printStackTrace();
        }
        return playgrounds;
    }

    @Override
    public void createPlayground(User activeUser, Playground playgroundToBeCreated) throws DALException {
        if (!hasStatus(activeUser, Controller.ADMIN))
            throw new DALException(String.format("User %s does not have %s privileges", activeUser.getId(), Controller.ADMIN));

        try {
            playgroundDAO.createPlayground(playgroundToBeCreated);
        } catch (DALException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean addPedagogueToPlayground(User activeUser, String playgroundName, String pedagogueID) throws DALException {
        if (!hasStatus(activeUser, Controller.ADMIN))
            throw new DALException(String.format("User %s does not have %s privileges", activeUser.getId(), Controller.ADMIN));

        boolean isUserUpdated = false, isPedagogueAdded = false;
        try {
            // add playground id to user
            User user = userDAO.getUser(pedagogueID);
            user.setPlaygroundID(playgroundName);
            isUserUpdated = userDAO.updateUser(user);

            // add user id to playground
            Jongo jongo = new Jongo(DataSource.getDB()); // TODO: do we create new connection or reuse?
            MongoCollection playgrounds = jongo.getCollection(IPlaygroundDAO.COLLECTION);
            isPedagogueAdded = playgrounds
                    .update("{name : #}", playgroundName)
                    .with("{$push: {assignedPedagogue : #}}", withOid(pedagogueID))
                    .wasAcknowledged();

        } catch (DALException e) {
            e.printStackTrace();
            // TODO: how do we handle if user is updated successfully but playground is not?
        }

        return isUserUpdated && isPedagogueAdded;
    }

    @Override
    public boolean removePedagogueFromPlayground(User activeUser, String playgroundName, String pedagogueID) throws DALException {
        if (!hasStatus(activeUser, Controller.ADMIN))
            throw new DALException(String.format("User %s does not have %s privileges", activeUser.getId(), Controller.ADMIN));

        boolean isUserUpdated = false, isPedagogueRemoved = false;
        try {
            // remove playground id from user
            User user = userDAO.getUser(pedagogueID);
            user.setPlaygroundID(null);
            isUserUpdated = userDAO.updateUser(user);

            // remove user id from playground
            Jongo jongo = new Jongo(DataSource.getDB());
            MongoCollection playgrounds = jongo.getCollection(IPlaygroundDAO.COLLECTION);
            isPedagogueRemoved = playgrounds
                    .update("{name : #}", playgroundName)
                    .with("{$pull: {assignedUsers : {_id : #}}}", withOid(pedagogueID))
                    .wasAcknowledged();

        } catch (DALException e) {
            e.printStackTrace();
        }

        return isUserUpdated && isPedagogueRemoved;
    }

    @Override
    public Set<Event> getEventsInPlayground(String playgroundName) throws DALException {
        return playgroundDAO.getPlayground(playgroundName).getEvents();
    }

    @Override
    public boolean addEventToPlayground(User activeUser, String playgroundName, Event eventToBeAdded) throws DALException {
        if (!hasStatus(activeUser, Controller.PEDAGOGUE))
            throw new DALException(String.format("User %s does not have %s privileges", activeUser.getId(), Controller.PEDAGOGUE));


        boolean isEventCreated = false, isPlaygroundUpdated = false;
        try {
            // create event with playground reference
            eventToBeAdded.setPlayground(playgroundName);
            isEventCreated = eventDAO.createEvent(eventToBeAdded); // TODO: make Result.class with {Success, ID} or Failure

            // update existing playground with event reference
            Jongo jongo = new Jongo(DataSource.getDB());
            MongoCollection playgrounds = jongo.getCollection(IPlaygroundDAO.COLLECTION);
            isPlaygroundUpdated = playgrounds
                    .update("{name : #", playgroundName)
                    .with("{$push: {events : #}}", withOid(eventToBeAdded.getId()))
                    .wasAcknowledged();

        } catch (DALException e) {
            e.printStackTrace();
        }

        return isEventCreated && isPlaygroundUpdated;
    }

    @Override
    public boolean updateEvent(User activeUser, String playgroundName, Event updatedEvent) throws DALException {
        if (!hasStatus(activeUser, Controller.PEDAGOGUE))
            throw new DALException(String.format("User %s does not have %s privileges", activeUser.getId(), Controller.PEDAGOGUE));

        boolean isEventUpdated = false;
        try {
            isEventUpdated = eventDAO.updateEvent(updatedEvent);
        } catch (DALException e) {
            e.printStackTrace();
        }

        return isEventUpdated;
    }

    @Override
    public boolean deleteEventInPlayground(User activeUser, String playgroundName, String eventID) throws DALException {
        if (!hasStatus(activeUser, Controller.PEDAGOGUE))
            throw new DALException(String.format("User %s does not have %s privileges", activeUser.getId(), Controller.PEDAGOGUE));

        // TODO: is it enough to only remove event from event collection?
        return eventDAO.deleteEvent(eventID);
    }

    @Override
    public boolean signupForEventInPlayground(String playgroundName, String eventID, String userID) throws DALException {
        boolean isEventUpdated, isUserUpdated;

        // update event with user id
        Jongo jongo = new Jongo(DataSource.getDB());
        MongoCollection collection = jongo.getCollection(IEventDAO.COLLECTION);
        isEventUpdated = collection
                .update("{_id : #", withOid(eventID))
                .with("{$push: {assignedUsers : #}}", withOid(userID))
                .wasAcknowledged();

        // update user with event id
        collection = jongo.getCollection(IUserDAO.COLLECTION);
        isUserUpdated = collection
                .update("{_id : #", withOid(userID))
                .with("{$push: {events : #}}", withOid(eventID))
                .wasAcknowledged();

        return isEventUpdated && isUserUpdated;
    }

    @Override
    public Set<Message> getPlaygroundMessage(String playgroundName) throws DALException {
        // maybe optional Date as 2nd param
        return playgroundDAO.getPlayground(playgroundName).getMessages();
    }

    @Override
    public boolean createPlaygroundMessage(User activeUser, String playgroundName, Message message) throws DALException {
        if (!hasStatus(activeUser, Controller.PEDAGOGUE))
            throw new DALException(String.format("User %s does not have %s privileges", activeUser.getId(), Controller.PEDAGOGUE));

        boolean isMessageCreated = false, isPlaygroundUpdated = false;
        try {
            // create message with playground id
            message.setPlaygroundID(playgroundName);
            isMessageCreated = messageDAO.createMessage(message);

            // update existing playground with message reference
            Jongo jongo = new Jongo(DataSource.getDB());
            MongoCollection playgrounds = jongo.getCollection(IPlaygroundDAO.COLLECTION);
            isPlaygroundUpdated = playgrounds
                    .update("{name : #", playgroundName)
                    .with("{$push: {messages : #}}", withOid(message.getId()))
                    .wasAcknowledged();

        } catch (DALException e) {
            e.printStackTrace();
        }

        return isMessageCreated && isPlaygroundUpdated;
    }

    @Override
    public boolean updatePlaygroundMessage(User activeUser, String playgroundName, Message updatedMessage) throws DALException {
        if (!hasStatus(activeUser, Controller.PEDAGOGUE))
            throw new DALException(String.format("User %s does not have %s privileges", activeUser.getId(), Controller.PEDAGOGUE));

        boolean isMessageUpdated = false;
        try {
            isMessageUpdated = messageDAO.updateMessage(updatedMessage);
        } catch (DALException e) {
            e.printStackTrace();
        }
        return isMessageUpdated;
    }

    @Override
    public boolean deletePlaygroundMessage(User activeUser, String playgroundName, String messageID) throws DALException {
        boolean isMessageUpdated = false, isPlaygroundUpdated = false;

        try {
            // remove playground id from message
            Message message = messageDAO.getMessage(messageID);
            message.setPlaygroundID(null);
            isMessageUpdated = messageDAO.updateMessage(message);

            // remove message id from playground
            Jongo jongo = new Jongo(DataSource.getDB());
            MongoCollection playgrounds = jongo.getCollection(IPlaygroundDAO.COLLECTION);
            isPlaygroundUpdated = playgrounds
                    .update("{name : #}", playgroundName)
                    .with("{$pull: {messages : {_id : #}}}", withOid(messageID))
                    .wasAcknowledged();

        } catch (DALException e) {
            e.printStackTrace();
        }

        return isMessageUpdated && isPlaygroundUpdated;
    }

    @Override
    public User getUser(User activeUser, String userID) throws DALException {
        if (!hasStatus(activeUser, Controller.ADMIN, Controller.PEDAGOGUE) || activeUser.getId().equals(userID)) {
            throw new DALException(String.format("User %s does not have the required privileges", activeUser.getId()));
        }

        //do we allow to fetch all user information like password etc?
        return userDAO.getUser(userID);
    }

    //todo arbejder også her
    public User getUserWithUserName(String userName) throws DALException {
        //do we allow to fetch all user information like password etc?
        return userDAO.getUserWithUserName(userName);
    }

    @Override
    public boolean createUser(User activeUser, User userToBeCreated) throws DALException {
        if (!hasStatus(activeUser, Controller.ADMIN)) {
            throw new DALException(String.format("User %s does not have %s privileges", activeUser.getId(), Controller.ADMIN));
        }
        userDAO.createUser(userToBeCreated);
        return true;
    }

    // todo - Overloading
    public void createUser(User userToBeCreated) throws DALException {
        userDAO.createUser(userToBeCreated);
    }

    @Override
    public boolean updateUser(User activeUser, User updatedUser) throws DALException {
        if (!hasStatus(activeUser, Controller.ADMIN) || activeUser.getId().equals(updatedUser.getId())) {
            throw new DALException(String.format("User %s does not have the required privileges", activeUser.getId()));
        }

        // do we allow updates on all fields? godt spørgsmål :)
        userDAO.updateUser(updatedUser);
        return true;
    }

    //todo NJL
    public void updateUser(User updatedUser) throws DALException {
        userDAO.updateUser(updatedUser);
    }


    @Override
    public void deleteUser(User activeUser, String userID) throws DALException {
        if (!hasStatus(activeUser, Controller.ADMIN)) {
            throw new DALException(String.format("User %s does not have the required privileges", activeUser.getId()));
        }

        userDAO.deleteUser(userID);
    }

    private boolean hasStatus(User activeUser, String... types) {
        for (String type : types)
            if (activeUser.getStatus().equalsIgnoreCase(type))
                return true;

        return false;
    }

    public List <User> getAllUsers() {
        List<User> users = null;
        try {
            users = userDAO.getUserList();
        } catch (DALException e) {
            e.printStackTrace();
        }
        return users;
    }
}