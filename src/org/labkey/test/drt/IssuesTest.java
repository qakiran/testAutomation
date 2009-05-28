/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * User: tamram
 * Date: May 15, 2006
 */
public class IssuesTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "IssuesVerifyProject";
    private static final String ISSUE_TITLE_0 = "A very serious issue";
    private static final String ISSUE_TITLE_1 = "Even more serious issue";

    private static final String[] REQUIRED_FIELDS = {"Title", "AssignedTo", "Type", "Area", "Priority", "Milestone",
                "NotifyList", "String1", "Int1"};
    
    public String getAssociatedModuleDirectory()
    {
        return "issues";
    }

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {/* */}
    }

    protected void initProject()
    {
        createProject(PROJECT_NAME);
        createPermissionsGroup("testers");
        assertPermissionSetting("testers", "No Permissions");
        setPermissions("testers", "Editor");

        clickLinkWithText(PROJECT_NAME);
        addWebPart("Issues");
        addWebPart("Search");
        assertTextPresent("Open");
    }

    protected void doTestSteps()
    {
        initProject();
        
        clickLinkWithText("view open issues");
        assertNavButtonPresent("New Issue");

        // quick security test
        // TODO push lots of locations as we go and move this test to end
        pushLocation();
        pushLocation();
        signOut();
        popLocation();                          // try open issues as guest
        assertNavButtonNotPresent("New Issue");
        assertFormPresent("login");
        signIn();
        popLocation();                          // and logged in again
        assertNavButtonPresent("New Issue");

        // AdminAction
        clickNavButton("Admin");

        // AddKeywordAction
        setFormElement(Locator.formElement("addArea", "keyword"), "Area51");
        clickNavButton("Add Area");
        assertTextPresent("Area51");
        setFormElement(Locator.formElement("addArea", "keyword"), "Fremont");
        clickNavButton("Add Area");
        assertTextPresent("Fremont");
        setFormElement(Locator.formElement("addArea", "keyword"), "Downtown");
        clickNavButton("Add Area");
        assertTextPresent("Downtown");

        // AddKeywordAction
        setFormElement(Locator.formElement("addType", "keyword"), "UFO");
        clickNavButton("Add Type");
        assertTextPresent("UFO");
        setFormElement(Locator.formElement("addType", "keyword"), "SPEC");
        clickNavButton("Add Type");
        assertTextPresent("SPEC");
        setFormElement(Locator.formElement("addType", "keyword"), "TODO");
        clickNavButton("Add Type");
        assertTextPresent("TODO");
        setFormElement(Locator.formElement("addType", "keyword"), "AAA");
        clickNavButton("Add Type");
        assertTextPresent("AAA");
        //SetKeywordDefaultAction
        clickLinkWithText("set");
        // UNDONE: check AAA is bold???
        //SetKeywordDefaultAction
        clickLinkWithText("clear");
        // UNDONE: check AAA is not bold???
        clickLinkWithText("delete");
        assertTextNotPresent("AAA");

        // AddKeywordAction
        setFormElement(Locator.formElement("addMilestone", "keyword"), "2012");
        clickNavButton("Add Milestone");
        assertTextPresent("2012");
        setFormElement(Locator.formElement("addMilestone", "keyword"), "2013");
        clickNavButton("Add Milestone");
        assertTextPresent("2013");

        // UpdateRequiredFieldsAction
        //         <tr><td><input type="checkbox" name="requiredFields"  value="Milestone">Milestone</td></tr>
        checkCheckbox("requiredFields", "Milestone");
        clickNavButton("Update Required Fields");

        // SetCustomColumnConfigurationAction
        setText("int1", "MyInteger");
        clickNavButton("Update Custom Fields");

        // ListAction (empty)
        clickNavButton("Back to Issues");

        // InsertAction
        clickNavButton("New Issue");
        setFormElement("title", ISSUE_TITLE_0);
        selectOptionByText("type", "UFO");
        selectOptionByText("area", "Area51");
        selectOptionByText("priority", "2");
        setFormElement("comment", "a bright flash of light");
        clickNavButton("Submit");

        // test validate
        assertTextPresent("Field AssignedTo cannot be null");
        selectOptionByText("assignedTo", getDisplayName());
        clickNavButton("Submit");
        assertTextPresent("Field Milestone cannot be null");
        selectOptionByText("milestone", "2012");
        clickNavButton("Submit");

        // find issueId
        String title = getLastPageTitle();
        title = title.substring(0,title.indexOf(':')).trim();
        int issueId = Integer.parseInt(title);

        // DetailsAction
        assertTextPresent("" + issueId + " : " + ISSUE_TITLE_0);
        assertTextPresent("Milestone");

        // ListAction
        clickLinkWithText("view grid");

        //Click the issue id based on the text issue title
        //String xpath = "//td/a[text() = '" + ISSUE_TITLE_0 + "']/../../td[2]/a";
        //clickAndWait(Locator.xpath(xpath));
        clickLinkWithText("" + issueId);

        // UpdateAction
        clickLinkWithText("update");
        setFormElement("comment", "don't believe the hype");
        clickNavButton("Submit");
        searchFor(PROJECT_NAME, "2012", 1, ISSUE_TITLE_0);

        // ResolveAction
        clickLinkWithText("resolve");
        clickNavButton("Submit");

        // ReopenAction
        clickLinkWithText("reopen");
        clickNavButton("Submit");

        // ResolveAction
        clickLinkWithText("resolve");
        clickNavButton("Submit");

        // CloseAction
        clickLinkWithText("close");
        clickNavButton("Submit");

        // Test .lastFilter
        testLastFilter(issueId);

        // JumpToIssueAction
        setText("issueId",""+ issueId);
        submit(Locator.formWithName("jumpToIssue"));
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent("Invalid");

        // SearchAction
        clickLinkWithText("view grid");
        setText("search","hype");
        clickNavButton("Search");
        assertLinkPresentWithText(ISSUE_TITLE_0);

        // SearchWebPart
        searchFor(PROJECT_NAME, "2012", 1, ISSUE_TITLE_0);

        queryTest();

        // back to grid view
        clickLinkWithText("Issues Summary");

        requiredFieldsTest();
        viewSelectedDetailsTest();
        entryTypeNameTest();
        
        // UNDONE test these actions
        // CompleteUserAction
        // EmailPrefsAction
        // ExportTsvAction
        // PurgeAction
        // RssAction
    }

    private void entryTypeNameTest()
    {
        clickNavButton("Admin");
        setFormElement(Locator.formElement("entryTypeNames", "entrySingularName"), "Ticket");
        setFormElement(Locator.formElement("entryTypeNames", "entryPluralName"), "Tickets");
        clickNavButton("Update Entry Type Names");

        assertFormElementEquals("entrySingularName", "Ticket");
        assertFormElementEquals("entryPluralName", "Tickets");

        assertTextPresent("Tickets Admin Page");
        clickLinkWithText("Tickets List");

        assertTextPresent("Tickets List");
        assertTextNotPresent("Issues List");
        assertNavButtonPresent("New Ticket");
        assertNavButtonPresent("Jump to Ticket");
        assertTextPresent("Ticket ID");
        assertTextNotPresent("Issue ID");

        clickNavButton("Admin");
        setFormElement(Locator.formElement("entryTypeNames", "entrySingularName"), "Issue");
        setFormElement(Locator.formElement("entryTypeNames", "entryPluralName"), "Issues");
        clickNavButton("Update Entry Type Names");
    }

    private void requiredFieldsTest()
    {
        clickNavButton("Admin");
        setFormElement("int1", "Contract Number");
        setFormElement("string1", "Customer Name");
        clickNavButton("Update Custom Fields");

        //setWorkingForm("requiredFieldsForm");
        for (String field : REQUIRED_FIELDS)
            checkRequiredField(field, true);

        clickNavButton("Update Required Fields");
        clickNavButton("Back to Issues");
        clickNavButton("Admin");

        //setWorkingForm("requiredFieldsForm");
        for (String field : REQUIRED_FIELDS)
        {
            verifyFieldChecked(field);
            checkRequiredField(field, false);
        }
        clickNavButton("Update Required Fields");

        checkRequiredField("Title", true);
        clickNavButton("Update Required Fields");
        clickNavButton("Back to Issues");
        clickNavButton("New Issue");
        clickNavButton("Submit");

        assertTextPresent("Field Title cannot be null.");
        clickNavButton("View Grid");
    }

    private void checkRequiredField(String name, boolean select)
    {
        Locator checkBoxLocator = Locator.checkboxByNameAndValue("requiredFields", name);

        if (select)
            checkCheckbox("requiredFields", name);
        else
        {
            if (isChecked(checkBoxLocator))
                click(checkBoxLocator);
        }
    }

    private void verifyFieldChecked(String fieldName)
    {
        if (isChecked(Locator.checkboxByNameAndValue("requiredFields", fieldName)))
            return;

        assertFalse("Checkbox not set for element: " + fieldName, false);
    }

    private void viewSelectedDetailsTest()
    {
        setFilter("Issues", "Status", "<has any value>");
        clickCheckbox(".toggle");
        clickNavButton("View Details");
        assertTextPresent("a bright flash of light");
        assertTextPresent("don't believe the hype");
        clickLinkWithText("view grid");
    }
    
    public void testLastFilter(int issueId)
    {
        log("Testing .lastFilter");

        // insert a new issue
        clickLinkWithText("new issue");
        setFormElement("title", ISSUE_TITLE_1);
        selectOptionByText("type", "UFO");
        selectOptionByText("area", "Area51");
        selectOptionByText("priority", "1");
        setFormElement("comment", "alien autopsy");
        selectOptionByText("milestone", "2013");
        selectOptionByText("assignedTo", getDisplayName());
        clickNavButton("Submit");

        // assert both issues are present
        clickLinkWithText("view grid");
        clearAllFilters("Issues", "IssueId");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextPresent(ISSUE_TITLE_1);

        // Filter out all pri-1 bugs; assert newly created issue is filtered out
        setFilter("Issues", "Priority", "Does not Equal", "1");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent(ISSUE_TITLE_1);

        // view an issue
        clickLinkWithText(String.valueOf(issueId));

        // assert .lastFilter is applied
        clickLinkWithText("view grid");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent(ISSUE_TITLE_1);

        clearAllFilters("Issues", "IssueId");
    }

    protected void queryTest()
    {
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Query");
        setFormElement("schemaName", "issues");
        submit();
        clickLinkWithText("Issues Queries");
        clickLinkWithText("create new query");
        setFormElement("ff_newQueryName", "xxyzzy");
        clickNavButton("Create and edit SQL");
        clickNavButton("Run Query");
        clickLinkWithText("issues Schema");
        clickLinkWithText("delete");
        clickNavButton("OK");
        clickLinkWithText(PROJECT_NAME);
    }
}
