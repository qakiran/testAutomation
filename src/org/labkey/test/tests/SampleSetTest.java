/*
 * Copyright (c) 2007-2018 LabKey Corporation
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

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleSetHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({DailyA.class})
@BaseWebDriverTest.ClassTimeout(minutes = 20)
public class SampleSetTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleSetTestProject";
    private static final String FOLDER_NAME = "SampleSetTestFolder";
    private static final String LINEAGE_FOLDER = "LineageSampleSetFolder";
    private static final String LINEAGE_SAMPLE_SET_NAME = "LineageSampleSet";
    private static final String PROJECT_SAMPLE_SET_NAME = "ProjectSampleSet";
    private static final String PROJECT_PARENT_SAMPLE_SET_NAME = "ProjectParentSampleSet";
    private static final String FOLDER_SAMPLE_SET_NAME = "FolderSampleSet";
    private static final String PARENT_SAMPLE_SET_NAME = "ParentSampleSet";
    private static final String FOLDER_CHILDREN_SAMPLE_SET_NAME = "FolderChildrenSampleSet";
    private static final String FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME = "FolderGrandchildrenSampleSet";
    private static final String CASE_INSENSITIVE_SAMPLE_SET = "CaseInsensitiveSampleSet";
    private static final String LOWER_CASE_SAMPLE_SET = "caseinsensitivesampleset";

    protected static final String PIPELINE_PATH = "/sampledata/xarfiles/expVerify";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject()
    {
        SampleSetTest init = (SampleSetTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[]{"Experiment"});
        _containerHelper.createSubfolder(PROJECT_NAME, LINEAGE_FOLDER, new String[]{"Experiment"});

        projectMenu().navigateToProject(PROJECT_NAME);
        portalHelper.addWebPart("Sample Sets");

        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        portalHelper.addWebPart("Sample Sets");

        projectMenu().navigateToFolder(PROJECT_NAME, LINEAGE_FOLDER);
        portalHelper.addWebPart("Sample Sets");
    }

    @Test
    public void doLineageDerivationTest()
    {
        String sampleText = "Name\tIntCol\tStringCol\n" +
                "Sample12ab\t1012\talpha\n" +
                "Sample13c4\t1023\tbeta\n" +
                "Sample14d5\t1024\tgamma\n" +
                "Sampleabcd\t1035\tepsilon\n" +
                "Sampledefg\t1046\tzeta";
        projectMenu().navigateToFolder(PROJECT_NAME, LINEAGE_FOLDER);
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(LINEAGE_SAMPLE_SET_NAME, null,
                Map.of("IntCol", FieldDefinition.ColumnType.Integer,
                        "StringCol", FieldDefinition.ColumnType.String),
                sampleText);

        // at this point, we're in the LINEAGE_FOLDER, on the Experiment tab, looking at the sample sets properties and Sample Set contents webparts.
        // now we add more samples,
        String deriveSamples = "Name\tMaterialInputs/LineageSampleSet\n" +
                "A\t\n" +
                "B\tA\n" +      // B and C both derive from A, so should get the same Run
                "C\tA\n" +      // D derives from B, so should get its own run
                "D\tB\n";
        sampleHelper.bulkImport(deriveSamples);

        log("foo");
        SelectRowsResponse samples = executeSelectRowCommand("samples", LINEAGE_SAMPLE_SET_NAME, ContainerFilter.Current, getProjectName()+"/"+LINEAGE_FOLDER, null);
        Map<String, Object> rowA =  samples.getRows().stream().filter((a)-> a.get("Name").equals("A")).collect(Collectors.toList()).get(0);
        Map<String, Object> rowB =  samples.getRows().stream().filter((a)-> a.get("Name").equals("B")).collect(Collectors.toList()).get(0);
        Map<String, Object> rowC =  samples.getRows().stream().filter((a)-> a.get("Name").equals("C")).collect(Collectors.toList()).get(0);
        Map<String, Object> rowD =  samples.getRows().stream().filter((a)-> a.get("Name").equals("D")).collect(Collectors.toList()).get(0);

        assertNull("Row A shouldn't have a parent", rowA.get("Run"));
        assertEquals("Rows B and C should both derive from A and get the same run", rowB.get("Run"), rowC.get("Run"));
        assertNotNull("RowD should have a parent", rowD.get("Run"));
        assertNotEquals("RowD should not equal B", rowD.get("Run"), rowB.get("Run"));
        assertNotEquals("RowD should not equal C", rowD.get("Run"), rowC.get("Run"));
    }

    @Test
    public void testCreateSampleSetNoExpression()
    {
        String sampleSetName = "SimpleCreateNoExp";
        Map<String, FieldDefinition.ColumnType> fields = Map.of("StringValue", FieldDefinition.ColumnType.String, "IntValue", FieldDefinition.ColumnType.Integer);
        log("Create a new sample set with a name and no name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);

        SampleSetHelper sampleSetHelper = new SampleSetHelper(this);
        sampleSetHelper.goToCreateNewSampleSet();
        log("Verify the name field is required");
        clickButton("Create");
        assertTextPresent("You must supply a name for the sample set.");
        clickButton("Cancel");

        sampleSetHelper.createSampleSet(sampleSetName)
                .addFields(fields)
                .goToSampleSet(sampleSetName)
                .verifyFields();

        log("Add a single row to the sample set");
        Map<String, String> fieldMap = Map.of("Name", "S-1", "StringValue", "Ess", "IntValue", "1");
        sampleSetHelper.insertRow(fieldMap);

        log("Verify values were saved");
        sampleSetHelper.verifyDataValues(Collections.singletonList(fieldMap));

        List<Map<String, String>> data = new ArrayList<>();
        data.add(Map.of("Name", "S-2", "StringValue", "Tee", "IntValue", "2"));
        data.add(Map.of("Name", "S-3", "StringValue", "Ewe", "IntValue", "3"));
        sampleSetHelper.bulkImport(data);

        assertEquals("Number of samples not as expected", 3, sampleSetHelper.getSampleCount());

        sampleSetHelper.verifyDataValues(data);

        log("Try to create a sample set with the same name.");
        click(Locator.linkWithText("Sample Sets"));
        sampleSetHelper = new SampleSetHelper(this, false);
        sampleSetHelper.createSampleSet(sampleSetName, null);
        assertTextPresent("A sample set with that name already exists.");

        clickButton("Cancel");
        DataRegionTable drt = DataRegion(this.getDriver()).find();
        assertEquals("Data region should be sample sets listing", "SampleSet", drt.getDataRegionName());
    }

    @Test
    public void testCreateSampleSetWithExpression()
    {
        String sampleSetName = "SimpleCreateWithExp";
        List<String> fieldNames = Arrays.asList("StringValue", "FloatValue");
        Map<String, FieldDefinition.ColumnType> fields = Map.of(fieldNames.get(0), FieldDefinition.ColumnType.String, fieldNames.get(1), FieldDefinition.ColumnType.Double);
        SampleSetHelper sampleSetHelper = new SampleSetHelper(this);
        log("Create a new sample set with a name and name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        sampleSetHelper.createSampleSet(sampleSetName, "${" + fieldNames.get(0) + "}-${batchRandomId}-${randomId}")
                .addFields(fields)
                .goToSampleSet(sampleSetName)
                .verifyFields();

        log("Add data without supplying the name");
        Map<String, String> fieldMap = Map.of(fieldNames.get(0), "Vee", fieldNames.get(1), "1.6");
        sampleSetHelper.insertRow(fieldMap);

        log("Verify values are as expected with name expression saved");
        DataRegionTable drt = sampleSetHelper.getSamplesDataRegionTable();
        int index = drt.getRowIndex(fieldNames.get(0), "Vee");
        assertTrue("Did not find row containing data", index >= 0);
        Map<String, String> rowData = drt.getRowDataAsMap(index);
        assertTrue("Name not as expected", rowData.get("Name").startsWith("Vee-"));
        assertEquals(fieldNames.get(0) + " not as expected", "Vee", rowData.get(fieldNames.get(0)));
        assertEquals(fieldNames.get(1) + "not as expected", "1.6", rowData.get(fieldNames.get(1)));

        log("Add data with name provided");
        sampleSetHelper.insertRow(Map.of("Name", "NoExpression"));

        log("Verify values are as expected with name value saved");
        drt = sampleSetHelper.getSamplesDataRegionTable();
        index = drt.getRowIndex("Name", "NoExpression");
        assertTrue("Did not find row with inserted name", index >= 0);

        log ("Add multiple rows via simple (default) import mechanism");
        List<Map<String, String>> data = new ArrayList<>();
        data.add(Map.of(fieldNames.get(0), "Dubya", fieldNames.get(1), "2.1"));
        data.add(Map.of(fieldNames.get(0), "Ex", fieldNames.get(1), "4.2"));
        sampleSetHelper.bulkImport(data);

        assertEquals("Number of samples not as expected", 4, sampleSetHelper.getSampleCount());

        assertTrue("Should have row with first imported value", drt.getRowIndex(fieldNames.get(0), "Dubya") >= 0);
        assertTrue("Should have row with second imported value", drt.getRowIndex(fieldNames.get(0), "Ex") >= 0);
    }

    private void selectInsertOption(String value, int index)
    {
        List<WebElement> buttons = Locator.radioButtonByNameAndValue("insertOption", value).findElements(this.getDriver());
        buttons.get(index).click();
    }

    @Test
    public void testImportTypeOptions()
    {
        String sampleSetName = "ImportErrors";
        List<String> fieldNames = Arrays.asList("StringValue");

        log("Create a new sample set with a name");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(sampleSetName, null, Map.of("StringValue", FieldDefinition.ColumnType.String));

        log("Go to the sample set and add some data");
        click(Locator.linkWithText(sampleSetName));
        DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents")
                .clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "Name1");
        setFormElement(Locator.name("quf_" + fieldNames.get(0)), "Bee");
        clickButton("Submit");

        log("Try to import overlapping data with TSV");

        DataRegionTable drt = sampleHelper.getSamplesDataRegionTable();
        drt.clickHeaderMenu("Insert data", SampleSetHelper.BULK_IMPORT_MENU_TEXT);
        String header = "Name\t" + fieldNames.get(0) + "\n";
        String overlap =  "Name1\tToBee\n";
        String newData = "Name2\tSee\n";
        setFormElement(Locator.name("text"), header + overlap + newData);
        clickButton("Submit", "duplicate key");

        log("Switch to 'Insert and Update'");
        sampleHelper.selectImportOption(SampleSetHelper.MERGE_DATA_OPTION, 1);
        clickButton("Submit");

        log("Validate data was updated and new data added");
        drt = sampleHelper.getSamplesDataRegionTable();
        assertEquals("Number of samples not as expected", 2, drt.getDataRowCount());

        int index = drt.getRowIndex("Name", "Name1");
        assertTrue("Should have row with first sample name", index >= 0);
        Map<String, String> rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name1' not as expected", "ToBee", rowData.get(fieldNames.get(0)));

        index = drt.getRowIndex("Name", "Name2");
        assertTrue("Should have a row with the second sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name2' not as expected", "See", rowData.get(fieldNames.get(0)));

        log("Try to import overlapping data from file");
        drt.clickHeaderMenu("Insert data", SampleSetHelper.BULK_IMPORT_MENU_TEXT);
        click(Locator.tagWithText("h3", "Upload file (.xlsx, .xls, .csv, .txt)"));
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("simpleSampleSet.xls").getAbsolutePath());
        clickButton("Submit", "duplicate key");

        log ("Switch to 'Insert and Update'");
        selectInsertOption("MERGE", 0);
        clickButton("Submit");
        log ("Validate data was updated and new data added");
        assertEquals("Number of samples not as expected", 3, drt.getDataRowCount());

        index = drt.getRowIndex("Name", "Name1");
        assertTrue("Should have row with first sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name1' not as expected", "NotTwoBee", rowData.get(fieldNames.get(0)));

        index = drt.getRowIndex("Name", "Name2");
        assertTrue("Should have a row with the second sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name2' not as expected", "Sea", rowData.get(fieldNames.get(0)));

        index = drt.getRowIndex("Name", "Name3");
        assertTrue("Should have a row with the thrid sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name' not as expected", "Dee", rowData.get(fieldNames.get(0)));

    }

    @Test
    public void testReservedFieldNames()
    {

        log("Validate that reservered values cannot be used as field names.");

        List<String> reserveredNames = Arrays.asList("Name", "Description", "Flag", "RowId", "SampleSet", "Folder", "Run", "Inputs", "Outputs");

        clickProject(PROJECT_NAME);

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet("InvalidFieldNames");

        PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getWrappedDriver()).withTitle("Field Properties").waitFor();

        fieldProperties.addField();

        StringBuilder errorMsg = new StringBuilder();

        reserveredNames.forEach(value -> {
            setFormElement(Locator.tagWithName("input", "ff_name0"), value);
            try
            {
                waitForElementToBeVisible(Locator.xpath("//input[@title=\"'" + value + "' is reserved\"]"));
            }
            catch (NoSuchElementException nse)
            {
                errorMsg.append(value);
                errorMsg.append(" is not marked as a reserved field name.");
                errorMsg.append("\n");
            }
        });

        if(errorMsg.length() > 0)
            Assert.fail(errorMsg.toString());

        clickButton("Cancel");

        log("Looks like all reserved filed names were caught.");
    }

    @Test
    public void testUpdateAndDeleteWithCommentsAndFlags()
    {
        final String SAMPLE_SET_NAME = "UpdateAndDeleteFields";
        final String SAMPLE_NAME_TO_DELETE = "ud01";
        final String SAMPLE_FLAG_UPDATE = "ud02";
        final String FLAG_UPDATE = "Updated Flag Value";
        final String SAMPLE_DESC_UPDATE = "ud03";
        final String DESC_UPDATE = "This is the updated description";
        final String SAMPLE_UPDATE_BOTH = "ud04";
        final String FLAG_UPDATE_1 = "New Flag Value";
        final String DESC_UPDATE_1 = "New description when one did not exist before.";
        final String FLAG_UPDATE_2 = "Flag Value Updated After Add";
        final String DESC_UPDATE_2 = "Updated description after adding a description.";

        StringBuilder errorLog = new StringBuilder();

        log("Validate that update and delete works correctly with the Comment and Flag fields.");

        clickProject(PROJECT_NAME);

        // Map.of creates an immutable collection I want to be able to update these data/collection items.
        Map<String, String> descriptionUpdate = new HashMap<>();
        descriptionUpdate.put("Name", SAMPLE_DESC_UPDATE);
        descriptionUpdate.put("Field01", "cc");
        descriptionUpdate.put("Description", "Here is the second description.");
        descriptionUpdate.put("Flag", "");

        Map<String, String> flagUpdate = new HashMap<>();
        flagUpdate.put("Name", SAMPLE_FLAG_UPDATE);
        flagUpdate.put("Field01", "bb");
        flagUpdate.put("Description", "");
        flagUpdate.put("Flag", "Flag Value 2");

        Map<String, String> updateBoth = new HashMap<>();
        updateBoth.put("Name", SAMPLE_UPDATE_BOTH);
        updateBoth.put("Field01", "dd");
        updateBoth.put("Description", "");
        updateBoth.put("Flag", "");

        List<Map<String, String>> sampleData = new ArrayList<>();

        sampleData.add(Map.of("Name", SAMPLE_NAME_TO_DELETE, "Field01", "aa", "Description", "This is description number 1.", "Flag", "Flag Value 1"));
        sampleData.add(flagUpdate);
        sampleData.add(descriptionUpdate);
        sampleData.add(updateBoth);

        // Some extra samples not really sure I will need them.
        sampleData.add(Map.of("Name", "ud05", "Field01", "ee", "Description", "This is description for sample 5.", "Flag", "Flag Value 5"));
        sampleData.add(Map.of("Name", "ud06", "Field01", "ff", "Description", "This is description for sample 6.", "Flag", "Flag Value 6"));

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(SAMPLE_SET_NAME, null,
                Map.of("Field01",  FieldDefinition.ColumnType.String),
                sampleData);

        List<Map<String, String>> resultsFromDB = getSampleDataFromDB("/SampleSetTestProject","UpdateAndDeleteFields", Arrays.asList("Name", "Flag/Comment", "Field01", "Description"));

        Assert.assertTrue("Newly inserted Sample Set data not as expected. Stopping the test here.", areDataListEqual(resultsFromDB, sampleData));

        // Change the view so screen shot on failure is helpful.
        sampleHelper = new SampleSetHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.addColumn("Description");
        cv.saveCustomView();

        log("Delete a record that has a description and an flag/comment");
        int rowIndex = drtSamples.getIndexWhereDataAppears(SAMPLE_NAME_TO_DELETE, "Name");
        drtSamples.checkCheckbox(rowIndex);
        drtSamples.clickHeaderButton("Delete");
        waitForElementToBeVisible(Locator.lkButton("Confirm Delete"));
        clickAndWait(Locator.lkButton("Confirm Delete"));

        // Remove the same row from the Sample Set input data.
        int testDataIndex = getSampleIndexFromTestInput(SAMPLE_NAME_TO_DELETE, sampleData);
        sampleData.remove(testDataIndex);

        log("Check that the Sample has been removed.");

        // Not going to use asserts (and possibly fail on first test), will try all the scenarios and then check at the end.
        String errorMsg = "Sample Set data is not as expected after a delete.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'delete sample' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now update a sample's description.");
        sampleHelper = new SampleSetHelper(this);

        testDataIndex = getSampleIndexFromTestInput(SAMPLE_DESC_UPDATE, sampleData);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE);

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after a update of Description.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'update description' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now delete the sample's description.");
        sampleHelper = new SampleSetHelper(this);

        sampleData.get(testDataIndex).replace("Description", "");

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after deleting the Description.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'delete description' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Let's repeat it all again for a sample's flag/comment.");
        sampleHelper = new SampleSetHelper(this);

        testDataIndex = getSampleIndexFromTestInput(SAMPLE_FLAG_UPDATE, sampleData);
        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE);

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after a update of Flag/Comment.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'update flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now delete the sample's Flag/Comment.");
        sampleHelper = new SampleSetHelper(this);

        sampleData.get(testDataIndex).replace("Flag", "");

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after deleting the Flag/Comment.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'delete flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Finally update and delete both flag and description for a sample.");
        sampleHelper = new SampleSetHelper(this);

        testDataIndex = getSampleIndexFromTestInput(SAMPLE_UPDATE_BOTH, sampleData);
        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE_1);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE_1);

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after a adding a Description and a Flag/Comment to an existing sample.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'adding a description and flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now update both values.");

        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE_2);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE_2);

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after a updating both a Description and a Flag/Comment.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'updating both a description and flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now delete both the Description and Flag/Comment from the sample.");
        sampleHelper = new SampleSetHelper(this);

        sampleData.get(testDataIndex).replace("Flag", "");
        sampleData.get(testDataIndex).replace("Description", "");

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after deleting the Description and Flag/Comment.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'deleting both a description and flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        if(errorLog.length() > 0)
            Assert.fail(errorLog.toString());

        log("All done.");
    }

    private void updateSampleSet(Map<String, String> updatedFields)
    {
        List<Map<String, String>> updateSampleData = new ArrayList<>();
        updateSampleData.add(updatedFields);

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.bulkImport(updateSampleData, SampleSetHelper.MERGE_DATA_OPTION);

    }

    private boolean checkExpectedAgainstDB(List<Map<String, String>> expectedData, String errorMsg)
    {
        List<Map<String, String>> resultsFromDB;

        resultsFromDB = getSampleDataFromDB("/SampleSetTestProject","UpdateAndDeleteFields", Arrays.asList("Name", "Flag/Comment", "Field01", "Description"));

        if(!areDataListEqual(resultsFromDB, expectedData))
        {
            log("\n*************** ERROR ***************");
            log(errorMsg);
            log("*************** ERROR ***************\n");

            return false;
        }

        return true;

    }

    protected boolean areDataListEqual(List<Map<String, String>> list01, List<Map<String, String>> list02)
    {
        return areDataListEqual(list01, list02, true);
    }
    protected boolean areDataListEqual(List<Map<String, String>> list01, List<Map<String, String>> list02, boolean logMismatch)
    {
        if( list01.size() != list02.size())
            return false;

        // Order the two lists so compare can be done by index and not by searching the two lists.
        Collections.sort(list01, (Map<String, String> o1, Map<String, String> o2)->
                {
                    return o1.get("Name").compareTo(o2.get("Name"));
                }
        );

        Collections.sort(list02, (Map<String, String> o1, Map<String, String> o2)->
                {
                    return o1.get("Name").compareTo(o2.get("Name"));
                }
        );

        for(int i = 0; i < list01.size(); i++)
        {
            if(!list01.get(i).equals(list02.get(i)))
            {
                if(logMismatch)
                {
                    log("Found a mismatch in the lists.");
                    log("list01(" + i + "): " + list01.get(i));
                    log("list02(" + i + "): " + list02.get(i));
                }
                return false;
            }
        }

        return true;
    }

    protected int getSampleIndexFromTestInput(String sampleName, List<Map<String, String>> testData)
    {
        int index;
        for(index = 0; index < testData.size(); index++)
        {
            if(testData.get(index).get("Name").toString().equalsIgnoreCase(sampleName))
                break;
        }

        if(index < testData.size())
            return index;

        Assert.fail("Ummm... I couldn't find a sample with the name '" + sampleName + "' in the test data, are you sure it should be there?");

        // Need this otherwise I get a red squiggly.
        return -1;

    }

    protected List<Map<String, String>> getSampleDataFromDB(String folderPath, String sampleSetName, List<String> fields)
    {
        List<Map<String, String>> results = new ArrayList<>(6);
        Map<String, String> tempRow;

        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        SelectRowsCommand cmd = new SelectRowsCommand("samples", sampleSetName);
        cmd.setColumns(fields);

        try
        {
            SelectRowsResponse response = cmd.execute(cn, folderPath);

            for (Map<String, Object> row : response.getRows())
            {

                tempRow = new HashMap<>();

                for(String key : row.keySet())
                {

                    if (fields.contains(key))
                    {

                        String tmpFlag = key;

                        if(key.equalsIgnoreCase("Flag/Comment"))
                            tmpFlag = "Flag";

                        if (null == row.get(key))
                        {
                            tempRow.put(tmpFlag, "");
                        }
                        else
                        {
                            tempRow.put(tmpFlag, row.get(key).toString());
                        }

                    }

                }

                results.add(tempRow);

            }

        }
        catch(CommandException | IOException excp)
        {
            Assert.fail(excp.getMessage());
        }

        return results;
    }

    @Test
    public void testCreateAndDeriveSamples()
    {
        Map<String, FieldDefinition.ColumnType> sampleSetFields = Map.of("IntCol", FieldDefinition.ColumnType.Integer,
                "StringCol", FieldDefinition.ColumnType.String,
                "DateCol", FieldDefinition.ColumnType.DateTime,
                "BoolCol", FieldDefinition.ColumnType.Boolean);
        File sampleSetFile = TestFileUtils.getSampleData("sampleSet.xlsx");

        clickProject(PROJECT_NAME);
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(PROJECT_SAMPLE_SET_NAME, null, sampleSetFields, sampleSetFile);

        clickFolder(FOLDER_NAME);
        sampleHelper.createSampleSet(FOLDER_SAMPLE_SET_NAME, null,
                Map.of("IntCol-Folder",  FieldDefinition.ColumnType.Integer,
                       "StringCol-Folder", FieldDefinition.ColumnType.String),
                "Name\tIntCol-Folder\tStringCol-Folder\n" +
                        "SampleSetBVT11\t101\taa\n" +
                        "SampleSetBVT4\t102\tbb\n" +
                        "SampleSetBVT12\t102\tbb\n" +
                        "SampleSetBVT13\t103\tcc\n" +
                        "SampleSetBVT14\t104\tdd");

        // Do some manual derivation
        clickAndWait(Locator.linkWithText("Sample Sets"));
        assertTextPresent(PROJECT_SAMPLE_SET_NAME, FOLDER_NAME);

        clickButton("Show All Materials");
        assertTextPresent(FOLDER_SAMPLE_SET_NAME);
        assertTextNotPresent(PROJECT_SAMPLE_SET_NAME);

        checkCheckbox(Locator.name(".toggle"));
        clickButton("Derive Samples");

        if (isElementPresent(Locator.linkWithText("configure a valid pipeline root for this folder")))
        {
            setPipelineRoot(TestFileUtils.getLabKeyRoot() + PIPELINE_PATH);
        }

        clickFolder(FOLDER_NAME);
        assertTextPresent(FOLDER_SAMPLE_SET_NAME, PROJECT_SAMPLE_SET_NAME);
        clickAndWait(Locator.linkWithText(FOLDER_SAMPLE_SET_NAME));
        checkCheckbox(Locator.name(".toggle"));
        clickButton("Derive Samples");

        selectOptionByText(Locator.name("inputRole0"), "Add a new role...");
        setFormElement(Locator.id("customRole0"), "FirstRole");
        selectOptionByText(Locator.name("inputRole1"), "Add a new role...");
        setFormElement(Locator.id("customRole1"), "SecondRole");
        selectOptionByText(Locator.name("inputRole2"), "Add a new role...");
        setFormElement(Locator.id("customRole2"), "ThirdRole");
        selectOptionByText(Locator.name("inputRole3"), "Add a new role...");
        setFormElement(Locator.id("customRole3"), "FourthRole");
        selectOptionByText(Locator.name("outputCount"), "2");
        selectOptionByText(Locator.name("targetSampleSetId"), "FolderSampleSet in /SampleSetTestProject/SampleSetTestFolder");
        clickButton("Next");

        setFormElement(Locator.name("outputSample1_Name"), "SampleSetBVT15");
        setFormElement(Locator.name("outputSample2_Name"), "SampleSetBVT16");
        checkCheckbox(Locator.name("outputSample1_IntColFolderCheckBox"));
        setFormElement(Locator.name("outputSample1_IntColFolder"), "500a");
        setFormElement(Locator.name("outputSample1_StringColFolder"), "firstOutput");
        setFormElement(Locator.name("outputSample2_StringColFolder"), "secondOutput");
        clickButton("Submit");

        assertTextPresent("must be of type Integer");
        checkCheckbox(Locator.name("outputSample1_IntColFolderCheckBox"));
        setFormElement(Locator.name("outputSample1_IntColFolder"), "500");
        clickButton("Submit");

        clickAndWait(Locator.linkContainingText("Derive 2 samples"));
        clickAndWait(Locator.linkContainingText("Text View"));
        assertTextPresent("FirstRole", "SecondRole", "ThirdRole", "FourthRole");

        clickAndWait(Locator.linkContainingText("16"));
        clickAndWait(Locator.linkContainingText("derive samples from this sample"));

        selectOptionByText(Locator.name("inputRole0"), "FirstRole");
        selectOptionByText(Locator.name("targetSampleSetId"), "ProjectSampleSet in /SampleSetTestProject");
        clickButton("Next");

        setFormElement(Locator.name("outputSample1_Name"), "200");
        setFormElement(Locator.name("outputSample1_IntCol"), "600");
        setFormElement(Locator.name("outputSample1_StringCol"), "String");
        setFormElement(Locator.name("outputSample1_DateCol"), "BadDate");
        uncheckCheckbox(Locator.name("outputSample1_BoolCol"));
        clickButton("Submit");

        assertTextPresent("must be of type Date and Time");
        setFormElement(Locator.name("outputSample1_DateCol"), "1/1/2007");
        clickButton("Submit");

        assertElementPresent(Locator.linkWithText("Derive sample from SampleSetBVT16"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT11"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT12"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT13"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT14"));

        clickAndWait(Locator.linkWithText("SampleSetBVT11"));

        assertElementPresent(Locator.linkWithText("Derive sample from SampleSetBVT16"));
        assertElementPresent(Locator.linkWithText("Derive 2 samples from SampleSetBVT11, SampleSetBVT12, SampleSetBVT13, SampleSetBVT14, SampleSetBVT4"));

        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText(FOLDER_SAMPLE_SET_NAME));

        assertTextPresent("aa", "bb", "cc", "dd", "firstOutput", "secondOutput");

        clickAndWait(Locator.linkWithText("Sample Sets"));
        clickButton("Show All Materials");
        assertTextPresent("ProjectSampleSet", "200");
    }

    @Test
    public void testAuditLog()
    {
        String sampleSetName = "TestAuditLogSampleSet";
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleSetHelper helper = new SampleSetHelper(this);
        helper.createSampleSet(sampleSetName, null, Map.of(
                "First", FieldDefinition.ColumnType.String,
                "Second", FieldDefinition.ColumnType.Integer),
                "Name\tFirst\tSecond\n" +
                        "Audit-1\tsome\t100");

        goToModule("Query");
        viewQueryData("auditLog", "SampleSetAuditEvent");
        assertTextPresent(
                "Samples inserted or updated in: " + sampleSetName);

    }


    @Test
    public void testParentChild()
    {
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        log("Create parent sample set");
        sampleHelper.createSampleSet(PARENT_SAMPLE_SET_NAME, null,
                Map.of("IntCol",  FieldDefinition.ColumnType.Integer),
                "Name\tIntCol\n" +
                        "SampleSetBVT11\t101\n" +
                        "SampleSetBVT4\t102\n" +
                        "SampleSetBVT12\t102\n" +
                        "SampleSetBVT13\t103\n" +
                        "SampleSetBVT14\t104");

        log("Create child sample set");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        sampleHelper.createSampleSet(FOLDER_CHILDREN_SAMPLE_SET_NAME, null,
                Map.of("OtherProp", FieldDefinition.ColumnType.Double),
                "Name\tMaterialInputs/" + PARENT_SAMPLE_SET_NAME + "\tOtherProp\n" +
                        "SampleSetBVTChildA\tSampleSetBVT11\t1.1\n" +
                        "SampleSetBVTChildB\tSampleSetBVT4\t2.2\n"
                );


        // Make sure that the parent got wired up
        log("Verify parent references");
        DataRegionTable table = sampleHelper.getSamplesDataRegionTable();
        table.openCustomizeGrid();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addColumn(new String[]{"Inputs", "Materials", PARENT_SAMPLE_SET_NAME});
        _customizeViewsHelper.clickViewGrid();
        waitAndClickAndWait(Locator.linkWithText("SampleSetBVT4"));

        // Check out the run
        clickAndWait(Locator.linkWithText("Derive sample from SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVTChildB"));

        // Make a grandchild set
        log("Create a grandparent sample set");
        clickTab("Experiment");
        clickAndWait(Locator.linkWithText("Sample Sets"));
        sampleHelper.setInWebPart(false);

        sampleHelper.createSampleSet(FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME, null,
                Map.of("OtherProp", FieldDefinition.ColumnType.Double),
                "Name\tMaterialInputs/" + FOLDER_CHILDREN_SAMPLE_SET_NAME + "\tOtherProp\n" +
                        "SampleSetBVTGrandchildA\tSampleSetBVTChildA,SampleSetBVTChildB\t11.11\n");

        waitAndClickAndWait(Locator.linkWithText("SampleSetBVTGrandchildA"));

        // These two regions are used throughout the remaining jumps comparing parent/child sets
        DataRegionTable childMaterialsRegion = new DataRegionTable("childMaterials", this.getDriver());
        DataRegionTable parentMaterialsRegion = new DataRegionTable("parentMaterials", this.getDriver());

        // Filter out any child materials, though there shouldn't be any
        childMaterialsRegion.setFilter("Name", "Is Blank");
        // Check for parents and grandparents
        assertTextPresent("SampleSetBVTChildB", "SampleSetBVT4", "SampleSetBVT11");

        // Verify that we've chained things together properly
        clickAndWait(Locator.linkWithText("SampleSetBVTChildA"));
        // Filter out any child materials so we can just check for parents
        childMaterialsRegion.setFilter("Name", "Is Blank");
        assertTextPresent("SampleSetBVT11");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Switch to filter out any parent materials so we can just check for children
        parentMaterialsRegion.setFilter("Name", "Is Blank");
        childMaterialsRegion.clearFilter("Name");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVT11"));
        assertTextPresent("SampleSetBVTGrandchildA");

        // Go up the chain one more hop
        parentMaterialsRegion.clearAllFilters("Name");
        clickAndWait(Locator.linkWithText("SampleSetBVT11"));
        // Filter out any child materials so we can just check for parents
        childMaterialsRegion.setFilter("Name", "Is Blank");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTChildA"));
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Switch to filter out any parent materials so we can just check for children
        parentMaterialsRegion.setFilter("Name", "Is Blank");
        childMaterialsRegion.clearFilter("Name");
        assertTextPresent("SampleSetBVTChildA", "SampleSetBVTGrandchildA");

        log("Change parents for the child samples");
        clickAndWait(Locator.linkWithText(FOLDER_CHILDREN_SAMPLE_SET_NAME));
        String REPARENTED_CHILD_SAMPLE_SET_TSV = "Name\tMaterialInputs/" + PARENT_SAMPLE_SET_NAME + "\tOtherProp\n" +
                "SampleSetBVTChildA\tSampleSetBVT13\t1.111\n" +
                "SampleSetBVTChildB\tSampleSetBVT14\t2.222\n";

        sampleHelper.bulkImport(REPARENTED_CHILD_SAMPLE_SET_TSV, SampleSetHelper.MERGE_DATA_OPTION);

        clickAndWait(Locator.linkWithText("SampleSetBVTChildB"));
        assertTextPresent("2.222");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVT4"));
        // Filter out any child materials so we can just check for parents
        childMaterialsRegion.setFilter("Name", "Is Blank");
        assertElementPresent(Locator.linkWithText("SampleSetBVT14"));
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Switch to filter out any parent materials so we can just check for children
        parentMaterialsRegion.setFilter("Name", "Is Blank");
        childMaterialsRegion.clearFilter("Name");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVT14"));
        assertElementPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
    }

    @Test
    public void testParentInProjectSampleSet()
    {
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        clickProject(PROJECT_NAME);
        sampleHelper.createSampleSet(PROJECT_PARENT_SAMPLE_SET_NAME, null,
                Map.of("Field1", FieldDefinition.ColumnType.String),
                "Name\tField1\n" +
                        "ProjectS1\tsome value\n");

        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        sampleHelper.createSampleSet("ChildOfProject", null,
                Map.of("IntCol",  FieldDefinition.ColumnType.Integer),
                "Name\tMaterialInputs/" + PROJECT_PARENT_SAMPLE_SET_NAME + "\n" +
                        "COP1\tProjectS1\n");

        // Verify it got linked up correctly
        clickAndWait(Locator.linkWithText("COP1"));
        assertElementPresent(Locator.linkWithText("ProjectS1"));
    }

    @Test
    public void testCaseSensitivity()
    {
        SampleSetHelper sampleHelper = new SampleSetHelper(this);

        // make sure we are case-sensitive when creating samplesets -- regression coverage for issue 33743
        clickProject(PROJECT_NAME);
        sampleHelper.createSampleSet(CASE_INSENSITIVE_SAMPLE_SET);

        clickProject(PROJECT_NAME);
        sampleHelper.createSampleSet(LOWER_CASE_SAMPLE_SET);
        waitForElement(Locator.tagWithClass("div", "labkey-error").containing("A sample set with that name already exists."));
        clickProject(PROJECT_NAME);
        assertElementPresent(Locator.linkWithText(CASE_INSENSITIVE_SAMPLE_SET));
        assertElementNotPresent(Locator.linkWithText(LOWER_CASE_SAMPLE_SET));
    }

    @Test
    public void fileAttachmentTest()
    {
        File experimentFilePath = new File(TestFileUtils.getLabKeyRoot() + PIPELINE_PATH, "experiment.xar.xml");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);

        String sampleSetName = "FileAttachmentSampleSet";
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(sampleSetName, null,
                Map.of("OtherProp", FieldDefinition.ColumnType.String,
                        "FileAttachment", FieldDefinition.ColumnType.File),
                "Name\tOtherProp\n" +
                        "FA-1\tOne\n" +
                        "FA-2\tTwo\n");

        Set<String> expectedHeaders = new HashSet<>();
        expectedHeaders.add("Name");
        expectedHeaders.add("Flag");
        expectedHeaders.add("Other Prop");
        expectedHeaders.add("File Attachment");

        setFileAttachment(0, experimentFilePath);
        setFileAttachment(1, new File(TestFileUtils.getLabKeyRoot() +  "/sampledata/sampleset/RawAndSummary~!@#$%^&()_+-[]{};',..xlsx"));

        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        drt.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "SampleSetInsertedManually");
        setFormElement(Locator.name("quf_FileAttachment"), experimentFilePath);
        clickButton("Submit");
        //a double upload causes the file to be appended with a count
        assertTextPresent("experiment-1.xar.xml");
        int attachIndex = drt.getColumnIndex("File Attachment");

        // Added these last two test to check for regressions with exporting a grid with a file attachment column and deleting a file attachment column.
        exportGridWithAttachment(3, expectedHeaders, attachIndex, "experiment-1.xar.xml", "experiment.xar.xml", "rawandsummary~!@#$%^&()_+-[]{};',..xlsx");

        log("Remove the attachment columns and validate that everything still works.");
        waitAndClickAndWait(Locator.lkButton("Edit Fields"));
        PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("Field Properties").waitFor();
        fieldProperties.selectField("FileAttachment").markForDeletion();

        // Can't use _listHelper.clickSave, it waits for a "Edit Design" button and a "Done" button.
        waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Save"), 0);
        waitForElement(Locator.lkButton("Edit Fields"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        expectedHeaders.remove("File Attachment");

        exportGridVerifyRowCountAndHeader(3, expectedHeaders);
    }


    private void setFileAttachment(int index, File attachment)
    {
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        drt.clickEditRow(index);
        setFormElement(Locator.name("quf_FileAttachment"),  attachment);
        clickButton("Submit");

        String path = drt.getDataAsText(index, "File Attachment");
        assertNotNull("Path shouldn't be null", path);
        assertTrue("Path didn't contain " + attachment.getName() + ", but was: " + path, path.contains(attachment.getName()));
    }

    private Sheet exportGridVerifyRowCountAndHeader(int numRows, Set<String> expectedHeaders)
    {
        DataRegionTable list;
        DataRegionExportHelper exportHelper;
        File exportedFile;
        Workbook workbook;
        Sheet sheet;

        log("Export the grid to excel.");
        list = new DataRegionTable("Material", this.getDriver());
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        try
        {
            workbook = ExcelHelper.create(exportedFile);
            sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), numRows, sheet.getLastRowNum());
            if (expectedHeaders != null)
            {
                Set<String> actualHeaders = new HashSet<>(ExcelHelper.getRowData(sheet, 0));
                assertEquals("Column headers not as expected", expectedHeaders, actualHeaders);
            }

            return sheet;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }

    private void exportGridWithAttachment(int numOfRows, Set<String> expectedHeaders, int exportColumn, String... expectedFilePaths)
    {
        Sheet sheet = exportGridVerifyRowCountAndHeader(numOfRows, expectedHeaders);
        List<String> exportedColumn;
        int row;

        log("Validate that the value for the attachment columns is as expected.");
        exportedColumn = ExcelHelper.getColumnData(sheet, exportColumn);
        row = 1;
        for (String filePath : expectedFilePaths)
        {
            if (filePath.length() == 0)
            {
                assertEquals("Value of attachment column for row " + row + " not exported as expected.", "", exportedColumn.get(row).trim());
            }
            else
            {
                assertThat("Value of attachment column for row " + row + " not exported as expected.", exportedColumn.get(row).trim().toLowerCase(), CoreMatchers.containsString(filePath));
            }
            row++;
        }
    }


    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
