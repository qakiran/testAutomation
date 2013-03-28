/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4HelperWD;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: cnathe
 * Date: 1/25/13
 */
public class NWBioTrustTest extends SurveyTest
{
    private static final String requestorFolder1 = "Requestor 1";
    private static final String requestorFolder2 = "Requestor 2";
    private static final String provisionTableName = "Sample Request Responses";
    private static final List<Map<String, String>> designs = new ArrayList<Map<String, String>>();
    private static final String[] unsubmittedRequestLabels = {"registration with request"};
    private static final String[] submittedRequestLabels = {"first registration", "second registration", "third registration"};
    private static final String[] allRequestLabels = ArrayUtils.addAll(unsubmittedRequestLabels, submittedRequestLabels);
    private static final File TEST_FILE_1 = new File( getLabKeyRoot() + "/sampledata/survey/TestAttachment.txt");
    private static final File TEST_FILE_2 = new File( getLabKeyRoot() + "/sampledata/survey/TestAttachment2.txt");

    private static final String[] NWBT_REQUEST_CATEGORIES = {"NWBT RC1", "NWBT RC2", "NWBT Repository"};
    private static final NwbtRequestStatuses[] NWBT_REQUEST_STATUSES = NwbtRequestStatuses.values();

    private enum NwbtRequestStatuses
    {
        SUBMITTED("Submitted"),
        SUBMISSION_REVIEW("Submission Review"),
        FEASIBILITY_REVIEW("Feasibility Review"),
        PRIORITIZATION_REVIEW("Prioritization Review"),
        APPROVED("Approved"),
        ROUTED("Routed"),
        CLOSED("Closed"),
        COMPLETE("Complete");

        private String _status;

        private NwbtRequestStatuses(String status)
        {
            _status = status;
        }

        public String toString()
        {
            return _status;
        }

        public Integer sortOrder()
        {
            return this.ordinal() + 1;
        }
    }

    private static final String[] NWBT_DOCUMENT_TYPES = {
            "Proposal Summary (Specimen Request)",
            "Proposal Summary (Future Study Request)",
            "IRB Approval Packet",
            "Signed MTDUA Agreement",
            "Signed Confidentiality Pledge",
            "Specimen Processing Protocol",
            "Blank Unique Consent Form",
            "Approval Reviewer Response",};
    private static final Boolean[][] NWBT_DOCUMENT_TYPE_FLAGS = { // multiple upload allowed, expriation
            {false, false}, //Proposal Summary (Specimen Request) (multi-upload changed for test purposes)
            {true, false}, //Proposal Summary (Future Study Request)
            {true, true}, //IRB Approval Packet
            {true, false}, //Signed MTDUA Agreement(s)
            {true, false}, //Signed Confidentiality Pledge(s)
            {true, false}, //Specimen Processing Protocol
            {true, true}, //Blank Unique Consent Form (by Study)
            {true, false}}; //Approval Reviewer Response

    private static final String NWBT_PRINCIPAL_INVESTIGATOR = "pi_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_STUDY_CONTACT = "sc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_RESEARCH_COORD = "rc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_RC_EMAIL = "rc_notification@nwbiotrust.test";
    private static final String NWBT_FACULTY_CHAIR = "fc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_FACULTY_REVIEWER = "fr_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_SURGICAL_REVIEWER = "sr_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_NON_SURGICAL_REVIEWER = "nsr_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_DISCARDED_REVIEWER = "dr_nwbiotrust@nwbiotrust.test";
    private static final String[] NWBT_USERS = {NWBT_PRINCIPAL_INVESTIGATOR, NWBT_STUDY_CONTACT, NWBT_RESEARCH_COORD,
                                                NWBT_FACULTY_CHAIR, NWBT_FACULTY_REVIEWER, NWBT_SURGICAL_REVIEWER, NWBT_NON_SURGICAL_REVIEWER, NWBT_DISCARDED_REVIEWER};
    private static final String NWBT_SURGICAL_REVIEWER_GROUP = "Surgical Reviewer";
    private static final String NWBT_NON_SURGICAL_REVIEWER_GROUP = "Non Surgical Reviewer";
    private static final String NWBT_DISCARDED_REVIEWER_GROUP = "Discarded Reviewer";
    private static final String NWBT_RESEARCH_COORD_GROUP = "Research Coordinators";

    private final File studyRegistrationJson = new File(getDownloadDir(), "study-registration.json");
    private final File prospectiveSampleRequestJson = new File(getDownloadDir(), "prospective-sample-request.json");
    private final File discardedSampleRequestJson = new File(getDownloadDir(), "discarded-sample-request.json");
    private final File surgicalTissueJson = new File(getDownloadDir(), "surgical-tissue-sample.json");
    private final File nonSurgicalTissueJson = new File(getDownloadDir(), "non-surgical-tissue-sample.json");
    private final File bloodSampleJson = new File(getDownloadDir(), "blood-tissue-sample.json");
    private final File discardedBloodSampleJson = new File(getDownloadDir(), "discarded-blood-sample.json");

    private int fileCount = 0;

    private final PortalHelper portalHelper = new PortalHelper(this);

    @Override
    protected String getProjectName()
    {
        return "NWBioTrustTest";
    }

    @Override
    @LogMethod protected void doTestSteps() throws Exception
    {
        enableEmailRecorder();

        setupResearchCoordAndRequstorFolders();
        setupUsersAndPermissions();
        setupProjectAdminProperties();
        setupSurveysTableDefinition();
        setupProvisionTableForResponses();
        setupSurveyDesignsAndRequests();

        verifyFolderTypes();
        verifyResearchCoordDashboard();
        verifyRequestorDashboard();
        verifySecondRequestorDashboard();
        verifySampleRequests();
        verifyDocumentSetFromDashboard();
    }

    @LogMethod
    private void deleteSurveyDesign()
    {
        if (designs.size() == 0)
            configureDesigns();

        log("Delete the survey designs for this project (which will delete the document sets and requests");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable drt = new DataRegionTable("query", this);
        int toDelete = 0;
        for (Map<String, String> design : designs)
        {
            if (drt.getRow("Label", design.get("label")) > -1)
            {
                String rowId = drt.getDataAsText(drt.getRow("Label", design.get("label")), "RowId");
                checkDataRegionCheckbox("query", rowId);
                toDelete++;
            }
        }

        if (toDelete > 0)
        {
            clickButton("Delete", 0);
            if (toDelete == 1)
                assertAlert("Are you sure you want to delete this survey design and its associated surveys?");
            else
                assertAlert("Are you sure you want to delete these " + toDelete + " survey designs and their associated survey instances?");

            waitAndClickAndWait(Locator.linkWithText("New Registrations"));
            waitForText("No study registrations to show", WAIT_FOR_PAGE);
            clickFolder(requestorFolder1);
            clickAndWait(Locator.linkWithText("Study Registrations"));
            waitForText("No study registrations to show", 2, WAIT_FOR_PAGE);
            assertTextNotPresent(submittedRequestLabels);
        }
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyDocumentSetFromDashboard()
    {
        goToProjectHome();
        populateDocumentSetForRequests();

        log("Verify documents and types via RC Dashboard");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("New Registrations"));
        waitForGridToLoad("div", "x4-grid-group-title", submittedRequestLabels.length + unsubmittedRequestLabels.length);
        assertElementPresent(Locator.linkWithText("Document Set (0)"), 3);
        assertElementPresent(Locator.linkWithText("Document Set (" + NWBT_DOCUMENT_TYPES.length + ")"), 1);
        click(Locator.linkWithText("Document Set (" + NWBT_DOCUMENT_TYPES.length + ")")); // link for the first registration
        waitForText(TEST_FILE_1.getName());
        assertElementPresent(Locator.linkWithText(TEST_FILE_1.getName()), NWBT_DOCUMENT_TYPES.length);
        assertElementPresent(Locator.linkWithText(TEST_FILE_2.getName()), NWBT_DOCUMENT_TYPES.length - 1); // one doc type doesn't allow multiple file upload
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
            assertTextPresent(NWBT_DOCUMENT_TYPES[index], NWBT_DOCUMENT_TYPE_FLAGS[index][0] ? 2 : 1);

        log("Verify removing documents from document set");
        clickButton("Manage");
        waitForGridToLoad("tr", "x4-grid-row", fileCount);
        // verify that we navigated to the appropriate subfolder for the manage document set page
        assertTextNotPresent("NW BioTrust Research Coordinator Dashboard");
        Locator loc = getEditLinkLocator(NWBT_DOCUMENT_TYPES[0], false);
        click(loc);
        _extHelper.waitForExtDialog("Edit Document");
        assertTextPresentInThisOrder("File Name:", "Document Type:", "Created By:", "Created:");
        clickButton("Delete", 0);
        fileCount--;
        waitForTextToDisappear(NWBT_DOCUMENT_TYPES[0]);
        waitForGridToLoad("tr", "x4-grid-row", fileCount);
    }

    @LogMethod
    private void populateDocumentSetForRequests()
    {
        log("Add documents to a document set for requests");
        clickFolder(requestorFolder1);
        clickAndWait(Locator.linkWithText("Study Registrations"));
        waitForGridToLoad("div", "x4-grid-group-title", 3);
        assertElementPresent(Locator.linkWithText("Document Set (0)"), 4);
        click(Locator.linkWithText("Document Set (0)")); // link for the first registration
        waitForText("No documents to show");
        waitForText(submittedRequestLabels[0]);
        clickButton("Manage");
        waitForText("No documents to show");
        waitForText(NWBT_REQUEST_STATUSES[0].toString());
        fileCount = 0;
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
        {
            String documentType = NWBT_DOCUMENT_TYPES[index];
            clickButton("Add Document(s)", 0);
            _extHelper.waitForExtDialog("Add Document(s)");
            _ext4Helper.selectComboBoxItem("Document Type:", documentType);
            _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.name("attachmentfile0")));
            setFormElement(Locator.name("attachmentfile0"), TEST_FILE_1);
            fileCount++;
            // the first doc type was set to not allow multiple file uploads
            if (NWBT_DOCUMENT_TYPE_FLAGS[index][0])
            {
                assertElementPresent(Locator.linkContainingText("Attach a file"));
                click(Locator.linkContainingText("Attach a file"));
                _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.name("attachmentfile1")));
                setFormElement(Locator.name("attachmentfile1"), TEST_FILE_2);
                fileCount++;
            }
            else
                assertElementNotPresent(Locator.linkContainingText("Attach a file"));
            sleep(500); // give the submit button a split second to enable base on form changes
            clickButton("Submit", 0);
            waitForGridToLoad("tr", "x4-grid-row", fileCount);
        }

        log("Verify file attachment links and document types exist");
        assertElementPresent(Locator.linkWithText(TEST_FILE_1.getName()), NWBT_DOCUMENT_TYPES.length);
        assertElementPresent(Locator.linkWithText(TEST_FILE_2.getName()), NWBT_DOCUMENT_TYPES.length - 1); // one doc type doesn't allow multiple file upload
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
            assertTextPresent(NWBT_DOCUMENT_TYPES[index], NWBT_DOCUMENT_TYPE_FLAGS[index][0] ? 2 : 1);

        log("Test document type allow multiple file setting");
        clickButton("Add Document(s)", 0);
        _extHelper.waitForExtDialog("Add Document(s)");
        _ext4Helper.selectComboBoxItem("Document Type:", NWBT_DOCUMENT_TYPES[0]);
        _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.name("attachmentfile0")));
        setFormElement(Locator.name("attachmentfile0"), TEST_FILE_2);
        sleep(500); // give the submit button a split second to enable base on form changes
        clickButton("Submit", 0);
        waitForText("This document type does not allow multiple files and one already exists in this document set.");
        clickButton("OK", 0);
        _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.name("attachmentfile0")));
        setFormElement(Locator.name("attachmentfile0"), TEST_FILE_1);
        sleep(500); // give the submit button a split second to enable base on form changes
        clickButton("Submit", 0);
        waitForText("A document with the following name already exists for this document type: " + TEST_FILE_1.getName());
        clickButton("OK", 0);
        clickButton("Close", 0);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifySecondRequestorDashboard()
    {
        // TODO: this will be put to better use once we implement the NWBT security roles/permissions
        log("Verify that the 2nd requestor folder does not contain data from the first requestor");
        clickFolder(requestorFolder2);
        clickAndWait(Locator.linkWithText("Study Registrations"));
        waitForText("No study registrations to show", 2, WAIT_FOR_PAGE);
        assertElementNotPresent(Locator.linkWithText("Click Here"));
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyRequestorDashboard()
    {
        log("Verify updated requests show up in Requestor Dashboard");
        clickFolder(requestorFolder1);
        clickAndWait(Locator.linkWithText("Study Registrations"));
        waitForGridToLoad("div", "x4-grid-group-title", 3);
        assertElementNotPresent(getGroupingTitleLocator(NWBT_REQUEST_STATUSES[submittedRequestLabels.length].toString()));
        for (Object category : Arrays.copyOfRange(NWBT_REQUEST_STATUSES, 0, submittedRequestLabels.length - 1))
            assertElementPresent(getGroupingTitleLocator(category.toString()));
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifySampleRequests()
    {
        impersonate(NWBT_PRINCIPAL_INVESTIGATOR);
        clickFolder(getProjectName());
        clickFolder(requestorFolder1);
        clickAndWait(Locator.folderTab("Sample Requests"));

        log("Request specimens from surguries or clinic procedures");
        clickAndWait(Locator.linkWithText("Click Here"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tr").withPredicate(Locator.xpath("td/label").containing("Associated study")).append("/td[2]/table"), unsubmittedRequestLabels[0] + " (ID: ", true);
        _ext4Helper.checkCheckbox("Surgical tissue samples");
        clickButton("Next", 0);
        _ext4Helper.checkCheckbox("Stomach");
        clickButton("Next", 0);
        _ext4Helper.waitForMaskToDisappear();
        clickButton("Add Record", 0);
        {// Tissue Samples dialog
            _extHelper.waitForExtDialog("Add Surgical Tissue Samples");
            waitForElementToDisappear(Locator.css(".x4-mask").index(2));

            // Tissue Type Information
            _ext4Helper.selectComboBoxItem("Surgical Tissue Type:", "Tumor from primary site");
            _ext4Helper.selectComboBoxItem("Anatomical Site:", "Stomach");
            setFormElement(Ext4HelperWD.Locators.formItemWithLabel("Minimum Size:").append("//input"), "5");
            _ext4Helper.selectComboBoxItem("Minimum Size Units:", "gr");
            setFormElement(Ext4HelperWD.Locators.formItemWithLabel("Preferred Size:").append("//input"), "6");
            _ext4Helper.selectComboBoxItem("Preferred Size Units:", "gr");
            _ext4Helper.selectComboBoxItem("Preservation:", "Flash Frozen");

            // Participant Eligibility
            _extHelper.clickExtButton("Add Surgical Tissue Samples", "Next", 0);
            setFormElement(Ext4HelperWD.Locators.formItemWithLabel("Number of Cases/Participants:").append("//input"), "6");
            click(Ext4HelperWD.Locators.formItemWithLabel("Are patients with a prior cancer OK?").append("//label").withText("Yes"));
            click(Ext4HelperWD.Locators.formItemWithLabelContaining("Are patients with a history of Basal").append("//label").withText("Yes"));

            // Sample Pickup
            _extHelper.clickExtButton("Add Surgical Tissue Samples", "Next", 0);
            click(Ext4HelperWD.Locators.formItemWithLabelContaining("If sample is available after 5pm").append("//label").withText("Yes"));
            click(Ext4HelperWD.Locators.formItemWithLabelContaining("Prefer samples be held overnight").append("//label").withText("Yes"));

            _extHelper.clickExtButton("Add Surgical Tissue Samples", "Next", 0);
            _extHelper.clickExtButton("Add Surgical Tissue Samples", "Save", 0);
        }// done with Tissue Samples dialog

        _extHelper.waitForExtDialogToDisappear("Add Surgical Tissue Samples");
        waitForElement(Locator.css(".x4-action-col-0"));
        String requestId = getText(Locator.css(".x4-grid-cell-first"));
        setFormElement(Locator.name("surgicalstartdate"), "2013-03-20");
        setFormElement(Locator.name("surgicalenddate"), "2013-03-21");
        click(Ext4HelperWD.Locators.formItemWithLabelContaining("Are samples collected after neoadjuvant").append("//label").withText("Yes"));
        clickButton("Next", 0);
        clickButton("Save");

        clickTab("Overview");
        waitForElement(Locator.linkWithText("Pending Sample Request (1)", 0));
        assertElementPresent(Locator.linkWithText("Submitted Sample Request (0)", 0));

        clickTab("Sample Requests");
        waitForElement(Locator.id("submitted-dashboard-3").append("//span").withText("No study registrations to show"));
        waitForElement(Locator.id("active-dashboard-4").append("//span").withText("No study registrations to show"));
        waitForElement(Locator.id("closed-dashboard-5").append("//span").withText("No study registrations to show"));
        waitForElement(Locator.id("pending-dashboard-2").append(Locator.linkWithText(requestId)));
        clickTab("Study Registrations");
        waitAndClick(Locator.linkWithText(requestId.split("-")[0]));
        waitAndClick(Locator.linkWithText("view details"));
        waitAndClick(Locator.xpath("//li[text()='Save / Submit']"));

        clickButton("Submit completed form");
        clickTab("Sample Requests");
        waitForElement(Locator.id("pending-dashboard-2").append("//span").withText("No study registrations to show"));
        waitForElement(Locator.id("submitted-dashboard-3").append(Locator.linkWithText(requestId)));
        waitForElement(Locator.id("active-dashboard-4").append("//span").withText("No study registrations to show"));
        waitForElement(Locator.id("closed-dashboard-5").append("//span").withText("No study registrations to show"));

        clickTab("Overview");
        waitForElement(Locator.linkWithText("Pending Sample Request (0)", 0));
        assertElementPresent(Locator.linkWithText("Submitted Sample Request (1)", 0));

        stopImpersonating();

        goToModule("Dumbster");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyResearchCoordDashboard()
    {
        log("Verify submitted requests show up in RC Dashboard");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("New Registrations"));
        waitForGridToLoad("div", "x4-grid-group-title", 1); // all request should still be in the Unassigned category
        assertTextNotPresent("No study registrations to show");
        assertElementPresent(getGroupingTitleLocator("Unassigned"));
        assertTextPresentInThisOrder(submittedRequestLabels);
        assertTextNotPresent(unsubmittedRequestLabels);

        log("Update request status and categories");
        for (int i = 0; i < submittedRequestLabels.length; i++)
            setRequestStatusAndCategory(i, submittedRequestLabels[i], NWBT_REQUEST_STATUSES[i].toString(), NWBT_REQUEST_CATEGORIES[i]);
        goToProjectHome();
        clickAndWait(Locator.linkWithText("New Registrations"));
        waitForGridToLoad("div", "x4-grid-group-title", NWBT_REQUEST_CATEGORIES.length);
        assertElementNotPresent(getGroupingTitleLocator("Unassigned"));
        for (String category : NWBT_REQUEST_CATEGORIES)
            assertElementPresent(getGroupingTitleLocator(category));
        assertTextPresentInThisOrder(Arrays.copyOfRange(NWBT_REQUEST_STATUSES, 0, submittedRequestLabels.length - 1));
    }

    private void waitForGridToLoad(final String tag, final String className, final int expectedCount)
    {
        Locator l = Locator.xpath("//" + tag + "[contains(@class, '" + className + "')]");
        startTimer();
        while (getElementCount(l) < expectedCount && elapsedMilliseconds() < WAIT_FOR_JAVASCRIPT)
            sleep(1000);
        assertElementPresent(l, expectedCount);
    }

    private Locator getGroupingTitleLocator(String title)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-group-title') and contains(text(), '" + title + "')]");
    }

    private void setRequestStatusAndCategory(int index, String label, String status, String category)
    {
        Locator loc = getEditLinkLocator(label, false);
        click(loc);
        _extHelper.waitForExtDialog("Edit Registration");
        sleep(1000); // this is tricky because there is a loading mask for the combos, but they can load very quickly so that the test misses it if we wait for the mask to disappear
        _ext4Helper.selectComboBoxItem("NWBT Resource:", category);
        _ext4Helper.selectComboBoxItem("Status:", status);
        setFormElement(Locator.name("Comment"), "resource and status changed");
        sleep(500); // update button is enabled based on form state
        clickButton("Update", 0);
        waitForElement(Locator.xpath("//div[contains(@class, 'x4-grid-group-title') and contains(text(), '" + category + "')]"));
        assertTextPresent(status);
    }

    private Locator getEditLinkLocator(String label, boolean isLink)
    {
        if (isLink)
            return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//a[contains(text(),'" + label + "')]/../../..//td//div//span[contains(@class, 'edit-views-link')]");
        else
            return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner') and contains(text(),'" + label + "')]/../..//td//div//span[contains(@class, 'edit-views-link')]");
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupSurveyDesignsAndRequests()
    {
        if (designs.size() == 0)
            configureDesigns();

        log("Create survey designs in project folder");
        for (Map<String, String> entry : designs)
        {
            createSurveyDesign(getProjectName(), "Manage", entry.get("label"), entry.get("description"),
                    "biotrust", entry.get("table"), entry.get("metadataPath"));
        }

        log("Configure dashboard");
        clickAndWait(Locator.linkWithText("New Registrations"));
        waitForText("No study registrations to show");
        customizeDashboard("RC Dashboard - New Registrations", designs.get(0).get("label"));

        log("Submit requests from the requestor subfolder");
        clickFolder(requestorFolder1);
        clickAndWait(Locator.linkWithText("Study Registrations"));
        customizeDashboard("Requestor Dashboard - Study Registrations", designs.get(0).get("label"));

        pushLocation();
        impersonate(NWBT_PRINCIPAL_INVESTIGATOR);
        popLocation();
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        assertTextPresentInThisOrder(designs.get(0).get("label"), designs.get(0).get("description"), "Pending Registrations", "Submitted Registrations");
        waitForText("No study registrations to show", 2, WAIT_FOR_PAGE); // both pending and submitted should be empty
        for (String requestLabel : submittedRequestLabels)
        {
            waitAndClickAndWait(Locator.linkWithText("Click Here"));
            List<Map<String, String>> fields = new ArrayList<Map<String, String>>();
            fields.add(createFieldInfo("Study Information", "studydescription", "test study description: " + requestLabel));
            fields.add(createFieldInfo("Study Information", "irbapprovalstatus", "Approved Human Subjects Research"));
            fields.add(createFieldInfo("Study Information", "irbfilenumber", "TEST123"));
            fields.add(createFieldInfo("Study Information", "irbexpirationdate", "2013-03-07"));
            fields.add(createFieldInfo("Study Information", "reviewingirb", "Other"));
            fields.add(createRadioFieldInfo("Study Information", "Do you anticipate submitting data from this study to a public database (e.g. dbGAP)?*", "Yes"));
            fields.add(createComboFieldInfo("Contact Information", "Study Principal Investigator*", "pi nwbiotrust"));
            fields.add(createFieldInfo("Billing", "fundingsource", "test funding source description"));
            createNewStudyRegistration(requestLabel, fields, true);
        }
        waitForGridToLoad("div", "x4-grid-group-title", 1); // all study registrations should be in the Submitted group
        waitForText("No study registrations to show", 1, WAIT_FOR_PAGE); // pending grid should still be empty
        assertTextPresentInThisOrder(submittedRequestLabels);
        for (String requestLabel : unsubmittedRequestLabels)
        {
            waitAndClickAndWait(Locator.linkWithText("Click Here"));
            List<Map<String, String>> fields = new ArrayList<Map<String, String>>();
            fields.add(createFieldInfo("Study Information", "studydescription", "test study description: " + requestLabel));
            fields.add(createFieldInfo("Study Information", "irbapprovalstatus", "Pending"));
            fields.add(createFieldInfo("Study Information", "irbfilenumber", "TEST123"));
            fields.add(createFieldInfo("Study Information", "reviewingirb", "Other"));
            fields.add(createRadioFieldInfo("Study Information", "Do you anticipate submitting data from this study to a public database (e.g. dbGAP)?*", "Yes"));
            fields.add(createComboFieldInfo("Contact Information", "Study Principal Investigator*", "pi nwbiotrust"));
            fields.add(createFieldInfo("Billing", "fundingsource", "test funding source description"));
            createNewStudyRegistration(requestLabel, fields, false);
        }
        clickTab("Study Registrations");
        waitForGridToLoad("div", "x4-grid-group-title", 1); // all study registrations should be in the Submitted group, pending registration is in no group
        waitForElement(Locator.tagWithText("div", unsubmittedRequestLabels[0]));
        assertTextPresentInThisOrder(allRequestLabels);

        pushLocation();
        stopImpersonating();
        popLocation();
        waitForGridToLoad("div", "x4-grid-group-title", 1); // all study registrations should be in the Submitted group, pending registration is in no group
        assertTextPresentInThisOrder(allRequestLabels);
    }

    private Map<String, String> createFieldInfo(String section, String name, String value)
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("section", section);
        info.put("name", name);
        info.put("value", value);
        return info;
    }

    private Map<String, String> createRadioFieldInfo(String section, String label, String boxLabel)
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("section", section);
        info.put("label", label);
        info.put("boxLabel", boxLabel);
        return info;
    }

    private Map<String, String> createComboFieldInfo(String section, String label, String selection)
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("section", section);
        info.put("label", label);
        info.put("selection", selection);
        return info;
    }

    private void createNewStudyRegistration(String label, List<Map<String, String>> fields, Boolean submitStudy)
    {
        waitForText("Study Name*");
        setFormElement(Locator.name("_surveyLabel_"), label);
        for (Map<String, String> field : fields)
        {
            Locator sectionLoc = Locator.xpath("//li[text()='" + field.get("section") + "']");
            if (isElementPresent(sectionLoc))
                click(sectionLoc);

            if (field.get("boxLabel") != null)
            {
                _ext4Helper.selectRadioButton(field.get("label"), field.get("boxLabel"));
            }
            else if (field.get("selection") != null)
            {
                _ext4Helper.selectComboBoxItem(field.get("label"), field.get("selection"));
            }
            else
            {
                waitForElement(Locator.name(field.get("name")));
                setFormElement(Locator.name(field.get("name")), field.get("value"));
            }
        }
        sleep(500); // give the submit button a split second to enable base on form changes
        click(Locator.xpath("//li[text()='Save / Submit']"));
        if (submitStudy)
            clickButton("Submit completed form");
        else
        {
            clickButton("Save", 0);
            _extHelper.waitForExtDialog("Success");
            _ext4Helper.waitForMaskToDisappear();
        }

    }

    private void customizeDashboard(String webpartTitle, String formName)
    {
        portalHelper.clickWebpartMenuItem(webpartTitle, "Customize");
        waitForText(formName);
        _ext4Helper.checkCheckbox(formName);
        clickButton("Save");
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProvisionTableForResponses()
    {
        log("Create provision table in biotrust schema for responses");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        waitForText("Existing Request Response Schemas");
        click(Locator.linkWithText("Create new Request Response Schema"));
        _extHelper.waitForExtDialog("New Request Response Schema");
        setFormElement(Locator.name("queryName"), provisionTableName);
        sleep(500); // give the save button a split second to be enabled on form change
        clickButton("Save");
        waitForText("Edit Fields in " + provisionTableName);
        _listHelper.addField(new ListHelper.ListColumn("testfield1", "Test Field 1", ListHelper.ListColumnType.String, null));
        clickButton("Save");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        assertElementPresent(Locator.linkWithText(provisionTableName));
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupSurveysTableDefinition()
    {
        log("Add fields to extensible survey.Surveys table");
        goToProjectHome();
        goToSurveysTable("create definition");
        waitForText("Extensible Table 'Surveys'");
        waitForText("No fields have been defined.");
        _listHelper.addField("Field Properties", 0, "Category", "Category", ListHelper.ListColumnType.Integer);
        _listHelper.setColumnType(0, new ListHelper.LookupInfo("", "biotrust", "RequestCategory"));
        clickButton("Save");

        log("Verify that the subfolders use the same Surveys domain");
        clickFolder(requestorFolder1);
        goToSurveysTable("view data");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Category");
        _customizeViewsHelper.applyCustomView();
        assertTextPresentInThisOrder("Modified", "Status", "Category");
    }

    private void goToSurveysTable(String textLink)
    {
        goToSchemaBrowser();
        selectQuery("survey", "Surveys");
        waitForText(textLink);
        clickAndWait(Locator.linkContainingText(textLink));
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProjectAdminProperties()
    {
        log("Populate the Request Category dashboard lookup table");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        Object[] categoriesToInsert = checkForValuesToInsert("RequestCategory", "Category", NWBT_REQUEST_CATEGORIES);
        List<Map<String,Object>> rows = new ArrayList<Map<String, Object>>();
        for (Object category : categoriesToInsert)
        {
            Map<String, Object> rowMap = new HashMap<String, Object>();
            rowMap.put("Category", category.toString());
            rowMap.put("SortOrder", Arrays.asList(NWBT_REQUEST_CATEGORIES).indexOf(category.toString()) + 1);
            rows.add(rowMap);
        }
        insertLookupTableRecords("RequestCategory", rows);

        log("Populate the Request Status dashboard lookup table");
        Object[] statusesToInsert = checkForValuesToInsert("RequestStatus", "Status", NWBT_REQUEST_STATUSES);
        rows = new ArrayList<Map<String, Object>>();
        for (Object status : statusesToInsert)
        {
            Map<String, Object> rowMap = new HashMap<String, Object>();
            rowMap.put("Status", status.toString());
            rowMap.put("SortOrder", ((NwbtRequestStatuses)status).sortOrder());
            rows.add(rowMap);
        }
        insertLookupTableRecords("RequestStatus", rows);

        log("Populate the Document Types lookup table");
        Object[] docTypesToInsert = checkForValuesToInsert("DocumentTypes", "Name", NWBT_DOCUMENT_TYPES);
        rows = new ArrayList<Map<String, Object>>();
        for (Object docType : docTypesToInsert)
        {
            int index = Arrays.asList(NWBT_DOCUMENT_TYPES).indexOf(docType);
            Map<String, Object> rowMap = new HashMap<String, Object>();
            rowMap.put("Name", docType.toString());
            // set the first doc type as not allowing multiple uploads
            rowMap.put("AllowMultipleUpload", NWBT_DOCUMENT_TYPE_FLAGS[index][0]);
            rowMap.put("Expiration", NWBT_DOCUMENT_TYPE_FLAGS[index][1]);
            rows.add(rowMap);
        }
        insertLookupTableRecords("DocumentTypes", rows);
    }

    private void insertLookupTableRecords(String queryName, List<Map<String,Object>> rowsMap)
    {
        if (!rowsMap.isEmpty())
        {
            log("Inserting values into the lookup table via InsertRows API");
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
            InsertRowsCommand insertCommand = new InsertRowsCommand("biotrust", queryName);
            insertCommand.setRows(rowsMap);
            try
            {
                SaveRowsResponse saveResp = insertCommand.execute(cn, getProjectName());
                Assert.assertEquals("Problem inserting records", saveResp.getRowsAffected(), (long)rowsMap.size());
            }
            catch (Exception e)
            {
                Assert.fail(e.getMessage());
            }
        }
    }

    private Object[] checkForValuesToInsert(String queryName, String colName, Object[] values)
    {
        List<Object> valuesToInsert = new ArrayList<Object>(Arrays.asList(values));

        log("Checking for values to be inserted via SelectRows API");
        Filter filter = new Filter(colName, StringUtils.join(values, ";"), Filter.Operator.IN);
        SelectRowsResponse response = executeSelectRowCommand("biotrust", queryName, ContainerFilter.Current, "/" + getProjectName(), Collections.singletonList(filter));
        List<String> col = new ArrayList<String>();
        for (Map<String, Object> row : response.getRows())
        {
            col.add(row.get(colName).toString());
        }

        Iterator<Object> it = valuesToInsert.iterator();
        while (it.hasNext())
        {
            Object item = it.next();
            if (col.contains(item.toString()))
                it.remove();
        }

        return valuesToInsert.toArray();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyFolderTypes()
    {
        log("Verify folder type default webparts");
        goToProjectHome();
        verifyFolderTabLinks(new String[]{"Overview", "New Registrations", "Active Sample Requests", "Patient ID", "Patient Consent", "Sample Orders", "Manage"});
        verifyWebpartTitleOrder(new String[]{"Pending Registrations"});
        clickFolder(requestorFolder1);
        verifyWebpartTitleOrder(new String[]{"Overview"});
        verifyFolderTabLinks(new String[]{"Overview", "Study Registrations", "Sample Requests", "Contacts"});
        clickFolder(requestorFolder2);
        verifyWebpartTitleOrder(new String[]{"Overview"});
    }

    private void verifyFolderTabLinks(String[] folders)
    {
        for (String folder : folders)
            assertElementPresent(Locator.linkWithText(folder));
    }

    private void verifyWebpartTitleOrder(String[] titles)
    {
        Locator titleLoc = Locator.css(".labkey-wp-title-text");
        Iterator<WebElement> it = titleLoc.findElements(_driver).iterator();
        WebElement curEl = it.next();
        for (String expectedTitle : titles)
        {
            while (!curEl.getText().equals(expectedTitle))
            {
                if (it.hasNext())
                    curEl = it.next();
                else
                {
                    assertElementPresent(titleLoc.withText(expectedTitle));
                    org.junit.Assert.fail("Webpart found out of order: " + expectedTitle);
                }
            }
        }
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupUsersAndPermissions()
    {
        log("Create all of the users for this test");
        for (String user : NWBT_USERS)
            createUser(user, null);

        log("Grant the appropriate permissions for each of these users at the project level");
        goToProjectHome();
        enterPermissionsUI();
        setUserPermissions(NWBT_STUDY_CONTACT, "Reader");
        setUserPermissions(NWBT_PRINCIPAL_INVESTIGATOR, "Reader");
        setUserPermissions(NWBT_FACULTY_CHAIR, "Reader");
        setUserPermissions(NWBT_FACULTY_REVIEWER, "Reader");
        clickButton("Save and Finish");
        addUserToProjGroup(NWBT_RESEARCH_COORD, getProjectName(), NWBT_RESEARCH_COORD_GROUP);
        addUserToProjGroup(NWBT_SURGICAL_REVIEWER, getProjectName(), NWBT_SURGICAL_REVIEWER_GROUP);
        addUserToProjGroup(NWBT_NON_SURGICAL_REVIEWER, getProjectName(), NWBT_NON_SURGICAL_REVIEWER_GROUP);
        addUserToProjGroup(NWBT_DISCARDED_REVIEWER, getProjectName(), NWBT_DISCARDED_REVIEWER_GROUP);

        log("Grant the appropriate permissions for 1st requestor subfolder");
        //note: don't give them perm to the 2nd requestor folder so that we can test the container permissions
        clickFolder(requestorFolder1);
        enterPermissionsUI();
        setUserPermissions(NWBT_RESEARCH_COORD, "NWBT Research Coordinator");
        setUserPermissions(NWBT_STUDY_CONTACT, "NWBT Additional Study Contact");
        setUserPermissions(NWBT_PRINCIPAL_INVESTIGATOR, "NWBT Principal Investigator");
        setUserPermissions(NWBT_FACULTY_CHAIR, "NWBT Faculty Chair");
        setUserPermissions(NWBT_FACULTY_REVIEWER, "NWBT Faculty Reviewer");
        clickButton("Save and Finish");
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupResearchCoordAndRequstorFolders()
    {
        log("Create project folder and requestor subfolders");
        // use the project folder as the Research Coordinator folder, this will enable the survey and biotrust modules
        _containerHelper.createProject(getProjectName(), "NW BioTrust Research Coordinator");
        // create two requestor folders for this project
        _containerHelper.createSubfolder(getProjectName(), requestorFolder1, "NW BioTrust Specimen Requestor");
        _containerHelper.createSubfolder(getProjectName(), requestorFolder2, "NW BioTrust Specimen Requestor");

        // set up the rc email notification
        List<ModulePropertyValue> properties = new ArrayList<ModulePropertyValue>();
        properties.add(new ModulePropertyValue("BioTrust", "/", "RC email address", NWBT_RC_EMAIL));

        setModuleProperties(properties);
    }

    private void deleteDashboardLookupRows(String tableName, String filterColName, String[] valuesToBeDeleted)
    {
        log("Deleting values from lookup table via DeleteRows API");
        Filter filter = new Filter(filterColName, StringUtils.join(valuesToBeDeleted, ";"), Filter.Operator.IN);
        SelectRowsResponse response = executeSelectRowCommand("biotrust", tableName, ContainerFilter.Current, "/" + getProjectName(), Collections.singletonList(filter));
        if (response.getRows().size() > 0)
        {
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
            DeleteRowsCommand deleteCommand = new DeleteRowsCommand("biotrust", tableName);
            deleteCommand.setRows(response.getRows());
            try
            {
                SaveRowsResponse saveResp = deleteCommand.execute(cn, getProjectName());
                Assert.assertEquals("Problem deleting records", saveResp.getRowsAffected(), (long)response.getRows().size());
            }
            catch (Exception e)
            {
               Assert.fail(e.getMessage());
            }
        }
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void configureDesigns()
    {
        log("download survey metadata");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        waitForText("Metadata for Study Registration and Sample Requests");
        click(Locator.linkWithText("Study Registration"));
        downloadFileFromLink(Locator.linkWithText("Prospective Sample Request"));
        downloadFileFromLink(Locator.linkWithText("Discarded Sample Request"));
        downloadFileFromLink(Locator.linkWithText("Surgical Tissue Record"));
        downloadFileFromLink(Locator.linkWithText("Non-surgical Tissue Record"));
        downloadFileFromLink(Locator.linkWithText("Blood Sample Record"));
        downloadFileFromLink(Locator.linkWithText("Discarded Blood Sample Record"));

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return studyRegistrationJson.exists();
            }
        }, "failed to download study registration json", WAIT_FOR_JAVASCRIPT);

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return prospectiveSampleRequestJson.exists();
            }
        }, "failed to download prospective sample request json", WAIT_FOR_JAVASCRIPT);

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return discardedSampleRequestJson.exists();
            }
        }, "failed to download discarded blood sample request json", WAIT_FOR_JAVASCRIPT);

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return surgicalTissueJson.exists();
            }
        }, "failed to download surgical tissue request json", WAIT_FOR_JAVASCRIPT);

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return nonSurgicalTissueJson.exists();
            }
        }, "failed to download non-surgical tissue request json", WAIT_FOR_JAVASCRIPT);

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return bloodSampleJson.exists();
            }
        }, "failed to download blood sample request json", WAIT_FOR_JAVASCRIPT);

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return discardedBloodSampleJson.exists();
            }
        }, "failed to download discarded blood sample request json", WAIT_FOR_JAVASCRIPT);

        Map<String, String> design = new HashMap<String, String>();
        design.put("label", "NW BioTrust Study Registration");
        design.put("description", "Create a new study registration with associated sample requests");
        design.put("table", "StudyRegistrations");
        design.put("metadataPath", studyRegistrationJson.getAbsolutePath());
        designs.add(design);

        design = new HashMap<String, String>();
        design.put("label", "ProspectiveSampleRequest");
        design.put("description", "");
        design.put("table", "SampleRequests");
        design.put("metadataPath", prospectiveSampleRequestJson.getAbsolutePath());
        designs.add(design);

        design = new HashMap<String, String>();
        design.put("label", "DiscardedBloodSampleRequest");
        design.put("description", "");
        design.put("table", "SampleRequests");
        design.put("metadataPath", discardedSampleRequestJson.getAbsolutePath());
        designs.add(design);

        design = new HashMap<String, String>();
        design.put("label", "SurgicalTissueSample");
        design.put("description", "");
        design.put("table", "TissueRecords");
        design.put("metadataPath", surgicalTissueJson.getAbsolutePath());
        designs.add(design);

        design = new HashMap<String, String>();
        design.put("label", "NonSurgicalTissueSample");
        design.put("description", "");
        design.put("table", "TissueRecords");
        design.put("metadataPath", nonSurgicalTissueJson.getAbsolutePath());
        designs.add(design);

        design = new HashMap<String, String>();
        design.put("label", "BloodSample");
        design.put("description", "");
        design.put("table", "TissueRecords");
        design.put("metadataPath", bloodSampleJson.getAbsolutePath());
        designs.add(design);

        design = new HashMap<String, String>();
        design.put("label", "DiscardedBloodSample");
        design.put("description", "");
        design.put("table", "TissueRecords");
        design.put("metadataPath", discardedBloodSampleJson.getAbsolutePath());
        designs.add(design);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        goToHome();
        if(isElementPresent(Locator.linkWithText(getProjectName())))
        {
            deleteSurveyDesign();
        }
        deleteUsers(afterTest, NWBT_USERS);
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/biotrust";
    }
}
