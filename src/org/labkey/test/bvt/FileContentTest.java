/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FileContentTest extends BaseSeleniumWebTest
{
    // Use a special exotic character in order to make sure we don't break
    // i18n. See https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=5369
    private static final String PROJECT_NAME = "File Content T�st Project";
    private static final String PROJECT_ENCODED = "File%20Content%20T%C3%A9st%20Project";

    public String getAssociatedModuleDirectory()
    {
        return "filecontent";
    }

    public boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        try {deleteFile(getTestTempDir()); } catch (Throwable t) {}
    }

    protected void doTestSteps() throws Exception
    {
        log("Create a new web part, upload file, log out and navigate to it");
        log("Note that we use a space and a non-ascii character in the project name, "+
            "so if this fails, check that tomcat's server.xml contains the following attribute " +
            "in its Connector element: URIEncoding=\"UTF-8\"");


        createProject(PROJECT_NAME);
        clickNavButton("Done");
        addWebPart("Files");

        clickLinkWithText("Configure Directories");

        File dir = getTestTempDir();
        dir.mkdirs();
        setFormElement("rootPath", dir.getAbsolutePath());
        clickNavButton("Submit");
        if (isFileUploadAvailable())
        {
            clickLinkWithText("Manage Files");

            clickLinkWithText("Upload File", false);
            selenium.waitForPopUp("uploadFiles", "30000");
            selenium.selectWindow("uploadFiles");

            String filename = "InlineFile.html";
            String sampleRoot = getLabKeyRoot() + "/sampledata/security";
            File f = new File(sampleRoot, filename);
            setFormElement("formFiles[0]", f);
            clickNavButton("Submit");

            selenium.selectWindow("null");
            assertTextPresent("Sign Out");

            signOut();

            // Test that renderAs can be observed through a login
            beginAt("files/" + encode(PROJECT_NAME) + "/" + filename + "?renderAs=INLINE");
            assertTitleEquals("Sign In");
            assertFormPresent("login");
            setText("email", PasswordUtil.getUsername());
            setText("password", PasswordUtil.getPassword());
            submit("SUBMIT");

            if (isTextPresent("Type in your email address and password"))
                fail("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");

            log("Test that page was rendered inside of server UI");

            assertTextPresent("Sign Out");
            assertTextPresent("My Account");

            assertTextPresent("antidisestablishmentarianism");
        }
    }

    private static String encode(String data) throws UnsupportedEncodingException
    {
        return URLEncoder.encode(data, "UTF-8").replace("+","%20");
    }
    

}
