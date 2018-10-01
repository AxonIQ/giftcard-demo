package io.axoniq.demo.giftcard.query;


import java.util.Objects;

public class CountCardSummariesQuery {

    private CardSummaryFilter filter;

    public CountCardSummariesQuery() {
    }

    public CountCardSummariesQuery(CardSummaryFilter filter) {
        this.filter = filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountCardSummariesQuery that = (CountCardSummariesQuery) o;
        return Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filter);
    }

    public CardSummaryFilter getFilter() {
        return filter;
    }

    public void setFilter(CardSummaryFilter filter) {
        this.filter = filter;
    }
}
