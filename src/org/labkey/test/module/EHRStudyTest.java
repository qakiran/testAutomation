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

package org.labkey.test.module;

import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.tests.SimpleApiTest;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Mar 21, 2011
 * Time: 1:59:12 PM
 */
public class EHRStudyTest extends SimpleApiTest
{
    // Project/folder names are hard-coded into some links in the module.
    private static final String PROJECT_NAME = "WNPRC";
    private static final String FOLDER_NAME = "EHR";
    private static final String STUDY_ZIP = "/sampledata/study/EHR Study Anon.zip";
    private static final String SCRIPT_TEMPLATE = "/server/test/data/api/ehr-security-template.xml";
    private static final String PROJECT_ID = "357658"; // project with one participant
    private static final String PROJECT_MEMBER_ID = "2312318"; // PROJECT_ID's single participant
    private static final String ROOM_ID = "2400443"; // room of PROJECT_MEMBER_ID
    private static final String CAGE_ID = "5122545"; // cage of PROJECT_MEMBER_ID
    private static final String AREA_ID = "A1/AB190"; // arbitrary area
    private static final String PROTOCOL_ID = "g00101"; // Protocol with exactly 5 members
    private static final String[] PROTOCOL_MEMBER_IDS = {"2008446", "3804589", "4551032", "5904521", "6390238"}; // Protocol members, sorted ASC alphabetically
    private static final String[] MORE_ANIMAL_IDS = {"1020148","1099252","1112911","727088","9118022"}; // Some more, distinct, Ids
    private static final EHRUser DATA_ADMIN = new EHRUser("admin@ehrstudy.test", "EHR Administrators", EHRRole.DATA_ADMIN);
    private static final EHRUser REQUESTER = new EHRUser("requester@ehrstudy.test", "EHR Requestors", EHRRole.REQUESTER);
    private static final EHRUser BASIC_SUBMITTER = new EHRUser("basicsubmitter@ehrstudy.test", "EHR Basic Submitters", EHRRole.BASIC_SUBMITTER);
    private static final EHRUser FULL_SUBMITTER = new EHRUser("fullsubmitter@ehrstudy.test", "EHR Full Submitters", EHRRole.FULL_SUBMITTER);

    private static class EHRUser
    {
        private final String _userId;
        private final String _groupName;
        private final EHRRole _role;
        
        public EHRUser(String userId, String groupName, EHRRole role)
        {
            _userId = userId;
            _groupName = groupName;
            _role = role;
        }
        
        public String getUser()
        {
            return _userId;
        }
        
        public String getGroup()
        {
            return _groupName;
        }
        
        public EHRRole getRole()
        {
            return _role;
        }
    }

    @Override
    protected boolean isDatabaseSupported(DatabaseInfo info)
    {
        return info.productName.equals("PostgreSQL");
    }
       
    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/ehr";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public boolean enableLinkCheck()
    {
        if ( super.enableLinkCheck() )
            log("EHR test has too many hard coded links and special actions to crawl effectively. Skipping crawl.");
        return false;
    }

    @Override
    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[] {
            Pattern.compile("qcstate", Pattern.CASE_INSENSITIVE)//qcstate IDs aren't predictable
        };
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[0];
    }

    @Override
    public void doCleanup()
    {
        long startTime = System.currentTimeMillis();
        try {deleteProject(PROJECT_NAME);} catch (Throwable t) { /*ignore*/ }
        if(isTextPresent(PROJECT_NAME))
        {
            log("Wait extra long for folder to finish deleting.");
            while (isTextPresent(PROJECT_NAME) && System.currentTimeMillis() - startTime < 300000) // 5 minutes max.
            {
                sleep(5000);
                refresh();
            }
            if (!isTextPresent(PROJECT_NAME)) log("Test Project deleted in " + (System.currentTimeMillis() - startTime) + "ms");
            else fail("Test Project not finished deleting after 5 minutes");
        }
        try{deleteUser(DATA_ADMIN.getUser());}catch(Throwable T){}
        try{deleteUser(REQUESTER.getUser());}catch(Throwable T){}
        try{deleteUser(BASIC_SUBMITTER.getUser());}catch(Throwable T){}
        try{deleteUser(FULL_SUBMITTER.getUser());}catch(Throwable T){}
    }

    @Override
    public void runUITests()
    {
        enableEmailRecorder();

        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "Collaboration", new String[]{"EHR", "Pipeline", "Study"});
        enableModule(PROJECT_NAME, "EHR");
        clickLinkWithText(FOLDER_NAME);
        beginAt(getBaseURL()+"/ehr/"+PROJECT_NAME+"/"+FOLDER_NAME+"/_initEHR.view");
        clickNavButton("Populate All", 0);
        goToModule("Study");
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());

        log("Remove all webparts");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithImage(getContextPath() + "/_images/partdelete.png", 0);
        clickLinkWithImage(getContextPath() + "/_images/partdelete.png", 0);
        clickWebpartMenuItem("Pages", false, "Layout", "Remove From Page");
        addWebPart("EHR Navigation");
        addWebPart("EHR Datasets");
        addWebPart("Project Sponsors");
        addWebPart("Last EHR Sync");

        log("Setup EHR Menu Bar.");
        clickAdminMenuItem("Manage Project", "Project Settings");
        clickLinkWithText("Menu Bar");
        clickLinkWithText("Turn On Custom Menus");
        addWebPart("Electronic Health Record");
        addWebPart("Quick Search");

        animalHistoryTest();
        quickSearchTest();
        setupEhrPermissions();
        defineQCStates();
        
        /* super.runApiTests() */
    }

    private void animalHistoryTest()
    {
        String dataRegionName;

        mouseOver(Locator.linkWithText("Electronic Health Record"));
        waitAndClick(Locator.linkWithText("Animal History"));
        waitForPageToLoad();

        log("Verify Single animal history");
        setFormElement("subjectBox", PROTOCOL_MEMBER_IDS[0]);
        sleep(200);//weird timing issue.  Nothing to wait for, so we just pause for a moment.
        clickNavButton("Refresh", 0);
        waitForElement(Locator.linkWithText(PROTOCOL_MEMBER_IDS[0]), WAIT_FOR_JAVASCRIPT);

        crawlReportTabs();

        log("Verify Entire colony history");
        checkRadioButton("selector", "renderColony");
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", 44, getDataRegionRowCount(dataRegionName));

        log("Verify location based history");
        checkRadioButton("selector", "renderRoomCage");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='areaField']/.."), AREA_ID);
        setFormElement("roomField", ROOM_ID);
        setFormElement("cageField", CAGE_ID);
        clickNavButton("Refresh", 0);
        // No results expected due to anonymized cage info.
        waitForText("No records found", WAIT_FOR_JAVASCRIPT);

        log("Verify Project search");
        checkRadioButton("selector", "renderMultiSubject");
        waitAndClick(Locator.xpath("//table[text()='[Search By Project/Protocol]']"));
        ExtHelper.waitForExtDialog(this, "Search By Project/Protocol");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./label[text()='Project:']]/div/div"), PROJECT_ID);
        clickNavButton("Submit", 0);
        waitForElement(Locator.button(PROJECT_MEMBER_ID + " (X)"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Refresh", 0);
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT);

        log("Verify Protocol search");
        checkRadioButton("selector", "renderMultiSubject");
        waitAndClick(Locator.xpath("//table[text()='[Search By Project/Protocol]']"));
        ExtHelper.waitForExtDialog(this, "Search By Project/Protocol");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./label[text()='Protocol:']]/div/div"), PROTOCOL_ID);
        clickNavButton("Submit", 0);
        waitForElement(Locator.button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);

        // Check protocol search results.
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", 5, getDataRegionRowCount(dataRegionName));
        assertLinkPresentWithText(PROTOCOL_MEMBER_IDS[0]);

        // Check animal count after removing one from search.
        waitAndClick(Locator.button(PROTOCOL_MEMBER_IDS[0] + " (X)"));
        waitForElementToDisappear(Locator.button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", 4, getDataRegionRowCount(dataRegionName));
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[0]);

        log("Verify custom actions");
        log("Return Distinct Values - no selections");
        clickMenuButtonAndContinue("More Actions", "Return Distinct Values");
        assertAlert("No records selected");

        log("Return Distinct Values");
        dataRegionName = getDataRegionName("Weight");
        checkAllOnPage(dataRegionName);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Return Distinct Values");
        ExtHelper.waitForExtDialog(this, "Return Distinct Values");
        ExtHelper.selectComboBoxItem(this, "Select Field", "Animal Id");
        clickNavButton("Submit", 0);
        ExtHelper.waitForExtDialog(this, "Distinct Values");
        assertFormElementEquals("distinctValues", PROTOCOL_MEMBER_IDS[1]+"\n"+PROTOCOL_MEMBER_IDS[2]+"\n"+PROTOCOL_MEMBER_IDS[3]+"\n"+PROTOCOL_MEMBER_IDS[4]);
        clickNavButton("Close", 0);

        log("Return Distinct Values - filtered");
        setFilterAndWait(dataRegionName, "Id", "Does not Equal", PROTOCOL_MEMBER_IDS[1], 0);
        waitForText("filtered", WAIT_FOR_JAVASCRIPT);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Return Distinct Values");
        ExtHelper.waitForExtDialog(this, "Return Distinct Values");
        ExtHelper.selectComboBoxItem(this, "Select Field", "Animal Id");
        clickNavButton("Submit", 0);
        ExtHelper.waitForExtDialog(this, "Distinct Values");
        assertFormElementEquals("distinctValues", PROTOCOL_MEMBER_IDS[2]+"\n"+PROTOCOL_MEMBER_IDS[3]+"\n"+PROTOCOL_MEMBER_IDS[4]);
        clickNavButton("Close", 0);

        log("Compare Weights - no selection");
        uncheckAllOnPage(dataRegionName);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        assertAlert("No records selected");

        log("Compare Weights - one selection");
        checkDataRegionCheckbox(dataRegionName, 0);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        ExtHelper.waitForExtDialog(this, "Weights");
        clickNavButton("OK", 0);

        log("Compare Weights - two selections");
        checkDataRegionCheckbox(dataRegionName, 1);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        ExtHelper.waitForExtDialog(this, "Weights");
        clickNavButton("OK", 0);

        log("Compare Weights - three selections");
        checkDataRegionCheckbox(dataRegionName, 2);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        ExtHelper.waitForExtDialog(this, "Weights"); // After error dialog.
        clickNavButton("OK", 0);

        log("Jump to Other Dataset - no selection");
        uncheckAllOnPage(dataRegionName);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Jump To Other Dataset");
        assertAlert("No records selected");

        log("Jump to Other Dataset - two selection");
        dataRegionName = getDataRegionName("Abstract");
        checkDataRegionCheckbox(dataRegionName, 0); // PROTOCOL_MEMBER_IDS[1]
        checkDataRegionCheckbox(dataRegionName, 3); // PROTOCOL_MEMBER_IDS[4]
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Jump To Other Dataset");
        ExtHelper.selectComboBoxItem(this, "Dataset", "Blood Draws");
        ExtHelper.selectComboBoxItem(this, "Filter On", "Animal Id");
        clickNavButton("Submit");
        waitForElement(Locator.linkWithText(PROTOCOL_MEMBER_IDS[1]), WAIT_FOR_JAVASCRIPT);
        setSort("query", "Id", SortDirection.ASC, 0);
        waitForElementToDisappear(Locator.linkWithText(PROTOCOL_MEMBER_IDS[4]), WAIT_FOR_JAVASCRIPT);
        clickMenuButtonAndContinue("Page Size", "Show All");
        waitForElement(Locator.linkWithText(PROTOCOL_MEMBER_IDS[4]), WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[2]);

        log("Jump to History");
        checkDataRegionCheckbox("query", 0); // PROTOCOL_MEMBER_IDS[1]
        clickMenuButton("More Actions", "Jump To History");
        assertTitleContains("Animal History");
        clickNavButton("  Append -->", 0);
        setFormElement("subjectBox", PROTOCOL_MEMBER_IDS[2]);
        clickNavButton("  Append -->", 0);
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", 2, getDataRegionRowCount(dataRegionName));
        assertTextPresent(PROTOCOL_MEMBER_IDS[1], PROTOCOL_MEMBER_IDS[2]);

        log("Check subjectBox parsing");
        setFormElement("subjectBox",  MORE_ANIMAL_IDS[0]+","+MORE_ANIMAL_IDS[1]+";"+MORE_ANIMAL_IDS[2]+" "+MORE_ANIMAL_IDS[3]+"\n"+MORE_ANIMAL_IDS[4]);
        clickNavButton("  Replace -->", 0);
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", 5, getDataRegionRowCount(dataRegionName));
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[1]);
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[2]);
                                      
        clickNavButton(" Clear ", 0);
        clickNavButton("Refresh", 0);
        assertAlert("Must Enter At Least 1 Animal ID");
        assertElementNotPresent(Locator.buttonContainingText("(X)"));
    }

    private void quickSearchTest()
    {
        log("Quick Search - Show Animal");
        mouseOver(Locator.linkWithText("Quick Search"));
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        setFormElement("animal", MORE_ANIMAL_IDS[0]);
        clickNavButton("Show Animal");
        assertTitleContains("Animal - "+MORE_ANIMAL_IDS[0]);

        log("Quick Search - Show Group");
        mouseOver(Locator.linkWithText("Quick Search"));
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='animalGroup']/.."), "Alive, at WNPRC");
        clickNavButton("Show Group");
        waitForText("1 - 44 of 44", WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Project");
        mouseOver(Locator.linkWithText("Quick Search"));
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='projectField']/.."), PROJECT_ID);
        clickNavButton("Show Project");
        waitForElement(Locator.linkWithText(PROJECT_ID), WAIT_FOR_JAVASCRIPT);

        // TODO: blocked by 12225: Exception in ETLAuditViewFactory.addAuditEntry
        log("Quick Search - Show Protocol");
        mouseOver(Locator.linkWithText("Quick Search"));
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='protocolField']/.."), PROTOCOL_ID);
        clickNavButton("Show Protocol");
        waitForElement(Locator.linkWithText(PROTOCOL_ID), WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Room");
        mouseOver(Locator.linkWithText("Quick Search"));
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        setFormElement("room", ROOM_ID);
        clickNavButton("Show Room");
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT);
    }

    private void setupEhrPermissions()
    {
        clickLinkWithText(PROJECT_NAME);
        createUserAndNotify(DATA_ADMIN.getUser(), "");
        clickLinkWithText(PROJECT_NAME);
        createUserAndNotify(REQUESTER.getUser(), "");
        clickLinkWithText(PROJECT_NAME);
        createUserAndNotify(BASIC_SUBMITTER.getUser(), "");
        clickLinkWithText(PROJECT_NAME);
        createUserAndNotify(FULL_SUBMITTER.getUser(), "");
        clickLinkWithText(PROJECT_NAME);
        setInitialPassword(DATA_ADMIN.getUser(), PasswordUtil.getPassword());
        setInitialPassword(REQUESTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(BASIC_SUBMITTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(FULL_SUBMITTER.getUser(), PasswordUtil.getPassword());

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        pushLocation();
        createPermissionsGroup(DATA_ADMIN.getGroup(), DATA_ADMIN.getUser());
        createPermissionsGroup(REQUESTER.getGroup(), REQUESTER.getUser());
        createPermissionsGroup(BASIC_SUBMITTER.getGroup(), BASIC_SUBMITTER.getUser());
        createPermissionsGroup(FULL_SUBMITTER.getGroup(), FULL_SUBMITTER.getUser());
        popLocation();
        enterPermissionsUI();
        uncheckInheritedPermissions();
        setPermissions(DATA_ADMIN.getGroup(), "Reader");
        setPermissions(REQUESTER.getGroup(), "Reader");
        setPermissions(BASIC_SUBMITTER.getGroup(), "Reader");
        setPermissions(FULL_SUBMITTER.getGroup(), "Reader");
        savePermissions();
        ExtHelper.clickExtTab(this, "Study Security");
        waitAndClickNavButton("Study Security");

        checkRadioButton(getRadioButtonLocator(DATA_ADMIN.getGroup(), "READOWN"));
        checkRadioButton(getRadioButtonLocator(REQUESTER.getGroup(), "READOWN"));
        checkRadioButton(getRadioButtonLocator(BASIC_SUBMITTER.getGroup(), "READOWN"));
        checkRadioButton(getRadioButtonLocator(FULL_SUBMITTER.getGroup(), "READOWN"));
        clickNavButtonByIndex("Update", 1);

        selectOptionByValue(Locator.xpath("//select[@name='"+DATA_ADMIN.getGroup()+"']"), DATA_ADMIN.getRole().toString());
        selectOptionByValue(Locator.xpath("//select[@name='"+REQUESTER.getGroup()+"']"), REQUESTER.getRole().toString());
        selectOptionByValue(Locator.xpath("//select[@name='"+BASIC_SUBMITTER.getGroup()+"']"), BASIC_SUBMITTER.getRole().toString());
        selectOptionByValue(Locator.xpath("//select[@name='"+FULL_SUBMITTER.getGroup()+"']"), FULL_SUBMITTER.getRole().toString());

        clickNavButton("Save");
    }

    @Override
    public void runApiTests() throws Exception
    {
        testUserAgainstAllStates(DATA_ADMIN);
        testUserAgainstAllStates(REQUESTER);
        testUserAgainstAllStates(BASIC_SUBMITTER);
        testUserAgainstAllStates(FULL_SUBMITTER);
    }

    private void testUserAgainstAllStates(EHRUser user) throws Exception
    {
        File[] scriptFiles = new File[EHRQCState.values().length];

        int i = 0;
        for(EHRQCState qcState : EHRQCState.values())
        {
            scriptFiles[i++] = prepareScript(user, qcState);
        }

        super.runApiTests(scriptFiles, user.getUser(), PasswordUtil.getPassword(), true);
    }

    private File prepareScript(EHRUser user, EHRQCState qcState) throws java.io.IOException
    {
        File preparedFile = new File(System.getProperty("java.io.tmpdir"), "ehr-"+user.getRole()+"-"+qcState.label.replace(":", "_")+"-insert.xml");

        BufferedReader reader = new BufferedReader(new FileReader(getLabKeyRoot() + SCRIPT_TEMPLATE));
        BufferedWriter writer = new BufferedWriter(new FileWriter(preparedFile));

        String line;
        boolean permitted = successExpected(user.getRole(), qcState);

        while ( (line = reader.readLine()) != null)
        {
            line = line.replace("${AnimalId}", MORE_ANIMAL_IDS[0]);
            line = line.replace("${QCState}", qcState.label);
            line = line.replace("${Role}", user.getRole().toString());
            line = line.replace(permitted ? "successresponse>" : "failresponse>", "response>");

            writer.write(line + "\n");
        }

        writer.close();
        return preparedFile;
    }

    private void crawlReportTabs()
    {
        String tabs[] = {"-Assignments", "Active Assignments", "Assignment History",
                         "-Clin Path", "Bacteriology", "Chemistry:Blood Chemistry Results", "Clinpath Runs", "Hematology:Hematology Results", "Immunology:Immunology Results", "Parasitology", "Urinalysis:Urinalysis Results", "Viral Challenges", "Virology",
                         "-Clinical", "Abstract:Active Assignments", "Clinical Encounters", "Clinical Remarks", "Diarrhea Report", "Drug Administration", "Full History", "Full History Plus Obs", "Irregular Obs:Irregular Observations", "Surgical History", "Tasks", "Treatment Orders", "Treatment Schedule", "Weights:Weight",
                         "-Colony Management", "Arrival/Departure:Arrivals", "Behavior Remarks", "Birth Records", "Housing - Active", "Housing History", "Inbreeding Coefficients", "Kinship", "Menstrual Data", "Pedigree:Offspring", "Pregnancies", "Roommate History", "TB Tests",
                         "-Pathology", "Biopsies", "Biopsy Diagnosis", "Histology", "Necropsies", "Necropsy Diagnosis",
                         "-Physical Exam", "Alopecia", "Body Condition", "Dental Status", "Exams", "Teeth", "Vitals",
                         "-Today At WNPRC", "Irregular Observations", "Obs/Treatment:Obs/Treatments", "Problem List", /*"Today's History",*/ "Treatment Schedule", 
                         "-General", "Blood Draw History", "Charges", "Current Blood", "Deaths", "Demographics", "Major Events", "Notes", "Abstract:Active Assignments"};

        log("Check all Animal History report tabs");
        for (String tab : tabs)
        {
            if(tab.startsWith("-")) // High level tab
            {
                ExtHelper.clickExtTab(this, tab.substring(1));
            }
            else
            {
                if(tab.contains(":"))
                {
                    ExtHelper.clickExtTab(this, tab.split(":")[0]);
                    getDataRegionName(tab.split(":")[1]);
                }
                else
                {
                    ExtHelper.clickExtTab(this, tab);
                    getDataRegionName(tab);
                }
            }
        }

        //Clear out lingering text on report pages
        mouseOver(Locator.linkWithText("Electronic Health Record"));
        waitAndClick(Locator.linkWithText("Animal History"));
        waitForPageToLoad();
    }

    private boolean successExpected(EHRRole role, EHRQCState qcState)
    {
        // Expand to other request types once we start testing them. Insert only for now.
        return allowedActions.contains(new Permission(role, qcState, "insert"));
    }

    private static final ArrayList<Permission> allowedActions = new ArrayList<Permission>()
    {
        {
            // Data Admin - Users with this role are permitted to make any edits to datasets
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.ABNORMAL, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.COMPLETED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_APPROVED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.SCHEDULED, "insert"));

            // Requester - Users with this role are permitted to submit requests, but not approve them
            add(new Permission(EHRRole.REQUESTER, EHRQCState.REQUEST_PENDING, "insert"));

            // Full Submitter - Users with this role are permitted to submit and approve records.  They cannot modify public data.
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.ABNORMAL, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.COMPLETED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_APPROVED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.SCHEDULED, "insert"));

            // Basic Submitter - Users with this role are permitted to submit and edit non-public records, but cannot alter public ones
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.SCHEDULED, "insert"));
        }
    };

    private static class Permission 
    {
        EHRRole role;
        EHRQCState qcState;
        String action;
        public Permission(EHRRole role, EHRQCState qcState, String action)
        {
            this.role = role;
            this.qcState = qcState;
            this.action = action;
        }

        @Override
        public boolean equals(Object other)
        {
            return other.getClass().equals(Permission.class) &&
                this.role == ((Permission)other).role &&
                this.qcState == ((Permission)other).qcState &&
                this.action.equals(((Permission)other).action);
        }
    }

    private static enum EHRRole
    {
        DATA_ADMIN ("EHR Data Admin"),
        REQUESTER ("EHR Requestor"),
        BASIC_SUBMITTER ("EHR Basic Submitter"),
        FULL_SUBMITTER ("EHR Full Submitter");
        private final String name;
        private EHRRole (String name)
        {this.name = name;}
        public String toString()
        {return name;}
    }

    private static enum EHRQCState
    {
        ABNORMAL("Abnormal", "Value is abnormal", true, false, false),
        COMPLETED("Completed", "Data has been approved for public release", true, false, false),
        DELETE_REQUESTED("Delete Requested", "Records are requested to be deleted", true, true, false),
        IN_PROGRESS("In Progress", "Draft Record, not public", false, true, false),
        REQUEST_APPROVED("Request: Approved", "Request has been approved", true, true, true),
        REQUEST_COMPLETE("Request: Complete", "Request has been completed", true, false, true),
        REQUEST_DENIED("Request: Denied", "Request has been denied", true, false, true),
        REQUEST_PENDING("Request: Pending", "Part of a request that has not been approved", false, false, true),
        REVIEW_REQUIRED("Review Required", "Review is required prior to public release", false, false, false),
        SCHEDULED("Scheduled", "Record is scheduled, but not performed", true, true, false);

        public final String label;
        public final String description;
        public final boolean publicData;

        public final boolean draftData;
        public final boolean isRequest;

        EHRQCState(String label, String description, boolean publicData, boolean draftData, boolean isRequest)
        {
            this.label = label;
            this.description = description;
            this.publicData = publicData;
            this.draftData = draftData;
            this.isRequest = isRequest;
        }
    }

    private void defineQCStates()
    {
        log("Define QC states for EHR study");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        goToModule("Study");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Dataset QC States");

        for(EHRQCState qcState : EHRQCState.values())
        {
            setFormElement("newLabel", qcState.label);
            setFormElement("newDescription", qcState.description);
            if(!qcState.publicData) uncheckCheckbox("newPublicData");
            clickNavButton("Save");
        }
    }

    private Locator getRadioButtonLocator(String groupName, String setting)
    {
        //not sure why the radios are in TH elements, but they are...
        return Locator.xpath("//form[@id='groupUpdateForm']/table/tbody/tr/td[text()='"
                + groupName + "']/../th/input[@value='" + setting + "']");
    }

    private String getDataRegionName(String title)
    {
        // Specific to the EHR Animal History page.
        waitForElement(Locator.xpath("//table[@name='webpart' and ./*/*/*/a[text()='"+title+"' or starts-with(text(), '"+title+":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]"), WAIT_FOR_JAVASCRIPT);
        return getAttribute(Locator.xpath("//table[@name='webpart' and ./*/*/*/a[text()='"+title+"' or starts-with(text(), '"+title+":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]"), "id").substring(11);
    }
}
