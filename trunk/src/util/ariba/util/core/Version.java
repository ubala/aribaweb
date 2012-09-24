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

    $Id: //ariba/platform/util/core/ariba/util/core/Version.java#9 $
*/

package ariba.util.core;

import ariba.util.formatter.DateFormatter;
import ariba.util.formatter.IntegerFormatter;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;

/**
    This class provides access to build version information.  This information
    is stored in a resource file named BuildInfo.csv, which should exist in a
    component which is product specific.  The build system will modify this
    file with the version information from the build, and check the file back
    in.  This will allow developers to build with the same values (if they
    happen to build and install the component containing BuildInfo.csv) that
    their backdrop was compiled with.  This should allow the RPC client-server
    version matching logic to continue to work.  The BuildInfo.csv resource
    file contains build numbers and dates and is not localized in any way.

    <p>There is another resource file named ariba.util.version.csv, which is
    for the non-build related resources referenced in this class.  This
    resource file lives in this component.  It should be localized.

    <p>The 'rescheck' tool would normally report the BuildInfo resources as
    missing, however these are specifically filtered out.

    <p>NOTE: The resources from BuildInfo.csv are fetched with warnings turned
    off so that if a simple "product" (e.g. a test program) does not define
    BuildInfo.csv, things will still work, but they'll just get zero for the
    major, minor, path, and build values, and the date will be null.

    @aribaapi private
*/
public final class Version
{
    public static final String BuildStringTable = "BuildInfo";
    public static final String LocalStringTable = "ariba.util.version";

    public static final String product;
    public static final byte  major;
    public static final byte  minor;
    public static final byte  patch;
    public static final short build;
    public static final short svcpack;
    public static final String hfpack;
    public static final String langpack;
    public static final String date;
    public static final int year;
    /** Build name is only valid for shared services.  It is normally null for CD. */
    public static final String buildName;

    static
    {
        product = ResourceService.getString(BuildStringTable,
            "product", ResourceService.LocaleOfLastResort, false);

        String majorString = ResourceService.getString(BuildStringTable,
            "major", ResourceService.LocaleOfLastResort, false);
        major = (byte)IntegerFormatter.parseIntWithDefault(majorString, 0);

        String minorString = ResourceService.getString(BuildStringTable,
            "minor", ResourceService.LocaleOfLastResort, false);
        minor = (byte)IntegerFormatter.parseIntWithDefault(minorString, 0);

        String patchString = ResourceService.getString(BuildStringTable,
            "patch", ResourceService.LocaleOfLastResort, false);
        patch = (byte)IntegerFormatter.parseIntWithDefault(patchString, 0);

        String buildString = ResourceService.getString(BuildStringTable,
            "build", ResourceService.LocaleOfLastResort, false);
        build = (short)IntegerFormatter.parseIntWithDefault(buildString, 0);

        String svcpackString = ResourceService.getString(BuildStringTable,
            "svcpack", ResourceService.LocaleOfLastResort, false);
        svcpack = (short)IntegerFormatter.parseIntWithDefault(svcpackString, 0);

        hfpack = ResourceService.getString(BuildStringTable,
            "hfpack", ResourceService.LocaleOfLastResort, false);

        langpack = ResourceService.getString(BuildStringTable,
            "langpack", ResourceService.LocaleOfLastResort, false);

        String dateString = ResourceService.getString(BuildStringTable,
            "date", ResourceService.LocaleOfLastResort, false);

        int buildYear;

        try {
            Date date = DateFormatter.parseDate(dateString, "MM/dd/yyyy");
            dateString = DateFormatter.toPaddedConciseDateString(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            buildYear = calendar.get(Calendar.YEAR);
        }
        catch (ParseException e) {
            dateString = null;
            Calendar calendar = Calendar.getInstance();
            buildYear = calendar.get(Calendar.YEAR);
        }

        date = dateString;
        year = buildYear;
        buildName = readBuildName();
    }

    public static final long version =
        (major << 26) | (minor << 21) | (patch << 16) | build;

    public static final String copyrightImage = makeVersionString();


    public static final String versionImage =
        makeVersionString(product, major, minor, patch, build, svcpack, date, hfpack, langpack);

    public static final String makeString (long version)
    {
        long major = (version & 0x7C000000) >> 26;
        long minor = (version & 0x03E00000) >> 21;
        long patch = (version & 0x001F0000) >> 16;
        long build = (version & 0x0000FFFF);

        return makeVersionString(null, major, minor, patch, build,
                                 (short) 0, null, null, null);
    }

    public static void main (String[] args)
    {
        Fmt.F(SystemUtil.out(), "%s\n", versionImage);
        Fmt.F(SystemUtil.out(), "%s\n", copyrightImage);
        SystemUtil.exit(0);
    }

    private Version ()
    {
    }

    private static final String makeVersionString (String product, long major,
        long minor, long patch, long build, short svcpack, String date, String hfpack, String langpack)
    {
        boolean haveSvPack = (svcpack > 0);
        boolean haveHfPack = !StringUtil.nullOrEmptyString(hfpack);
        boolean haveLangPack = !StringUtil.nullOrEmptyString(langpack);
        boolean haveDate = !StringUtil.nullOrEmptyString(date);
        boolean havePatch = (patch > 0);

        String svPackSuffix = haveSvPack ? "WithSvPack" : "NoSvPack";
        String hfPackSuffix = haveHfPack ? "WithHfPack" : "NoHfPack";
        String langPackSuffix = haveLangPack ? "WithLangPack" : "NoLangPack";
        String dateSuffix = haveDate ? "WithDate" : "NoDate";
        String patchSuffix = havePatch ? "WithPatch" : "NoPatch";
        String key = StringUtil.strcat("version", svPackSuffix,
                                       dateSuffix, patchSuffix, langPackSuffix);


        String versionString = Fmt.Sil(LocalStringTable, key,
            String.valueOf(major), String.valueOf(minor),
            String.valueOf(patch), String.valueOf(build), date,
            String.valueOf(svcpack), new String[] {langpack});


        if (!StringUtil.nullOrEmptyString(product)) {
            versionString = StringUtil.strcat(product, " ", versionString);
        }

        return versionString;
    }

    private static final String makeVersionString ()
    {
        String copyrightStr = ResourceService.getString(LocalStringTable, "copyright");
        return Fmt.Si(copyrightStr,Integer.toString(year));
    }

    /**
     * Looks for the BuildName file in well known locations.
     * If the file is found, returns the build name in the file.
     * File is expected to have a single line containing just the build name string.
     * @return
     * @aribaapi private
     */
    private static final String readBuildName ()
    {
        String[] possiblePaths = {
            "config/BuildName",
            "internal/build/config/BuildName"
        };
        String buildName = null;

        for (int i = 0; i < possiblePaths.length; i++) {
            buildName = readBuildName(possiblePaths[i]);
            if (!StringUtil.nullOrEmptyString(buildName)) {
                return buildName;
            }
        }
        return null;
    }

    private static final String readBuildName (String path)
    {
        URL buildNameFileURL = null;
        // create URL of the path
        buildNameFileURL = URLUtil.url(path);
        // read the content of that URL
        String buildName = IOUtil.stringFromURL(buildNameFileURL, null);
        return (buildName == null) ? null :
            StringUtil.removeCarriageReturns(buildName).trim();
    }
}