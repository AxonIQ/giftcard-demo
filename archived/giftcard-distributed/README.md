# giftcard-distributed

This example is functionally the same as the giftcard-multiconfig-monolith, but split out across
3 microservices:
* gc-gui: the Vaadin GUI part of the application
* gc-command: contains the GiftCard Aggregate handling the commands
* gc-query: contains the query handler for the GiftCard overview

The gc-common project describes the command, query and event messages being exchanged.

The example relies on AxonHub (currently still in beta) for communication between the components.
Please contact sales@axoniq.io if you are interested in getting access to the AxonHub beta.
