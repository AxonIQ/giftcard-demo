package io.axoniq.demo.giftcard.api;

import io.axoniq.plugin.data.protection.annotation.SensitiveData;
import io.axoniq.plugin.data.protection.annotation.SensitiveDataHolder;
import io.axoniq.plugin.data.protection.annotation.SubjectId;
import org.axonframework.serialization.Revision;

/**
 * Example event for data protection plugin config generation
 *
 * @param id
 * @param ssn
 * @param address
 */
@SensitiveDataHolder
//Only needs to be placed on the events, not on any contained objects such as the address in this example.
@Revision("1")
public record ExampleEvent(
        @SubjectId String id,
        @SensitiveData String ssn,
        Address address
) {

}