/*
 * Copyright (c) 2010-2013 LabKey Corporation
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.WorkbookHelper;

import java.util.ArrayList;

import static org.junit.Assert.*;

@Category({DailyB.class})
public class WorkbookTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "Workbook Test Project";
    private static final String PROJECT_NAME2 = "Workbook Test Project 2";
    private static final String DEFAULT_WORKBOOK_NAME = "TestWorkbook";
    private static final String DEFAULT_WORKBOOK_DESCRIPTION = "Test Default Workbook Type";
    private static final String FILE_WORKBOOK_NAME = "TestFileWorkbook";
    private static final String FILE_WORKBOOK_DESCRIPTION = "Test File Workbook Type";
    private static final String ASSAY_WORKBOOK_DESCRIPTION = "Test Assay Workbook Type";
    private static final String ASSAY_WORKBOOK_NAME = "TestAssayWorkbook";
    private static final String APITEST_NAME = "WorkbookAPIs";
    private static final String APITEST_FILE = "workbookAPITest.html";


    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/workbook";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        deleteProject(PROJECT_NAME2, afterTest);
    }

    @Override @LogMethod
    public void doTestSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        addWebPart("Workbooks");
        int[] ids = createWorkbooks(PROJECT_NAME, FILE_WORKBOOK_NAME, FILE_WORKBOOK_DESCRIPTION, ASSAY_WORKBOOK_NAME,
                ASSAY_WORKBOOK_DESCRIPTION, DEFAULT_WORKBOOK_NAME, DEFAULT_WORKBOOK_DESCRIPTION);
        //id's generated when workbooks are created should be sequential
        int lastid = 0;
        for(int i=0; i>ids.length; i++)
        {
            assertEquals("nonsequential name for workbook found",ids[i],lastid + 1);
        }
        // Edit Workbook Name
        waitAndClick(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]"));
        waitForElement(Locator.xpath("//input[@value='"+DEFAULT_WORKBOOK_NAME+"']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@value='"+DEFAULT_WORKBOOK_NAME+"']"), "Renamed"+DEFAULT_WORKBOOK_NAME);
        click(Locator.css("body"));
        assertTextPresent("Renamed"+DEFAULT_WORKBOOK_NAME);

        // Clear description
        click(Locator.xpath("//div[@id='wb-description']"));
        setFormElement(Locator.xpath("//textarea"), ""); // textarea is a barely used tag, so this xpath is sufficient for now.
        waitForText("No description provided. Click to add one.", WAIT_FOR_JAVASCRIPT); // Takes a moment to appear.

        // Check that title and description are saved
        refresh();
        assertTextPresent("Renamed"+DEFAULT_WORKBOOK_NAME);
        waitForText("No description provided. Click to add one.", WAIT_FOR_JAVASCRIPT); // Takes a moment to appear.

        clickProject(PROJECT_NAME);

        // Check for all workbooks in list.
        assertLinkPresentWithText("Renamed"+DEFAULT_WORKBOOK_NAME);
        assertLinkPresentWithText(ASSAY_WORKBOOK_NAME);
        assertLinkPresentWithText(FILE_WORKBOOK_NAME);
        assertTextPresentInThisOrder(FILE_WORKBOOK_NAME, ASSAY_WORKBOOK_NAME, "Renamed"+DEFAULT_WORKBOOK_NAME);

        // Delete a workbook
        checkDataRegionCheckbox("query", 2); // Select renamed workbook
        clickButton("Delete", 0);
        assertAlert("Are you sure you want to delete the selected row?");
        waitForTextToDisappear("Renamed"+DEFAULT_WORKBOOK_NAME);

        // Test Workbook APIs

        // Initialize the Creation Wiki
        clickProject(PROJECT_NAME);
        addWebPart("Wiki");

        createNewWikiPage();
        setFormElement(Locator.name("name"), APITEST_NAME);
        setFormElement(Locator.name("title"), APITEST_NAME);
        setWikiBody("Placeholder text.");
        saveWikiPage();

        setSourceFromFile(APITEST_FILE, APITEST_NAME);


        clickButton("RunAPITest", 0);

        waitForText("Insert complete", WAIT_FOR_JAVASCRIPT);
        waitForText("Delete complete", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Insert complete - Success.", "Delete complete - Success.");

        //Create new project, add a workbook to it and ensure that the id is 1
        _containerHelper.createProject(PROJECT_NAME2, null);
        addWebPart("Workbooks");
        WorkbookHelper workbookHelper = new WorkbookHelper(this);
        int id = workbookHelper.createWorkbook(PROJECT_NAME2, FILE_WORKBOOK_NAME, FILE_WORKBOOK_DESCRIPTION, WorkbookHelper.WorkbookFolderType.FILE_WORKBOOK);
        assertEquals("workbook added to new project did not have id=1",id,1);
    }

    public int[] createWorkbooks(String projectName, String fileWorkbookName, String fileWorkbookDescription,
                                 String assayWorkbookName, String assayWorkbookDescription, String defaultWorkbookName, String defaultWorkbookDescription)
    {
        int[] names = new int[3];
        WorkbookHelper workbookHelper = new WorkbookHelper(this);
        names[0] = (workbookHelper.createFileWorkbook(projectName, fileWorkbookName, fileWorkbookDescription));

        // Create Assay Workbook
        names[1] = (workbookHelper.createWorkbook(projectName, assayWorkbookName, assayWorkbookDescription, WorkbookHelper.WorkbookFolderType.ASSAY_WORKBOOK));
        assertLinkPresentWithText("Experiment Runs");
        assertEquals(assayWorkbookName, getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        assertEquals(assayWorkbookDescription, getText(Locator.xpath("//div[@id='wb-description']")));
        assertLinkNotPresentWithText(assayWorkbookName); // Should not appear in folder tree.

        // Create Default Workbook
        names[2] = (workbookHelper.createWorkbook(projectName, defaultWorkbookName, defaultWorkbookDescription, WorkbookHelper.WorkbookFolderType.DEFAULT_WORKBOOK));
        assertLinkPresentWithText("Files");
        assertLinkPresentWithText("Experiment Runs");
        assertEquals(defaultWorkbookName, getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        assertEquals(defaultWorkbookDescription, getText(Locator.xpath("//div[@id='wb-description']")));
        assertLinkNotPresentWithText(defaultWorkbookName); // Should not appear in folder tree.
        return names;
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
