/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/BigDecimalMoneyFormatter.java#2 $
*/
package ariba.ui.validation;

import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.Date;
import ariba.util.core.Assert;
import ariba.util.core.IOUtil;
import ariba.util.core.MapUtil;
import ariba.util.io.CSVReader;
import ariba.util.io.CSVConsumer;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWGenericException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.IOException;

/**
    A currency-specific formatter for BigDecimals.  (The currency is implied by the formatter
    instance rather than being tagged on the "Money" object itself).
 */
public class BigDecimalMoneyFormatter extends GenericMoneyFormatter
{
    static Map<String, Currency> _Currencies;
    static BigDecimalMoneyAdaptor _MoneyAdapter;
    static CurrencyAdapter _CurrencyAdapter;
    static CurrencyFormatterMap _CurrencyFormatterMap = new CurrencyFormatterMap(false);
    static CurrencyFormatterMap _CurrencyFormatterWithSuffixMap = new CurrencyFormatterMap(true);

    static {
        _MoneyAdapter = (BigDecimalMoneyAdaptor)MoneyAdapter.registerClassExtension(Number.class, new BigDecimalMoneyAdaptor());
        _CurrencyAdapter = (CurrencyAdapter)GenericMoneyFormatter.CurrencyAdapter.registerClassExtension(Currency.class, new CurrencyAdapter());
    }
    public static class Currency
    {
        String _code;
        String _prefix;
        String _suffix;
        int _precision;

        public Currency (String code, String prefix, String suffix, int precision)
        {
            _code = code;
            _prefix = prefix;
            _suffix = suffix;
            _precision = precision;
        }

        public String getCode ()
        {
            return _code;
        }

        public String getPrefix ()
        {
            return _prefix;
        }

        public String getSuffix ()
        {
            return _suffix;
        }

        public int getPrecision ()
        {
            return _precision;
        }

        public boolean isEuro ()
        {
            return GenericMoneyFormatter.EuroUniqueName.equals(_code);
        }
    }

    public static class BigDecimalMoneyAdaptor extends MoneyAdapter
    {
        public boolean isInstance (Object obj)
        {
            return obj instanceof Number;
        }

        public BigDecimal getAmount (Object target)
        {
            return (target instanceof BigDecimal) ? (BigDecimal)target : new BigDecimal(((Number)target).doubleValue());
        }

        public Object create (BigDecimal amount, Object currency)
        {
            return amount;
        }

        public int getSign (Object target)
        {
            return ((BigDecimal)target).signum();
        }

        public Object negate (Object target)
        {
            return ((BigDecimal)target).negate();
        }

        public BigDecimal convertAmount (Object money, Object toCurrency, Date date, int precision)
        {
            return ((BigDecimal)money);
        }

        public CurrencyAdapter getCurrencyAdapter ()
        {
            return _CurrencyAdapter;
        }
    }

    public static class CurrencyAdapter extends GenericMoneyFormatter.CurrencyAdapter
    {
        public String getPrefix (Object currency)
        {
            return ((Currency)currency).getPrefix();
        }

        public String getSuffix (Object currency)
        {
            return ((Currency)currency).getSuffix();
        }

        public boolean isEuro (Object currency)
        {
            return ((Currency)currency).isEuro();
        }

        public int getPrecision (Object currency)
        {
            return ((Currency)currency).getPrecision();
        }

        // Factory methods
        public List getCurrencyGivenPrefixAndSuffix (String prefix, String suffix)
        {
            List result = new ArrayList();
            for (Currency currency : getCurrencies().values()) {
                if ((suffix == null || eq(currency.getSuffix(), suffix))
                    && (prefix == null || eq(currency.getPrefix(), prefix)))  {
                    result.add(currency);
                }
            }
            return result;
        }

        public Object getCurrency (String name)
        {
            return getCurrencies().get(name);
        }
    }

    static boolean eq (Object one, Object two)
    {
        return (one == null && two == null) || (!(one == null || two == null) && one.equals(two));
    }

    public static BigDecimalMoneyFormatter formatterForCurrency (String isoName)
    {
        return (BigDecimalMoneyFormatter)_CurrencyFormatterMap.get(isoName);
    }

    public static CurrencyFormatterMap formattersByCurrency ()
    {
        return _CurrencyFormatterMap;
    }

    public static CurrencyFormatterMap formattersByCurrencyWithSuffix ()
    {
        return _CurrencyFormatterWithSuffixMap;
    }

    public BigDecimalMoneyFormatter (Object currency)
    {
        super(_MoneyAdapter);
        setLeadCurrency(currency);
    }

    public static Map<String, Currency> getCurrencies ()
    {
        if (_Currencies == null) {
            _Currencies = MapUtil.map();
            // Todo: load currecies from CSV
            AWResource resource = AWConcreteServerApplication.sharedInstance().resourceManager().resourceNamed("ariba/ui/validation/currencies.csv");
            Assert.that(resource != null, "Can't find currencies.csv");

            InputStream in = resource.inputStream();
            // File format:
            // "ISO_CODE","ISO_NUMERIC","NAME","PREFIX","SUFFIX","CURRENCY_GROUP","SCALE","IS_SUPPORTED","ACTIVE_BEGIN_DATE","ACTIVE_END_DATE"
            try {
                CSVReader reader = new CSVReader(new CSVConsumer() {
                    public void consumeLineOfTokens (String path, int lineNumber, List line)
                    {
                        if (lineNumber == 1) return;
                        List<String> l = line;
                        Currency currency = new Currency(l.get(0), l.get(3), l.get(4), Integer.parseInt(l.get(6)));
                        _Currencies.put(currency.getCode(), currency);
                    }
                });
                reader.readForSpecifiedEncoding(in, resource.relativePath(), "UTF-8");
            }
            catch (IOException e) {
                throw new AWGenericException(e);
            }
            finally {
                IOUtil.close(in);
            }
        }
        return _Currencies;
    }

    /**
        Lazy map for producing formatters for a specific currency
     */
    public static class CurrencyFormatterMap extends GrowOnlyHashtable
    {
        boolean _includeSuffix;

        public CurrencyFormatterMap (boolean includeSuffix)
        {
            _includeSuffix = includeSuffix;
        }

        public Object get (Object key)
        {
            BigDecimalMoneyFormatter formatter = (BigDecimalMoneyFormatter)super.get(key);
            if (formatter == null) {
                Currency currency = getCurrencies().get(key);
                Assert.that(currency != null, "Unknown currency: " + key);
                formatter = new BigDecimalMoneyFormatter(currency);
                formatter.setAppendSuffix(_includeSuffix);
                super.put(key, formatter);
            }
            return formatter;
        }
    }

    public boolean containsKey (Object key)
    {
        return getCurrencies().get(key) != null;
    }
}
