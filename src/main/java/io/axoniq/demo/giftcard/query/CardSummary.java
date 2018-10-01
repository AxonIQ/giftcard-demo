package io.axoniq.demo.giftcard.query;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * JPA entity class for read-model card summary records. To support easy updates to the model Vaadin-side,
 * we implement equals and hashcode based on id only.
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "CardSummary.fetch",
                query = "SELECT c FROM CardSummary c WHERE c.id LIKE CONCAT(:idStartsWith, '%') ORDER BY c.id"),
        @NamedQuery(name = "CardSummary.count",
                query = "SELECT COUNT(c) FROM CardSummary c WHERE c.id LIKE CONCAT(:idStartsWith, '%')")})
@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CardSummary {

    @Id @EqualsAndHashCode.Include private String id;
    private int initialValue;
    private int remainingValue;

}
