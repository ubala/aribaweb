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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/PunchoutUtil.java#2 $
*/

package ariba.ui.demoshell;

import org.w3c.dom.Document;
import java.io.ByteArrayInputStream;
import java.util.Map;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWDirectActionUrl;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWFormRedirect;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.core.Assert;
import ariba.util.core.MapUtil;
import ariba.util.core.ThreadDebugState;

public class PunchoutUtil extends AWDirectAction
{
    private static final String PunchInDoc = "PunchInDoc";
    private static final String POSessionID = "POSessionID";
    protected static Map _PunchInMap = MapUtil.map();
    protected static int _poSeqNum=0;

    protected boolean shouldValidateSession ()
    {
        return false;
    }

    public AWResponseGenerating setupAction ()
    {
        // stash request DOM in session and send them back here to dispatch to demo page
        ByteArrayInputStream is = new ByteArrayInputStream(request().content());
        Document doc = XMLFactory.documentFromStream (is);

        String poSID = Integer.toString(_poSeqNum++);
        _PunchInMap.put(poSID, doc);

        AWXPunchOutSetupResponse posr = (AWXPunchOutSetupResponse)pageWithName(AWXPunchOutSetupResponse.class.getName());
        posr.returnURL = AWDirectActionUrl.fullUrlForDirectAction("start/PunchoutUtil", requestContext(),
                POSessionID, poSID);

        return posr;
    }

    public AWResponseGenerating startAction ()
    {
        // get state back from stashed POSR
        String poSID = request().formValueForKey(POSessionID);
        Document doc = (Document)_PunchInMap.get(poSID);
        _PunchInMap.remove(poSID);

        // Store in session for later
        session().dict().put(PunchInDoc, doc);

        String partId = (String)FieldValue.getFieldValue(doc, "Request.PunchOutSetupRequest.SelectedItem.ItemID.SupplierPartID.text");
        String auxId = (String)FieldValue.getFieldValue(doc, "Request.PunchOutSetupRequest.SelectedItem.ItemID.SupplierPartAuxiliaryID.text");
        Assert.that((partId!=null), "Missing SupplierPartID in request: %s", doc);
        Assert.that((auxId!=null), "Missing SupplierPartAuxilaryID in request: %s", doc);

        // use AuxId as name of component to return
        AWComponent page = pageWithName(auxId);
        Assert.that((page!=null), "Couldn't find page matching auxId: %s", auxId);

        return page;
    }

    public AWResponseGenerating checkoutAction ()
    {
        AWXPunchOutOrderMessage poom = (AWXPunchOutOrderMessage)pageWithName(AWXPunchOutOrderMessage.class.getName());
        poom.quantity = request().formValueForKey("quantity");
        poom.price = request().formValueForKey("price");
        poom.mfgName = request().formValueForKey("mfgName");
        poom.mfgPartId = request().formValueForKey("mfgPartId");
        return returnFromPunchout(poom, requestContext());
    }

    public static AWResponseGenerating returnFromPunchout (AWXPunchOutOrderMessage poom, AWRequestContext requestContext)
    {
        // get state back from stash POSR
        Document doc = (Document)requestContext.session().dict().get(PunchInDoc);

        // Create PunchoutOrderMessage from AW template
        poom.posr = doc;
        String poomString = null;
        try {
            poomString = poom.generateStringContents();
        } catch (Exception e) {
            System.out.println("*** Exception: " + e.toString() + " --> " + ThreadDebugState.getUnsafeThreadDebugState());
            e.printStackTrace();
        }

        String returnURL = (String)FieldValue.getFieldValue(doc, "Request.PunchOutSetupRequest.BrowserFormPost.URL.text");
        Assert.that((returnURL!=null), "Missing BrowserFormPost.URL in request: %s", doc);

        // use Form Redirect to return it
        AWFormRedirect formRedirect =
            (AWFormRedirect)requestContext.createPageWithName(AWFormRedirect.PageName);
        formRedirect.setFormActionUrl(returnURL);
        formRedirect.addFormValue("cxml-urlencoded", poomString);

        return formRedirect;
    }
}

/*
NOTE:

To prep a saved supplier web page:
    - Remove script blocks
    - remove on* handlers via regexp:
        \son(\w)+=\"[^\"]*\"
*/