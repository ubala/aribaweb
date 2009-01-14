/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWRecordDirectAction.java#14 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.StringUtil;


public final class AWRecordDirectAction extends AWDirectAction
{
    public final static String StartActionName = "start/AWRecordDirectAction";
    public final static String StopActionName = "stop/AWRecordDirectAction";
    public final static String StartRecordActionName = "startRecord/AWRecordDirectAction";
    public final static String StopRecordActionName = "stopRecord/AWRecordDirectAction";
    public final static String StartPlaybackActionName = "startPlayback/AWRecordDirectAction";
    public final static String StopPlaybackActionName = "stopPlayback/AWRecordDirectAction";
    public final static String RecordingPathKey = "recordingPath";

    protected boolean shouldValidateSession ()
    {
        // disable automatic session validation for this class
        // enable on a per action method as necessary
        return false;
    }

    public AWResponseGenerating startAction ()
    {
        return startRecordAction();
    }

    public AWResponseGenerating startRecordAction ()
    {
        AWRequest request = request();
        String recordingPath = request.formValueForKey(RecordingPathKey);
        AWResponse response = redirectResponse();
        AWRecordingManager.getInstance().startRecording(requestContext(), response, recordingPath);
        return response;
    }

    public AWResponseGenerating stopAction ()
    {
        return stopRecordAction();
    }

    public AWResponseGenerating stopRecordAction ()
    {
        AWResponse response = redirectResponse();
        AWRecordingManager.getInstance().stopRecording(requestContext(), response);
        return response;
    }

    private AWResponse redirectResponse() {
        AWRequestContext requestContext = requestContext();
        AWRedirect redirectComponent = (AWRedirect)application().createPageWithName(AWRedirect.PageName, requestContext);
        String url = AWDirectActionUrl.defaultAppUrl(requestContext);
        redirectComponent.setUrl(url);
        redirectComponent.setSelfRedirect(true);
        AWResponse response = redirectComponent.generateResponse();
        return response;
    }

    public AWResponseGenerating startPlaybackAction ()
    {
        AWRequest request = request();
        String recordingPath = request.formValueForKey(RecordingPathKey);
        AWResponse response = application().createResponse();
        AWRecordingManager.startPlayback(requestContext(), response,
                                         recordingPath);
        response.appendContent("playback mode on");
        return response;
    }

    public AWResponseGenerating stopPlaybackAction ()
    {
        AWResponse response = application().createResponse();
        AWRecordingManager.stopPlayback(requestContext(), response);
        response.appendContent("playback mode off");
        return response;
    }

}


