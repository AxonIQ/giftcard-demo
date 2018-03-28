package com.example.giftcard.endurancetest.web;

import com.example.giftcard.endurancetest.EnduranceTestInfo;
import com.example.giftcard.endurancetest.ExceptionInfo;
import com.example.giftcard.endurancetest.GcEnduranceTest;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * UI for the endurance test where is possible to start/stop the test and check information about progress.
 *
 * @author Milan Savic
 */
@SpringUI
@Push
public class GcEnduranceTestUI extends UI {

    private final GcEnduranceTest gcEnduranceTest;
    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * Instantiates UI with reference to the endurance test.
     *
     * @param gcEnduranceTest endurance test
     */
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
        TextField successfulCommandsOneMinuteRate = new TextField("Successful commands (1 minute rate / s)");
        TextField failedCommands = new TextField("Failed commands");
        TextField failedCommandsOneMinuteRate = new TextField("Failed commands (1 minute rate / s)");
        TextField testDuration = new TextField("Test duration");
        TextArea errorsArea = new TextArea("Errors");
        startedTestCases.setReadOnly(true);
        successfulCommands.setReadOnly(true);
        successfulCommandsOneMinuteRate.setReadOnly(true);
        failedCommands.setReadOnly(true);
        failedCommandsOneMinuteRate.setReadOnly(true);
        testDuration.setReadOnly(true);
        errorsArea.setWordWrap(true);
        errorsArea.setReadOnly(true);
        errorsArea.setRows(20);
        errorsArea.setWidth(100, Unit.PERCENTAGE);

        scheduledExecutorService.scheduleWithFixedDelay(() -> access(() -> {
            EnduranceTestInfo enduranceTestInfo = gcEnduranceTest.getInfo();
            startedTestCases.setValue("" + enduranceTestInfo.getStartedTestCases());
            successfulCommands.setValue("" + enduranceTestInfo.getSuccessfulCommands());

            successfulCommandsOneMinuteRate.setValue(
                    "" + BigDecimal.valueOf(enduranceTestInfo.getSuccessfulCommandsOneMinuteRate())
                                   .setScale(2, RoundingMode.CEILING));
            failedCommands.setValue("" + enduranceTestInfo.getNumberOfFailedCommands());
            failedCommandsOneMinuteRate.setValue(
                    "" + BigDecimal.valueOf(enduranceTestInfo.getNumberOfFailedCommandsOneMinuteRate())
                                   .setScale(2, RoundingMode.CEILING));

            String exceptions = Stream.of(enduranceTestInfo.getFailedCommands(), enduranceTestInfo.getExceptions())
                                      .flatMap(List::stream)
                                      .sorted(Comparator.comparing(ExceptionInfo::getTimestamp))
                                      .map(Object::toString)
                                      .collect(Collectors.joining("\n"));
            errorsArea.setValue(exceptions.trim());

            long testDurationValue = enduranceTestInfo.getTestDuration();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(testDurationValue);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(testDurationValue) - TimeUnit.MINUTES.toSeconds(minutes);
            testDuration.setValue(format("%02dm : %02ds", minutes, seconds));
        }), 0, 2, TimeUnit.SECONDS);

        VerticalLayout basicInfo = new VerticalLayout(startedTestCases,
                                                      successfulCommands,
                                                      successfulCommandsOneMinuteRate,
                                                      failedCommands,
                                                      failedCommandsOneMinuteRate,
                                                      testDuration);
        HorizontalLayout layout = new HorizontalLayout(basicInfo, errorsArea);
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
