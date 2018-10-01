# giftcard-multiconfig-monolith

This example shows, among other things, how an Axon application can easily be configured 
to run on different types of infrastructure, because Axon Framework encourages a very clear
separation between business logic and infrastructure.

Dependent on the activated profile, this application can run:
* Fully on an embedded H2 database
* On 2 separate Postgres databases
* On a combination of Postgres and AxonIQ's [AxonDB](https://axoniq.io/products/axondb.html)

The application has a small GUI running on port 8080 (implemented using [Vaadin](https://vaadin.com/)) where you can issue single cards, bulk issue cards, redeem cards,
and view a list of cards.

 