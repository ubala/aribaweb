/*
    Copyright (c) 1996-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/validator/TaxIDValidator.java#1 $

    Responsible: jzeng
*/

package ariba.util.validator;

import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
    Validation for Tax ID based on country.

    Currently we only do validation for Brazil CNPJ.

    @aribaapi ariba
*/
public class TaxIDValidator
{ 
    private static final String CountryAustralia = "AU";
    private static final String CountryBrazil = "BR";
    private static final String CountryChile = "CL";
    private static final String CountryItaly = "IT";
    private static final String CountryPeru = "PE";
    private static final String CountryUS = "US";

    private static final Map<String, TaxIDFormat> TaxIDFormats = MapUtil.map();
    static {
        TaxIDFormats.put(CountryAustralia,
                         new TaxIDFormat("(\\d{11})",
                                         "$1",
                                         "11"));
        TaxIDFormats.put(CountryBrazil,
                         new TaxIDFormat("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})",
                                         "$1.$2.$3/$4-$5",
                                         "14"));
        TaxIDFormats.put(CountryChile,
                         new TaxIDFormat("(\\d{2})(\\d{3})(\\d{3})(\\w{1})",
                                         "$1.$2.$3-$4",
                                         "9"));
        TaxIDFormats.put(CountryItaly,
                         new TaxIDFormat("(\\w{11})",
                                         "$1",
                                         "11"));
        TaxIDFormats.put(CountryPeru,
                         new TaxIDFormat("(\\d{11})",
                                         "$1",
                                         "11"));
        TaxIDFormats.put(CountryUS,
                         new TaxIDFormat("(\\d{2})(\\d{7})",
                                         "$1-$2",
                                         "9"));
    }

    public static Map<String, TaxIDFormat> getTaxIDFormats ()
    {
        return TaxIDFormats;
    }

    public static String formattedValue (TaxIDFormat idFormat, String orgValue)
    {
        if (StringUtil.nullOrEmptyString(orgValue)) {
            return null;
        }
                
        Pattern p = Pattern.compile(idFormat.pattern());
        Matcher m = p.matcher(orgValue);
        if (!m.matches()) {
            return null;
        }

        int groupCount = m.groupCount();
        //same as original value, no need to format
        if (groupCount == 1) {
            return null;
        }
        
        String formattedValue = idFormat.format();
        for (int i = 1; i <= m.groupCount(); i++) {
            String value = m.group(i);
            formattedValue = formattedValue.replace(
                StringUtil.strcat("$", Integer.toString(i)), value);                
        }        
        return formattedValue;
    }
    
    /**
        Checks whether the given taxid is valid for given country.
        
        @param country unique name of the country to check against
        @param taxid the taxid value to validate
        @return boolean to indicate whether the given taxid is valid
        
        @aribaapi ariba
    */
    public static boolean isTaxIDValid (String country, String taxid)
    {
        if (StringUtil.nullOrEmptyString(country) ||
            StringUtil.nullOrEmptyString(taxid)) {
            return true;
        }

        if (!validTaxIdFormat(country, taxid)) {
            return false;
        }

        if (!validTaxIdChecksum(country, taxid)) {
            return false;
        }        

        return true;
    }

    private static boolean validTaxIdFormat (String country, String taxid)
    {
        TaxIDFormat format = TaxIDFormats.get(country);
        if (format == null) {
            return true;
        }
       
        return matchPattern(format.pattern(), taxid);
    }

    private static boolean matchPattern (String pattern, String value)
    {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(value);
        return m.matches();
    }

    private static boolean validTaxIdChecksum (String country, String taxid)
    {
        if (CountryBrazil.equals(country)) {
            return isValidCNPJ(taxid);            
        }
        return true;
    }
    
    /*
        For Brazil CNPJ, the id needs to be 14 numbers.
        The last two digits of the number are used as checksum of the whole number.
        The formula is as below:
        13th digit = )(1stx6 + 2ndx7 + 3rdx8 + 4thx9+ 5thx2+ 6thx3 + 7thx4 + 8thx5 + 9thx6 +10thx7 +11thx8 +12thx9) % 11) % 10
        14th digit = ((1stx5 + 2ndx6 + 3rdx7 + 4thx8+ 5thx9+ 6thx2 + 7thx3 + 8thx4 + 9thx5 +10thx6 +11thx7 +12thx8 + 13thx9) % 11) % 10
    */
    private static boolean isValidCNPJ (String value)
    {        
        //CNPJ should be 14 digits
        int[] num = new int[14];
        
        Pattern p = Pattern.compile("[0-9]{1}");
        Matcher m = p.matcher(value);
        int index = 0;
        while (m.find()) {
            if (index >= 14) {
                return false;
            }
            num[index] = Integer.parseInt(m.group());
            index++;
        }

        if (index != 14) {
            return false;
        }
        
        int thirdteen = ((num[0]*6 + num[1]*7 + num[2]*8 + num[3]*9 +
                          num[4]*2 + num[5]*3 + num[6]*4 + num[7]*5 +
                          num[8]*6 + num[9]*7 + num[10]*8 + num[11]*9) % 11) % 10;
        if (num[12] != thirdteen) {
            return false;
        }

        int fourteen = ((num[0]*5 + num[1]*6 + num[2]*7 + num[3]*8 +
                         num[4]*9 + num[5]*2 + num[6]*3 + num[7]*4 +
                         num[8]*5 + num[9]*6 + num[10]*7 + num[11]*8 +
                         num[12]*9) % 11) % 10;
        if (num[13] != fourteen) {
            return false;
        }
            
        return true;
    }

    /**
        This class provides TaxID format information for different countries.
        This is used on UI to validate and display a formatted value.
    */
    public static class TaxIDFormat
    {
        private String m_pattern;
        private String m_format;        
        private String m_maxLength;
        
        public TaxIDFormat (String pattern,
                            String format,
                            String maxLength)
        {
            m_pattern = pattern;
            m_format = format;
            m_maxLength = maxLength;
        }

        /**
            Returns the regular expression pattern that can be used to vaidate tax id.
            E.g. for Brazil, it is "(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})".
        */
        public String pattern ()
        {            
            return  m_pattern;
        }
        
        /**
            Returns the expected format of the tax id with data replacement defined as "$n".
            E.g. for Brazil, it is " "$1.$2.$3/$4-$5".
            
        */
        public String format ()
        {
            return m_format;
        }

        /**
            Returns the max length allowed for the tax id.
        */
        public String maxLength ()
        {
            return m_maxLength;
        }
    }
}
