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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;

import java.io.File;
import java.io.FilenameFilter;

/**
 * User: elvan
 * Date: 1/11/12
 * Time: 7:04 PM
 */
public class ListExportTest extends ListTest
{
    String downloadDirectory = "";
    @Override
    protected String getProjectName()
    {
        return "List Download Test";
    }

    @Override
    protected void doTestSteps()
    {
        downloadDirectory = System.getenv("DL_DIR");

        setUpList(getProjectName());
        String filter = ".*Colors.*\\.xlsx";
        int fileCount = getNumFilesWithNameInDlDir(downloadDirectory, filter);
        exportList();
        sleep(1000);
        File[] fileAfterDownload =  getFilesWithNameInDlDir(downloadDirectory, filter);
        Assert.assertEquals(fileCount+1, fileAfterDownload.length);

        File exportedFile = fileAfterDownload[fileAfterDownload.length-1];
        exportedFile.deleteOnExit();
    }

    private File[] getFilesWithNameInDlDir(String dir, String fileRegEx)
    {
        File folder = new File(dir);
        File[] list = folder.listFiles(new FilterOnName((fileRegEx)));
        return list;

    }
    private int getNumFilesWithNameInDlDir(String dir, String fileRegEx)
    {
        return getFilesWithNameInDlDir(dir, fileRegEx).length;
    }

    private void exportList()
    {
        clickButton("Export", 0);
        click(Locator.name("excelExportType", 1));
        clickButton("Export to Excel", 0);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
       deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/list";
    }

    private class FilterOnName implements FilenameFilter
    {
        protected String regex = "";

        public FilterOnName(String regex)
        {
            this.regex = regex;
        }
        @Override
        public boolean accept(File dir, String name)
        {
            boolean b = name.matches(regex);
            return b;
        }
    }
}
