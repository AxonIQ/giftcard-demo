# giftcard-demo-series
Series of Axon Framework demo applications focused around a simple gift card domain, designed
to show various aspects of the framework. The examples show configurations for multiple technology
stacks: running both locally and distributed using AxonHub, and running with single or multiple relational databases,
and AxonDB as event store. All examples have a GUI built using Vaadin.

See [the wikipedia article](https://en.wikipedia.org/wiki/Gift_card) for a basic definition of gift cards. Essentially, there are just two events in the life cycle of a gift card:
* They get _issued_: a new gift card gets created with some amount of money stored.
* They get _redeemed_: all or part of the monetary value stored on the gift card is used to purchase something.
