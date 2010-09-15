package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: kevink
 * Date: Sep 13, 2010
 * Time: 1:41:34 PM
 */
public class ReagentTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "ReagentProject";
    protected static final String FOLDER_NAME = "ReagentFolder";

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/reagent";
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try { deleteProject(PROJECT_NAME); } catch (Throwable t) { }
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        createProject();
        _testInsert();
        _testUpdate();
        _testBulkUpdate();
    }

    public void createProject()
    {
        log("** Create Project");
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[] { "Query" });

        addWebPart("Query");
        setFormElement("schemaName", "reagent");
        submit();

        beginAt("reagent/" + PROJECT_NAME + "/" + FOLDER_NAME + "/initialize.view");
    }

    public void _testInsert()
    {
        log("** Inserting new Reagent");
        beginAt("query/" + PROJECT_NAME + "/" + FOLDER_NAME + "/executeQuery.view?schemaName=reagent&query.queryName=Reagents");
        clickButton("Insert New");

        waitForElement(Locator.extButton("Cancel"), WAIT_FOR_JAVASCRIPT);

        log("** Selecting AntigenId from ComboBox list");
        // click on ComboBox trigger image
        click(Locator.xpath("//input[@name='AntigenId']/../img"));
        waitForElement(Locator.xpath("//div[contains(@class, 'x-combo-list-item')]//b[text()='AVDLSHFLK']"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//div[contains(@class, 'x-combo-list-item')]//b[text()='AVDLSHFLK']"));
        assertFormElementEquals(Locator.xpath("//input[@name='AntigenId']/../input[contains(@class, 'x-form-field')]"), "AVDLSHFLK");

        log("** Filtering LabelId ComboBox by 'Alexa'");
        click(Locator.xpath("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]"));
        selenium.typeKeys("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]", "Alexa");
        Number alexaLabels = selenium.getXpathCount("//div[contains(@class, 'x-combo-list')]//b[text()='Alexa 405']/../../..//b");
        assertEquals("Expected to find 5 Alexa labels", 5, alexaLabels.intValue());

        pressDownArrow("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]");
        pressDownArrow("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]");
        pressEnter("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]");
        assertFormElementEquals(Locator.xpath("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]"), "Alexa 680");
    }

    public void _testUpdate()
    {

    }

    public void _testBulkUpdate()
    {
        
    }
}
