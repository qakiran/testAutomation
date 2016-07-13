/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_PAGE;
import static org.labkey.test.WebDriverWrapper.sleep;
import static org.labkey.test.WebDriverWrapper.waitFor;
import static org.labkey.test.components.ext4.Window.Window;

public class FileBrowserHelper
{
    public static final String IMPORT_SIGNAL_NAME = "import-actions-updated";
    public static final Locator fileGridCell = Locator.tagWithClass("div", "labkey-filecontent-grid").append(Locator.tagWithClass("div", "x4-grid-cell-inner"));

    BaseWebDriverTest _test;
    public FileBrowserHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    @LogMethod(quiet = true)
    public void expandFileBrowserRootNode()
    {
        expandFileBrowserTree();
        _test.waitAndClick(Locators.treeRow(0));
        _test.waitForElement(Locators.selectedTreeRow(0), WAIT_FOR_JAVASCRIPT);
    }

    public void expandFileBrowserTree()
    {
        if (_test.isElementPresent(Locators.collapsedFolderTree))
        {
            clickFileBrowserButton(BrowserAction.FOLDER_TREE);
            _test.waitForElementToDisappear(Locators.folderTree);
        }
    }

    @LogMethod
    public void selectFileBrowserItem(@LoggedParam String path)
    {
        waitForFileGridReady();
        openFolderTree();

        String[] parts;
        if (path.equals("/"))
            parts = new String[]{""}; // select root node
        else
            parts = path.split("/");

        String baseNodeId;
        try
        {
            baseNodeId = Locators.treeRow(0).findElement(_test.getDriver()).getAttribute("data-recordid");
        }
        catch (StaleElementReferenceException retry)
        {
            baseNodeId = Locators.treeRow(0).findElement(_test.getDriver()).getAttribute("data-recordid");
        }

        StringBuilder nodeId = new StringBuilder();
        nodeId.append(baseNodeId);

        for (int i = 0; i < parts.length; i++)
        {
            nodeId.append(parts[i]);
            if (!parts[i].equals(""))
            {
                nodeId.append('/');
            }

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory
                checkFileBrowserFileCheckbox(parts[i]);// select last item: click on tree node name
            else
                selectFolderTreeNode(nodeId.toString());
        }
    }

    @LogMethod(quiet = true)
    private void selectFolderTreeNode(@LoggedParam String nodeId)
    {
        final Locator.XPathLocator fBrowser = Locator.tagWithClass("div", "fbrowser");
        final Locator.XPathLocator folderTreeNode = fBrowser.append(Locator.tag("tr").withPredicate("starts-with(@id, 'treeview')").attributeEndsWith("data-recordid", nodeId));

        _test.waitForElement(folderTreeNode);
        _test.waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]")); // temoporary row exists during expansion animation

        final Locator folderTreeNodeSelected = folderTreeNode.withClass("x4-grid-row-selected");
        if (!_test.isElementPresent(folderTreeNodeSelected))
        {
            try
            {
                _test.doAndWaitForPageSignal(() -> _test.click(folderTreeNode), IMPORT_SIGNAL_NAME);
            }
            catch (StaleElementReferenceException staleSignal)
            {
                _test.waitForElement(Locators.pageSignal(IMPORT_SIGNAL_NAME));
            }
            waitForGrid();
        }
    }

    private WebElement waitForGrid()
    {
        final Locator.XPathLocator fBrowser = Locator.tagWithClass("div", "fbrowser");
        final Locator.XPathLocator emptyGrid = fBrowser.append(Locator.tagWithClass("div", "x4-grid-empty"));
        final Locator.XPathLocator gridRow = fBrowser.append(Locators.gridRow());

        _test._ext4Helper.waitForMaskToDisappear();
        return Locator.waitForAnyElement(_test.shortWait(), gridRow, emptyGrid);
    }

    @LogMethod
    public void checkFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        scrollToGridRow(fileName);

        if(!_test._ext4Helper.isChecked(Locators.gridRowCheckbox(fileName)))
        {
            _test.scrollIntoView(Locators.gridRowCheckbox(fileName));
            _test.doAndWaitForPageSignal(() -> _test._ext4Helper.checkCheckbox(Locators.gridRowCheckbox(fileName)), IMPORT_SIGNAL_NAME);
        }
    }

    public boolean fileIsPresent(String nodeIdEndsWith)
    {
        Locator targetFile = Locators.gridRowWithNodeId(nodeIdEndsWith);
        return _test.isElementPresent(targetFile);
    }

    //In case desired element is not present due to infinite scrolling
    private void scrollToGridRow(String nodeIdEndsWith)
    {
        Locator lastFileGridItem = Locators.gridRow().withPredicate("last()");
        Locator targetFile = Locators.gridRowWithNodeId(nodeIdEndsWith);

        waitForFileGridReady();
        _test.waitForElement(lastFileGridItem);

        String previousLastItemText = null;
        String currentLastItemText = null;
        while (!_test.isElementPresent(targetFile) && (currentLastItemText == null || !currentLastItemText.equals(previousLastItemText)))
        {
            try
            {
                _test.scrollIntoView(lastFileGridItem);
                previousLastItemText = currentLastItemText;
                currentLastItemText = lastFileGridItem.findElement(_test.getDriver()).getAttribute("data-recordid");
            }
            catch (StaleElementReferenceException ignore) {}
        }
    }

    public void selectFileBrowserRoot()
    {
        selectFileBrowserItem("/");
    }

    public void renameFile(String currentName, String newName)
    {
        selectFileBrowserItem(currentName);
        clickFileBrowserButton(BrowserAction.RENAME);
        Window renameWindow = Window().withTitle("Rename").waitFor(_test.getDriver());
        _test.setFormElement(Locator.name("renameText-inputEl").findElement(renameWindow), newName);
        renameWindow.clickButton("Rename", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElement(fileGridCell.withText(newName));
    }

    public void moveFile(String fileName, String destinationPath)
    {
        selectFileBrowserItem(fileName);
        clickFileBrowserButton(BrowserAction.MOVE);
        Window moveWindow = Window().withTitle("Choose Destination").waitFor(_test.getDriver());
        //NOTE:  this doesn't yet support nested folders
        WebElement folder = Locator.tagWithClass("span", "x4-tree-node-text").withText(destinationPath).waitForElement(moveWindow, 1000);
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(folder));
        sleep(500);
        _test.click(folder);
        moveWindow.clickButton("Move", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        _test.waitForElementToDisappear(fileGridCell.withText(fileName));
        selectFileBrowserItem(destinationPath + "/" + fileName);
        _test.waitForElement(fileGridCell.withText(fileName));
    }

    public void deleteFile(String fileName)
    {
        selectFileBrowserItem(fileName);
        clickFileBrowserButton(BrowserAction.DELETE);
        Window().withTitle("Delete Files").waitFor(_test.getDriver())
                .clickButton("Yes", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElementToDisappear(fileGridCell.withText(fileName));
    }

    public void createFolder(String folderName)
    {
        clickFileBrowserButton(BrowserAction.NEW_FOLDER);
        _test.setFormElement(Locator.name("folderName"), folderName);
        _test.clickButton("Submit", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElement(fileGridCell.withText(folderName));
    }

    public void addToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td[1]//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        _test.assertElementNotPresent(toolbarShownLocator);
        _test.click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
    }

    public void removeToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td[1]//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        _test.assertElementPresent(toolbarShownLocator);
        _test.click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
    }

    public void goToConfigureButtonsTab()
    {
        if (Window().withTitle("Manage File Browser Configuration").findOrNull(_test.getDriver()) == null)
            goToAdminMenu();

        _test._ext4Helper.clickExt4Tab("Toolbar and Grid Settings");
        _test.waitForText("Configure Toolbar Options");
    }

    public void goToAdminMenu()
    {
        clickFileBrowserButton(BrowserAction.ADMIN);
        Window().withTitle("Manage File Browser Configuration").waitFor(_test.getDriver());
    }

    public void selectImportDataAction(@LoggedParam String actionName)
    {
        clickFileBrowserButton(BrowserAction.IMPORT_DATA);
        Window importWindow = Window().withTitle("Import Data").waitFor(_test.getDriver());
        Locator.XPathLocator actionRadioButton = Locator.xpath("//input[@type='button' and not(@disabled)]/../label[contains(text(), " + Locator.xq(actionName) + ")]");
        long startTime = System.currentTimeMillis();
        while (!_test.isElementPresent(actionRadioButton) && (System.currentTimeMillis() - startTime) < WAIT_FOR_JAVASCRIPT)
        { // Retry until action is present
            importWindow.clickButton("Cancel", true);
            _test._ext4Helper.waitForMaskToDisappear();
            clickFileBrowserButton(BrowserAction.IMPORT_DATA);
            importWindow = Window().withTitle("Import Data").waitFor(_test.getDriver());
        }
        _test.click(actionRadioButton);
        _test.clickAndWait(Ext4Helper.Locators.ext4Button("Import"));
    }

    /** If the upload panel isn't visible, click the "Upload Files" button in the toolbar. */
    public void openUploadPanel()
    {
        Locator.XPathLocator uploadPanel = Locator.tagWithClass("div", "upload-files-panel").notHidden();
        _test.waitForElement(BrowserAction.UPLOAD.button());
        if (_test.isElementPresent(uploadPanel))
        {
            _test.log("Upload panel visible");
        }
        else
        {
            _test.log("Opening upload panel...");
            _test.click(BrowserAction.UPLOAD.button());
            WebElement uploadPanelEl = _test.waitForElement(uploadPanel, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);            _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(uploadPanelEl));
            _test.fireEvent(BrowserAction.UPLOAD.button(), BaseWebDriverTest.SeleniumEvent.mouseout); // Dismiss qtip
        }
    }

    public void uploadFile(File file)
    {
        uploadFile(file, null, null, false);
    }

    @LogMethod
    public void uploadFile(@LoggedParam final File file, @Nullable String description, @Nullable List<FileBrowserExtendedProperty> fileProperties, boolean replace)
    {
        waitForFileGridReady();

        openUploadPanel();

        _test.waitFor(() -> _test.getFormElement(Locator.xpath("//label[text() = 'Choose a File:']/../..//input[contains(@class, 'x4-form-field')]")).equals(""),
                "Upload field did not clear after upload.", WAIT_FOR_JAVASCRIPT);

        _test.setFormElement(Locator.css(".single-upload-panel input:last-of-type[type=file]"), file);
        _test.waitFor(() -> _test.getFormElement(Locator.xpath("//label[text() = 'Choose a File:']/../..//input[contains(@class, 'x4-form-field')]")).contains(file.getName()),
                "Upload field was not set to '" + file.getName() + "'.", WAIT_FOR_JAVASCRIPT);

        if (description != null)
            _test.setFormElement(Locator.name("description"), description);

        Runnable clickUpload = () -> {
            _test.clickButton("Upload", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

            if (replace)
            {
                Window confirmation = Window().withTitle("File Conflict:").waitFor(_test.getDriver());
                assertTrue("Unexpected confirmation message.", confirmation.getBody().contains("Would you like to replace it?"));
                confirmation.clickButton("Yes", true);
            }
        };
        Locator uploadedFile = fileGridCell.withText(file.getName());
        try
        {
            _test.doAndWaitForElementToRefresh(clickUpload, uploadedFile, _test.shortWait());
        }
        catch (NoSuchElementException retry)
        {
            _test.doAndWaitForElementToRefresh(clickUpload, uploadedFile, _test.shortWait());
        }

        if (description != null)
            _test.waitForElement(fileGridCell.withText(description));

        if (fileProperties != null && fileProperties.size() > 0)
        {
            Window propWindow = Window().withTitle("Extended File Properties").waitFor(_test.getDriver());
            _test.waitForText("File (1 of ");
            for (FileBrowserExtendedProperty prop : fileProperties)
            {
                if (prop.isCombobox())
                    _test._ext4Helper.selectComboBoxItem(prop.getName(), Ext4Helper.TextMatchTechnique.CONTAINS, prop.getValue());
                else
                    _test.setFormElement(Locator.name(prop.getName()), prop.getValue());
            }
            _test.doAndWaitForElementToRefresh(() -> propWindow.clickButton("Save", true), uploadedFile, _test.shortWait());
            _test._ext4Helper.waitForMaskToDisappear();

            for (FileBrowserExtendedProperty prop : fileProperties)
                _test.waitForElement(fileGridCell.withText(prop.getValue()));
        }

        // verify that the description field is empty
        assertEquals("Description didn't clear after upload", "", _test.getFormElement(Locator.name("description")));
    }

    public void importFile(String filePath, String importAction)
    {
        selectFileBrowserItem(filePath);
        selectImportDataAction(importAction);
    }

    @LogMethod (quiet = true)
    public void clickFileBrowserButton(@LoggedParam BrowserAction action)
    {
        waitForFileGridReady();
        WebElement button = action.findButton(_test);
        if (button.isDisplayed())
        {
            waitFor(() -> !button.getAttribute("class").contains("disabled"), WAIT_FOR_JAVASCRIPT);
            _test.clickAndWait(button, action._triggersPageLoad ? WAIT_FOR_PAGE : 0);
        }
        else
            clickFileBrowserButtonOverflow(action);
    }

    private void clickFileBrowserButtonOverflow(BrowserAction action)
    {
        Locator overflowMenuButton = Locator.css("div.fbrowser > div > a.x4-box-menu-after");
        Locator menuItem = Locator.css("a.x4-menu-item-link").withText(action._buttonText);

        _test.click(overflowMenuButton);
        _test.waitAndClick(WAIT_FOR_JAVASCRIPT, menuItem, action._triggersPageLoad ? WAIT_FOR_PAGE : 0);
    }

    public List<WebElement> findBrowserButtons()
    {
        waitForFileGridReady();
        return Locator.css(".fbrowser > .x4-toolbar a.x4-btn[data-qtip]").findElements(_test.getDriver());
    }

    public List<BrowserAction> getAvailableBrowserActions()
    {
        List<BrowserAction> actions = new ArrayList<>();
        List<WebElement> buttons = findBrowserButtons();

        for (WebElement button : buttons)
        {
            String cssClassString = button.getAttribute("class");
            String[] cssClasses = cssClassString.split("[ ]+");
            BrowserAction action = null;
            for (int i = 0; action == null && i < cssClasses.length; i++)
            {
                action = BrowserAction.getActionFromButtonClass(cssClasses[i]);
            }
            if (action == null)
                throw new IllegalStateException("No button found for unrecognized action: " + button.getAttribute("data-qtip"));
            actions.add(action);
        }

        return actions;
    }

    public enum BrowserAction
    {
        FOLDER_TREE("sitemap", "Toggle Folder Tree", "folderTreeToggle"),
        UP("arrow-up", "Parent Folder", "parentFolder"),
        RELOAD("refresh", "Refresh", "refresh"),
        NEW_FOLDER("folder", "Create Folder", "createDirectory"),
        DOWNLOAD("download", "Download", "download"),
        DELETE("trash-o", "Delete", "deletePath"),
        RENAME("pencil", "Rename", "renamePath"),
        MOVE("sign-out", "Move", "movePath"),
        EDIT_PROPERTIES("pencil", "Edit Properties", "editFileProps"),
        UPLOAD("file", "Upload Files", "upload"),
        IMPORT_DATA("database", "Import Data", "importData"),
        EMAIL_SETTINGS("envelope", "Email Preferences", "emailPreferences"),
        AUDIT_HISTORY("users", "Audit History", "auditLog", true),
        ADMIN("cog", "Admin", "customize");

        private String _iconName;
        private String _buttonText;
        private String _extId; // from Browser.js
        private boolean _triggersPageLoad;

        BrowserAction(String iconName, String buttonText, String extId, boolean triggersPageLoad)
        {
            _iconName = iconName;
            _buttonText = buttonText;
            _extId = extId;
            _triggersPageLoad = triggersPageLoad;
        }

        BrowserAction(String iconName, String buttonText, String extId)
        {
            this(iconName, buttonText, extId, false);
        }

        private static BrowserAction getActionFromButtonClass(String cssClass)
        {
            for (BrowserAction a : BrowserAction.values())
            {
                if (a.buttonCls().equals(cssClass))
                    return a;
            }
            return null;
        }

        public String toString()
        {
            return _buttonText;
        }

        public WebElement findButton(final BaseWebDriverTest test)
        {
            return Locator.css("." + buttonCls()).findElement(test.getDriver());
        }

        private String buttonCls()
        {
            return _extId + "Btn";
        }

        public Locator button()
        {
            return Locator.tagWithClass("a", buttonCls());
        }

        public Locator getButtonIconLocator()
        {
            return Locator.css(".fa-" + _iconName);
        }

        public Locator getButtonTextLocator()
        {
            return button().containing(_buttonText);
        }
    }

    public void waitForFileGridReady()
    {
        _test.waitForElement(Locators.pageSignal(IMPORT_SIGNAL_NAME));
        waitForGrid();
    }

    public void openFolderTree()
    {
        Locator collapsedTreePanel = Locator.css("div.fbrowser .treenav-panel.x4-collapsed");
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(Locator.css("div.fbrowser .treenav-panel")));
        if (_test.isElementPresent(collapsedTreePanel))
        {
            WebElement rootNode = Locator.css("div.fbrowser .treenav-panel tr[data-recordindex = '0']").findElement(_test.getDriver());
            clickFileBrowserButton(BrowserAction.FOLDER_TREE);
            _test.waitForElementToDisappear(collapsedTreePanel);
            _test.shortWait().until(ExpectedConditions.stalenessOf(rootNode));
            _test.waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]")); // temoporary row exists during expansion animation
        }
    }

    public static abstract class Locators extends org.labkey.test.Locators
    {
        static Locator.XPathLocator fBrowser = Locator.tagWithClass("div", "fbrowser");
        static Locator.XPathLocator folderTree = fBrowser.append(Locator.tagWithClass("div", "treenav-panel").withoutClass("x4-collapsed"));
        static Locator.XPathLocator collapsedFolderTree = fBrowser.append(Locator.tagWithClass("div", "treenav-panel").withClass("x4-collapsed"));

        public static Locator.XPathLocator gridRowCheckbox(String fileName, boolean checkForSelected)
        {
            return Locator.xpath("//tr[contains(@class, '" + (checkForSelected ? "x4-grid-row-selected" : "x4-grid-row") + "') and ./td//span[text()='" + fileName + "']]//div[@class='x4-grid-row-checker']");
        }

        public static Locator.XPathLocator gridRowCheckbox(String fileName)
        {
            return gridRowWithNodeId(fileName).append(Locator.tagWithClass("div", "x4-grid-row-checker"));
        }

        public static Locator.XPathLocator gridRow()
        {
            return Locator.tag("tr")
                    .withClass("x4-grid-data-row")
                    .withPredicate("starts-with(@id, 'gridview')");
        }

        public static Locator.XPathLocator gridRow(String fileName)
        {
            return gridRowWithNodeId("/" + fileName);
        }

        public static Locator.XPathLocator gridRowWithNodeId(String nodeIdEndsWith)
        {
            return gridRow().attributeEndsWith("data-recordid", nodeIdEndsWith);
        }

        public static Locator.XPathLocator treeRow(Integer recordIndex)
        {
            return Locators.folderTree.append(Locator.tagWithAttribute("tr", "data-recordindex", recordIndex.toString()));
        }

        public static Locator.XPathLocator selectedTreeRow(Integer recordIndex)
        {
            return Locators.folderTree.append(Locator.tagWithAttribute("tr", "data-recordindex", recordIndex.toString()).withClass("x4-grid-row-selected"));
        }
    }
}
