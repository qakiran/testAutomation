/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests;

import junit.framework.Assert;
import org.labkey.experimentQuery.xml.Folder;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;

import java.io.File;

public class TimeChartTest extends BaseSeleniumWebTest
{
//    /?TODO:  Folder names should contain TRICKY_CHARACTERS, don't due to Issue 12830
    private static final String PROJECT_NAME =  TRICKY_CHARACTERS_FOR_PROJECT_NAMES + "TimeChartTest Project";
    private static final String FOLDER_NAME =  "Demo Study";
    private static final String STUDY_ZIP = "/sampledata/study/LabkeyDemoStudy.zip";

    private static final String REPORT_NAME_1 = "TimeChartTest Report";
    private static final String REPORT_NAME_2 = "TimeChartTest 2Report";
    private static final String REPORT_NAME_3 = "TimeChartTest Multi-Measure Report";
    private static final String REPORT_DESCRIPTION = "This is a report generated by the TimeChartTest";
    private static final String X_AXIS_LABEL = "New X-Axis Label";
    private static final String Y_AXIS_LABEL = "New Y-Axis Label";
    private static final String CHART_TITLE = "New Chart Title";
    private static final String ADD_MEASURE_TITLE = "Add Measure";
    private static final String CHOOSE_MEASURE_DIALOG = "Choose a Measure...";

    private static final String USER1 = "user1@timechart.test";

    private static final String WIKIPAGE_NAME = "VisualizationGetDataAPITest";
    private static final String[] GETDATA_API_TEST_TITLES = {
        "Single Measure",
        "Two Measures from the same dataset",
        "Two Measures from different datasets",
        "Two Measures from different datasets (#2)",
        "Two Measures - without dimension selected for second, inner join",
        "Two Measures - without dimension selected for second, outer join",
        "Two Measures - WITH dimension selected for second, inner join",
        "Two Measures - WITH dimension selected for second, outer join"
    };
    private static final String[] GETDATA_API_TEST_NUMROWS = {
        "1 - 33 of 33", 
        "1 - 33 of 33", 
        "1 - 33 of 33",
        "1 - 33 of 33",
        "1 - 75 of 75",
        "1 - 83 of 83",
        "1 - 25 of 25",
        "1 - 33 of 33"
    };

    private static final String[][] GETDATA_API_TEST_COLNAMES = {
        {"Study Lab Results Participant Id", "Study Lab Results Date", "Study Lab Results CD4", "Days"},
        {"Study Lab Results Participant Id", "Study Lab Results Date", "Study Lab Results CD4", "Study Lab Results Hemoglobin", "Days"},
        {"Study Lab Results Participant Id", "Study Lab Results Date", "Study Lab Results CD4", "Study Lab Results Participant Visit Visit Date", "Study Physical Exam Weight Kg", "Days"},
        {"Study Lab Results Participant Id", "Study Lab Results Date", "Study Lab Results CD4", "Study Lab Results Participant Visit Visit Date", "Study HIVTest Results HIVLoad Quant", "Days"},
        {"Study Lab Results Participant Id", "Study Lab Results Date", "Study Lab Results CD4", "Study Lab Results Participant Visit Visit Date", "Study Luminex Assay Obs Conc", "Study Luminex Assay Obs Conc OORIndicator", "Days"},
        {"Study Lab Results Participant Id", "Study Lab Results Date", "Study Lab Results CD4", "Study Lab Results Participant Visit Visit Date", "Study Luminex Assay Obs Conc", "Study Luminex Assay Obs Conc OORIndicator", "Days"},
        {"Study Lab Results Participant Id", "Study Lab Results Date", "Study Lab Results CD4", "Study Lab Results Participant Visit Visit Date", "IL-10 (23)", "IL-2 (3)", "TNF-alpha (40)", "Days"},
        {"Study Lab Results Participant Id", "Study Lab Results Date", "Study Lab Results CD4", "Study Lab Results Participant Visit Visit Date", "IL-10 (23)", "IL-2 (3)", "TNF-alpha (40)", "Days"}
    };

    private static final String[][] GETDATA_API_TEST_DAYS = {
        {"44", "79", "108", "190", "246", "276", "303", "335", "364", "394"},
        {"44", "79", "108", "190", "246", "276", "303", "335", "364", "394"},
        {"44", "79", "108", "190", "246", "276", "303", "335", "364", "394"},
        {"44", "79", "108", "190", "246", "276", "303", "335", "364", "394"},
        {"44", "44", "44", "79", "79", "79", "108", "108", "108", "190", "190", "190", "246", "246", "246"},
        {"44", "44", "44", "79", "79", "79", "108", "108", "108", "190", "190", "190", "246", "246", "246", "276", "303", "335", "364", "394"},
        {"44", "79", "108", "190", "246"},
        {"44", "79", "108", "190", "246"}
    };

    private static final String[] GETDATA_API_TEST_MEASURES = {
        "Study Lab Results CD4",
        "Study Lab Results Hemoglobin",
        "Study Physical Exam Weight Kg",
        "Study HIVTest Results HIVLoad Quant",
        "Study Luminex Assay Obs Conc",
        "Study Luminex Assay Obs Conc",
        "IL-10 (23)",
        "IL-10 (23)"
    };

    private static final String[][] GETDATA_API_TEST_MEASURE_VALUES = {
        {"43", "520", "420", "185", "261", "308", "177", "144", "167", "154"},
        {"14.5", "16.0", "12.2", "15.5", "13.9", "13.7", "12.9", "11.1", "13.2", "16.1"},
        {"86", "84", "83", "80", "79", "79", "79", "78", "77", "75"},
        {"4345", "3452", "98354", "32453", "324234", "345452", "235671", "456674", "567432", "653465"},
        {"35.87", "40.07", "52.74", "13.68", "28.35", "42.38", "2.82", "5.19", "7.99", "5.12", "6.69", "32.33", "3.09", "5.76", "12.49"},
        {"35.87", "40.07", "52.74", "13.68", "28.35", "42.38", "2.82", "5.19", "7.99", "5.12", "6.69", "32.33", "3.09", "5.76", "12.49"},
        {"40.07", "42.38", "7.99", "32.33", "12.49"},
        {"40.07", "42.38", "7.99", "32.33", "12.49"}
    };

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    public void configureStudy()
    {
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "Study", null);
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());


    }

    @Override
    public void doTestSteps()
    {
        configureStudy();

        createChartTest();

        stdDevRegressionTest();

        visualizationTest();

        generateChartPerParticipantTest();

        saveTest();

        timeChartPermissionsTest();

        multiMeasureTimeChartTest();
    }

    private void generateChartPerParticipantTest()
    {

        ExtHelper.clickExtTab(this, "Chart(s)");
        checkRadioButton("chart_layout", "per_subject");
        setFormElement("chart-title-textfield", CHART_TITLE);
        fireEvent(Locator.name("chart-title-textfield"), SeleniumEvent.blur);
        assertTextPresent(CHART_TITLE, 5);
        ExtHelper.prevClickFileBrowserFileCheckbox(this, "249320127"); // re-select participant
        waitForText(CHART_TITLE+": 249320127", WAIT_FOR_JAVASCRIPT);
        assertTextPresent(CHART_TITLE, 6);
    }

    private void createChartTest()
    {
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Manage Views");
        clickMenuButton("Create", "Time Chart");
        waitForElement(Locator.button("Choose a Measure"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Choose a Measure", 0);
        ExtHelper.waitForExtDialog(this, CHOOSE_MEASURE_DIALOG);
        waitForText("NAbAssay", WAIT_FOR_JAVASCRIPT);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);

        log("Test measure search.");
        ExtHelper.setExtFormElementByType(this, CHOOSE_MEASURE_DIALOG, "text", "nab");
        pressEnter(ExtHelper.getExtDialogXPath(CHOOSE_MEASURE_DIALOG)+"//input[contains(@class, 'x-form-field') and @type='text']");
        // Count search results (11 in study's NAb assay)
        assertEquals("", 11, getXpathCount(Locator.xpath(ExtHelper.getExtDialogXPath(CHOOSE_MEASURE_DIALOG)+"//div[contains(@class, 'x-list-body-inner')]/dl")));

        log("Check for appropriate message for measure with no data.");
        click(Locator.xpath(ExtHelper.getExtDialogXPath(CHOOSE_MEASURE_DIALOG)+"//dl[./dt/em[text()='Cutoff Percentage (3)']]"));
        clickNavButton("Select", 0);
        waitForText("No data found", WAIT_FOR_JAVASCRIPT);
    }

    private void saveTest()
    {


        ExtHelper.clickExtTab(this, "Overview");
        setFormElement("reportName", REPORT_NAME_1);
        setFormElement("reportDescription", REPORT_DESCRIPTION);
        clickNavButton("Save", 0);
        waitForPageToLoad(); // page will reload on save success
        waitForText(CHART_TITLE, WAIT_FOR_JAVASCRIPT);

        clickNavButton("Save As", 0);
        ExtHelper.waitForExtDialog(this, "Save As");
        setFormElement(Locator.id("reportNameSaveAs"), REPORT_NAME_2);
        setFormElement(Locator.id("reportDescriptionSaveAs"), "This is another report generated by the TimeChartTest");
        click(Locator.id("reportSharedMeSaveAs"));
        clickNavButtonByIndex("Save", 1, 0);
        waitForPageToLoad(); // page will reload on save success
        waitForText(CHART_TITLE, WAIT_FOR_JAVASCRIPT);

        log("Verify saved report");
        clickLinkWithText("Manage Views");
        waitForText(REPORT_NAME_1, WAIT_FOR_JAVASCRIPT);
        assertTextPresent(REPORT_NAME_2);
        click(Locator.tagWithText("div", REPORT_NAME_1));
        assertTextPresent(REPORT_DESCRIPTION);
        clickAndWait(Locator.xpath("//a[text()='view' and contains(@href, '"+REPORT_NAME_1.replace(" ", "%20")+"')]"));
        waitForText(Y_AXIS_LABEL, WAIT_FOR_JAVASCRIPT);
        assertTextPresent(X_AXIS_LABEL);
        assertTextPresent(CHART_TITLE, 6);
        pushLocation();
        pushLocation();
    }

    private void visualizationTest()
    {
        log("Check visualization");
        clickNavButton("Remove Measure", 0);
        waitForText("No measure selected.", WAIT_FOR_JAVASCRIPT);
        clickNavButton("Add Measure", 0);
        ExtHelper.waitForExtDialog(this, ADD_MEASURE_TITLE);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        ExtHelper.setExtFormElementByType(this, ADD_MEASURE_TITLE, "text", "viral");
        pressEnter(ExtHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//input[contains(@class, 'x-form-text') and @type='text']");
        assertEquals("", 1, getXpathCount(Locator.xpath(ExtHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//div[contains(@class, 'x-list-body-inner')]/dl")));
        click(Locator.xpath(ExtHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//dl[./dt/em[text()='HIV Test Results']]"));
        clickNavButton("Select", 0);
        waitForText("Days Since Start Date", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("No data found");

        clickNavButton("View Data", 0);
        waitForText("1 - 33 of 33", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker')]/div")); // Select all participants checkbox
        waitForText("1 - 38 of 38", WAIT_FOR_JAVASCRIPT);
        ExtHelper.prevClickFileBrowserFileCheckbox(this, "249320127"); // de-select one participant
        waitForText("1 - 31 of 31", WAIT_FOR_JAVASCRIPT);

        log("Test X-Axis");
        clickNavButton("View Chart(s)", 0);

        ExtHelper.clickExtTab(this, "X-Axis");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='x-axis-interval-combo']/.."), "Weeks");
        waitForText("Weeks Since Start Date", WAIT_FOR_JAVASCRIPT);
        setFormElement("x-axis-label-textfield", X_AXIS_LABEL);
        fireEvent(Locator.name("x-axis-label-textfield"), SeleniumEvent.blur);
        waitForText(X_AXIS_LABEL, WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='x-axis-interval-combo']/.."), "Days");
        assertTextNotPresent("Days Since Start Date"); // Label shouldn't change automatically once it has been set manually
//        checkRadioButton("xaxis_range", "manual");
//        setFormElement(Locator.xpath("//input[@name='xaxis_range']/../../input[1]"), "20");
//        fireEvent(Locator.xpath("//input[@name='xaxis_range']/../../input[1]"), SeleniumEvent.blur);
//        setFormElement(Locator.xpath("//input[@name='xaxis_range']/../../input[2]"), "40");
//        fireEvent(Locator.xpath("//input[@name='xaxis_range']/../../input[2]"), SeleniumEvent.blur);
//        mouseDown(Locator.xpath("/html/body"));
//        assertTextNotPresent("15");
//        assertTextNotPresent("45");
        setAxisValue("x", "15", "40", X_AXIS_LABEL, null, null, new String[] {"15", "45"});

        log("Test Y-Axis");
        setAxisValue("y", "200000", "400000", Y_AXIS_LABEL, null, null, new String[] {"500,000","200,000"});
//        ExtHelper.clickExtTab(this, "Y-Axis");
//        setFormElement("y-axis-label-textfield", Y_AXIS_LABEL);
//        fireEvent(Locator.name("y-axis-label-textfield"), SeleniumEvent.blur);
//        waitForText(Y_AXIS_LABEL, WAIT_FOR_JAVASCRIPT);
//        checkRadioButton("yaxis_range", "manual");
//        setFormElement(Locator.xpath("//input[@name='yaxis_range']/../../input[1]"), "200000");
//        fireEvent(Locator.xpath("//input[@name='yaxis_range']/../../input[1]"), SeleniumEvent.blur);
//        setFormElement(Locator.xpath("//input[@name='yaxis_range']/../../input[2]"), "400000");
//        fireEvent(Locator.xpath("//input[@name='yaxis_range']/../../input[2]"), SeleniumEvent.blur);
//        waitForText("400,000", WAIT_FOR_JAVASCRIPT);
//        assertTextNotPresent("500,000");
//        assertTextNotPresent("200,000");

        setAxisValue("y", "10000", "1000000", null,"Log", new String[] {"10,000", "100,000", "1,000,000"}, new String[] {"500,000"});
//        ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./label[text()='Scale:']]/div/div"), "Log");
//        setFormElement(Locator.xpath("//input[@name='yaxis_range']/../../input[1]"), "10000");
//        fireEvent(Locator.xpath("//input[@name='yaxis_range']/../../input[1]"), SeleniumEvent.blur);
//        setFormElement(Locator.xpath("//input[@name='yaxis_range']/../../input[2]"), "1000000");
//        fireEvent(Locator.xpath("//input[@name='yaxis_range']/../../input[2]"), SeleniumEvent.blur);
//        waitForText("1,000,000", WAIT_FOR_JAVASCRIPT);
//        assertTextPresent("10,000", "100,000", "1,000,000");
//        assertTextNotPresent("500,000");
    }

    /**
     *
     * @param axis must be X or Y, case is unimportant
     * @param lowerBound
     * @param upperBound
     * @param textNotPresent intended to be used for numbers that should no longer be present in the axes.
     *                      ideally we'd calculate this automatically, but that's too complicated a problem for now
     *                      TODO:  calculate not-present number automatically
     */
    protected void setAxisValue(String axis, String lowerBound, String upperBound, String label, String scale, String[] textPresent, String[] textNotPresent)
    {
        axis = axis.toLowerCase(); //don't want to worry about case for the rest of the function
        if(!(axis.equals("x") || axis.equals("y")))
        {
            Assert.fail("Invalid axis marker");
        }
        ExtHelper.clickExtTab(this, axis.toUpperCase() + "-Axis");


        if(scale!=null)
        {
            ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./label[text()='Scale:']]/div/div"), scale);
        }

        if(label!=null)
        {
            setFormElement(axis + "-axis-label-textfield", label);
        }

        fireEvent(Locator.name(axis + "-axis-label-textfield"), SeleniumEvent.blur);
        waitForText(label, WAIT_FOR_JAVASCRIPT);
        checkRadioButton(axis + "axis_range", "manual");
        setFormElement(Locator.xpath("//input[@name='" + axis + "axis_range']/../../input[1]"), lowerBound);
        fireEvent(Locator.xpath("//input[@name='" + axis + "axis_range']/../../input[1]"), SeleniumEvent.blur);
        setFormElement(Locator.xpath("//input[@name='" + axis + "axis_range']/../../input[2]"), upperBound);
        fireEvent(Locator.xpath("//input[@name='" + axis + "axis_range']/../../input[2]"), SeleniumEvent.blur);
        mouseDown(Locator.xpath("/html/body"));

        if(textPresent!=null)
            waitForText((textPresent[0]), WAIT_FOR_JAVASCRIPT);

        assertTextPresent(textPresent);
        assertTextNotPresent(textNotPresent);


    }

    private void multiMeasureTimeChartTest()
    {
        log("Create multi-measure time chart.");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Manage Views");
        clickMenuButton("Create", "Time Chart");
        waitForElement(Locator.button("Choose a Measure"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Choose a Measure", 0);
        ExtHelper.waitForExtDialog(this, CHOOSE_MEASURE_DIALOG);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(ExtHelper.getExtDialogXPath(CHOOSE_MEASURE_DIALOG)+"//dl[./dt/em[starts-with(text(), 'CD4+')]]"));
        clickNavButton("Select", 0);
        clickNavButton("Add Measure", 0);
        ExtHelper.waitForExtDialog(this, ADD_MEASURE_TITLE);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(ExtHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//dl[./dt/em[starts-with(text(), 'Lymphs')]]"));
        clickNavButton("Select", 0);
        ExtHelper.clickExtTab(this, "Chart(s)");
        checkRadioButton("chart_layout", "per_dimension");
        setFormElement("chart-title-textfield", CHART_TITLE);
        fireEvent(Locator.name("chart-title-textfield"), SeleniumEvent.blur );
        ExtHelper.clickExtTab(this, "Overview");
        setFormElement("reportName", REPORT_NAME_3);
        clickNavButton("Save", 0);
        waitForPageToLoad(); // page will reload on save success
        waitForText(CHART_TITLE, WAIT_FOR_JAVASCRIPT);

        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Manage Views");
        waitAndClick(Locator.tagWithText("div", REPORT_NAME_3));
        clickLinkWithText("view");
        waitForText(CHART_TITLE, WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Days Since Start Date", 2); // Y-Axis labels for each measure
        assertTextPresent(CHART_TITLE+": Lymphocytes", 1); // Title
        assertTextPresent(CHART_TITLE+": CD4", 1); // Title

        // check multi-measure calls to LABKEY.Visualization.getData API
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        // create new wiki to add to Demo study folder
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement("name", WIKIPAGE_NAME);
        setFormElement("title", WIKIPAGE_NAME);
        // insert JS for getData calls and querywebpart
        setWikiBody(getFileContents("server/test/data/api/getDataTest.html"));
        saveWikiPage();
        waitForText("Current Config", WAIT_FOR_JAVASCRIPT);

        // loop through the getData calls to check grid for: # rows, column headers, and data values (for a single ptid)
        int testCount = Integer.parseInt(getFormElement(Locator.name("configCount")));
        int testIndex = 0;
        while(testIndex < testCount){
            // check title is present
            assertTextPresent(GETDATA_API_TEST_TITLES[testIndex]);
            // check # of rows
            waitForText(GETDATA_API_TEST_NUMROWS[testIndex], WAIT_FOR_JAVASCRIPT);
            waitForText("Study Lab Results", WAIT_FOR_JAVASCRIPT);
            // check column headers
            for(int i = 0; i < GETDATA_API_TEST_COLNAMES[testIndex].length; i++){
                waitForText(GETDATA_API_TEST_COLNAMES[testIndex][i], WAIT_FOR_JAVASCRIPT); // Table takes a moment to render.
                assertTableCellTextEquals("dataregion_apiTestDataRegion",  1, GETDATA_API_TEST_COLNAMES[testIndex][i], GETDATA_API_TEST_COLNAMES[testIndex][i]);
            }
            // check values in interval column for the first participant
            for(int i = 0; i < GETDATA_API_TEST_DAYS[testIndex].length; i++){
                assertTableCellContains("dataregion_apiTestDataRegion",  i+2, "Days", GETDATA_API_TEST_DAYS[testIndex][i]);
            }
            // check values in measure column
            for(int i = 0; i < GETDATA_API_TEST_MEASURE_VALUES[testIndex].length; i++){
                assertTableCellContains("dataregion_apiTestDataRegion",  i+2, GETDATA_API_TEST_MEASURES[testIndex], GETDATA_API_TEST_MEASURE_VALUES[testIndex][i]);
            }

            if(testIndex < testCount-1)
                clickNavButton("Next", 0);

            testIndex++;
        }
    }

    private void timeChartPermissionsTest()
    {
        log("Check Time Chart Permissions");
        createUser(USER1, null);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        setUserPermissions(USER1, "Reader");
        setSiteGroupPermissions("Guests", "Reader");
        clickNavButton("Save and Finish");
        impersonate(USER1);
        popLocation(); // Saved chart
        waitForText(CHART_TITLE, WAIT_FOR_JAVASCRIPT);
        assertElementNotVisible(Locator.button("Save"));
        assertElementPresent(Locator.button("Save As"));
        clickLinkWithText(FOLDER_NAME);
        assertTextNotPresent(REPORT_NAME_2);
        stopImpersonating();
        signOut();
        popLocation(); // Saved chart
        waitForText(CHART_TITLE, WAIT_FOR_JAVASCRIPT);
        assertElementNotVisible(Locator.button("Save"));
        assertElementNotVisible(Locator.button("Save As"));
        simpleSignIn();
    }

    // Regression test for "11764: Time Chart Wizard raises QueryParseException on 'StdDev' measure"
    private void stdDevRegressionTest()
    {
        log("StdDev regression check");
        clickNavButton("Remove Measure", 0);
        waitForText("No measure selected.", WAIT_FOR_JAVASCRIPT);
        clickNavButton("Add Measure", 0);
        ExtHelper.waitForExtDialog(this, ADD_MEASURE_TITLE);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(ExtHelper.getExtDialogXPath(ADD_MEASURE_TITLE)+"//dl[./dt/em[text()='StdDev']]"));
        clickNavButton("Select", 0);
        waitForText("Days Since Start Date", WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void doCleanup()
    {
        try {deleteUser(USER1);} catch (Throwable T) {}
        try {deleteProject(PROJECT_NAME);} catch (Throwable T) {}
    }
}
