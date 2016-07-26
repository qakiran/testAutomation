package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class ReopenPage extends UpdatePage
{
    public ReopenPage(WebDriver driver)
    {
        super(driver);
    }

    public static ReopenPage beginAt(WebDriverWrapper driver, int issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static ReopenPage beginAt(WebDriverWrapper driver, String containerPath, int issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "repoen", Maps.of("issueId", String.valueOf(issueId))));
        return new ReopenPage(driver.getDriver());
    }
}