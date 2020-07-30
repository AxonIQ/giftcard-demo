package io.axoniq.demo.giftcard;

import com.palantir.docker.compose.DockerComposeExtension;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.IssuedEvt;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Scenario:
 * - given an axon-server with a huge amount of events to process
 * - given a tracking-processor consumer
 * - when the consumer is restarted often
 * - then the number of overall events need to be consistent
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = GcApp.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles(/*NONE*/)
public class RestartTest {

    @RegisterExtension
    static final DockerComposeExtension AXON_SERVER = DockerComposeExtension
            .builder()
            .file("src/test/resources/docker-compose-restarttest.yml")
            .waitingForService(
                    "axonserver",
                    HealthChecks.toRespond2xxOverHttp(
                            8024,
                            (port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")
                    )
            )
            .shutdownStrategy(ShutdownStrategy.GRACEFUL)
            .saveLogsTo("target/docker-compose-restarttest.logs")
            .build();

    static final Container GIFTCARD = AXON_SERVER.containers().container("giftcard");

    static final long NUMBER_OF_EVENTS = 1L << 16; // 1<<32 = 4.294.967.296

    static final Random R = new Random(1L);

    @Autowired
    EventGateway eventGateway;

    @Autowired
    QueryGateway queryGateway;

    @Test
    @Order(0)
    public void resetTokenStore() throws IOException {
        // wipe token-stores for dockerized giftcards and events-populator
        Files.deleteIfExists(
                Paths.get("./target/giftcard-volume/database.mv.db")
        );

        Files.deleteIfExists(
                Paths.get("./target/giftcard-volume/database.trace.db")
        );

        Files.deleteIfExists(
                Paths.get("./target/giftcard-volume/database.lock.db")
        );
    }


    @Test
    @Order(1)
    public void eventsPopulate() {

        // generate randomized events at sufficiently large scale
        for (long i = 0L; i < NUMBER_OF_EVENTS; i++)
            eventGateway.publish(
                    new IssuedEvt(
                            UUID.randomUUID().toString(),
                            R.nextInt()
                    )
            );
    }

    @Disabled
    @RepeatedTest(10)
    @Order(2)
    public void testRestartingConsumer() throws IOException, InterruptedException {
        try {
            GIFTCARD.start();

            Thread.sleep(300L);
        } finally {
            GIFTCARD.kill();
        }
    }

    @Test
    @Order(3)
    public void eventCompletenessCheck() throws ExecutionException, InterruptedException, IOException {
        try {
            // TODO disable further event handling here
            GIFTCARD.start();

            Thread.sleep(16000L);

            CompletableFuture<CountCardSummariesResponse> query = queryGateway.query(
                    new CountCardSummariesQuery(),
                    CountCardSummariesResponse.class
            );

            int issued_giftcards_count = query.get().getCount();

            assertEquals(
                    NUMBER_OF_EVENTS,
                    issued_giftcards_count,
                    "There should not be skipped events."
            );
        } finally {
            GIFTCARD.stop();
        }
    }
}
