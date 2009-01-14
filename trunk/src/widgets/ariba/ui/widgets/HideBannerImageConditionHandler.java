package ariba.ui.widgets;

import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWBrand;
import ariba.ui.aribaweb.util.AWImageInfo;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWChecksum;
import ariba.ui.aribaweb.core.AWSession;

import java.io.InputStream;

/**
     Prod Image Branded | Banner Image Branded | Prod Image Wide | Display
     N	                  N	                     x	               Normal
     N	                  Y	                     x                 Normal
     Y	                  N	                     N         	       Hide banner image
     Y	                  N	                     Y	               Hide banner image, display prod image in banner area
     Y	                  Y	                     x                 Normal
*/
public class HideBannerImageConditionHandler extends ConditionHandler
{

    private static long ProductImageCRC = 0;
    private static long BannerImageCRC = 0;

    public boolean evaluateCondition (AWRequestContext requestContext)
    {
        AWImageInfo imageInfo =
            productImageBrandedOnly(requestContext);
        return imageInfo != null;
    }

    /**
         Returns the product image's info if the product image is branded AND
         the banner image is not branded
     */
    public static AWImageInfo productImageBrandedOnly (AWRequestContext requestContext)
    {
        setupSystemImagesCRC();
        AWConcreteApplication application =
                (AWConcreteApplication)requestContext.application();
        if (application.allowBrandingImages()) {
            AWBrand brand =
                    application.getBrand(requestContext);
            if (brand != null) {
                AWResourceManager rm = application.resourceManager();
                String brandVersion = brand.getSessionVersion(requestContext);
                rm = rm.resolveBrand(brand.getName(), brandVersion);
                boolean isProductImageBranded =
                    isImageBranded("cmdbar_prod.gif", ProductImageCRC, rm, requestContext, brandVersion);
                if (isProductImageBranded) {
                    boolean isBannerImageBranded =
                        isImageBranded("cmdbar_banner.gif", BannerImageCRC, rm, requestContext, brandVersion);
                    if (!isBannerImageBranded) {
                        return rm.imageInfoForName("cmdbar_prod.gif");
                    }
                }
            }
        }
        return null;
    }

    private static boolean isImageBranded (String imageName, long systemImageCRC,
                                           AWResourceManager rm, AWRequestContext requestContext,
                                           String brandVersion)
    {
        String key = brandVersion + imageName;
        Boolean isImagedBranded = null;
        AWSession session = requestContext.session(false);
        if (session != null) {
            isImagedBranded = (Boolean)session.dict().get(key);
        }
        if (isImagedBranded == null) {
            AWResource resource = rm.resourceNamed(imageName, true);
            long crc = imageCRC(resource);
            isImagedBranded = Boolean.valueOf(crc != systemImageCRC);
            if (session != null) {
                session.dict().put(key, isImagedBranded);                
            }
        }
        return isImagedBranded.booleanValue();
    }

    private static long imageCRC (AWResource resource)
    {
        InputStream inputStream = resource.inputStream();
        byte[] bytes = AWUtil.getBytes(inputStream);
        return AWChecksum.crc32(1L, bytes, bytes.length);
    }

    private static void setupSystemImagesCRC ()
    {
        if (ProductImageCRC == 0) {
            AWResourceManager rm = AWConcreteApplication.SharedInstance.resourceManager();
            AWResource resource = rm.resourceNamed("cmdbar_prod.gif");
            ProductImageCRC = imageCRC(resource);
            resource = rm.resourceNamed("cmdbar_banner.gif");
            BannerImageCRC = imageCRC(resource);
        }
    }

}
