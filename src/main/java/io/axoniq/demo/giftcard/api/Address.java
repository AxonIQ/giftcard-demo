package io.axoniq.demo.giftcard.api;

import io.axoniq.plugin.data.protection.annotation.SensitiveData;

public record Address(
        @SensitiveData String addressLine1,
        String postalCode
) {

}
