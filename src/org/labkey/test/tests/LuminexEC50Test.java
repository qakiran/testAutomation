/*
 * Copyright (c) 2011-2014 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MiniTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class, MiniTest.class, Assays.class})
public class LuminexEC50Test extends LuminexRTransformTest
{
    private final String EC50_RUN_NAME = "EC50";
    private final String rum4 = "Four Parameter";
    private final String rum5 = "Five Parameter";
    private final String trapezoidal = "Trapezoidal";

    @Override
    protected void ensureConfigured()
    {
        setUseXarImport(true);
        super.ensureConfigured();
    }

    protected void runUITests()
    {
        runEC50Test();
    }

    @LogMethod
    private void runEC50Test()
    {
        ensureRTransformPresent();
        createNewAssayRun(EC50_RUN_NAME);
        checkCheckbox(Locator.name("curveFitLogTransform"));
        uploadMultipleCurveData();
        clickButton("Save and Finish", 2 * WAIT_FOR_PAGE);

        //add transform script
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "CurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        assertTextPresent("Four Parameter");

        waitForText("3.45399");

        checkEC50dataAndFailureFlag();
    }

    private void checkEC50dataAndFailureFlag()
    {
        // expect to already be viewing CurveFit query
        assertTextPresent("CurveFit");

        // quick check to see if we are using 32-bit or 64-bit R
        log("Checking R 32-bit vs 64-bit");
        pushLocation();
        _extHelper.clickMenuButton("Views", "Create", "R View");
        boolean is64bit = _rReportHelper.executeScript("print(.Machine$sizeof.pointer)", "[1] 8", true);
        _rReportHelper.clickSourceTab();
        _rReportHelper.saveReport("dummy");
        popLocation();
        waitForText("CurveFit");

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("TitrationId/Name");
        _customizeViewsHelper.applyCustomView();

        DataRegionTable table = new DataRegionTable("query", this, false);
        table.setFilter("TitrationId/Name", "Equals One Of (e.g. \"a;b;c\")", "Standard1;Standard2");

        List<String> analyte = table.getColumnDataAsText("Analyte");
        List<String> formula = table.getColumnDataAsText("Curve Type");
        List<String> ec50 = table.getColumnDataAsText("EC50");
        List<String> auc= table.getColumnDataAsText("AUC");
        List<String> inflectionPoint = table.getColumnDataAsText("Inflection");
        int rum5ec50count = 0;

        log("Write this");
        for(int i=0; i<formula.size(); i++)
        {
            if(formula.get(i).equals(rum4))
            {
                //ec50=populated=inflectionPoint
                assertEquals(ec50.get(i), inflectionPoint.get(i));
                //auc=unpopulated
                assertEquals(" ", auc.get(i));
            }
            else if(formula.get(i).equals(rum5))
            {
                // ec50 will be populated for well formed curves (i.e. not expected for every row, so we'll keep a count and check at the end of the loop)
                if (!ec50.get(i).equals(" ") && ec50.get(i).length() > 0)
                    rum5ec50count++;

                // auc should not be populated
                assertEquals(" ", auc.get(i));
            }
            else if(formula.get(i).equals(trapezoidal))
            {
                //ec50 should not be populated
                assertEquals(" ", ec50.get(i));
                //auc=populated (for all non-blank analytes)
                if (!analyte.get(i).startsWith("Blank"))
                    assertTrue( "AUC was unpopulated for row " + i, auc.get(i).length()>0);
            }
        }
        assertEquals("Unexpected number of Five Parameter EC50 values (expected 9 of 13).", 9, rum5ec50count);

        // check that the 5PL parameters are within the expected ranges (note: exact values can change based on R 32-bit vs R 64-bit)
        Double[] FiveParameterEC50mins = {110.24, 460.75, 36465.56, 21075.08, 7826.89, 32211.66, 44975.52, 0.4199, 0.03962};
        Double[] FiveParameterEC50maxs = {112.85, 480.26, 36469.5, 21075.29, 7826.90, 32211.67, 45012.09, 0.43771, 0.03967};
        table.setFilter("CurveType", "Equals", "Five Parameter");
        table.setFilter("EC50", "Is Not Blank", "");
        table.setSort("AnalyteId", SortDirection.ASC);
        table.setSort("TitrationId", SortDirection.ASC);
        ec50 = table.getColumnDataAsText("EC50");
        assertEquals("Unexpected number of Five Parameter EC50 values (expected " + FiveParameterEC50maxs.length + ")", FiveParameterEC50maxs.length, ec50.size());
        for (int i = 0; i < ec50.size(); i++)
        {
            Double val = Double.parseDouble(ec50.get(i));
            Double min = FiveParameterEC50mins[i];
            Double max = FiveParameterEC50maxs[i];
            Double expected = (max + min) / 2;
            Double delta = (max - min) / 2;
            assertEquals(String.format("Unexpected 5PL EC50 value for %s - %s", table.getDataAsText(i, "Titration"), table.getDataAsText(i, "Analyte")), expected, val, delta);
        }
        table.clearFilter("EC50");
        table.clearFilter("CurveType");

        // expect to already be viewing CurveFit query
        assertTextPresent("CurveFit");

        table = new DataRegionTable("query", this, false);
        table.setFilter("FailureFlag", "Equals", "true");

        // expect one 4PL curve fit failure (for Standard1 - ENV6 (97))
        table.setFilter("CurveType", "Equals", "Four Parameter");
        assertEquals("Expected one Four Parameter curve fit failure flag", 1, table.getDataRowCount());
        List<String> values = table.getColumnDataAsText("Analyte");
        assertTrue("Unexpected analyte for Four Parameter curve fit failure", values.size() == 1 && values.get(0).equals("ENV6 (97)"));
        table.clearFilter("CurveType");

        // expect four 5PL curve fit failures
        table.setFilter("CurveType", "Equals", "Five Parameter");
        assertEquals("Unexpected number of Five Parameter curve fit failure flags", 4, table.getDataRowCount());
        table.clearFilter("CurveType");

        table.clearFilter("FailureFlag");
    }
}
