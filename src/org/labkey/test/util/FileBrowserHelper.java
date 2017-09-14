/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;
import static org.labkey.test.components.ext4.RadioButton.RadioButton;
import static org.labkey.test.components.ext4.Window.Window;

public class FileBrowserHelper extends WebDriverWrapper
{
    public static final String IMPORT_SIGNAL_NAME = "import-actions-updated";
    public static final Locator fileGridCell = Locator.tagWithClass("div", "labkey-filecontent-grid").append(Locator.tagWithClass("div", "x4-grid-cell-inner"));

    WrapsDriver _driver;

    public FileBrowserHelper(WrapsDriver driver)
    {
        _driver = driver;
    }

    public FileBrowserHelper(WebDriver driver)
    {
        this(() -> driver);
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _driver.getWrappedDriver();
    }

    @LogMethod(quiet = true)
    public void expandFileBrowserRootNode()
    {
        expandFileBrowserTree();
        waitAndClick(Locators.treeRow(0));
        waitForElement(Locators.selectedTreeRow(0), WAIT_FOR_JAVASCRIPT);
    }

    public void expandFileBrowserTree()
    {
        if (isElementPresent(Locators.collapsedFolderTree))
        {
            clickFileBrowserButton(BrowserAction.FOLDER_TREE);
            waitForElementToDisappear(Locators.folderTree);
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
            baseNodeId = Locators.treeRow(0).findElement(getDriver()).getAttribute("data-recordid");
        }
        catch (StaleElementReferenceException retry)
        {
            baseNodeId = Locators.treeRow(0).findElement(getDriver()).getAttribute("data-recordid");
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

        waitForElement(folderTreeNode);
        waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]")); // temoporary row exists during expansion animation

        final Locator folderTreeNodeSelected = folderTreeNode.withClass("x4-grid-row-selected");
        if (!isElementPresent(folderTreeNodeSelected))
        {
            try
            {
                doAndWaitForPageSignal(() -> click(folderTreeNode), IMPORT_SIGNAL_NAME);
            }
            catch (StaleElementReferenceException staleSignal)
            {
                waitForElement(org.labkey.test.Locators.pageSignal(IMPORT_SIGNAL_NAME));
            }
            waitForGrid();
        }
    }

    private WebElement waitForGrid()
    {
        final Locator.XPathLocator fBrowser = Locator.tagWithClass("div", "fbrowser");
        final Locator.XPathLocator emptyGrid = fBrowser.append(Locator.tagWithClass("div", "x4-grid-empty"));
        final Locator.XPathLocator gridRow = fBrowser.append(Locators.gridRow());

        _ext4Helper.waitForMaskToDisappear();
        return Locator.waitForAnyElement(shortWait(), gridRow, emptyGrid);
    }

    @LogMethod
    public void checkFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        scrollToGridRow(fileName);

        final Checkbox checkbox = Ext4Checkbox().locatedBy(Locators.gridRowCheckbox(fileName)).find(getDriver());
        if(!checkbox.isChecked())
        {
            scrollIntoView(checkbox.getComponentElement());
            doAndWaitForPageSignal(checkbox::check, IMPORT_SIGNAL_NAME);
        }
    }

    public boolean fileIsPresent(String nodeIdEndsWith)
    {
        Locator targetFile = Locators.gridRowWithNodeId(nodeIdEndsWith);
        return isElementPresent(targetFile);
    }

    //In case desired element is not present due to infinite scrolling
    private void scrollToGridRow(String nodeIdEndsWith)
    {
        Locator lastFileGridItem = Locators.gridRow().withPredicate("last()");
        Locator targetFile = Locators.gridRowWithNodeId(nodeIdEndsWith);

        waitForFileGridReady();
        waitForElement(lastFileGridItem);

        String previousLastItemText = null;
        String currentLastItemText = null;
        while (!isElementPresent(targetFile) && (currentLastItemText == null || !currentLastItemText.equals(previousLastItemText)))
        {
            try
            {
                scrollIntoView(lastFileGridItem);
                previousLastItemText = currentLastItemText;
                currentLastItemText = lastFileGridItem.findElement(getDriver()).getAttribute("data-recordid");
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
        Window renameWindow = Window(getDriver()).withTitle("Rename").waitFor();
        setFormElement(Locator.name("renameText-inputEl").findElement(renameWindow), newName);
        renameWindow.clickButton("Rename", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        waitForElement(fileGridCell.withText(newName));
    }

    public void moveFile(String fileName, String destinationPath)
    {
        selectFileBrowserItem(fileName);
        clickFileBrowserButton(BrowserAction.MOVE);
        Window moveWindow = Window(getDriver()).withTitle("Choose Destination").waitFor();
        //NOTE:  this doesn't yet support nested folders
        WebElement folder = Locator.tagWithClass("span", "x4-tree-node-text").withText(destinationPath).waitForElement(moveWindow, 1000);
        shortWait().until(LabKeyExpectedConditions.animationIsDone(folder));
        sleep(500);
        folder.click();
        moveWindow.clickButton("Move", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        waitForElementToDisappear(fileGridCell.withText(fileName));
        selectFileBrowserItem(destinationPath + "/" + fileName);
        waitForElement(fileGridCell.withText(fileName));
    }

    public void deleteFile(String fileName)
    {
        selectFileBrowserItem(fileName);
        clickFileBrowserButton(BrowserAction.DELETE);
        Window(getDriver()).withTitle("Delete Files").waitFor()
                .clickButton("Yes", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        waitForElementToDisappear(fileGridCell.withText(fileName));
    }

    public void createFolder(String folderName)
    {
        clickFileBrowserButton(BrowserAction.NEW_FOLDER);
        setFormElement(Locator.name("folderName"), folderName);
        clickButton("Submit", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        waitForElement(fileGridCell.withText(folderName));
    }

    public void addToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td[1]//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        assertElementNotPresent(toolbarShownLocator);
        click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
    }

    public void removeToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td[1]//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        assertElementPresent(toolbarShownLocator);
        click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
    }

    public void goToConfigureButtonsTab()
    {
        if (Window(getDriver()).withTitle("Manage File Browser Configuration").findOrNull() == null)
            goToAdminMenu();

        _ext4Helper.clickExt4Tab("Toolbar and Grid Settings");
        waitForText("Configure Toolbar Options");
    }

    public void goToAdminMenu()
    {
        clickFileBrowserButton(BrowserAction.ADMIN);
        Window(getDriver()).withTitle("Manage File Browser Configuration").waitFor();
    }

    public void selectImportDataAction(@LoggedParam String actionName)
    {
        doAndWaitForPageSignal(() -> clickFileBrowserButton(BrowserAction.IMPORT_DATA), IMPORT_SIGNAL_NAME);
        Window importWindow = Window(getDriver()).withTitle("Import Data").waitFor();
        RadioButton().withLabelContaining(actionName).find(importWindow).check();
        importWindow.clickButton("Import");
    }

    /** If the upload panel isn't visible, click the "Upload Files" button in the toolbar. */
    public void openUploadPanel()
    {
        Locator.XPathLocator uploadPanel = Locator.tagWithClass("div", "upload-files-panel").notHidden();
        waitForElement(BrowserAction.UPLOAD.button());
        if (isElementPresent(uploadPanel))
        {
            log("Upload panel visible");
        }
        else
        {
            log("Opening upload panel...");
            click(BrowserAction.UPLOAD.button());
            WebElement uploadPanelEl = waitForElement(uploadPanel, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            shortWait().until(LabKeyExpectedConditions.animationIsDone(uploadPanelEl));
            fireEvent(BrowserAction.UPLOAD.button(), BaseWebDriverTest.SeleniumEvent.mouseout); // Dismiss qtip
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

        waitFor(() -> getFormElement(Locator.xpath("//label[text() = 'Choose a File:']/../..//input[contains(@class, 'x4-form-field')]")).equals(""),
                "Upload field did not clear after upload.", WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.css(".single-upload-panel input:last-of-type[type=file]"), file);
        waitFor(() -> getFormElement(Locator.xpath("//label[text() = 'Choose a File:']/../..//input[contains(@class, 'x4-form-field')]")).contains(file.getName()),
                "Upload field was not set to '" + file.getName() + "'.", WAIT_FOR_JAVASCRIPT);

        if (description != null)
            setFormElement(Locator.name("description"), description);

        Runnable clickUpload = () -> {
            clickButton("Upload", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

            if (replace)
            {
                Window confirmation = Window(getDriver()).withTitle("File Conflict:").waitFor();
                assertTrue("Unexpected confirmation message.", confirmation.getBody().contains("Would you like to replace it?"));
                confirmation.clickButton("Yes", true);
            }
        };
        Locator uploadedFile = fileGridCell.withText(file.getName());
        try
        {
            doAndWaitForElementToRefresh(clickUpload, uploadedFile, shortWait());
        }
        catch (NoSuchElementException retry)
        {
            doAndWaitForElementToRefresh(clickUpload, uploadedFile, shortWait());
        }

        if (description != null)
            waitForElement(fileGridCell.withText(description));

        if (fileProperties != null && fileProperties.size() > 0)
        {
            Window propWindow = Window(getDriver()).withTitle("Extended File Properties").waitFor();
            waitForText("File (1 of ");
            for (FileBrowserExtendedProperty prop : fileProperties)
            {
                if (prop.isCombobox())
                    _ext4Helper.selectComboBoxItem(prop.getName(), Ext4Helper.TextMatchTechnique.CONTAINS, prop.getValue());
                else
                    setFormElement(Locator.name(prop.getName()), prop.getValue());
            }
            doAndWaitForElementToRefresh(() -> propWindow.clickButton("Save", true), uploadedFile, shortWait());
            _ext4Helper.waitForMaskToDisappear();

            for (FileBrowserExtendedProperty prop : fileProperties)
                waitForElement(fileGridCell.withText(prop.getValue()));
        }

        // verify that the description field is empty
        assertEquals("Description didn't clear after upload", "", getFormElement(Locator.name("description")));
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
        WebElement button = action.findButton(getDriver());
        if (button.isDisplayed())
        {
            waitFor(() -> !button.getAttribute("class").contains("disabled"), WAIT_FOR_JAVASCRIPT);
            clickAndWait(button, action._triggersPageLoad ? WAIT_FOR_PAGE : 0);
        }
        else
            clickFileBrowserButtonOverflow(action);
    }

    private void clickFileBrowserButtonOverflow(BrowserAction action)
    {
        Locator overflowMenuButton = Locator.css("div.fbrowser > div > a.x4-box-menu-after");
        Locator menuItem = Locator.css("a.x4-menu-item-link").withText(action._buttonText);

        click(overflowMenuButton);
        waitAndClick(WAIT_FOR_JAVASCRIPT, menuItem, action._triggersPageLoad ? WAIT_FOR_PAGE : 0);
    }

    public List<WebElement> findBrowserButtons()
    {
        waitForFileGridReady();
        return Locator.css(".fbrowser > .x4-toolbar a.x4-btn[data-qtip]").findElements(getDriver());
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

        public WebElement findButton(final SearchContext context)
        {
            return Locator.css("." + buttonCls()).findElement(context);
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
        waitForElement(org.labkey.test.Locators.pageSignal(IMPORT_SIGNAL_NAME));
        waitForGrid();
    }

    public void openFolderTree()
    {
        Locator collapsedTreePanel = Locator.css("div.fbrowser .treenav-panel.x4-collapsed");
        shortWait().until(LabKeyExpectedConditions.animationIsDone(Locator.css("div.fbrowser .treenav-panel")));
        if (isElementPresent(collapsedTreePanel))
        {
            WebElement rootNode = Locator.css("div.fbrowser .treenav-panel tr[data-recordindex = '0']").findElement(getDriver());
            clickFileBrowserButton(BrowserAction.FOLDER_TREE);
            waitForElementToDisappear(collapsedTreePanel);
            shortWait().until(ExpectedConditions.stalenessOf(rootNode));
            waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]")); // temoporary row exists during expansion animation
        }
    }

    public static abstract class Locators
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
