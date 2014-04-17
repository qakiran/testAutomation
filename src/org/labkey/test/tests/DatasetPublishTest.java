/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Category({DailyA.class})
public class DatasetPublishTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "DatasetPublishTestProject";
    private static final String SUBFOLDER_NAME = "TargetDatasets";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, "Study");

        importStudyFromZip(new File(getSampledataPath(), "/study/LabkeyDemoStudy.zip"));
        goToProjectHome();
        hideDatasets();

        List<String> hiddenDatasetNames = new ArrayList<>();
        hiddenDatasetNames.add("ELISpotAssay");
        _studyHelper.publishStudy(SUBFOLDER_NAME, 2, "Participants", "Timepoints", hiddenDatasetNames);

        modifySourceDataset();
        checkTargetDataset();
        addRowToSourceDataset();
        assertNewRowPresentInTarget();
        assertHiddenDatasetPresentInTarget();
    }

    @LogMethod
    private void hideDatasets()
    {
        goToProjectHome();
        goToManageStudy();
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText("Change Properties"));
        click(getDatasetCheckboxLocator("ELISpotAssay"));
        clickButton("Save");
    }

    @LogMethod
    private void modifySourceDataset()
    {
        goToProjectHome();
        goToDataset("Demographics");
        waitAndClick(Locator.linkWithText("edit"));
        waitForElement(Locator.name("quf_Comments"));
        setFormElement(Locator.name("quf_Comments"), "Comment on modified participant");
        clickButton("Submit");
    }

    @LogMethod
    private void addRowToSourceDataset()
    {
        goToProjectHome();
        goToDataset("Demographics");
        waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        waitForElement(Locator.name("quf_ParticipantId"));
        setFormElement(Locator.name("quf_ParticipantId"), "67676");
        setFormElement(Locator.name("quf_date"), "1/1/2001");
        setFormElement(Locator.name("quf_Comments"), "Comment on added participant");
        clickButton("Submit");
    }

    @LogMethod
    private void checkTargetDataset()
    {
        clickFolder(SUBFOLDER_NAME);
        refreshDataset("Demographics");
        assertTextPresent("Comment on modified participant");
    }

    @LogMethod
    private void assertNewRowPresentInTarget()
    {
        clickFolder(SUBFOLDER_NAME);
        refreshDataset("Demographics");
        assertTextPresent("Comment on added participant");
        //ID should be masked on new participant
        assertTextNotPresent("67676");
    }

    @LogMethod
    public void goToDataset(@LoggedParam String datasetName)
    {
        click(Locator.linkWithText("Clinical and Assay Data"));
        waitAndClickAndWait(Locator.linkWithText(datasetName));
    }

    @LogMethod
    public void refreshDataset(@LoggedParam String datasetName)
    {
        goToDataset(datasetName);
        _extHelper.clickMenuButton("Views", "Edit Snapshot");
        prepForPageLoad();
        clickButton("Update Snapshot", 0);
        assertAlertContains("Updating will replace all existing data with a new set of data. Continue?");
        waitForPageToLoad();
    }

    @LogMethod
    private void assertHiddenDatasetPresentInTarget()
    {
        clickFolder(SUBFOLDER_NAME);
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        assertTextPresent("ELISpotAssay");              // it did get published
        clickAndWait(Locator.linkWithText("ELISpotAssay"));
        Locator locator = Locator.xpath("//tr[td='Show In Overview' and td='false']");
        waitForElement(locator);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    protected Locator getDatasetCheckboxLocator(String datasetLabel)
    {
        return Locator.xpath("//tr[./td/input[@value='" + datasetLabel + "']]/td/input[@name='visible']");
    }
}
