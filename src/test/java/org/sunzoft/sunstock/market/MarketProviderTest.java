/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunzoft.sunstock.market;

import java.util.*;
import org.sunzoft.sunstock.TradeSummary;
import static org.testng.Assert.*;
import org.testng.annotations.*;

/**
 *
 * @author sunzhu
 */
public class MarketProviderTest
{
    
    
    public MarketProviderTest()
    {
    }
    
    @Test
    public void testSina() throws Exception
    {
        System.out.println("Testing sina...");
        testMarketProvider(new SinaMarketProvider());        
    }
    
    @Test
    public void testYashoo() throws Exception
    {
        System.out.println("Testing yahoo...");
        testMarketProvider(new YahooMarketProvider());
    }
    
    protected void testMarketProvider(MarketProvider provider) throws Exception
    {
        provider.init();
        testGetTradeSummary(provider);
        provider.close();
    }

    protected void testGetTradeSummary(MarketProvider provider) throws Exception
    {
        SortedMap<String,TradeSummary> trades=provider.getTradeSummary("600019", "20070101", "20070104");
        for(String date:trades.keySet())
        {
            System.out.println(date+"\t"+trades.get(date).close);
        }
    }
    
    protected void testGetTradeSummarySingleDate(MarketProvider provider) throws Exception
    {
        TradeSummary trade=provider.getTradeSummary("600019", "20070104");
        System.out.println(20070104+"\t"+trade.close);
    }    
}
