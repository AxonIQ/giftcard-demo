package com.example.giftcard.endurancetest;

import com.vaadin.annotations.Push;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Milan Savic
 */
@SpringUI
@Push
public class GcEnduranceTestUI extends UI {

    private final GcEnduranceTest gcEnduranceTest;
    private final ScheduledExecutorService scheduledExecutorService;

    public GcEnduranceTestUI(GcEnduranceTest gcEnduranceTest) {
        this.gcEnduranceTest = gcEnduranceTest;
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    protected void init(VaadinRequest request) {
        HorizontalLayout content = new HorizontalLayout(commandPanel(), infoPanel());
        content.setMargin(true);
        setContent(content);
    }

    private Panel commandPanel() {
        TextField parallelism = new TextField("Parallelism");
        TextField maxDelay = new TextField("Max delay");

        Button startButton = new Button("Start",
                                        event -> gcEnduranceTest.start(getIntegerValue(parallelism),
                                                                       getIntegerValue(maxDelay),
                                                                       TimeUnit.MILLISECONDS));
        Button stopButton = new Button("Stop", event -> gcEnduranceTest.stop());

        HorizontalLayout buttonLayout = new HorizontalLayout(startButton, stopButton);

        FormLayout form = new FormLayout(parallelism, maxDelay, buttonLayout);
        form.setMargin(true);

        Panel panel = new Panel("Command panel");
        panel.setContent(form);
        return panel;
    }

    private Panel infoPanel() {
        TextField startedCommands = new TextField("Started commands");
        TextField failedCommands = new TextField("Failed commands");
        startedCommands.setEnabled(false);
        failedCommands.setEnabled(false);

        scheduledExecutorService.scheduleWithFixedDelay(() -> access(() -> {
            EnduranceTestInfo enduranceTestInfo = gcEnduranceTest.getInfo();
            startedCommands.setValue("" + enduranceTestInfo.getStartedCommands());
            failedCommands.setValue("" + enduranceTestInfo.getFailedCommands());
        }), 0, 2, TimeUnit.SECONDS);

        VerticalLayout layout = new VerticalLayout(startedCommands, failedCommands);
        Panel panel = new Panel("Info panel");
        panel.setContent(layout);
        return panel;
    }

    private int getIntegerValue(TextField tf) {
        return Integer.parseInt(tf.getValue());
    }

    @Override
    public void close() {
        super.close();
        scheduledExecutorService.shutdown();
    }
}
