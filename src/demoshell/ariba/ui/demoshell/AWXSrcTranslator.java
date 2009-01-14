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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXSrcTranslator.java#4 $
*/

package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWBaseRequest;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.FastStringBuffer;

import java.io.File;


public class AWXSrcTranslator extends AWComponent
{
    protected static final String[] SupportedBindingNames = {AWBindingNames.src};
    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    // to be overridden by subclasses to determine which binding we translate.  Default is 'SRC"
    protected String tranlatedAttributeName ()
    {
        return supportedBindingNames()[0];
    }

    public String translatedPath ()
    {
        String _srcString = stringValueForBinding(tranlatedAttributeName());
        if (_srcString == null) {
            _srcString = stringValueForBinding(tranlatedAttributeName().toUpperCase());
        }
        if (_srcString != null) {
            if (_srcString.indexOf('%') != -1) {
                _srcString = AWBaseRequest.decodeString(_srcString, new FastStringBuffer());
            }
            if (_srcString.startsWith("http://") || _srcString.startsWith("https://")) {
                // nothing -- already absolute
            }
            else if (_srcString.startsWith("/")) {
                String siteRoot = AWXHTMLComponentFactory.sharedInstance().siteRootUrl();
                _srcString = siteRoot + _srcString;
            }
            else {
                 // relative URL.  Make relative to the parent template.  For now, use file:// URLs
                // (should later take into account the webserver and the path from there to the file.
                String parentDirectory = AWXHTMLComponentFactory.sharedInstance().directoryForParent(this);
                File file = new File(parentDirectory, _srcString);
                _srcString = AWXHTMLComponentFactory.sharedInstance().siteRelativeUrlForFile(file);
            }
        }
        else {
            _srcString = null; // "/No_SRC_attribute_specified";
        }
        return _srcString; // AWEncodedString.sharedEncodedString(_srcString);
    }

    /*
    protected static final String NoSrcString = "NoSrcString";
    private String _filename;
    private String _srcString;
    private AWEncodedString _imageUrl;


    protected void awake ()
    {
        super.awake();
        _srcString = NoSrcString;
        _filename = NoFilename;
        _imageUrl = null;
    }

    private void ensureSrcProcessed ()
    {
        if (_srcString == NoSrcString) {
            _srcString = stringValueForBinding(AWBindingNames.src);
            if (_srcString == null) {
                _srcString = stringValueForBinding("SRC");
            }
            if (_srcString != null) {
                if (_srcString.indexOf('%') != -1) {
                    _srcString = CGI.decodeString(_srcString);
                }
                if (_srcString.startsWith("http://") || _srcString.startsWith("https://")) {
                    //
                }
                else if (_srcString.startsWith("/")) {
                    String personalityUrlPrefix = ((ANSession)session()).personality().urlPrefix();
                    // XXX clloyd: may need to convert this to path without ".." etc.
                    _srcString = AWCat.strcat(personalityUrlPrefix, _srcString);
                }
                else {
                        // set the _filename so that we compute height & width
                    _filename = _srcString;
                    _imageUrl = imageUrl(_srcString);
                    return;
                }
            }
            else {
                _srcString = "/No_SRC_attribute_specified";
            }
        }
        _imageUrl = AWEncodedString.sharedEncodedString(_srcString);
        return;
    }

    public AWEncodedString imageUrl ()
    {
        ensureSrcProcessed();
        return _imageUrl;
    }

    protected String filename ()
    {
        ensureSrcProcessed();
        if (_filename == NoFilename) {
            return super.filename();
        }
        return _filename;
    }

*/
}
