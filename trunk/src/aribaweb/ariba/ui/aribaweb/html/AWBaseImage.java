/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWBaseImage.java#31 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWDirectActionUrl;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWImageInfo;
import ariba.ui.aribaweb.util.AWNodeManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.util.AWBrand;

/** @aribaapi private */

abstract public class AWBaseImage extends AWComponent
{
    public static final String[] SupportedBindingNames = {
        BindingNames.src,
        BindingNames.filename,
        BindingNames.border,
        BindingNames.width,
        BindingNames.height,
        BindingNames.alt,
        BindingNames.title,
        "fullUrl"
    };

    private static final AWEncodedString AutoSizeString = new AWEncodedString("-1");
    private static final String NoFilename = "NoFilename";
    private static final AWImageInfo NoImageInfo = new AWImageInfo(null);
    private AWImageInfo _imageInfo = NoImageInfo;
    private String _filename;
    // Bindings
    public AWBinding _srcBinding;
    public AWBinding _filenameBinding;
    public AWBinding _widthBinding;
    public AWBinding _heightBinding;
    public AWBinding _altBinding;
    public AWBinding _titleBinding;
    public AWBinding _borderBinding;
    public AWBinding _fullUrlBinding;

    public String[] supportedBindingNames ()
    {
        return AWBaseImage.SupportedBindingNames;
    }

    protected boolean useLocalPool ()
    {
        return true;
    }

    public static String imageUrl (AWRequestContext requestContext, AWComponent component,
                                   String filename)
    {
        // Use full URL by default so URLs work when served into remote dashboard content
        return imageUrl(requestContext, component, filename, true);
    }

    public static String imageUrl (AWRequestContext requestContext, AWComponent component,
                                   String filename, boolean useFullUrl)
    {
        String imageUrl = null;
        AWConcreteApplication application =
            (AWConcreteApplication)AWConcreteApplication.sharedInstance();

        AWBrand brand = null;
        if (application.allowBrandingImages() &&
            (brand = requestContext.application().getBrand(requestContext)) != null) {

            imageUrl = AWDirectAction.brandUrlForResourceNamed(requestContext,
                                                                   filename,
                                                                   brand);
            if (useFullUrl) {
                AWNodeManager nodeManager = application.getNodeManager();
                if (nodeManager != null) {
                    imageUrl = nodeManager.prepareUrlForNodeValidation(imageUrl);
                }
            }

            Log.aribawebResource_brand.debug("AWBaseImage: imageUrl() directConnect %s", 
                imageUrl);
        }
        else if (AWConcreteApplication.IsRapidTurnaroundEnabled) {
            // The way we're constructing the URL is a bit messy.
            // If we need a full URL, use checkoutFullUrl in order to contruct the
            // full URL and pass requestContext as is required to create full URL.
            // But, do not set set AWRequestContext on the AWDirectActionUrl as doing
            // this will cause an response id to be added to the URL.  This can lead
            // to our image URL's causing FPR's.
            // Also, explicitly constructing the URL prevents URL decorators
            // from being used.
            AWDirectActionUrl url = useFullUrl ?
                        AWDirectActionUrl.checkoutFullUrl(requestContext) :
                        AWDirectActionUrl.checkoutUrl();
            url.setDirectActionName(AWDirectAction.AWImgActionName);
            url.put("name", filename);
            imageUrl = url.finishUrl();
        }
        else if (component != null) {
            //Set versioned to true if this is a UI request. If this is used for 
            //generating email content, the request will be null.
            imageUrl = component.urlForResourceNamed(filename, useFullUrl, 
                    requestContext.request() != null);
            Log.aribawebResource_brand.debug(
                "AWBaseImage: imageUrl() component.urlForResourceNamed %s", imageUrl);
        }

        return imageUrl;
    }

    // ** Thread Safety Considerations: see AWComponent.

    protected void awake ()
    {
        _imageInfo = NoImageInfo;
        _filename = NoFilename;
    }

    protected void sleep ()
    {
        _imageInfo = null;
        _filename = null;
    }

    protected String filename ()
    {
        // fkolar
        // Please keep this == as it is and do not change it.
        // We need to compare the references!
        if (_filename == NoFilename) {
            _filename = initFilename();
        }
        return _filename;
    }

    protected String initFilename ()
    {
        return stringValueForBinding(_filenameBinding);
    }

    protected AWImageInfo imageInfo (String filename)
    {
        if (_imageInfo == NoImageInfo) {
            if (filename == null) {
                _imageInfo = null;
            }
            else {
                _imageInfo = resourceManager().imageInfoForName(filename);
            }
        }
        return _imageInfo;
    }

    protected AWEncodedString imageUrl (String filename)
    {
        AWEncodedString imageUrl = null;
        if (filename == null) {
            imageUrl = encodedStringValueForBinding(_srcBinding);
            Log.aribawebResource_brand.debug("AWBaseImage: filename %s", imageUrl);
        }
        else {
            AWConcreteApplication application =
                (AWConcreteApplication)AWConcreteApplication.sharedInstance();

            if (application.allowBrandingImages()) {
                String urlString = imageUrl(requestContext(), this, filename, useFullUrl());
                imageUrl = AWEncodedString.sharedEncodedString(urlString);
            }
            else {
                AWImageInfo imageInfo = imageInfo(filename);
                if (imageInfo == null) {
                    String urlString = AWUtil.formatErrorUrl(filename);
                    imageUrl = AWEncodedString.sharedEncodedString(urlString);
                }
                else {
                    // Todo: remove MetaTemplateMode
                    boolean useFullUrl = useFullUrl() || requestContext().isMetaTemplateMode();                      
                    String url = urlForResourceNamed(filename, useFullUrl, true);
                    imageUrl = AWEncodedString.sharedEncodedString(url);
                    Log.aribawebResource_brand.debug("AWBaseImage: imageInfo.url %s", imageUrl);
                }
            }
        }
        return imageUrl;
    }

    public AWEncodedString imageUrl ()
    {
        String filename = filename();
        return imageUrl(filename);
    }

    public AWEncodedString width ()
    {
        AWEncodedString width = null;
        if (_widthBinding == null) {
            String filename = filename();
            AWImageInfo imageInfo = imageInfo(filename);
            if (imageInfo != null) {
                width = imageInfo.widthString;
            }
        }
        else {
            width = encodedStringValueForBinding(_widthBinding);
            if (width != null && width.equals(AutoSizeString)) {
                width = null;
            }
        }
        return width;
    }

    public AWEncodedString height ()
    {
        AWEncodedString height = null;
        if (_heightBinding == null) {
            String filename = filename();
            AWImageInfo imageInfo = imageInfo(filename);
            if (imageInfo != null) {
                height = imageInfo.heightString;
            }
        }
        else {
            height = encodedStringValueForBinding(_heightBinding);
            if (height != null && height.equals(AutoSizeString)) {
                height = null;
            }
        }
        return height;
    }

    public String alt ()
    {
        String alt = stringValueForBinding(_altBinding);
        if (alt == null) {
            alt = stringValueForBinding(_titleBinding);
        }
        return alt == null ? "" : alt;
    }

    private boolean useFullUrl ()
    {
        if (_fullUrlBinding == null) {
            return false;
        }
        else {
            return booleanValueForBinding(_fullUrlBinding);
        }
    }
}
