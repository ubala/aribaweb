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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWResponseGenerating.java#7 $
*/

package ariba.ui.aribaweb.core;

/**
    Interface for results of an action.  The most common action result is {@link AWComponent}.
    Actions may also return {@link AWResponse} objects (when, for instance, returning a file).
 
    @aribaapi private
 */

public interface AWResponseGenerating
{
    public AWResponse generateResponse ();
    public String generateStringContents ();

    public interface ResponseSubstitution {
        public AWResponseGenerating replacementResponse();
    }
}
