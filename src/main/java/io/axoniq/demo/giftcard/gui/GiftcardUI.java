package io.axoniq.demo.giftcard.gui;

import com.vaadin.annotations.Push;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import io.axoniq.demo.giftcard.api.CardSummary;
import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.IssueCmd;
import io.axoniq.demo.giftcard.api.RedeemCmd;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SpringUI
@Push
@Profile("gui")
public class GiftcardUI extends UI {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    private CardSummaryDataProvider cardSummaryDataProvider;
    private ScheduledFuture<?> updaterThread;

    public GiftcardUI(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout commandBar = new HorizontalLayout();
        commandBar.setWidth("100%");
        commandBar.addComponents(issuePanel(), bulkIssuePanel(), redeemPanel());

        Grid summary = summaryGrid();

        HorizontalLayout statusBar = new HorizontalLayout();
        Label statusLabel = new Label("Status");
        statusBar.setDefaultComponentAlignment(Alignment.MIDDLE_RIGHT);
        statusBar.addComponent(statusLabel);
        statusBar.setWidth("100%");

        VerticalLayout layout = new VerticalLayout();
        layout.addComponents(commandBar, summary, statusBar);
        layout.setExpandRatio(summary, 1f);
        layout.setSizeFull();

        setContent(layout);

        UI.getCurrent().setErrorHandler(new DefaultErrorHandler() {
            @Override
            public void error(com.vaadin.server.ErrorEvent event) {
                Throwable cause = event.getThrowable();
                logger.error("an error occured", cause);
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                Notification.show("Error", cause.getMessage(), Notification.Type.ERROR_MESSAGE);
            }
        });

        setPollInterval(1000);
        int offset = Page.getCurrent().getWebBrowser().getTimezoneOffset();
        // offset is in milliseconds
        ZoneOffset instantOffset = ZoneOffset.ofTotalSeconds(offset / 1000);
        StatusUpdater statusUpdater = new StatusUpdater(statusLabel, instantOffset);
        updaterThread = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(statusUpdater, 1000,
                                                                                5000, TimeUnit.MILLISECONDS);
        setPollInterval(1000);
        getSession().getSession().setMaxInactiveInterval(30);
        addDetachListener((DetachListener) detachEvent -> {
            logger.warn("Closing UI");
            updaterThread.cancel(true);
        });
    }

    private Panel issuePanel() {
        TextField id = new TextField("Card id");
        TextField amount = new TextField("Amount");
        Button submit = new Button("Submit");

        submit.addClickListener(evt -> {
            commandGateway.sendAndWait(new IssueCmd(id.getValue(), Integer.parseInt(amount.getValue())));
            Notification.show("Success", Notification.Type.HUMANIZED_MESSAGE)
                        .addCloseListener(e -> cardSummaryDataProvider.refreshAll());
        });

        FormLayout form = new FormLayout();
        form.addComponents(id, amount, submit);
        form.setMargin(true);

        Panel panel = new Panel("Issue single card");
        panel.setContent(form);
        return panel;
    }

    private Panel bulkIssuePanel() {
        TextField number = new TextField("Number");
        TextField amount = new TextField("Amount");
        Button submit = new Button("Submit");
        Panel panel = new Panel("Bulk issue cards");

        submit.addClickListener(evt -> {
            submit.setEnabled(false);
            new BulkIssuer(commandGateway, Integer.parseInt(number.getValue()), Integer.parseInt(amount.getValue()),
                           bulkIssuer -> {
                               access(() -> {
                                   if (bulkIssuer.getRemaining().get() == 0) {
                                       submit.setEnabled(true);
                                       panel.setCaption("Bulk issue cards");
                                       Notification.show("Bulk issue card completed",
                                                         Notification.Type.HUMANIZED_MESSAGE)
                                                   .addCloseListener(e -> cardSummaryDataProvider.refreshAll());
                                   } else {
                                       panel.setCaption(String.format("Progress: %d suc, %d fail, %d rem",
                                                                      bulkIssuer.getSuccess().get(),
                                                                      bulkIssuer.getError().get(),
                                                                      bulkIssuer.getRemaining().get()));
                                       cardSummaryDataProvider.refreshAll();
                                   }
                               });
                           });
        });

        FormLayout form = new FormLayout();
        form.addComponents(number, amount, submit);
        form.setMargin(true);

        panel.setContent(form);
        return panel;
    }

    private Panel redeemPanel() {
        TextField id = new TextField("Card id");
        TextField amount = new TextField("Amount");
        Button submit = new Button("Submit");

        submit.addClickListener(evt -> {
            commandGateway.sendAndWait(new RedeemCmd(id.getValue(), Integer.parseInt(amount.getValue())));
            Notification.show("Success", Notification.Type.HUMANIZED_MESSAGE)
                        .addCloseListener(e -> cardSummaryDataProvider.refreshAll());
        });

        FormLayout form = new FormLayout();
        form.addComponents(id, amount, submit);
        form.setMargin(true);

        Panel panel = new Panel("Redeem card");
        panel.setContent(form);
        return panel;
    }

    private Grid summaryGrid() {
        cardSummaryDataProvider = new CardSummaryDataProvider(queryGateway);
        Grid<CardSummary> grid = new Grid<>();
        grid.addColumn(CardSummary::getId).setCaption("Card ID");
        grid.addColumn(CardSummary::getInitialValue).setCaption("Initial value");
        grid.addColumn(CardSummary::getRemainingValue).setCaption("Remaining value");
        grid.setSizeFull();
        grid.setDataProvider(cardSummaryDataProvider);
        return grid;
    }

    public class StatusUpdater implements Runnable {

        private final Label statusLabel;
        private final ZoneOffset instantOffset;

        public StatusUpdater(Label statusLabel, ZoneOffset instantOffset) {
            this.statusLabel = statusLabel;
            this.instantOffset = instantOffset;
        }

        @Override
        public void run() {
            CountCardSummariesQuery query = new CountCardSummariesQuery();
            queryGateway.query(
                    query, CountCardSummariesResponse.class).whenComplete((r, exception) -> {
                if (exception == null) {
                    statusLabel.setValue(Instant.ofEpochMilli(r.getLastEvent())
                                                .atOffset(instantOffset).toString());
                }
            });
        }
    }
}
