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

    $Id: //ariba/platform/util/core/ariba/util/io/TagHandler.java#5 $
*/

package ariba.util.io;

import java.util.List;

/**
    The TagHandler interface is used by the TaggedStringProcessor class.  When
    a tagged string is being processed, i.e. one that has special tags embedded
    in it that are meant to be replaced at runtime, the tagged string processor
    uses an instance of a TagHandler to process each tag that it finds in the
    given string.  It is up to the implementer of this interface to interpret
    the tag string passed to it by the TemplateParser.

    @see TaggedStringProcessor

    @aribaapi private
*/
public interface TagHandler
{
    /**
        Returns the string that should be substituted for the given tag.
    */
    public String stringForTag (String tag, List arguments);
}
