package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static org.labkey.test.components.html.Checkbox.Checkbox;
import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.components.html.RadioButton.RadioButton;
import static org.labkey.test.components.html.SelectWrapper.Select;

public class CustomizeSitePage extends LabKeyPage<CustomizeSitePage.ElementCache>
{
    public CustomizeSitePage(WebDriver driver)
    {
        super(driver);
    }

    public static CustomizeSitePage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", "customizeSite"));
        return new CustomizeSitePage(driver.getDriver());
    }

    public ShowAdminPage save()
    {
        clickAndWait(elementCache().saveButton);

        return new ShowAdminPage(getDriver());
    }

    public CustomizeSitePage setPrimarySiteAdmin(String value)
    {
        elementCache().primaryAdmin.selectByVisibleText(value);
        return this;
    }

    public CustomizeSitePage setDefaultDomain(String value)
    {
        elementCache().defaultDomain.set(value);
        return this;
    }

    public CustomizeSitePage setBaseServerUrl(String value)
    {
        elementCache().baseServerUrl.set(value);
        return this;
    }

    public CustomizeSitePage setContainerRelativeUrl(boolean enable)
    {
        elementCache().containerRelativeUrl.set(enable);
        return this;
    }

    public CustomizeSitePage setUsageReportingLevel(ReportingLevel level)
    {
        elementCache().usageReportingLevel(level).check();
        return this;
    }

    public CustomizeSitePage setExceptionReportingLevel(ReportingLevel level)
    {
        elementCache().exceptionReportingLevel(level).check();
        return this;
    }

    public CustomizeSitePage setExceptionSelfReporting(boolean enable)
    {
        elementCache().selfReportExceptions.set(enable);
        return this;
    }

    public CustomizeSitePage setMemoryUsageDumpInterval(String value)
    {
        elementCache().memoryUsageDumpInterval.set(value);
        return this;
    }

    public CustomizeSitePage setMaxBLOBSize(String value)
    {
        elementCache().maxBLOBSize.set(value);
        return this;
    }

    public CustomizeSitePage setExt3Required(boolean enable)
    {
        elementCache().ext3Required.set(enable);
        return this;
    }

    public CustomizeSitePage setExt3APIRequired(boolean enable)
    {
        elementCache().ext3APIRequired.set(enable);
        return this;
    }

    public CustomizeSitePage setSslRequired(boolean enable)
    {
        elementCache().sslRequired.set(enable);
        return this;
    }

    public CustomizeSitePage setSslPort(String value)
    {
        elementCache().sslPort.set(value);
        return this;
    }

    public CustomizeSitePage setAllowSessionKeys(boolean enable)
    {
        elementCache().allowSessionKeys.set(enable);
        return this;
    }

    public CustomizeSitePage setPipelineToolsDirectory(String value)
    {
        elementCache().pipelineToolsDirectory.set(value);
        return this;
    }

    public CustomizeSitePage setNetworkDriveLetter(String value)
    {
        elementCache().networkDriveLetter.set(value);
        return this;
    }

    public CustomizeSitePage setNetworkDrivePath(String value)
    {
        elementCache().networkDrivePath.set(value);
        return this;
    }

    public CustomizeSitePage setNetworkDriveUser(String value)
    {
        elementCache().networkDriveUser.set(value);
        return this;
    }

    public CustomizeSitePage setShowRibbonMessage(boolean enable)
    {
        elementCache().showRibbonMessage.set(enable);
        return this;
    }

    public CustomizeSitePage setRibbonMessageHtml(String value)
    {
        elementCache().ribbonMessageHtml.set(value);
        return this;
    }

    public CustomizeSitePage setAdminOnlyMode(boolean enable)
    {
        elementCache().adminOnlyMode.set(enable);
        return this;
    }

    public CustomizeSitePage setAdminOnlyMessage(String value)
    {
        elementCache().adminOnlyMessage.set(value);
        return this;
    }

    public CustomizeSitePage setCSRFCheck(CSRFCheck value)
    {
        elementCache().CSRFCheck.selectByValue(value.name());
        return this;
    }

    public CustomizeSitePage setXFrameOptions(XFrameOptions value)
    {
        elementCache().XFrameOptions.selectByValue(value.name());
        return this;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);

        // Site Administrators
        protected final Select primaryAdmin = Select(Locator.id("administratorContactEmail")).findWhenNeeded(this);

        // Site URLs
        protected final Input defaultDomain = Input(Locator.id("defaultDomain"), getDriver()).findWhenNeeded(this);
        protected final Input baseServerUrl = Input(Locator.id("baseServerUrl"), getDriver()).findWhenNeeded(this);
        protected final Checkbox containerRelativeUrl = Checkbox(Locator.id("useContainerRelativeURL")).findWhenNeeded(this);

        // Usage Reporting
        protected RadioButton usageReportingLevel(ReportingLevel level)
        {
            return RadioButton(Locator.radioButtonByNameAndValue("usageReportingLevel", level.name())).findWhenNeeded(this);
        }
        protected final WebElement testUsageReport = Locator.id("testUsageReport").findWhenNeeded(this);

        // Exception Reporting
        protected RadioButton exceptionReportingLevel(ReportingLevel level)
        {
            return RadioButton(Locator.radioButtonByNameAndValue("exceptionReportingLevel", level.name())).findWhenNeeded(this);
        }
        protected final WebElement testExceptionReport = Locator.id("testExceptionReport").findWhenNeeded(this);
        protected final Checkbox selfReportExceptions = Checkbox(Locator.id("selfReportExceptions")).findWhenNeeded(this);

        // System Properties
        protected final Input memoryUsageDumpInterval = Input(Locator.id("memoryUsageDumpInterval"), getDriver()).findWhenNeeded(this);
        protected final Input maxBLOBSize = Input(Locator.id("maxBLOBSize"), getDriver()).findWhenNeeded(this);
        protected final Checkbox ext3Required = Checkbox(Locator.id("ext3Required")).findWhenNeeded(this);
        protected final Checkbox ext3APIRequired = Checkbox(Locator.id("ext3APIRequired")).findWhenNeeded(this);

        // Security
        protected final Checkbox sslRequired = Checkbox(Locator.id("sslRequired")).findWhenNeeded(this);
        protected final Input sslPort = Input(Locator.id("sslPort"), getDriver()).findWhenNeeded(this);
        protected final Checkbox allowSessionKeys = Checkbox(Locator.id("allowSessionKeys")).findWhenNeeded(this);

        // Pipeline Settings
        protected final Input pipelineToolsDirectory = Input(Locator.id("pipelineToolsDirectory"), getDriver()).findWhenNeeded(this);

        // Map Network Drive
        protected final Input networkDriveLetter = Input(Locator.id("networkDriveLetter"), getDriver()).findWhenNeeded(this);
        protected final Input networkDrivePath = Input(Locator.id("networkDrivePath"), getDriver()).findWhenNeeded(this);
        protected final Input networkDriveUser = Input(Locator.id("networkDriveUser"), getDriver()).findWhenNeeded(this);
        protected final Input networkDrivePassword = Input(Locator.id("networkDrivePassword"), getDriver()).findWhenNeeded(this);
        protected final WebElement testNetworkDrive = Locator.linkWithText("Test network drive settings").findWhenNeeded(this);

        // Ribbon Bar
        protected final Checkbox showRibbonMessage = Checkbox(Locator.id("showRibbonMessage")).findWhenNeeded(this);
        protected final Input ribbonMessageHtml = Input(Locator.id("ribbonMessageHtml"), getDriver()).findWhenNeeded(this);

        // Site Admin Mode
        protected final Checkbox adminOnlyMode = Checkbox(Locator.id("adminOnlyMode")).findWhenNeeded(this);
        protected final Input adminOnlyMessage = Input(Locator.id("adminOnlyMessage"), getDriver()).findWhenNeeded(this);

        // HTTP Security Settings
        protected final Select CSRFCheck = Select(Locator.id("CSRFCheck")).findWhenNeeded(this);
        protected final Select XFrameOptions = Select(Locator.id("XFrameOptions")).findWhenNeeded(this);
    }

    public enum ReportingLevel
    {
        OFF, LOW, MEDIUM, HIGH
    }

    public enum CSRFCheck
    {
        POST, ADMINONLY
    }

    public enum XFrameOptions
    {
        SAMEORIGIN, ALLOW
    }
}