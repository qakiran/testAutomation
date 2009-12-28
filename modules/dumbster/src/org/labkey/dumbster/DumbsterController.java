/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.dumbster;

import org.apache.log4j.Logger;
import org.labkey.api.action.*;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.settings.WriteableAppProps;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.template.PageConfig;
import org.labkey.dumbster.model.DumbsterManager;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.labkey.dumbster.view.MailWebPart;

/**
 * View action is like MultiActionController, but each action is a class not a method
 */
public class DumbsterController extends SpringActionController
{
    static Logger _log = Logger.getLogger(DumbsterController.class);

    static DefaultActionResolver _actionResolver = new DefaultActionResolver(DumbsterController.class);

    public DumbsterController()
    {
        super();
        setActionResolver(_actionResolver);
    }

    public PageConfig defaultPageConfig()
    {
        return new PageConfig();
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new MailWebPart();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    @RequiresPermissionClass(AdminPermission.class)
    public class SetRecordEmailAction extends ApiAction<RecordEmailForm>
    {
        public ApiResponse execute(RecordEmailForm form, BindException errors) throws Exception
        {
            if (form.isRecord())
            {
                if (!DumbsterManager.get().start())
                    return new ApiSimpleResponse("error", "Error starting mail recorder.  Check log for more information.");
            }
            else
            {
                DumbsterManager.get().stop();
            }

            WriteableAppProps props = AppProps.getWriteableInstance();
            props.setMailRecorderEnabled(form.isRecord());
            props.save();
            return null;
        }
    }

    public static class RecordEmailForm
    {
        private boolean _record;

        public boolean isRecord()
        {
            return _record;
        }

        public void setRecord(boolean record)
        {
            _record = record;
        }
    }
}