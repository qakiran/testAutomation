/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;

/**
 * User: tamram
 * Date: May 15, 2006
 */
public class MessagesLongTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "MessagesVerifyProject";
    private static final String EXPIRES1 = "2107-07-19";
    private static final String EXPIRES2 = "2108-07-19";
    private static final String MSG1_TITLE = "test message 1";
    private static final String MSG1_BODY = "this is a test message to Banana";
    private static final String MSG2_TITLE = "test message 2";
    private static final String MSG3_TITLE = "test message 3";
    private static final String MSGB_TITLE = "admin broadcast";
    private static final String MSGB_BODY = "this is a broadcast message";
    private static final String RESP1_TITLE = "test response 1";
    private static final String RESP1_BODY = "this is another test, thanks";
    private static final String RESP2_BODY = "third test, thanks";
    private static final String USER1 = "user1@messages.test";
    private static final String USER2 = "user2@messages.test";
    private static final String USER3 = "user3@messages.test";
    private static final String HTML_BODY = "1 <b>x</b>\n" +
            "<b>${labkey.webPart(partName='Lists')}</b>\n";
    private static final String HTML_BODY_WEBPART_TEST = "manage lists";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/announcements";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void permissionCheck(String permission, boolean readAbility)
    {
        clickLinkWithText(PROJECT_NAME);
        enterPermissionsUI();
        removePermission("Users","Reader");
        removePermission("Users","Author");
        removePermission("Users","Editor");
        setPermissions("Users", permission);
        exitPermissionsUI();
        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        if (readAbility)
            assertTextPresent(MSG1_BODY);
        else
            assertTextNotPresent(MSG1_BODY);
        stopImpersonating();
    }

    protected void doCleanup()
    {
        deleteUser(USER1);
        deleteUser(USER2);
        deleteUser(USER3);
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doTestSteps()
    {
        log("Open new project, add group, alter permissions");
        createProject(PROJECT_NAME, "Collaboration");
        createPermissionsGroup("Administrators");
        setPermissions("Administrators", "Project Administrator");
        createPermissionsGroup("testers1");
        assertPermissionSetting("testers1", "No Permissions");
        exitPermissionsUI();
        enableModule(PROJECT_NAME, "Dumbster");
        log("Add search to project");
        addWebPart("Search");

        enableEmailRecorder();

        log("Check email preferences");
        clickWebpartMenuItem("Messages", "Email", "Preferences");
        checkRadioButton("emailPreference", "1");
        clickNavButton("Update");
        clickNavButton("Done");

        log("Customize message board");
        clickWebpartMenuItem("Messages", "Customize");
        checkCheckbox("expires");
        clickNavButton("Save");

        log("Check email admin works");
        clickWebpartMenuItem("Messages", "Email", "Administration");

        click(Locator.navButton("Update Settings"));
        selenium.mouseDown("//li/a[text()='messages']");
        Locator.XPathLocator folderDefaultCombo = Locator.xpath("//input[@name='defaultEmailOption']/../../div");

        waitForElement(Locator.xpath("//input[@name='defaultEmailOption']"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Update Folder Default", 0);

        waitForExtMaskToDisappear();
        assertTextPresent("All conversations");
        ExtHelper.selectComboBoxItem(this, folderDefaultCombo, "Broadcast messages only");
        clickNavButton("Update Folder Default", 0);

        waitForExtMaskToDisappear();
        clickLinkWithText(PROJECT_NAME);

        log("Check message works in Wiki");
        clickWebpartMenuItem("Messages", "New");
        setFormElement("title", MSG1_TITLE);
        setFormElement("expires", EXPIRES1);
        setFormElement("body", "1 <b>x</b>");
        selectOptionByText("rendererType", "Wiki Page");
        assertTextPresent("Admin Broadcast");
        submit();
        assertTextPresent(MSG1_TITLE);
        clickLinkWithText("view message or respond");
        assertTextPresent(EXPIRES1);
        assertTextPresent("<b>x</b>");
        clickNavButton("Delete Message");
        clickNavButton("Delete");

        log("Check that HTML message works");
        clickNavButton("New");
        setFormElement("title", MSG1_TITLE);
        setFormElement("body", HTML_BODY);
        selectOptionByText("rendererType", "HTML");
        submit();
        assertTextPresent("1 x");
        assertLinkPresentWithText(HTML_BODY_WEBPART_TEST);

        log("Check that edit works");
        clickLinkWithText("view message or respond");
        clickLinkWithText("edit");
        setFormElement("body", MSG1_BODY);
        submit();
        assertTextPresent(MSG1_BODY);

        log("Add response");
        clickNavButton("Respond");
        setFormElement("title", RESP1_TITLE);
        setFormElement("expires", EXPIRES2);
        setFormElement("body", RESP1_BODY);
        submit();

        log("Make sure response was entered correctly");
        assertTextPresent(RESP1_TITLE);
        assertTextPresent(EXPIRES2);
        assertTextPresent(RESP1_BODY);

        log("Add second response, make sure it was entered and recognized");
        clickNavButton("Respond");
        setFormElement("body", RESP2_BODY);
        submit();
        assertTextPresent(RESP2_BODY);
        clickLinkWithText("Messages");
        assertTextPresent("2 responses");

        log("Create fake user for permissions check");
        enterPermissionsUI();
        clickManageGroup("Users");
        setFormElement("names", USER1);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");

        log("Check if permissions work without security");
        permissionCheck("Reader", true);
        permissionCheck("Editor", true);

        log("Check with security");
        clickLinkWithText(PROJECT_NAME);
        clickWebpartMenuItem("Messages", "Customize");
        checkRadioButton("secure", 1);
        clickNavButton("Save");
        permissionCheck("Reader", false);
        permissionCheck("Editor", true);

        log("Check if the customized names work");
        clickLinkWithText(PROJECT_NAME);
        clickWebpartMenuItem("Messages", "Customize");
        setFormElement("boardName", "Notes");
        setFormElement("conversationName", "Thread");
        clickNavButton("Save");
        assertTextPresent("Notes");
        assertTextPresent("thread");
        clickWebpartMenuItem("Notes", "Customize");
        setFormElement("boardName", "Messages");
        setFormElement("conversationName", "Message");
        clickNavButton("Save");

        log("Check if sorting works");
        clickWebpartMenuItem("Messages", "New");
        setFormElement("title", MSG2_TITLE);
        submit();
        clickLinkWithText("Messages");
        clickLinkWithText("view message or respond");
        assertTextPresent(MSG2_TITLE);
        clickLinkWithText("Messages");
        clickLinkWithText("view message or respond", 1);
        clickNavButton("Respond");
        submit();
        clickLinkWithText("Messages");
        clickLinkWithText("Customize");
        checkRadioButton("sortOrderIndex", 1);
        clickNavButton("Save");
        clickLinkWithText("view message or respond");
        assertTextPresent(MSG1_TITLE);

        log("Edit other customize options");
        clickLinkWithText("Messages");
        clickLinkWithText("Customize");
        uncheckCheckbox("titleEditable");
        checkCheckbox("memberList");
        checkCheckbox("status");
        uncheckCheckbox("expires");
        checkCheckbox("assignedTo");
        uncheckCheckbox("formatPicker");
        selectOptionByText("defaultAssignedTo", USER1);
        clickNavButton("Save");

        log("Check if status and expires work");
        clickNavButton("New");
        assertTextPresent(USER1);
        clickNavButton("Cancel");
        clickLinkWithText(MSG2_TITLE);
        clickNavButton("Respond");
        selectOptionByText("status", "Closed");
        assertFormElementEquals("assignedTo", "");
        submit();
        assertTextPresent("Status: Closed");
        assertTextNotPresent("Expires:");
        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        assertTextNotPresent(MSG2_TITLE);
        stopImpersonating();
        clickLinkWithText(PROJECT_NAME);

        // USER1 is now a reader
        log("Test member list");
        enterPermissionsUI();
        removePermission("Users", "Editor");
        setPermissions("Users", "Reader");
        exitPermissionsUI();

        // USER2 is a nobody
        clickLinkWithText("Site Users");
        clickNavButton("Add Users");
        setFormElement("newUsers", USER2);
        uncheckCheckbox("sendMail");
        clickNavButton("Add Users");
        clickLinkWithText(PROJECT_NAME);

        // USER3 is a Project Administrator
        enterPermissionsUI();
        clickManageGroup("Administrators");
        setFormElement("names", USER3);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
        
        clickLinkWithText(PROJECT_NAME);
        clickWebpartMenuItem("Messages", "New");
        setFormElement("emailList", USER2);
        clickButtonContainingText("Submit", "Title must not be blank");
        clickButtonContainingText("OK", 0);
        waitForExtMaskToDisappear();
        setFormElement("title", MSG3_TITLE);
        submit();
        assertTextPresent("This user doesn't have permission");
        setFormElement("emailList", USER1);
        selectOptionByText("assignedTo", USER3);
        submit();
        clickLinkWithText("view message or respond");
        assertTextPresent("Members: "+USER1);
        assertTextPresent("Assigned To: "+USER3);
        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        assertTextPresent(MSG3_TITLE);
        stopImpersonating();
        clickLinkWithText(PROJECT_NAME);
        clickWebpartMenuItem("Messages", "Customize");
        checkRadioButton("secure", 0);
        clickNavButton("Save");
        clickLinkWithText(MSG3_TITLE);
        clickNavButton("Delete Message");
        clickNavButton("Delete");

        log("Check delete response works and is recognized");
        clickLinkWithText("view message or respond", 1);
        clickLinkWithText("delete");
        clickNavButton("Delete");
        assertTextNotPresent(RESP1_BODY);
        clickLinkWithText("Messages");
        assertTextPresent("2 response");
        clickLinkWithText(PROJECT_NAME);
        assertTextNotPresent(MSG2_TITLE);
        clickLinkWithText("view message or respond");

        log("Check delete message works fully");
        clickNavButton("Delete Message");
        clickNavButton("Delete");
        assertTextNotPresent(MSG1_TITLE);
        clickLinkWithText(PROJECT_NAME);
        assertTextNotPresent(MSG1_TITLE);

        log("Check emailed messages");
        goToModule("Dumbster");
        assertTextPresent("RE: " + MSG1_TITLE, 4); // TODO: switch to 3 when empty messages are emailed
        clickLinkWithText(MSG1_TITLE, 0, false);
        assertTextPresent("1 x");
        assertLinkPresentWithText(HTML_BODY_WEBPART_TEST);
        clickLinkWithText(MSG1_TITLE, 1, false);
        assertTextPresent("<b>x</b>");
        assertLinkNotPresentWithText(MSG3_TITLE);
        assertLinkNotPresentWithText(MSG2_TITLE);

        log("Clear mail record");
        uncheckCheckbox("emailRecordOn");
        checkCheckbox("emailRecordOn");

        log("Set broadcast preferences.");
        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        clickWebpartMenuItem("Messages", "Email", "Preferences");
        checkRadioButton("emailPreference", "0");
        pushLocation();
        clickNavButton("Update");
        stopImpersonating();
        impersonate(USER2);
        popLocation();
        checkRadioButton("emailPreference", "3");
        clickNavButton("Update");
        stopImpersonating();
        clickLinkWithText(PROJECT_NAME);
        log("Check admin broadcast message");
        clickWebpartMenuItem("Messages", "New");
        setFormElement("title", MSGB_TITLE);
        setFormElement("body", MSGB_BODY);
        checkCheckbox("broadcast");
        submit();

        goToModule("Dumbster");
        assertTextPresent(USER2, USER3);
        assertTextNotPresent(USER1); // opted out of broadcasts
    }
}

