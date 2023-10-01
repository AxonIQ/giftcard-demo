package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.*;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Profile("query")
@Service
@ProcessingGroup("card-summary")
public class CardSummaryProjection {

    private static final String INSERT = """
            INSERT INTO cardsummary (id, initialValue, remainingValue, issued, lastUpdated)
            VALUES (:id, :initialValue, :remainingValue, :issued, :lastUpdated)
            """;
    private static final String UPDATE = """
            UPDATE cardsummary
            SET initialValue = :initialValue,
                remainingValue = :remainingValue,
                issued = :issued,
                lastUpdated = :lastUpdated
            WHERE id = :id
            """;

    private static final String QUERY_BY_ID = """
            SELECT id, initialValue, remainingValue, issued, lastUpdated
            FROM cardsummary
            WHERE id = :id
            """;

    private static final String QUERY_ALL = """
            SELECT id, initialValue, remainingvalue, issued, lastupdated
            FROM cardsummary
            ORDER BY lastupdated
            LIMIT :limit
            """;

    private static final String COUNT = """
            SELECT count(*) AS size
            FROM cardsummary
            """;

    private static final String ISSUED = """
            SELECT MAX(issued) AS issued
            FROM cardsummary
            """;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private final QueryUpdateEmitter queryUpdateEmitter;

    public CardSummaryProjection(QueryUpdateEmitter queryUpdateEmitter) {
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @EventHandler
    public void on(CardIssuedEvent event, @Timestamp Instant timestamp) {
        /*
         * Update our read model by inserting the new card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardSummary summary = CardSummary.issue(event.id(), event.amount(), timestamp);
        jdbcTemplate.update(INSERT, parameterSource(summary));

        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type CountCardSummariesQuery,
         * - for any CountCardSummariesQuery, since true is returned by default, and
         * - send a message that the count of queries matching this query has been changed.
         */
        queryUpdateEmitter.emit(CountCardSummariesQuery.class,
                query -> true,
                new CountCardSummariesResponse(cardCount(), lastUpdate()));
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type CountCardSummariesQuery,
         * - for any CountCardSummariesQuery, since true is returned by default, and
         * - send a message that the count of queries matching this query has been changed.
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, query -> true, summary);
    }

    @EventHandler
    public void on(CardRedeemedEvent event, @Timestamp Instant timestamp) {
        /*
         * Update our read model by updating the existing card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardSummary summary = jdbcTemplate.queryForObject(QUERY_BY_ID,
                new MapSqlParameterSource("id", event.id()),
                cardSummaryRowMapper()).redeem(event.amount(), timestamp);
        jdbcTemplate.update(UPDATE, parameterSource(summary));
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type FetchCardSummariesQuery
         * - for any FetchCardSummariesQuery, since true is returned by default, and
         * - send a message containing the new state of this gift card summary
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, query -> true, summary);
    }

    @EventHandler
    public void on(CardCanceledEvent event, @Timestamp Instant timestamp) {
        /*
         * Update our read model by updating the existing card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardSummary summary = jdbcTemplate.queryForObject(QUERY_BY_ID,
                new MapSqlParameterSource("id", event.id()),
                cardSummaryRowMapper()).cancel(timestamp);
        jdbcTemplate.update(UPDATE, parameterSource(summary));
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type FetchCardSummariesQuery
         * - for any FetchCardSummariesQuery, since true is returned by default, and
         * - send a message containing the new state of this gift card summary
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, query -> true, summary);
    }

    @QueryHandler
    public List<CardSummary> handle(FetchCardSummariesQuery query) {
        return jdbcTemplate.query(QUERY_ALL, new MapSqlParameterSource("limit", query.limit()), cardSummaryRowMapper());
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        return new CountCardSummariesResponse(cardCount(), lastUpdate());
    }

    @NotNull
    private static MapSqlParameterSource parameterSource(CardSummary summary) {
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("id", summary.id());
        parameterSource.addValue("initialValue", summary.initialValue());
        parameterSource.addValue("remainingValue", summary.remainingValue());
        parameterSource.addValue("issued", java.sql.Timestamp.from(summary.issued()));
        parameterSource.addValue("lastUpdated", java.sql.Timestamp.from(summary.lastUpdated()));
        return parameterSource;
    }

    @NotNull
    private static RowMapper<CardSummary> cardSummaryRowMapper() {
        return (rs, rowNum) -> new CardSummary(rs.getString("id"),
                rs.getInt("initialValue"),
                rs.getInt("remainingValue"),
                rs.getTimestamp("issued").toInstant(),
                rs.getTimestamp("lastUpdated").toInstant());
    }

    @Nullable
    private Instant lastUpdate() {
        return jdbcTemplate.queryForObject(ISSUED, EmptySqlParameterSource.INSTANCE,
                (rs, rowNum) -> rs.getTimestamp("issued").toInstant());
    }

    private int cardCount() {
        return jdbcTemplate.queryForObject(COUNT,
                new EmptySqlParameterSource(),
                (rs, rowNum) -> rs.getInt("size"));
    }
}
