package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SummaryStatisticsHelper
{
    public final static String BASE_STAT_SUM = "Sum";
    public final static String BASE_STAT_MEAN = "Mean";
    public final static String BASE_STAT_MIN = "Minimum";
    public final static String BASE_STAT_MAX = "Maximum";
    public final static String BASE_STAT_COUNT = "Count (non-blank)";

    public final static String PREMIUM_STAT_COUNT_BLANK = "Count (blank)";
    public final static String PREMIUM_STAT_COUNT_DISTINCT = "Count (distinct)";
    public final static String PREMIUM_STAT_STDDEV = "Standard Deviation (of mean)";
    public final static String PREMIUM_STAT_STDERR = "Standard Error (of mean)";

    public final static List<String> ALL_STATS;
    static {
        ALL_STATS = new ArrayList<>();
        ALL_STATS.addAll(Arrays.asList(BASE_STAT_COUNT, BASE_STAT_SUM, BASE_STAT_MEAN, BASE_STAT_MIN, BASE_STAT_MAX));
        ALL_STATS.addAll(Arrays.asList(PREMIUM_STAT_COUNT_BLANK, PREMIUM_STAT_COUNT_DISTINCT, PREMIUM_STAT_STDDEV, PREMIUM_STAT_STDERR));
    }

    private WebDriverWrapper _wrapper;
    private boolean _hasPremiumModule;

    public SummaryStatisticsHelper(BaseWebDriverTest test)
    {
        _wrapper = test;
        _hasPremiumModule = test.getContainerHelper().getAllModules().contains("Premium");
    }

    public List<String> getExpectedColumnStats(String colType, boolean isLookup, boolean isPK)
    {
        List<String> stats = new ArrayList<>();
        boolean isNumeric = "integer".equalsIgnoreCase(colType) || "double".equalsIgnoreCase(colType);

        stats.add(BASE_STAT_COUNT);
        if (_hasPremiumModule)
        {
            stats.add(PREMIUM_STAT_COUNT_BLANK);
            if (!"double".equalsIgnoreCase(colType))
                stats.add(PREMIUM_STAT_COUNT_DISTINCT);
        }

        if (isNumeric && !isLookup && !isPK)
        {
            stats.add(BASE_STAT_SUM);
            stats.add(BASE_STAT_MEAN);
            if (_hasPremiumModule)
            {
                stats.add(PREMIUM_STAT_STDDEV);
                stats.add(PREMIUM_STAT_STDERR);
            }
        }

        if ((isNumeric || "date".equalsIgnoreCase(colType)) && !isLookup)
        {
            stats.add(BASE_STAT_MIN);
            stats.add(BASE_STAT_MAX);
        }

        return stats;
    }

    public List<String> getUnexpectedColumnStats(String colType, boolean isLookup, boolean isPK)
    {
        return getUnexpectedColumnStats(getExpectedColumnStats(colType, isLookup, isPK));
    }

    public List<String> getUnexpectedColumnStats(List<String> expected)
    {
        List<String> stats = new ArrayList<>();
        for (String stat : ALL_STATS)
        {
            if (!expected.contains(stat))
                stats.add(stat);
        }
        return stats;
    }

    public void verifySummaryStatisticsSubmenu(String columnName, String colType)
    {
        verifySummaryStatisticsSubmenu(columnName, colType, false, false);
    }

    public void verifySummaryStatisticsSubmenu(String columnName, String colType, boolean isLookup, boolean isPK)
    {
        _wrapper.refresh(); // refresh to guarantee that we are only seeing the current column's menu items?

        // they all have the base count stat so safe to use here for submenu item
        Locator colLoc = DataRegionTable.Locators.columnHeader("query", columnName);
        _wrapper._ext4Helper.clickExt4MenuButton(false, colLoc, true /*openOnly*/, "Summary Statistics", SummaryStatisticsHelper.BASE_STAT_COUNT);

        for (String stat : getExpectedColumnStats(colType, isLookup, isPK))
            _wrapper.assertElementPresent(Ext4Helper.Locators.menuItem(stat));

        for (String stat : getUnexpectedColumnStats(colType, isLookup, isPK))
            _wrapper.assertElementNotPresent(Ext4Helper.Locators.menuItem(stat));
    }

    public String getSummaryStatisticFooterAsString(DataRegionTable drt, String columnName)
    {
        if (drt.hasSummaryStatisticRow())
            return drt.getTotal(columnName).replaceAll("\\s+", " ");

        return null;
    }
}