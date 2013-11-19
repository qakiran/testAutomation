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
package org.labkey.test.util;

import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateUserCommand;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.BaseSeleniumWebTest;

import static org.junit.Assert.*;

/**
 * User: elvan
 * Date: 9/12/12
 * Time: 11:52 AM
 */
public class APIUserHelper extends AbstractUserHelper
{
    public APIUserHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }


    public void createUser(String userName, boolean verifySuccess)
    {

            CreateUserCommand command = new CreateUserCommand(userName);
            Connection connection = _test.getDefaultConnection();
            try
            {
                CreateUserResponse response = command.execute(connection, "");

                if (verifySuccess)
                {
                    assertEquals(userName, response.getEmail());
                    assertTrue("Invalid userId", response.getUserId() != null);
                }
            }
            catch (Exception e)
            {
                if(verifySuccess)
                    fail("Error while creating user: " + e.getMessage());
            }
    }
}
