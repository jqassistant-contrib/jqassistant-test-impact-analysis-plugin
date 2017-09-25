package org.jqassistant.contrib.plugin.testimpactanalysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buschmais.jqassistant.core.analysis.api.Result;
import com.buschmais.jqassistant.core.analysis.api.rule.*;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.plugin.common.api.model.ArtifactDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.ClassTypeDescriptor;

/**
 * A {@link ReportPlugin} that creates files containing source file names of
 * test classes for execution using Maven Surefire Plugin.
 */
public class SurefireSuiteReportPlugin implements ReportPlugin {

    static final String REPORT_ID = "surefire-suite";
    private static final Logger LOGGER = LoggerFactory.getLogger(SurefireSuiteReportPlugin.class);

    private static final String PROPERTY_DIRECTORY = "testImpactAnalysis.report.directory";
    private static final String PROPERTY_ARTIFACT_COLUMN = "testImpactAnalysis.surefire.artifactColumn";
    private static final String PROPERTY_TESTS_COLUMN = "testImpactAnalysis.surefire.testsColumn";
    private static final String PROPERTY_REPORT_FILE = "testImpactAnalysis.surefire.file";

    private static final String DEFAULT_DIRECTORY = "jqassistant/report/testimpactanalysis";
    private static final String DEFAULT_ARTIFACT_COLUMN = "Artifact";
    private static final String DEFAULT_TESTS_COLUMN = "Tests";
    private static final String DEFAULT_REPORT_FILE = "surefire-tests";

    private File reportDirectory;

    private File reportFile;

    private String artifactColumn = DEFAULT_ARTIFACT_COLUMN;

    private String testsColumn = DEFAULT_TESTS_COLUMN;

    @Override
    public void initialize() throws ReportException {
    }

    @Override
    public void configure(Map<String, Object> properties) throws ReportException {
        String directoryName = (String) properties.get(PROPERTY_DIRECTORY);
        this.reportDirectory = directoryName != null ? new File(directoryName) : new File(DEFAULT_DIRECTORY);
        if (this.reportDirectory.mkdirs()) {
            LOGGER.info("Created directory '" + this.reportDirectory.getAbsolutePath() + "'.");
        }
        String reportFileName = (String) properties.get(PROPERTY_REPORT_FILE);
        this.reportFile = reportFileName != null ? new File(this.reportDirectory, reportFileName) : null;
        if (properties.containsKey(PROPERTY_ARTIFACT_COLUMN)) {
            this.artifactColumn = (String) properties.get(PROPERTY_ARTIFACT_COLUMN);
        }
        if (properties.containsKey(PROPERTY_TESTS_COLUMN)) {
            this.testsColumn = (String) properties.get(PROPERTY_TESTS_COLUMN);
        }
    }

    @Override
    public void begin() throws ReportException {
    }

    @Override
    public void end() throws ReportException {
    }

    @Override
    public void beginConcept(Concept concept) throws ReportException {
    }

    @Override
    public void endConcept() throws ReportException {
    }

    @Override
    public void beginGroup(Group group) throws ReportException {
    }

    @Override
    public void endGroup() throws ReportException {
    }

    @Override
    public void beginConstraint(Constraint constraint) throws ReportException {
    }

    @Override
    public void endConstraint() throws ReportException {
    }

    @Override
    public void setResult(Result<? extends ExecutableRule> result) throws ReportException {
        Report report = result.getRule().getReport();
        if (isTestSuiteReport(report)) {
            Set<File> files = new HashSet<>();
            for (Map<String, Object> row : result.getRows()) {
                ArtifactDescriptor artifactDescriptor = getColumnValue(row, artifactColumn, ArtifactDescriptor.class);
                Iterable<ClassTypeDescriptor> testClasses = getColumnValue(row, testsColumn, Iterable.class);
                File file = getReportFile(artifactDescriptor);
                if (testClasses == null) {
                    LOGGER.warn("Cannot determine tests from column '" + testsColumn + "'.");
                } else {
                    boolean append = !files.add(file);
                    writeTests(file, append, testClasses);
                }
            }
        }
    }

    /**
     * Verify if this report shall be executed.
     *
     * FIXME This logic should be provided by the framework.
     *
     * @param report
     *            The report configured for the executed rule.
     * @return <code>true</code> if this report is selected.
     */
    private boolean isTestSuiteReport(Report report) {
        Set<String> selectedTypes = report.getSelectedTypes();
        return selectedTypes != null && selectedTypes.contains(REPORT_ID);
    }

    /**
     * Extract the value of a column providing an expected type.
     *
     * @param row
     *            The row.
     * @param name
     *            The name of the column.
     * @param expectedType
     *            The expected type.
     * @param <T>
     *            The expected type.
     * @return The value.
     * @throws ReportException
     *             If the value type does not match the expected type.
     */
    private <T> T getColumnValue(Map<String, Object> row, String name, Class<T> expectedType) throws ReportException {
        Object value = row.get(name);
        if (value != null) {
            Class<?> valueType = value.getClass();
            if (!expectedType.isAssignableFrom(expectedType)) {
                throw new ReportException("Expecting a " + expectedType.getName() + " but got '" + value + "' of type '" + valueType.getName() + "'.");
            }
        }
        return expectedType.cast(value);
    }

    /**
     * Determines the report file for the given artifact.
     * 
     * @param artifactDescriptor
     *            The artifact descriptor.
     * @return The report file.
     */
    private File getReportFile(ArtifactDescriptor artifactDescriptor) {
        File file;
        if (this.reportFile != null) {
            file = this.reportFile;
        } else if (artifactDescriptor != null) {
            file = new File(reportDirectory, artifactDescriptor.getName());
        } else {
            file = new File(reportDirectory, DEFAULT_REPORT_FILE);
        }
        return file;
    }

    /**
     * Writes test classes to the given file.
     * 
     * @param file
     *            The file.
     * @param append
     *            If <code>true</code> the test classes will be appended if the file
     *            already exists.
     * @param testClasses
     *            The test classes.
     * @throws ReportException
     *             If the file cannot be written.
     */
    private void writeTests(File file, boolean append, Iterable<ClassTypeDescriptor> testClasses) throws ReportException {
        LOGGER.info((append ? "Appending " : "Writing " )+ " tests to '" + file.getPath() + "'.");
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, append))) {
            for (ClassTypeDescriptor testClass : testClasses) {
                String sourceFileName = testClass.getSourceFileName();
                String name = testClass.getName();
                String fullQualifiedName = testClass.getFullQualifiedName();
                String packageName = fullQualifiedName.substring(0, fullQualifiedName.length() - name.length());
                String fullSourceFileName = packageName.replace('.', '/') + sourceFileName;
                writer.println(fullSourceFileName);
                LOGGER.info("\t" + testClass.getFullQualifiedName() + " (" + fullSourceFileName+")");
            }
        } catch (IOException e) {
            throw new ReportException("Cannot write tests to '" + file.getAbsolutePath() + "'", e);
        }
    }
}
