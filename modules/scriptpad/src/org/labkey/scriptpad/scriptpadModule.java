/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

package org.labkey.scriptpad;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.view.WebPartFactory;

import java.util.Collection;
import java.util.Collections;

public class scriptpadModule extends DefaultModule
{
    @Override
    public String getName()
    {
        return "scriptpad";
    }

    @Override
    public double getVersion()
    {
        return 14.10;
    }

    @Override
    public boolean isAutoUninstall()
    {
        return true;
    }

    @Override
    public boolean hasScripts()
    {
        return false;
    }

    @NotNull
    @Override
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    @Override
    protected void init()
    {
        addController("scriptpad", scriptpadController.class);
    }

    @Override
    public void doStartup(ModuleContext moduleContext)
    {
    }
}