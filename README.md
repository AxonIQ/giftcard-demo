# giftcard-demo-series
The Axon Framework Giftcard demo applications focus around a simple gift card domain, designed to show various aspects of the framework. The app can be run in various modes, using [Spring-boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-profiles.html): by selecting a specific profile, only the corresponding parts of the app will be active. Select none, and the default behaviour is activated, which activates everything. This way you can experiment with Axon in a (structured) monolith as well as in micro-services.


## Background story
See [the wikipedia article](https://en.wikipedia.org/wiki/Gift_card) for a basic definition of gift cards. Essentially, there are just two events in the life cycle of a gift card:
* They get _issued_: a new gift card gets created with some amount of money stored.
* They get _redeemed_: all or part of the monetary value stored on the gift card is used to purchase something.

## Structure of the App
The Giftcard application is split into four parts, using four sub-packages of `io.axoniq.demo.giftcard`:
* The `api` package contains the ([Kotlin](https://kotlinlang.org/)) sourcecode of the messages and entity. They form the API (sic) of the application.
* The `command` package contains the GiftCard Aggregate class, with all command- and associated eventsourcing handlers.
* The `query` package provides the query handlers, with their associated event handlers.
* The `gui` package contains the [Vaadin](https://vaadin.com/)-based Web GUI.

Of these packages, `command`, `query`, and `gui` are also configured as profiles.

## Building the Giftcard app from the sources
To build the demo app, simply run the provided [Maven wrapper](https://www.baeldung.com/maven-wrapper):

    ```
        mvnw clean package
    ```
Note that for Mac OSX or Linux you probably have to add "`./`" in front of `mvnw`.

## Running Axon Server

By default the Axon Framework is configured to expect a running Axon Server instance, and it will complain if the server is not found. A copy of the server JAR file has been provided in the demo package. You can run it locally, in a Docker container (including Kubernetes or even Mini-kube), or on a separate server.

### The Axon Server HTTP server

Axon Server provides two servers; one serving HTTP requests, the other gRPC. By default these use ports 8024 and 8124 respectively, but you can change these in the settings.

The HTTP server has in its root context a management Web GUI, a health indicator is available at `/actuator/health`, and the REST API at `/v1`. The API's Swagger endpoint finally, is available at `/swagger-ui.html`, and gives the documentation on the REST API.

### Basic Axon Server configuration

To run Axon Server, you'll need a Java runtime (JRE versions 8 through 10 are currently supported, Java 11 still has Spring-boot related growing-pains), a location on disk to let it store data, and an `axonserver.properties` file. To start with the last, the following properties should be looked at:

* `axoniq.axonserver.hostname`

    This is the hostname clients will use to connect to the server. Axon Server will actually enforce this usage, by redirecting clients if the initially connect asing an alias or an IP address. Note that an IP address itself can be used here as well, in which case the redirect will go to the provided IP address.
* `server.port`

    This is the port where Axon Server will listen for HTTP requests.
* `axoniq.axonserver.port`

    This is the port where Axon Server will listen for gRPC requests.
* `axoniq.axonserver.event.storage`

    This setting determines where event messages are stored. If you are planning to run in production with Axon Server, you'll want to make sure there is enough diskspace here. Losing this data means losing your Events-sourced Aggregates' state! Cenversely, if you want a quick way to start from scratch, here's where to clean.
* `axoniq.axonserver.controldb-path`

    This setting determines where the message hub stores its information. Losing this data will affect Axon Server's ability to determine which applications are connected,and what types of messages they are interested in.

Following the above, a simple test setup can be configured with:

    ```
        axoniq.axonserver.hostname=localhost
        axoniq.axonserver.event.storage=./data
        axoniq.axonserver.controldb-path=./data
    ```
This implies Axon Server and the client (the Giftcard app) will run on the same machine, and data is stored in a subdirectory called `data`, relative to where you start Axon Server.

### Running Axon Server in a Docker container

If you want a simple setup for Docker, use the supplied files in the `docker` subdirectory. Please note this is not a fully worked-out setup for a production container, but it will work fine for development. To run it, give the following command:

    ```
        $ docker build --tag my-axon-server -f docker/Dockerfile .
        ...
        $ docker run -d --name my-axon-server -p 8024:8024 -p 8124:8124 my-axon-server
    ```

## Running the Giftcard app

The simplest way to run the app is by using the Spring-boot maven plugin:

    ```
        mvnw spring-boot:run
    ```
However, if you have copied the jar file `giftcard-distributed-1.0.jar` from the Maven `target` directory to some other location, you can also start it with:

    ```
        java -jar giftcard-distributed-1.0.jar
    ```
The Web GUI can be found at [`http://localhost:8080`](http://localhost:8080).

If you want to activate only the `command` profile, use:

    ```
        java -Dspring.profiles.active=command giftcard-distributed-1.0.jar
    ```
Idem for `query` and `gui`.

### Running the Giftcard app as micro-services

To run the Giftcard app as if it were three seperate micro-services, use the Spring-boot `spring.profiles.active` option as follows:

    ```
        $ java -Dspring.profiles.active=command -jar giftcard-distributed-1.0.jar
    ```
This will start only the command part. To complete the app, open two other command shells, and start one with profile `query`, and the last one with `gui`. Again you can open the Web GUI at [`http://localhost:8080`](http://localhost:8080). The three parts of the application work together through the running instance of the Axon Server, which distributes the Commands, Queries, and Events.