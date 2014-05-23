package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.openqa.selenium.NoSuchElementException;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * org.labkey.core.security.SecurityController#PermissionsAction
 */
public class PermissionsHelper
{
    private BaseWebDriverTest _test;

    public PermissionsHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    @LogMethod
    public void startCreateGlobalPermissionsGroup(@LoggedParam String groupName, boolean failIfAlreadyExists)
    {
        _test.goToHome();
        _test.goToSiteGroups();
        if(_test.isElementPresent(Locator.tagWithText("div", groupName)))
        {
            if(failIfAlreadyExists)
                throw new IllegalArgumentException("Group already exists: " + groupName);
            else
                return;
        }

        Locator l = Locator.xpath("//input[contains(@name, 'sitegroupsname')]");
        _test.waitForElement(l, _test.defaultWaitForPage);

        _test.setFormElement(l, groupName);
        _test.clickButton("Create New Group", 0);
        _test._extHelper.waitForExtDialog(groupName + " Information");
    }

    public void createGlobalPermissionsGroup(String groupName, String... users)
    {
        createGlobalPermissionsGroup(groupName, true, users);
    }

    @LogMethod
    public void createGlobalPermissionsGroup(@LoggedParam String groupName, boolean failIfAlreadyExists, @LoggedParam String... users)
    {
        startCreateGlobalPermissionsGroup(groupName, failIfAlreadyExists);
        StringBuilder namesList = new StringBuilder();

        for (String member : users)
        {
            namesList.append(member).append("\n");
        }

        _test.log("Adding [" + namesList.toString() + "] to group " + groupName + "...");
        _test.waitAndClickAndWait(Locator.tagContainingText("a", "manage group"));
        _test.waitForElement(Locator.name("names"));
        _test.setFormElement(Locator.name("names"), namesList.toString());
        _test.uncheckCheckbox(Locator.name("sendEmail"));
        _test.clickButton("Update Group Membership");
    }

    public void createPermissionsGroup(String groupName)
    {
        _test.log("Creating permissions group " + groupName);
        if (!_test.isElementPresent(Locator.permissionRendered()))
            enterPermissionsUI();
        _test._ext4Helper.clickTabContainingText("Project Groups");
        _test.setFormElement(Locator.xpath("//input[contains(@name, 'projectgroupsname')]"), groupName);
        _test.clickButton("Create New Group", 0);
        _test._extHelper.waitForExtDialog(groupName + " Information");
        _test.assertTextPresent("Group " + groupName);
        _test.click(Ext4Helper.Locators.ext4Button("Done"));
        _test.waitForElement(Locator.css(".groupPicker .x4-grid-cell-inner").withText(groupName), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public String toRole(String perm)
    {
        String R = "security.roles.";
        if ("No Permissions".equals(perm))
            return R + "NoPermissionsRole";
        if ("Project Administrator".equals(perm))
            return R + "ProjectAdminRole";
        else if (!perm.contains("."))
            return R + perm + "Role";
        return perm;
    }

    public void assertNoPermission(String groupName, String permissionSetting)
    {
        _test.waitForElement(Locator.permissionRendered(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElementToDisappear(Locator.permissionButton(groupName, permissionSetting), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void assertPermissionSetting(String groupName, String permissionSetting)
    {
        String role = toRole(permissionSetting);
        if ("security.roles.NoPermissionsRole".equals(role))
        {
            assertNoPermission(groupName, "Reader");
            assertNoPermission(groupName, "Editor");
            assertNoPermission(groupName, "Project Administrator");
            return;
        }
        _test.log("Checking permission setting for group " + groupName + " equals " + role);
        _test.waitForElement(Locator.permissionRendered(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.assertElementPresent(Locator.permissionButton(groupName, permissionSetting));
    }

    public void checkInheritedPermissions()
    {
        _test._ext4Helper.checkCheckbox("Inherit permissions from parent");
    }

    public void uncheckInheritedPermissions()
    {
        _test._ext4Helper.uncheckCheckbox("Inherit permissions from parent");
    }

    public void savePermissions()
    {
        _test.clickButton("Save", 0);
        _test.waitForElement(Locator.permissionRendered(), _test.defaultWaitForPage);
        _test._ext4Helper.waitForMaskToDisappear();
    }

    @LogMethod
    public void setPermissions(@LoggedParam String groupName, @LoggedParam String permissionString)
    {
        _setPermissions(groupName, permissionString, "pGroup");
    }

    @LogMethod
    public void setSiteGroupPermissions(@LoggedParam String groupName, @LoggedParam String permissionString)
    {
        _setPermissions(groupName, permissionString, "pSite");
    }

    @LogMethod
    public void setUserPermissions(@LoggedParam String userName, @LoggedParam String permissionString)
    {
        _test.log(new Date().toString());
        _setPermissions(userName, permissionString, "pUser");

        _test.log(new Date().toString());
    }

    @LogMethod
    public void setSiteAdminRoleUserPermissions(@LoggedParam String userName, @LoggedParam String permissionString)
    {
        _test.log(new Date().toString());
        _test.goToSiteAdmins();
        _test.clickAndWait(Locator.linkContainingText("Permissions"));
        _test._ext4Helper.clickTabContainingText("Permissions");
        _selectPermission(userName, userName, permissionString);
        _test.log(new Date().toString());
    }

    public void _setPermissions(String userOrGroupName, String permissionString, String className)
    {
        String role = toRole(permissionString);
        if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
        {
            throw new IllegalArgumentException("Can't set NoPermissionRole; call removePermission()");
        }
        else
        {
            _test.log("Setting permissions for group " + userOrGroupName + " to " + role);

            if (!_test.isElementPresent(Locator.permissionRendered()))
                enterPermissionsUI();
            _test._ext4Helper.clickTabContainingText("Permissions");

            String group = userOrGroupName;
            if (className.equals("pSite"))
                group = "Site: " + group;
            _selectPermission(userOrGroupName, group, permissionString);
        }
    }

    public void _selectPermission(String userOrGroupName, String group, String permissionString)
    {
        Locator.XPathLocator roleCombo = Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + permissionString + "']]");
        _test.waitForElement(roleCombo);
        _test._ext4Helper.selectComboBoxItem(roleCombo, Ext4Helper.TextMatchTechnique.STARTS_WITH, group);
        _test.waitForElement(Locator.permissionButton(userOrGroupName, permissionString));
        String oldId = _test.getAttribute(Locator.permissionButton(userOrGroupName, permissionString), "id");
        savePermissions();
        _test._ext4Helper.waitForMaskToDisappear();
        _test.waitForElementToDisappear(Locator.id(oldId), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT); // Elements get new ids after save
        assertPermissionSetting(userOrGroupName, permissionString);
    }

    public void removeSiteGroupPermission(String groupName, String permissionString)
    {
        _removePermission(groupName, permissionString, "pSite");
    }

    public void removePermission(String groupName, String permissionString)
    {
        _removePermission(groupName, permissionString, "pGroup");
    }

    public void _removePermission(String groupName, String permissionString, String className)
    {
        Locator close = Locator.closePermissionButton(groupName,permissionString);
        if (_test.isElementPresent(close))
        {
            _test.click(close);
            _test.waitForElementToDisappear(close);
            savePermissions();
            assertNoPermission(groupName, permissionString);
        }
    }

    public void addUserToSiteGroup(String userName, String groupName)
    {
        _test.goToHome();
        _test.goToSiteGroups();
        Locator.XPathLocator groupLoc = Locator.tagWithText("div", groupName);
        _test.waitForElement(groupLoc, _test.defaultWaitForPage);
        _test.click(groupLoc);
        _test.clickAndWait(Locator.linkContainingText("manage group"));
        _test.addUserToGroupFromGroupScreen(userName);
    }

    /**
     * Adds a new or existing user to an existing group within an existing project
     * @param userName new or existing user name
     * @param projectName existing project name
     * @param groupName existing group within the project to which we should add the user
     */
    public void addUserToProjGroup(String userName, String projectName, String groupName)
    {
        if (_test.isElementPresent(Locator.permissionRendered()))
        {
            exitPermissionsUI();
            _test.clickProject(projectName);
        }
        enterPermissionsUI();
        _test.clickManageGroup(groupName);
        _test.addUserToGroupFromGroupScreen(userName);
    } //addUserToProjGroup()

    public void enterPermissionsUI()
    {
        //if the following assert triggers, you were already in the permissions UI when this was called
        if (!_test.isElementPresent(Locator.permissionRendered()))
        {
            _test.clickAdminMenuItem("Folder", "Permissions");
            _test.waitForElement(Locator.permissionRendered());
        }
    }

    public void exitPermissionsUI()
    {
        _test._ext4Helper.clickTabContainingText("Permissions");
        _test.clickButton("Save and Finish");
    }

    public void deleteGroup(String groupName)
    {
        deleteGroup(groupName, false);
    }

    public void deleteAllUsersFromGroup()
    {
        Locator.XPathLocator l = Locator.xpath("//td/a/span[text()='remove']");

        while(_test.isElementPresent(l))
        {
            int i = _test.getElementCount(l) - 1;
            _test.click(l);
            _test.waitForElementToDisappear(l.index(i), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
    }

    @LogMethod(quiet = true)
    public void deleteGroup(@LoggedParam String groupName, boolean failIfNotFound)
    {
        _test.log("Attempting to delete group: " + groupName);
        if (selectGroup(groupName, failIfNotFound))
        {
            _test.assertElementPresent(Locator.css(".x4-grid-cell-first").withText(groupName));
            deleteAllUsersFromGroup();
            _test.click(Locator.xpath("//td/a/span[text()='Delete Empty Group']"));
            _test.waitForElementToDisappear(Locator.css(".x4-grid-cell-first").withText(groupName), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
    }

    public void removeUserFromGroup(String groupName, String userName)
    {
        if(!_test.isTextPresent("Group " + groupName))
            selectGroup(groupName);

        Locator l = Locator.xpath("//td[text()='" + userName +  "']/..//td/a/span[text()='remove']");
        _test.click(l);
    }

    public void addUserToGroup(String groupName, String userName)
    {
        if(!_test.isTextPresent("Group " + groupName))
            selectGroup(groupName);
        String dialogTitle = groupName + " Information";

        _test._ext4Helper.selectComboBoxItem(Locator.xpath(_test._extHelper.getExtDialogXPath(dialogTitle) + "//table[contains(@id, 'labkey-principalcombo')]"), userName);
        Locator.css(".userinfo td").withText(userName).waitForElement(_test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test._extHelper.clickExtButton(dialogTitle, "Done", 0);
        _test._extHelper.waitForExtDialogToDisappear(dialogTitle);

        _test.clickButton("Done");
    }

    public boolean selectGroup(String groupName, boolean failIfNotFound)
    {
        if(!_test.isElementPresent(Locator.css(".x4-tab-active").withText("Site Groups")))
            _test.goToSiteGroups();

        _test.waitForElement(Locator.css(".groupPicker .x4-grid-body"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        if (_test.isElementPresent(Locator.xpath("//div[text()='" + groupName + "']")))
        {
            _test.click(Locator.xpath("//div[text()='" + groupName + "']"));
            _test._extHelper.waitForExtDialog(groupName + " Information");
            return true;
        }
        else if (failIfNotFound)
            throw new NoSuchElementException("Group not found:" + groupName);

        return false;
    }

    public boolean selectGroup(String groupName)
    {
        return selectGroup(groupName, false);
    }

    public boolean doesGroupExist(String groupName, String projectName)
    {
        _test.ensureAdminMode();
        _test.clickProject(projectName);
        enterPermissionsUI();
        _test._ext4Helper.clickTabContainingText("Project Groups");
        _test.waitForText("Member Groups");
        List<Ext4CmpRef> refs = _test._ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        exitPermissionsUI();
        return (idx >= 0);
    }

    public void assertGroupExists(String groupName, String projectName)
    {
        _test.log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (!doesGroupExist(groupName, projectName))
            fail("group " + groupName + " does not exist in project " + projectName);
    }

    public void assertGroupDoesNotExist(String groupName, String projectName)
    {
        _test.log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (doesGroupExist(groupName, projectName))
            fail("group " + groupName + " exists in project " + projectName);
    }

    public boolean isUserInGroup(String email, String groupName, String projectName)
    {
        _test.ensureAdminMode();
        _test.clickProject(projectName);
        enterPermissionsUI();
        _test._ext4Helper.clickTabContainingText("Project Groups");
        _test.waitForElement(Locator.css(".groupPicker"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitAndClick(Locator.xpath("//div[text()='" + groupName + "']"));
        _test._extHelper.waitForExtDialog(groupName + " Information");
        boolean ret = _test.isElementPresent(Locator.xpath("//table[contains(@class, 'userinfo')]//td[starts-with(text(), '" + email + "')]"));
        _test.clickButton("Done", 0);
        _test._extHelper.waitForExtDialogToDisappear(groupName + " Information");
        return ret;
    }

    public void assertUserInGroup(String email, String groupName, String projectName)
    {
        _test.log("asserting that user " + email + " is in group " + projectName + "/" + groupName + "...");
        if (!isUserInGroup(email, groupName, projectName))
            fail("user " + email + " was not in group " + projectName + "/" + groupName);
    }

    public void assertUserNotInGroup(String email, String groupName, String projectName)
    {
        _test.log("asserting that user " + email + " is not in group " + projectName + "/" + groupName + "...");
        if (isUserInGroup(email, groupName, projectName))
            fail("user " + email + " was found in group " + projectName + "/" + groupName);
    }
}
