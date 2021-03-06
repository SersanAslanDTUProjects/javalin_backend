package database.unit;

import com.mongodb.WriteResult;
import database.TestDB;
import database.dao.IMessageDAO;
import database.dao.MessageDAO;
import database.dto.MessageDTO;
import database.exceptions.NoModificationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

class MessageDAOTest {
  static IMessageDAO messageDAO = new MessageDAO(TestDB.getInstance());

  @BeforeAll
  static void killAll() {
    messageDAO.deleteAllMessages();
  }

  @Test
  void createdMessageShouldBeFetchedMessage() throws NoModificationException {
    MessageDTO message = new MessageDTO.Builder()
      .setMessageString("Husk løbesko til fodbold")
      .build();

    WriteResult ws = messageDAO.createMessage(message);
    MessageDTO fetchedMessage = messageDAO.getMessage(ws.getUpsertedId().toString());
    Assertions.assertEquals(message, fetchedMessage);

    messageDAO.deleteMessage(ws.getUpsertedId().toString());
  }

  @Test
  void createTwoMessagesShouldFetchListSizeTwo() throws NoModificationException {
    MessageDTO message = new MessageDTO.Builder()
      .setMessageString("Husk løbesko til fodbold")
      .build();

    MessageDTO message2 = new MessageDTO.Builder()
      .setMessageString("Husk badebukser")
      .build();

    WriteResult wr = messageDAO.createMessage(message);
    WriteResult wr2 = messageDAO.createMessage(message2);

    List<MessageDTO> messageList = messageDAO.getMessageList();
    Assertions.assertAll(
      () -> Assertions.assertEquals(messageList.size(), 2),
      () -> Assertions.assertEquals(messageList.get(0), message),
      () -> Assertions.assertEquals(messageList.get(1), message2)
    );

    messageDAO.deleteMessage(wr.getUpsertedId().toString());
    messageDAO.deleteMessage(wr2.getUpsertedId().toString());
  }

  @Test
  void updateEventShouldFetchUpdatedEvent() throws NoModificationException {
    MessageDTO message = new MessageDTO.Builder()
      .setMessageString("Husk løbesko til fodbold")
      .build();

    WriteResult ws = messageDAO.createMessage(message);
    message.setMessageString("ny string");
    messageDAO.updateMessage(message);

    MessageDTO updatedMessage = messageDAO.getMessage(ws.getUpsertedId().toString());
    Assertions.assertEquals("ny string", updatedMessage.getMessageString());

    messageDAO.deleteMessage(ws.getUpsertedId().toString());
  }

  @Test
  void deleteAllMessagesInCollection() throws NoModificationException {
    MessageDTO message = new MessageDTO.Builder()
      .setMessageString("Husk løbesko til fodbold")
      .build();

    MessageDTO message2 = new MessageDTO.Builder()
      .setMessageString("Husk badebukser")
      .build();

    WriteResult wr = messageDAO.createMessage(message);
    WriteResult wr2 = messageDAO.createMessage(message2);

    Assertions.assertAll(
      () -> Assertions.assertNotNull(messageDAO.getMessage(wr.getUpsertedId().toString())),
      () -> Assertions.assertNotNull(messageDAO.getMessage(wr2.getUpsertedId().toString()))
    );

    messageDAO.deleteAllMessages();
    Assertions.assertThrows(NoSuchElementException.class, () -> messageDAO.getMessageList());
  }

  @Test
  void nullInCreateShouldThrowIllegalArgument() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> messageDAO.createMessage(null));
  }

  @Test
  void nullInGetShouldThrowIlleArgument() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> messageDAO.getMessage(null));
  }

  @Test
  void emptyIdInGetShouldThrowIlleArgument() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> messageDAO.getMessage(""));
  }

  @Test
  void noEventsInGetEventsShouldThrowNoSuchElements() {
    Assertions.assertThrows(NoSuchElementException.class, () -> messageDAO.getMessageList());
  }

  @Test
  void nullInUpdateShouldThrowIlleArgument() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> messageDAO.updateMessage(null));
  }

  @Test
  void nullInDeleteShouldThrowIlleArgument() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> messageDAO.deleteMessage(null));
  }

  @Test
  void emptyIdInDeleteShouldThrowIlleArgument() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> messageDAO.deleteMessage(""));
  }

}
