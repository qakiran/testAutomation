/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.test.ms2;

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.WindowsOnlyTest;

import java.io.File;

import static org.junit.Assert.*;

@Category({MS2.class, BVT.class})
public class CometTest extends AbstractMS2SearchEngineTest implements WindowsOnlyTest
{
    protected static final String SEARCH_BUTTON = "Comet";
    protected static final String SEARCH_TYPE = "comet";

    protected void doTestSteps()
    {
        doTestStepsSetDepth(false);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanPipe(SEARCH_TYPE);
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected void setupEngine()
    {
        log("Analyze " + SEARCH_BUTTON + " sample data.");
        selectImportDataAction(SEARCH_BUTTON +  " Peptide Search");
    }

    protected void doTestStepsSetDepth(boolean isQuickTest)
    {
        setIsQuickTest(isQuickTest);

        log("Verifying that pipeline files were cleaned up properly");
        File test2 = new File(PIPELINE_PATH + "/bov_sample/" + SEARCH_TYPE + "/test2");
        if (test2.exists())
            fail("Pipeline files were not cleaned up; test2("+test2.toString()+") directory still exists");

        super.doTestSteps();
    }

    protected void basicChecks()
    {
        goToModule("Query");
        selectQuery("ms2", "Fractions");
        waitForElement(Locator.linkWithText("view data"), WAIT_FOR_JAVASCRIPT);
        clickAndWait(Locator.linkWithText("view data"));
        assertTextPresent("CAexample_mini.mzXML");
        // There should be 200 scans total
        assertTextPresent("200");
        // There should be 100 MS1 scans and 100 MS2 scans
        assertTextPresent("100");

        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithImage(getContextPath() + "/MS2/images/runIcon.gif"));

        // Make sure we're not using a custom default view for the current user
        selectOptionByText("viewParams", "<Standard View>");
        clickButton("Go");

        // Check for a few high-scoring peptides
        assertTextPresent("K.MSKIRQVIAAR.L", "K.LRRPDPYK.G", "K.KSGRSSDLTSVR.L");
    }

    @Override
    protected boolean isPipelineToolsTest()
    {
        return true;
    }
}
