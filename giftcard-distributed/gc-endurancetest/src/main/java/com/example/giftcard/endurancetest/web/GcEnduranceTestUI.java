package com.example.giftcard.endurancetest.web;

import com.example.giftcard.endurancetest.EnduranceTestInfo;
import com.example.giftcard.endurancetest.GcEnduranceTest;
import com.google.common.collect.Lists;
import com.vaadin.annotations.Push;
import com.vaadin.data.HasValue;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        TextField maxDelay = new TextField("Max delay (ms)", "500");

        CheckBox enableTimeLimit = new CheckBox("Enable time limit", false);
        TextField duration = new TextField("Duration", "1");
        duration.setReadOnly(true);
        ComboBox<TimeUnit> durationTimeUnit = new ComboBox<>("Duration time unit",
                                                             Arrays.stream(TimeUnit.values())
                                                                   .collect(Collectors.toList()));
        duration.setReadOnly(true);
        durationTimeUnit.setSelectedItem(TimeUnit.HOURS);
        enableTimeLimit.addValueChangeListener((HasValue.ValueChangeListener<Boolean>) event -> {
            Boolean readonly = !event.getValue();
            duration.setReadOnly(readonly);
            durationTimeUnit.setReadOnly(readonly);
        });

        HorizontalLayout enableTimeLimitLayout = new HorizontalLayout(duration, durationTimeUnit);

        Button.ClickListener startClickListener = event -> {
            int parallelismValue = getIntegerValue(parallelism);
            int maxDelayValue = getIntegerValue(maxDelay);
            if (enableTimeLimit.getValue()) {
                int durationValue = getIntegerValue(duration);
                TimeUnit durationTimeUnitValue = durationTimeUnit.getValue();
                gcEnduranceTest.start(parallelismValue, maxDelayValue, durationValue, durationTimeUnitValue);
            } else {
                gcEnduranceTest.start(parallelismValue, maxDelayValue);
            }
        };

        Button startButton = new Button("Start", startClickListener);
        Button stopButton = new Button("Stop", event -> gcEnduranceTest.stop());

        HorizontalLayout buttonLayout = new HorizontalLayout(startButton, stopButton);

        FormLayout form = new FormLayout(parallelism, maxDelay, enableTimeLimit, enableTimeLimitLayout, buttonLayout);
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
        exceptionsArea.setRows(20);
        exceptionsArea.setWidth(100, Unit.PERCENTAGE);

        scheduledExecutorService.scheduleWithFixedDelay(() -> access(() -> {
            EnduranceTestInfo enduranceTestInfo = gcEnduranceTest.getInfo();
            startedTestCases.setValue("" + enduranceTestInfo.getStartedTestCases());
            successfulCommands.setValue("" + enduranceTestInfo.getSuccessfulCommands());
            failedCommands.setValue("" + enduranceTestInfo.getFailedCommands().size());
            String exceptions = enduranceTestInfo.getFailedCommands().stream()
                                                 .map(failedCommandInfo ->
                                                              failedCommandInfo.getCommand().getCommandName()
                                                                      + " -> " + failedCommandInfo.getCause()
                                                                                                  .getMessage())
                                                 .collect(Collectors.joining("\n"));

            exceptions += "\n" + enduranceTestInfo.getExceptions().stream()
                                                  .map(Throwable::getMessage)
                                                  .collect(Collectors.joining("\n"));
            exceptionsArea.setValue(exceptions.trim());
        }), 0, 2, TimeUnit.SECONDS);

        VerticalLayout basicInfo = new VerticalLayout(startedTestCases, successfulCommands, failedCommands);
        HorizontalLayout layout = new HorizontalLayout(basicInfo, exceptionsArea);
        layout.setWidth(600, Unit.PIXELS);
        layout.setMargin(true);
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
