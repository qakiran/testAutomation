/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.ExtHelper;

/**
 * User: tamram
 * Date: May 15, 2006
 */
// caBIG module is not installed by default any more, so this test isn't run
public class CaBigTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "CaBigVerifyProject";
    protected static final String FOLDER_NAME = "CaBigFolder";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/cabig";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup()
    {
        try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {/* */}
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {/* */}
    }

    protected void doTestSteps()
    {
        createProject(PROJECT_NAME);

        boolean caBigEnabled = isTextPresent("Publish to caBIG");

        if (!caBigEnabled)
            setCaBigSiteSetting(true);

        enterPermissionsUI();

        // Test publish/unpublish on the project
        ExtHelper.clickExtTab(this, "Publish to caBIG");
        assertTextPresent("This folder is not published to the caBIG");
        clickNavButton("Publish");
        ExtHelper.clickExtTab(this, "Publish to caBIG");
        assertTextPresent("This folder is published to the caBIG");
        clickNavButton("Unpublish");
        ExtHelper.clickExtTab(this, "Publish to caBIG");
        assertTextPresent("This folder is not published to the caBIG");
        clickNavButton("Save and Finish");

        // Create a subfolder
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[0]);
        enterPermissionsUI();
        clickNavButton("Publish");
        clickNavButton("Save and Finish");
        clickLinkWithText(PROJECT_NAME);
        enterPermissionsUI();

        // Test caBIG admin page
        clickNavButton("Admin");
        assertNavButtonPresent("Publish");
        assertNavButtonPresent("Unpublish");
        clickNavButton("Publish All");
        assertNavButtonNotPresent("Publish");
        clickNavButton("Unpublish All");
        assertNavButtonNotPresent("Unpublish");

        clickNavButton("Done");

        // Should be on the project permissions page
        assertTextPresent("Publish to caBIG");
        ExtHelper.clickExtTab(this, "Publish to caBIG");
        assertTextPresent("This folder is not published to the caBIG");
        clickNavButton("Save and Finish");

        // Turn off caBIG if it was originally off
        if (!caBigEnabled)
            setCaBigSiteSetting(false);
    }

    private void setCaBigSiteSetting(boolean enable)
    {
        pushLocation();

        gotoAdminConsole();
        clickLinkWithText("site settings");

        if (enable)
            checkCheckbox("caBIGEnabled");
        else
            uncheckCheckbox("caBIGEnabled");

        clickNavButton("Save");

        popLocation();
    }
}
