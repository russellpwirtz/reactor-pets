package com.reactor.pets.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetStatusView;
import java.util.UUID;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for TimeTickScheduler.
 *
 * <p>Tests verify: - Scheduler starts and runs - Time ticks are processed by pets - Stats degrade
 * over time
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TimeTickScheduler Integration")
class TimeTickSchedulerTest {

  @Autowired
  private CommandGateway commandGateway;

  @Autowired
  private QueryGateway queryGateway;

  @Autowired
  private TimeTickScheduler scheduler;

  @Test
  @DisplayName("should be created and configured correctly")
  void shouldBeCreatedAndConfiguredCorrectly() {
    assertThat(scheduler).isNotNull();
  }

  @Test
  @DisplayName("should process time ticks for alive pets")
  void shouldProcessTimeTicksForAlivePets() throws Exception {
    // Create a pet
    String petId = UUID.randomUUID().toString();
    commandGateway
        .sendAndWait(
            new CreatePetCommand(
                petId, "Time Test Pet", com.reactor.pets.aggregate.PetType.DOG));

    // Get initial status
    PetStatusView initialStatus =
        queryGateway.query(new GetPetStatusQuery(petId), PetStatusView.class).join();

    assertThat(initialStatus.getHunger()).isEqualTo(30);
    assertThat(initialStatus.getHappiness()).isEqualTo(70);
    assertThat(initialStatus.getTotalTicks()).isEqualTo(0);

    // Wait for at least one tick with retry logic
    // Tick interval is 10 seconds, so wait up to 15 seconds with polling
    int maxAttempts = 15;
    int attemptDelay = 1000; // 1 second between attempts
    PetStatusView afterTickStatus = null;

    for (int i = 0; i < maxAttempts; i++) {
      Thread.sleep(attemptDelay);
      afterTickStatus =
          queryGateway.query(new GetPetStatusQuery(petId), PetStatusView.class).join();

      if (afterTickStatus.getTotalTicks() > 0) {
        break; // Time tick processed!
      }
    }

    // Verify stats have degraded
    assertThat(afterTickStatus).isNotNull();
    assertThat(afterTickStatus.getTotalTicks()).isGreaterThan(0);
    assertThat(afterTickStatus.getHunger()).isGreaterThan(initialStatus.getHunger());
    assertThat(afterTickStatus.getHappiness()).isLessThan(initialStatus.getHappiness());
  }
}
