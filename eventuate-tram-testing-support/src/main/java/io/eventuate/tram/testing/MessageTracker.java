package io.eventuate.tram.testing;

import io.eventuate.tram.commands.common.Command;
import io.eventuate.tram.commands.common.CommandMessageHeaders;
import io.eventuate.tram.events.common.DomainEvent;
import io.eventuate.tram.events.common.EventMessageHeaders;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.consumer.MessageConsumer;
import io.eventuate.util.test.async.Eventually;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.fail;

/**
 * Subscribes to a specified set of channels
 * Provides methods for asserting that a command or event was received
 */
public class MessageTracker {

  private Set<String> channels;

  private LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();

  public MessageTracker(Set<String> channels, MessageConsumer messageConsumer) {
    this.channels = channels;
    messageConsumer.subscribe("MessageTracker-messages-" + System.currentTimeMillis(), channels, this::handleMessage);
  }

  private void handleMessage(Message message) {
    messages.add(message);
  }

  private void validateChannel(String commandChannel) {
    if (!channels.contains(commandChannel))
      throw new IllegalArgumentException(String.format("%s is not one of the specified channels: %s", commandChannel, channels));
  }

  public void reset() {
    messages.clear();
  }

  private List<Message> getMessages() {
    return Arrays.asList(this.messages.toArray(new Message[this.messages.size()]));
  }

  public <C extends Command> void assertCommandMessageSent(String channel, Class<C> expectedCommandClass) {
    validateChannel(channel);
    Eventually.eventually(() -> {
      List<Message> messages = getMessages();
      if (messages.stream()
              .noneMatch(m -> m.getHeader(CommandMessageHeaders.COMMAND_TYPE).map(ct -> ct.equals(expectedCommandClass.getName())).orElse(false)))
        fail(String.format("Cannot find command message of type %s in %s", expectedCommandClass.getName(), messages));
    });
  }


  public void assertDomainEventPublished(String channel, String expectedDomainEventClassName) {
    Class<DomainEvent> eventClass;
    try {
      eventClass = (Class<DomainEvent>) Class.forName(expectedDomainEventClassName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertDomainEventPublished(channel, eventClass);
  }

  public <C extends DomainEvent> void assertDomainEventPublished(String channel, Class<C> expectedDomainEventClass) {
    validateChannel(channel);
    Eventually.eventually(() -> {
      List<Message> messages = getMessages();
      if (messages.stream()
              .noneMatch(m -> m.getHeader(EventMessageHeaders.EVENT_TYPE).map(ct -> ct.equals(expectedDomainEventClass.getName())).orElse(false)))
        fail(String.format("Cannot find domain eventmessage of type %s in %s", expectedDomainEventClass.getName(), messages));
    });
  }
}
