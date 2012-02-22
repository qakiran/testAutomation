package org.labkey.test.tests;

import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 2/21/12
 * Time: 1:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class LuminexPositivityTest extends LuminexTest
{

    private String assayName = "Positivity";
    protected void runUITests()
    {
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE1));
//        sleep(10000);
        saveAssay();
        sleep(500);

        goToTestAssayHome();
        clickNavButton("Import Data");
        clickNavButton("Next");
        setFormElement("name", assayName);
        checkCheckbox("calculatePositivity");
        setFormElement("baseVisit", "1");
        setFormElement("positivityFoldChange", "3");
        setFormElement("__primaryFile__",  getLabKeyRoot() + "\\sampledata\\Luminex\\Positivity.xls");
        clickNavButton("Next");
        clickNavButton("Save and Finish");

        clickLinkWithText(assayName);

        assertTextPresent("positive", 6);
        assertTextPresent("negative", 4);

        DataRegionTable drt = new DataRegionTable( TEST_ASSAY_LUM+ " Data", this);
        List<String> posivitiy = drt.getColumnDataAsText("Positivity");
        List<String> wells = drt.getColumnDataAsText("Well");

        for(String well : new String[] {"A2", "B2", "A6", "B6"})
        {
            int i = wells.indexOf(well);
            assertEquals("positive", posivitiy.get(i));
        }


        for(String well : new String[] {"A3", "B3", "A5", "B5"})
        {
            int i = wells.indexOf(well);
            assertEquals("negative", posivitiy.get(i));
        }
    }
}
