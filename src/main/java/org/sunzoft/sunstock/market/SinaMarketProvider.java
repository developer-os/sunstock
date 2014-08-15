/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunzoft.sunstock.market;

import java.text.*;
import java.util.*;
import org.dom4j.*;
import org.dom4j.io.*;
import org.sunzoft.sunstock.TradeSummary;

/**
 * 新浪web接口，优点：速度快。小问题是偶尔有些日期的所有股票数据都没有，收盘价也可能有小错误(600019,2007/01/04)
 * @author sunzhu
 */
public class SinaMarketProvider implements MarketProvider
{
    private static final String URL_FORMAT="http://biz.finance.sina.com.cn/stock/flash_hq/kline_data.php?symbol={0}&begin_date={1}&end_date={2}";
    
    /**
     * Get the stock trade info for a single day
     * @param code 'shxxxxxx', 'szxxxxxx'
     * @param date format yyyymmdd
     * @return
     * @throws Exception 
     */
    @Override
    public TradeSummary getTradeSummary(String code,String date) throws Exception
    {
        return (TradeSummary)getTradeSummary(code,date,date).values().toArray()[0];
    }
    
    /**
     * Get the stock trade info
     * @param code 'shxxxxxx', 'szxxxxxx'
     * @param from format yyyymmdd
     * @param to format yyyymmdd
     * @return
     * @throws Exception 
     */
    @Override
    public SortedMap<String,TradeSummary> getTradeSummary(String code,String from,String to) throws Exception
    {
        TreeMap<String,TradeSummary> tss=new TreeMap();
        SAXReader reader = new SAXReader();
        String url=MessageFormat.format(URL_FORMAT,toInternalCode(code),from,to);
        //System.out.println("url="+url);
        Document document = reader.read(url);
        Element root = document.getRootElement();
        for (Iterator it=root.elementIterator("content");it.hasNext();) 
        {
            Element el = (Element) it.next();
            String d=el.attributeValue("d");
            TradeSummary ts=new TradeSummary();
            ts.code=code;
            ts.open=Float.valueOf(el.attributeValue("o"));
            ts.close=Float.valueOf(el.attributeValue("c"));
            ts.high=Float.valueOf(el.attributeValue("h"));
            ts.low=Float.valueOf(el.attributeValue("l"));
            ts.volumn=Integer.valueOf(el.attributeValue("v"));            
            tss.put(d.replace("-", ""),ts);
        }
        return tss;
    }
    
    private String toInternalCode(String code)
    {
        if(code.startsWith("6")||code.startsWith("5"))
            return "sh"+code;
        return "sz"+code;
    }
    
    @Override
    public void init() throws Exception
    {
        
    }
    
    @Override
    public void close()
    {
        
    }
}
