package com.example.giftcard.endurancetest.web;

import com.example.giftcard.endurancetest.EnduranceTestInfo;
import com.example.giftcard.endurancetest.GcEnduranceTest;
import com.vaadin.annotations.Push;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.axonframework.common.ExceptionUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        TextField parallelism = new TextField("Parallelism", "1");
        TextField maxDelay = new TextField("Max delay", "500");

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
        TextField startedTestCases = new TextField("Started test cases");
        TextField successfulCommands = new TextField("Successful commands");
        TextField failedCommands = new TextField("Failed commands");
        TextArea exceptionsArea = new TextArea("Exceptions");
        startedTestCases.setReadOnly(false);
        successfulCommands.setReadOnly(false);
        failedCommands.setReadOnly(false);
        exceptionsArea.setWordWrap(true);
        exceptionsArea.setReadOnly(true);
        exceptionsArea.setSizeFull();

        scheduledExecutorService.scheduleWithFixedDelay(() -> access(() -> {
            EnduranceTestInfo enduranceTestInfo = gcEnduranceTest.getInfo();
            startedTestCases.setValue("" + enduranceTestInfo.getStartedTestCases());
            successfulCommands.setValue("" + enduranceTestInfo.getSuccessfulCommands());
            failedCommands.setValue("" + enduranceTestInfo.getFailedCommands().size());
            String exceptions = enduranceTestInfo.getFailedCommands().stream()
                                              .map(failedCommandInfo ->
                                                           failedCommandInfo.getCommand().getCommandName()
                                                                   + " -> " + failedCommandInfo.getCause().getMessage())
                                              .collect(Collectors.joining("\n"));

            exceptions += "\n" + enduranceTestInfo.getExceptions().stream()
                                                  .map(Throwable::getMessage)
                                                  .collect(Collectors.joining("\n"));
            exceptionsArea.setValue(exceptions);
        }), 0, 2, TimeUnit.SECONDS);

        VerticalLayout basicInfo = new VerticalLayout(startedTestCases, successfulCommands, failedCommands);
        HorizontalLayout layout = new HorizontalLayout(basicInfo, exceptionsArea);
        layout.setMargin(true);
        Panel panel = new Panel("Info panel");
        panel.setSizeUndefined();
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
