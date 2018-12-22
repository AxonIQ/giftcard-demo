package io.axoniq.demo.giftcard;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.runner.RunWith;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "io.axoniq.demo.giftcard")
public class GcAppTest {

    /**
     * Testing if the classes in "..command.. , ..query.. " packages are package protected.
     * <p>
     * This will work with Java only. Kotlin does not have package modifier.
     */
    @ArchTest
    public static final ArchRule encapsulationJavaRule = ArchRuleDefinition.classes()
            .that().resideInAnyPackage("..command..", "..query..", "..gui..")
            .should().bePackagePrivate();
}
