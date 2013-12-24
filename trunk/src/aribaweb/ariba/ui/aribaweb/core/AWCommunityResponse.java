/*
    Copyright (c) 1996-2007 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWCommunityResponse.java#1 $

*/

package ariba.ui.aribaweb.core;

public class AWCommunityResponse extends AWComponent {

    private String iframeName;
    private String display;
    private String className;

    public String iframeName() {
        return iframeName;
    }

    public void setIframeName(String iframeName) {
        this.iframeName = iframeName;
    }

    public String display () {
        return display;
    }

     public void setDisplay (String display) {
        this.display = display;
     }


    public String className () {
        return className;
    }

    public void setClassName (String className) {
        this.className = className;
    }
}
