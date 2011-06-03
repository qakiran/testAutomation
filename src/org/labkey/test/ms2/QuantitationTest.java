/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;

import java.io.File;
import java.io.IOException;

/**
 * User: jeckels
 * Date: May 27, 2011
 */
public class QuantitationTest extends AbstractXTandemTest
{
    protected static final String LIBRA_PROTOCOL_NAME = "BasicLibra";

    protected static final String LIBRA_INPUT_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> \n" +
        "<bioml>\n" +
            "  <note label=\"pipeline, protocol name\" type=\"input\">" + LIBRA_PROTOCOL_NAME + "</note> \n" +
            "  <note label=\"pipeline, protocol description\" type=\"input\">Search with Libra quantitation</note> \n" +
            "  <note label=\"pipeline prophet, min peptide probability\" type=\"input\">0</note> \n" +
            "  <note label=\"pipeline prophet, min protein probability\" type=\"input\">0</note> \n" +
            "  <note label=\"pipeline quantitation, algorithm\" type=\"input\">libra</note> \n" +
            "  <note label=\"pipeline quantitation, libra normalization channel\" type=\"input\">2</note> \n" +
            "  <note label=\"pipeline quantitation, libra config name\" type=\"input\">LibraConfig1</note> \n" +
        "</bioml>";

    @Override
    protected void doTestSteps()
    {
        createProjectAndFolder();

        log("Start analysis running.");
        clickLinkWithText("MS2 Dashboard");
        clickNavButton("Process and Import Data");

        waitAndClick(Locator.fileTreeByName("bov_sample"));
        ExtHelper.waitForImportDataEnabled(this);
        ExtHelper.clickFileBrowserFileCheckbox(this, SAMPLE_BASE_NAME + ".mzXML");

        setupEngine();

        waitForElement(Locator.xpath("//select[@name='sequenceDB']/option[.='" + DATABASE + "']" ), WAIT_FOR_JAVASCRIPT);
        log("Set analysis parameters.");
        setFormElement("protocolName", LIBRA_PROTOCOL_NAME);
        setFormElement("protocolDescription", "Search with Libra quantitation");
        selectOptionByText("sequenceDB", DATABASE);
        setFormElement("configureXml", "");
        waitAndClick(Locator.xpath("//a[@class='labkey-button']/span[text() = 'OK']"));
        setFormElement("configureXml", LIBRA_INPUT_XML);
        submit();
        log("View the analysis log.");
        waitForElement(Locator.linkWithText("Data Pipeline"), WAIT_FOR_JAVASCRIPT);

        clickLinkWithText("Data Pipeline");

        String runDescription = SAMPLE_BASE_NAME + " (" + LIBRA_PROTOCOL_NAME + ")";
        waitForPipelineJobsToComplete(1, runDescription, false);

        clickLinkWithText(FOLDER_NAME);
        clickLinkContainingText(runDescription);
        selectOptionByText("viewParams", "<Standard View>");
        clickNavButton("Go");
        assertTextPresent(PEPTIDE2);
        assertTextPresent(PEPTIDE3);
        assertTextPresent(PEPTIDE4);
        assertTextPresent(PEPTIDE5);

        clickLinkWithText(FOLDER_NAME);

        // Jump to the flow chart view
        clickLinkWithText("Data Pipeline");
        clickLinkWithText("COMPLETE");
        clickNavButton("Data");
        
        pushLocation();
        clickImageMapLinkByTitle("graphmap", "Data: " + SAMPLE_BASE_NAME + ".libra.tsv (Run Output)");
        assertLinkPresentWithText("libra Protein Quantitation");

        clickLinkWithText("Lineage for " + SAMPLE_BASE_NAME + ".libra.tsv");
        clickImageMapLinkByTitle("graphmap", "libra Peptide Quantitation");
        // Check to see that arguments to xinteract are showing
        assertTextPresent("-LLibraConfig1.xml-2");

        boolean b = false;
    }

    @Override
    protected void basicChecks()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void cleanPipe(String search_type) throws IOException
    {
        File rootDir = new File(_pipelinePath);
        delete(new File(rootDir, ".labkey/protocols/"+search_type+"/" + LIBRA_PROTOCOL_NAME + ".xml"));
        delete(new File(rootDir, "bov_sample/"+search_type+"/" + LIBRA_PROTOCOL_NAME));
    }
}
