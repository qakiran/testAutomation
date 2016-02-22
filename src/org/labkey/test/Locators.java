/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
package org.labkey.test;

public abstract class Locators
{
    public static final Locator.XPathLocator ADMIN_MENU = Locator.xpath("id('adminMenuPopupLink')[@onclick]");
    public static final Locator.IdLocator USER_MENU = Locator.id("userMenuPopupLink");
    public static final Locator.IdLocator projectBar = Locator.id("projectBar");
    public static final Locator.IdLocator folderMenu = Locator.id("folderBar");
    public static final Locator.CssLocator labkeyError = Locator.css(".labkey-error");
    public static final Locator signInButtonOrLink = Locator.tag("a").withText("Sign\u00a0In"); // Will recognize link [BeginAction] or button [LoginAction]
    public static final Locator.CssLocator folderTab = Locator.css(".labkey-folder-header ul.tab-nav > li");
    public static final Locator.CssLocator labkeyHeader = Locator.css(".labkey-main .header-block");
    public static final Locator.CssLocator labkeyBody = Locator.css(".labkey-main .body-block");

    public static Locator pageSignal(String signalName)
    {
        return Locator.css("#testSignals > META[name=" + signalName + "]");
    }
    public static Locator pageSignal(String signalName, String value)
    {
        return Locator.css("#testSignals > META[name=" + signalName + "][value=" + value + "]");
    }
}
