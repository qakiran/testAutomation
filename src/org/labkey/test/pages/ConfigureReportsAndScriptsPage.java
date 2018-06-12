/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test.pages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * org.labkey.query.reports.ReportsController.ConfigureReportsAndScriptsAction
 */
public class ConfigureReportsAndScriptsPage extends LabKeyPage
{
    private static final String DEFAULT_ENGINE = "Mozilla Rhino";

    public ConfigureReportsAndScriptsPage(WebDriverWrapper test)
    {
        super(test);
        waitForEnginesGrid();
    }

    public void waitForEnginesGrid()
    {
        waitForElement(Locators.enginesGridRowForName(DEFAULT_ENGINE));
    }

    public boolean isEnginePresentForLanguage(String language)
    {
        return isElementPresent(Locators.enginesGridRowForLanguage(language));
    }

    public boolean isEnginePresent(String engineName)
    {
        return isElementPresent(Locators.enginesGridRowForName(engineName));
    }

    @LogMethod
    public void addEngineWithDefaults(@LoggedParam EngineType type)
    {
        addEngine(type, new EngineConfig(null));
    }

    @LogMethod
    public void addEngine(@LoggedParam EngineType type, EngineConfig config)
    {
        Map<Locator, String> configMap = config.getConfigMap();

        String menuText = "New " + type + " Engine";
        _extHelper.clickExtMenuButton(false, Locator.id("btn_addEngine"), menuText);
        WebElement menuItem = Locator.menuItem(menuText).findElementOrNull(getDriver());
        if (menuItem != null)
        {
            mouseOver(menuItem);
            menuItem.click(); // Retry for unresponsive button
        }
        WebElement window = waitForElement(Locators.editEngineWindow);

        for (Map.Entry<Locator, String> entry : configMap.entrySet())
        {
            setFormElement(entry.getKey(), entry.getValue());
        }

        String language = getFormElement(Locator.id("editEngine_languageName"));

        clickButton("Submit", 0);
        shortWait().until(ExpectedConditions.stalenessOf(window));
        waitForElement(Locators.enginesGridRowForLanguage(language));
    }

    public void deleteEngine(String engineName)
    {
        WebElement engineRow = selectEngineNamed(engineName);

        deleteSelectedEngine(engineRow);
    }

    @LogMethod
    public void deleteEnginesForLanguage(@LoggedParam String engineLanguage)
    {
        WebElement engineRow = selectFirstEngineForLanguage(engineLanguage);

        while (engineRow != null)
        {
            deleteSelectedEngine(engineRow);
            engineRow = selectFirstEngineForLanguage(engineLanguage);
        }
    }

    @LogMethod(quiet = true)
    public void editEngine(@LoggedParam String engineName)
    {
        selectEngineNamed(engineName);

        clickButton("Edit", 0);

        waitForElement(Locators.editEngineWindow);
    }

    private void deleteSelectedEngine(WebElement selectedEngineRow)
    {
        clickButton("Delete", 0);

        _extHelper.waitForExtDialog("Delete Engine Configuration", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        String confirmationMessage = Locator.byClass("ext-mb-text").findElement(getDriver()).getText();

        TestLogger.log("Deleting: " + confirmationMessage.substring(confirmationMessage.indexOf(":") + 1));

        clickButton("Yes", 0);
        shortWait().until(ExpectedConditions.stalenessOf(selectedEngineRow));
    }

    private WebElement selectEngineNamed(String engineName)
    {
        WebElement engineRow = Locators.enginesGridRowForName(engineName).findElement(getDriver());
        engineRow.click();
        return engineRow;
    }

    @Nullable
    private WebElement selectFirstEngineForLanguage(String engineLanguage)
    {
        WebElement engineRow = Locators.enginesGridRowForLanguage(engineLanguage).findElementOrNull(getDriver());
        if (engineRow != null)
            engineRow.click();
        return engineRow;
    }

    public enum EngineType
    {
        PERL,
        R,
        EXTERNAL,
        R_DOCKER
                {
                    @Override
                    public String toString()
                    {
                        return "R Docker";
                    }
                };

        public String toString()
        {
            return StringUtils.capitalize(name().toLowerCase());
        }
    }

    public static class EngineConfig
    {
        private String _name;
        private String _language;
        private String _version;
        private String _extensions;
        private File _path;
        private String _command;
        private String _outputFileName;

        private Map<Locator, String> configMap;

        public EngineConfig(File path)
        {
            _path = path;
        }

        public Map<Locator, String> getConfigMap()
        {
            configMap = new HashMap<>();
            addToConfigMap(Locator.id("editEngine_name"), getName());
            addToConfigMap(Locator.id("editEngine_languageName"), getLanguageName());
            addToConfigMap(Locator.id("editEngine_languageVersion"), getLanguageVersion());
            addToConfigMap(Locator.id("editEngine_extensions"), getExtensions());
            addToConfigMap(Locator.id("editEngine_exePath"), getPath() != null ? getPath().getAbsolutePath() : null);
            addToConfigMap(Locator.id("editEngine_exeCommand"), getCommand());
            addToConfigMap(Locator.id("editEngine_outputFileName"), getOutputFileName());

            return configMap;
        }

        private void addToConfigMap(Locator field, String value)
        {
            if (value != null)
                configMap.put(field, value);
        }

        public String getName()
        {
            return _name;
        }

        public EngineConfig setName(String name)
        {
            _name = name;
            return this;
        }

        public String getLanguageName()
        {
            return _language;
        }

        public EngineConfig setLanguage(String language)
        {
            _language = language;
            return this;
        }

        public String getLanguageVersion()
        {
            return _version;
        }

        public EngineConfig setVersion(String version)
        {
            _version = version;
            return this;
        }

        public String getExtensions()
        {
            return _extensions;
        }

        public EngineConfig setExtensions(String extensions)
        {
            _extensions = extensions;
            return this;
        }

        public File getPath()
        {
            return _path;
        }

        public EngineConfig setPath(File path)
        {
            _path = path;
            return this;
        }

        public String getCommand()
        {
            return _command;
        }

        public EngineConfig setCommand(String command)
        {
            _command = command;
            return this;
        }

        public String getOutputFileName()
        {
            return _outputFileName;
        }

        public EngineConfig setOutputFileName(String outputFileName)
        {
            _outputFileName = outputFileName;
            return this;
        }
    }

    public static class Locators
    {
        private static final int nameColumnIndex = 0;
        private static final int languageColumnIndex = 1;

        public static Locator.XPathLocator enginesGrid = Locator.id("enginesGrid");

        public static Locator enginesGridRowForName(String engineName)
        {
            return enginesGrid.append(Locator.tagWithClass("div", "x-grid3-row").withPredicate(Locator.xpath(String.format("//td[%d]", nameColumnIndex + 1)).containing(engineName + "enabled")));
        }

        public static Locator enginesGridRowForLanguage(String engineLanguage)
        {
            return enginesGrid.append(Locator.tagWithClass("div", "x-grid3-row").withPredicate(Locator.xpath(String.format("//td[%d]", languageColumnIndex + 1)).withText(engineLanguage)));
        }

        public static Locator editEngineWindow = ExtHelper.Locators.window("Edit Engine Configuration");
    }
}
