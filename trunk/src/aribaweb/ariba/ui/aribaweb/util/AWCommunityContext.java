/*
    Copyright 2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWCommunityContext.java#1 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.util.core.GrowOnlyHashSet;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.MapUtil;
import ariba.util.core.SetUtil;
import ariba.util.core.StringUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This models the community context (which will be passed onto
 * community when we click on a help link
 * or to the in situ frame as we navigate in AN, Sourcing, etc).
 *
 * The context is a set of attributes.  Not all have to be present
 * (in some cases the application itself, or the page, or perhaps the domain object will be
 * enough to determine the help or content to be shown to the user).
 *
 * The context consists of:
 *   Where (application, page)
 *   Activity (one of create, edit, review, approve, etc.)
 *   What (one of invoice, PO, contract, etc.)
 *   Tags (set of tags from the community vocabulary)
 *   Tags from othe domains (will allow on-the-fly creation of other domains -- for example "commodity" which we
 *      will not attempt to validate)
 *
 */

public class AWCommunityContext
{
    // these are the names of our well known domains, client code can add their own
    public final static String DomainDomainObject = "domainObject";
    public final static String DomainApplication = "app";
    public final static String DomainPage = "page";
    public final static String DomainActivity = "activity";

    // we keep these 3 seperately as it makes it easy to keep them as singletons, but we could revisit this
    private String _application;
    private String _page;
    private String _activity;
    // all other tags except above singletons as pairs of Domain and Value
    private Set<DomainAndValue> _values;
    
    // The registry is a map from domain to a set of tags for registered domains
    private static Map<String, GrowOnlyHashSet> _Registry = new GrowOnlyHashtable<String, GrowOnlyHashSet>();

    // for the domains with a few number of values we could choose to optimize storage (say by interning vals)
    private static final Set<String> _SmallDomains = makeSmallDomains();

    // we choose to optimize small domains by interning all values
    static private Set<String> makeSmallDomains()
    {
        Set<String> smallDomains = new HashSet<String>();
        smallDomains.add(DomainDomainObject);
        smallDomains.add(DomainApplication);
        smallDomains.add(DomainPage);
        smallDomains.add(DomainActivity);

        return smallDomains;
    }


    public AWCommunityContext()
    {
        
    }


    /**
     * Add a tag (may be from any domain, registered or not).
     * By the time we get here we should have already done any validation.
     *
     * @param domain
     * @param tag
     */
    private void addTag(String domain, String tag)
    {
        if (_values == null) {
            _values = SetUtil.set();
        }

        // optimization so we do not have multiple copies of the same value for domains like app, domainObject, etc.
        if (isSmallDomain(domain)) {
            tag = tag.intern();
        }

        if (DomainPage.equals(domain)) {
            _page = tag;
        }
        else if (DomainApplication.equals(domain)) {
            _application = tag;
        }
        else if (DomainActivity.equals(domain)) {
            _activity = tag;
        }
        else {
            _values.add(DomainAndValue.makeDomainAndTag(domain, tag));
        }
    }

    /**
     * Add a set of tags (may be from any domain, registered or not)
     * @param domain
     * @param tags
     */
    private void addTagsUnchecked(String domain, Set<String> tags)
    {
        for(String tag : tags) {
            addTag(domain, tag);
        }
    }

    /**
     * Add a set of tags (may be from any domain, registered or not).  If the domain is registered we will
     * validate the values for that domain
     *
     * @param tags map from domain to a set of strings
     */
    public void addTags(Map<String, Set<String>> tags)
    {
        for(String domain : tags.keySet()) {
            addTags(domain, tags.get(domain));
        }
    }

    /**
     * Add a set of tags (may be from any domain, registered or not)

     * @param domain   the type of tag (say activity, domainObject, page)
     * @param vals     set of values for that domain
     */
    public void addTags(String domain, Set<String> vals)
    {
        if (registeredDomain(domain)) {
            if (!validate(domain, vals)) {
                // in a non-prod environment thrown exception, else skip
                String s = String.format("For registered domain:%s cannot add one or more values:%s", domain,
                        StringUtil.fastJoin(vals, ","));
                if (AWConcreteApplication.IsDebuggingEnabled) {
                    throw new AWGenericException(s);
                }
                else {
                    Log.aribaweb.warn(s);
                    return;
                }
            }
        }
        addTagsUnchecked(domain, vals);
    }


    /**
     * Set the page (this will be the AWPage.perfPageName by default)
     * @param page
     */
    public void setPage(String page)
    {
        _page = page;
    }

    /**
     * Return the page
     * @return page
     */
    public String getPage()
    {
        return _page;
    }

    /**
     * Set the application
     * @param application
     */
    public void setApplication(String application)
    {
        _application = application;
    }

    /**
     * Return the application
     * @return application
     */
    public String getApplication()
    {
        return _application;
    }


    /**
     * Set the activity engaged in by user
     * @param activity  -- should be from among ActionCreate, ActionEdit, etc.
     */
    public void setActivity(String activity)
    {
        if (!validate(DomainActivity, activity)) {
            // in a non-prod environment thrown exception, else skip
            if (AWConcreteApplication.IsDebuggingEnabled) {
                throw new AWGenericException("Cannot add unregistered activity: " + activity);
            }
            else {
                Log.aribaweb.warn("Cannot add unregistered activity: " + activity);
                return;
            }
        }

        _activity = activity;
    }

    /**
     * Return the activity being engaged in
     * @return activity
     */
    public String getActivity()
    {
        return _activity;
    }

    /**
     * Generate a CSV strings with all the tags.  If we have no value (say no page) then
     * we will not return any tag from the page domain.
     *
     * @return    csv of all tags
     */
    public String getContextAsTagsCSV()
    {
        Set<DomainAndValue> dots = getContextAsDomainAndTags();
        return tagCollectionToCSV(dots);

    }

    /**
     * Generate a map (domain -> Set<Values>) all the tags.  If we have no value (say no page) then
     * we will not return any tag from the page domain.
     *
     * @return    csv of all tags
     */
    public Map<String, Set<String>> getContextAsMap()
    {
        Set<DomainAndValue> dovs = getContextAsDomainAndTags();
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        for(DomainAndValue dov : dovs) {
            Set<String> valSet = map.get(dov.getDomain());
            if (valSet == null) {
                valSet = new HashSet<String>();
                map.put(dov.getDomain(), valSet);
            }
            valSet.add(dov.getValue());
        }
        return map;

    }

    /**
     * Return the context as a set of domain/tags.
     *
     * @return    csv of all tags
     */
    private Set<DomainAndValue> getContextAsDomainAndTags()
    {
        Set<DomainAndValue> dots = SetUtil.set();
        if (!StringUtil.nullOrEmptyOrBlankString(_application)) {
            dots.add(DomainAndValue.makeDomainAndTag(DomainApplication, _application));
        }
        if (!StringUtil.nullOrEmptyOrBlankString(_page)) {
            dots.add(DomainAndValue.makeDomainAndTag(DomainPage, _page));
        }
        if (!StringUtil.nullOrEmptyOrBlankString(_activity)) {
            dots.add(DomainAndValue.makeDomainAndTag(DomainActivity, _activity));
        }
        if (!SetUtil.nullOrEmptySet(_values)) {
            dots.addAll(_values);
        }
        return dots;

    }

    //////////////////////
    /// Below here we register types of tags (tags from different domains)
    //////////////////////

    /**
     * Register the tags in a particular domain.
     *
     * @param domain
     * @param tag
     */
    static public void registerTag(String domain, String tag)
    {
        GrowOnlyHashSet tags = _Registry.get(domain);
        if (tags == null) {
            tags = new GrowOnlyHashSet();
            _Registry.put(domain, tags);
        }
        tags.add(tag);
    }



    /**
     * Given a domain, return the set of tags registered for that domain.  May be empty but will not
     * be null.
     *
     * @param domain
     *
     * @return the set of registered tags from a domain
     */
    static public Set<String> getRegisteredTags(String domain)
    {
        Set<String> tags = _Registry.get(domain);
        if (tags == null) {
            return SetUtil.set();
        }
        return Collections.unmodifiableSet(tags);
    }


    // utilities

    /**
     * Certain domains can be registered, in which case only the set of allowed values are allowed.
     *
     * @param domain
     * @param val
     * @return
     */
    private static boolean validate(String domain, String val)
    {
        Set<String> vals = _Registry.get(domain);
        // if not a registered domain, always valid
        if (vals == null) {
            return true;
        }
        return vals.contains(val);
    }

    /**
     * Certain domains can be registered, in which case only the set of allowed values are allowed.
     *
     * @param domain
     * @param checkVals values to check
     * @return
     */
    private static boolean validate(String domain, Set<String> checkVals)
    {
        Set<String> vals = _Registry.get(domain);
        // if not a registered domain, always valid
        if (vals == null) {
            return true;
        }
        return vals.containsAll(checkVals);
    }

    /**
     * Return true if this a registered domain
     *
     * @param domain
     * @return
     */
    private static boolean isSmallDomain(String domain)
    {
        return _SmallDomains.contains(domain);
    }

    /**
     * Return true if this a registered domain
     *
     * @param domain
     * @return
     */
    private static boolean registeredDomain(String domain)
    {
        return _Registry.containsKey(domain);
    }

    /**
     * Given a set of pairs <domain:value> return a CSV
     * 
     * @param tags
     * @return
     */
    public static String tagCollectionToCSV(Set<DomainAndValue> tags)
    {


        if (SetUtil.nullOrEmptySet(tags)) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        for(DomainAndValue dt : tags) {
            if (buf.length() != 0) {
                buf.append(',');
            }
            buf.append(dt.domain);
            buf.append(dt.Separator);
            buf.append(dt.value);
        }

        return buf.toString();
    }

    /**
     * Given a map from domain -> Set<values> return a CSV
     * @param domainTagsAndVals
     * @return
     */
    public static String tagCollectionToCSV(Map<String, Set<String>> domainTagsAndVals)
    {

        if (MapUtil.nullOrEmptyMap(domainTagsAndVals)) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        for(String domain : domainTagsAndVals.keySet()) {
            Set<String> vals = domainTagsAndVals.get(domain);
            for(String val : vals) {
                if (buf.length() != 0) {
                    buf.append(',');
                }
                buf.append(domain);
                buf.append(DomainAndValue.Separator);
                buf.append(val);
            }
        }

        return buf.toString();
    }

    /**
     * Given an input string of community context with a partiular seperator (say , or ;) break this up
     * and return a map from domain to a set of values.
     * For instance given: "domainObject:invoice,activity:reconcile,av:reconciliation,av:VAT"
     * then (assuming , is the seperator char) return
     * (domainObject -> <invoice>, activity-> <reconcile>
     * , av -> <reconciliation, VAT>
     *
     * Note we assume that the ":" char is the seperator between domain and value.
     *
     * Note this is not a full blown CSV parser, we do not check for seperators escaped or embeded in quotes.
     * We do trim the individual values.   (so " two     words " -> "two words"
     * with leading and trailing trailing spaces removed and all multiple instances of white space compressed to a single
     * space
     *
     * @param inStr        input
     * @param seperator    probably , or ;
     * @param ensureValid  if true then check that for registered domains that the values in that domain are indeed
     *     registered and throw an exception if not
     *
     * @return a map as described above
     */
    public static Map<String, Set<String>> parseCommunityContext(String inStr, char seperator, boolean ensureValid)
            throws AWGenericException
    {
        inStr = inStr.trim();
        String elems[] = StringUtil.delimitedStringToArray(inStr, seperator);

        Map<String, Set<String>> domain2Vals = new HashMap<String, Set<String>>();
        for(String s : elems) {
            s = s.trim();
            String domainAndVal[] = StringUtil.delimitedStringToArray(s, ':');
            if (domainAndVal.length != 2) {
                if (ensureValid) {
                    throw new AWGenericException("Unxpected value not of form domain:value " + s);
                }
                else {
                    continue;
                }
            }
            if (!validate(domainAndVal[0], domainAndVal[1])) {
                String sErr = String.format("Invalid value:%s for domain:%s",domainAndVal[0], domainAndVal[1]);
                if (ensureValid) {
                    throw new AWGenericException(sErr);
                }
                else {
                    continue;
                }
            }
            Set<String> vals = domain2Vals.get(domainAndVal[0]);
            if (vals == null) {
                vals = new HashSet<String>();
                domain2Vals.put(domainAndVal[0], vals);
            }
            vals.add(domainAndVal[1]);
        }
        return domain2Vals;
    }

    /**
     * We implement a tag with a domain so that always know the domain in which the tag
     * is located.
     */
    public static class DomainAndValue {
        private final String domain;
        private final String value;
        private Integer _hash;

        public static final String Separator = ":";

        private DomainAndValue(String domain, String value)
        {
            this.domain = domain.intern();
            this.value = value;
        }

        /**
         * By using a factory method we enable ourselves to later add optimizations.  For instance
         * we might save a set of DomainAndValue objects for small domains (domains like app, domainObject, etc.)
         * and not create new ones.
         *
         * @param domain
         * @param tag
         * @return
         */
        static DomainAndValue makeDomainAndTag(String domain, String tag)
        {
            return new DomainAndValue(domain, tag);
        }

        public String getValue()
        {
            return value;
        }

        public String getDomain()
        {
            return domain;
        }

        /**
         * We might choose to cache this for small domains
         * @return
         */
        public String toString()
        {
            return domain + Separator + value;
        }

        @Override
        public boolean equals(Object odt)
        {
            if (this == odt) {
               return true;
            }
            if (!(odt instanceof DomainAndValue)) {
                return false;
            }
            DomainAndValue dt = (DomainAndValue)odt;
            return this.domain.equals(dt.getDomain()) && this.value.equals(dt.getValue());
        }

        @Override
        public int hashCode()
        {
            if (_hash == null) {
                _hash = 31 * domain.hashCode() + value.hashCode();
            }
            return  _hash;
        }
    }


}
