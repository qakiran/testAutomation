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
package org.labkey.test.tests;

import org.labkey.test.Locator;

import java.io.File;

/**
 * User: elvan
 * Date: 4/5/12
 * Time: 3:16 PM
 */
public class StudyReloadTest extends StudyBaseTest
{
    @Override
    protected void doCreateSteps()
    {
        initializeFolder();
        importStudyFromZip(new File(getSampledataPath(), "studyreload/original.zip").getAbsolutePath());
        reloadStudyFromZip("C:\\Users\\elvan\\Downloads\\add_column.zip");
//        reloadStudyFromZip(new File(getSampledataPath(), "studyreload/edited.zip").getAbsolutePath());
    }

    @Override
    protected void doVerifySteps()
    {
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("1 dataset"));
        clickAndWait(Locator.linkWithText("update_test"));
        assertTextPresent("id006", "additional_column");
        //text that was present in original but removed in the update
        assertTextNotPresent("id005", "original_column_numeric");
    }
}
