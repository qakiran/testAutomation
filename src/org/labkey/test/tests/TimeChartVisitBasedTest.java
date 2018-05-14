/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.pages.EditDatasetDefinitionPage;
import org.labkey.test.pages.TimeChartWizard;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyC.class, Reports.class, Charting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
public class TimeChartVisitBasedTest extends TimeChartTest
{
    private static final String VISIT_REPORT_NAME = "TimeChartTest Visit Report";
    private static final String REPORT_DESCRIPTION = "This is a report generated by the TimeChartDateBasedTest";
    private static final String VISIT_CHART_TITLE = "APX-1: Abbreviated Physical Exam";
    private static final String QUERY_MEASURE_DATASET = "APX-1 (APX-1: Abbreviated Physical Exam)";

    private static final String[] VISIT_STRINGS = {
        "1 week Post-V#1",
        "Int. Vis. %{S.1.1} .%{S.2.1}",
        "Grp1:F/U/Grp2:V#2",
        "G1: 6wk/G2: 2wk",
        "6 week Post-V#2",
        "1 wk Post-V#2/V#3",
        "6 wk Post-V#2/V#3"
    };

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCreateSteps()
    {
        configureVisitStudy();
    }

    @Override
    public void doVerifySteps()
    {
        visitBasedChartTest();
        filteredViewQueryMeasureTest();
        errorMessageTest();
    }

    @LogMethod public void visitBasedChartTest()
    {
        log("Test changing from date-based to visit-based time chart.");
        clickFolder(VISIT_FOLDER_NAME);
        ChartTypeDialog chartTypeDialog = clickAddChart("study", "APX-1 (APX-1: Abbreviated Physical Exam)");
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Time)
                .setYAxis("1. Weight")
                .clickApply();
        waitForElement(Locator.css("svg").containing("Days Since Contact Date"));

        log("Change to Visit-based for x-axis");
        TimeChartWizard timeChartWizard = new TimeChartWizard(this);
        chartTypeDialog = timeChartWizard.clickChartTypeButton();
        chartTypeDialog.setTimeAxisType(ChartTypeDialog.TimeAxisType.Visit).clickApply();
        waitForElementToDisappear(Locator.css("svg").containing("Days Since Contact Date"));
        waitForElement(Locator.css("svg").containing("6 week Post-V#2"));
        assertTextPresentInThisOrder(VISIT_STRINGS);

        log("Check visit data.");
        assertElementPresent(Ext4Helper.Locators.ext4Button("Chart Type"));
        assertElementPresent(Ext4Helper.Locators.ext4Button("Chart Layout"));
        clickButton("View Data", 0);
        waitForElement(Locator.paginationText(19));
        // verify that other toolbar buttons have been hidden
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Chart Type"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Chart Layout"));

        DataRegionTable table = DataRegionTable.findDataRegion(this);
        List<String> displayOrders = table.getColumnDataAsText("Display Order");
        for (String str : displayOrders)
            assertEquals("Display order should default to zero.", "0", str);

        List<String> visits = table.getColumnDataAsText("Visit Label");
        List<String> missingVisits = new ArrayList<>(Arrays.asList(VISIT_STRINGS));
        missingVisits.removeAll(visits);
        assertTrue("Not all visits present in data table. Missing: " + missingVisits, missingVisits.isEmpty());

        clickButton("View Chart(s)", 0);
        waitForElementToDisappear(Locator.paginationText(19));
        waitForCharts(1);
        log("Revert to Date-based chart to check axis panel state.");
        chartTypeDialog = timeChartWizard.clickChartTypeButton();
        chartTypeDialog.setTimeAxisType(ChartTypeDialog.TimeAxisType.Date).clickApply();
        waitForTextToDisappear(VISIT_STRINGS[0]);
        assertTextNotPresent(VISIT_STRINGS);

        log("Back to visit-based chart for save.");
        chartTypeDialog = timeChartWizard.clickChartTypeButton();
        chartTypeDialog.setTimeAxisType(ChartTypeDialog.TimeAxisType.Visit).clickApply();
        waitForElement(Locator.css("svg").containing("6 week Post-V#2"));

        openSaveMenu();
        setFormElement(Locator.name("reportName"), VISIT_REPORT_NAME);
        setFormElement(Locator.name("reportDescription"), REPORT_DESCRIPTION);
        saveReport(true);
        waitForElement(Locator.css("svg").containing(VISIT_CHART_TITLE));
    }

    @LogMethod public void filteredViewQueryMeasureTest()
    {
        log("Create query over " + QUERY_MEASURE_DATASET + " dataset.");
        clickFolder(VISIT_FOLDER_NAME);
        goToModule("Query");
        createNewQuery("study");
        setFormElement(Locator.name("ff_newQueryName"), "My APX Query");
        selectOptionByText(Locator.name("ff_baseTableName"), QUERY_MEASURE_DATASET);
        clickButton("Create and Edit Source");
        setCodeEditorValue("queryText", "SELECT x.MouseId, x.MouseVisit, x.SequenceNum, x.APXtempc, x.sfdt_136 FROM \"APX-1: Abbreviated Physical Exam\" AS x");
        clickButton("Save & Finish");
        waitForElement(Locator.paginationText(47));

        // verify filtered view issue 16498
        log("Filter the default view of the query");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addFilter("sfdt_136", "Contains One Of", "1;2;");
        _customizeViewsHelper.saveCustomView();
        waitForElement(Locator.paginationText(31));

        log("Create a Time Chart from the measure in the new query");
        DataRegionTable query = new DataRegionTable("query", getDriver());
        query.goToReport("Create Chart");
        // note: the 'My APX Query' query should already be selected
        ChartTypeDialog chartTypeDialog = new ChartTypeDialog(getDriver());
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Time)
                .setYAxis("2. Body Temp")
                .clickApply();
        waitForText("No calculated interval values (i.e. Days, Months, etc.) for the selected 'Measure Date' and 'Interval Start Date'.");
        TimeChartWizard timeChartWizard = new TimeChartWizard(this);
        chartTypeDialog = timeChartWizard.clickChartTypeButton();
        chartTypeDialog.setTimeAxisType(ChartTypeDialog.TimeAxisType.Visit).clickApply();
        waitForCharts(1);
        waitForElement(Locator.css("svg").containing("My APX Query"));
        _ext4Helper.clickParticipantFilterGridRowText("999320016", 0);
        waitForElement(Locator.css("svg").containing("4 wk Post-V#2/V#3")); // last visit from ptid 999320016
        assertTextPresent("2. Body Temp: ", 18); // point hover text label
        clickButton("View Data", 0);
        waitForElement(Locator.paginationText(9));
        assertTextNotPresent("801.0", "G1: 6wk/G2: 2wk"); // sequenceNum filtered out by default view filter
        clickButton("View Chart(s)", 0);
        waitForElement(Locator.css("svg").containing("My APX Query"));

        openSaveMenu();
        setFormElement(Locator.name("reportName"), VISIT_REPORT_NAME + " 2");
        saveReport(true);
        waitForElement(Locator.css("svg").containing("My APX Query"));
    }

    @LogMethod private void errorMessageTest()
    {
        log("Test renaming time chart measure");
        clickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        waitAndClickAndWait(Locator.linkWithText(VISIT_REPORT_NAME));
        waitForElement(Locator.css("svg").containing("6 week Post-V#2"));
        EditDatasetDefinitionPage editDatasetPage = _studyHelper.goToManageDatasets()
                .selectDatasetByName("APX-1")
                .clickEditDefinition();
        waitForElement(Locator.xpath("//input[@name='dsName']"));
        assertEquals("APXwtkg", getFormElement(Locator.name("ff_name1")));
        setFormElement(Locator.name("ff_name1"), "APXwtkgCHANGED");
        editDatasetPage.save();
        clickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        waitAndClickAndWait(Locator.linkWithText(VISIT_REPORT_NAME));
        waitForText("Error: Unable to find field APXwtkg in study.APX-1.");
        assertTextPresent("The field may have been deleted, renamed, or you may not have permissions to read the data.");

        log("Test deleting time chart measure's dataset");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText("APX-1"));
        clickButton("Delete Dataset", 0);
        assertTrue(acceptAlert().contains("Are you sure you want to delete this dataset?"));
        waitForText("The study schedule defines"); // text on the Manage Datasets page
        clickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        waitAndClickAndWait(Locator.linkWithText(VISIT_REPORT_NAME));
        waitForText("Error: Unable to find table study.APX-1.");
        assertTextPresent("The table may have been deleted, or you may not have permissions to read the data.");

        log("Delete My APX Query so it doesn't fail query validation");
        goToSchemaBrowser();
        selectQuery("study", "My APX Query");
        clickAndWait(Locator.linkWithText("Delete Query"));
        waitForText("Are you sure you want to delete the query 'My APX Query'?");
        clickButton("OK");
    }
}
