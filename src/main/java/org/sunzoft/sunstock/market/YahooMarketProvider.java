/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunzoft.sunstock.market;

import au.com.bytecode.opencsv.*;
import java.io.*;
import java.text.*;
import java.util.*;
import org.sunzoft.sunstock.*;
import org.sunzoft.sunstock.utils.*;

/**
 * 缺点：速度慢，成交量可能不准确，无交易日也有数据
 * @author sunzhu
 */
public class YahooMarketProvider implements MarketProvider
{
    private static final String URL_FORMAT="http://ichart.yahoo.com/table.csv?s={0}&a={1}&b={2}&c={3}&d={4}&e={5}&f={6}&g=d";
    SimpleWebClient webClient=new SimpleWebClient();
    
    public SortedMap<String,TradeSummary> getTradeSummary(String code,String from,String to) throws Exception
    {
        String c =  from.substring(0,4);// c – 起始时间，年
        int a = Integer.parseInt(from.substring(4,6))-1;// a – 起始时间，月
        String b = from.substring(6);// b – 起始时间，日
        String f =  to.substring(0,4);// f – 结束时间，年
        int d = Integer.parseInt(to.substring(4,6))-1;// d – 结束时间，月
        String e = to.substring(6);// e – 结束时间，日
        
        TreeMap<String,TradeSummary> tss=new TreeMap();
        String url=MessageFormat.format(URL_FORMAT,toInternalCode(code),a,b,c,d,e,f);
        String s=webClient.getResponseForGet(url);
        //System.out.println("s="+s);
        StringReader sr=new StringReader(s);
        CSVReader reader = new CSVReader(sr,',','"', 1);
        String[] line;
        while ((line = reader.readNext()) != null)
        {
            TradeSummary ts=new TradeSummary();
            ts.code=code;
            ts.open=Float.valueOf(line[1]);
            ts.close=Float.valueOf(line[4]);
            ts.high=Float.valueOf(line[2]);
            ts.low=Float.valueOf(line[3]);
            ts.volumn=(int)(Long.valueOf(line[5])/100);            
            tss.put(line[0].replace("-", ""),ts);
        }
        return tss;
    }
    
    private String toInternalCode(String code)
    {
        if(code.startsWith("6")||code.startsWith("5"))
            return code+".ss";
        return code+".sz";
    }
    
    public void close()
    {
        webClient.close();
    }

    public TradeSummary getTradeSummary(String code, String date) throws Exception
    {
        return (TradeSummary)getTradeSummary(code,date,date).values().toArray()[0];
    }

    public void init() throws Exception
    {
        //no need to do anything yet.
    }
}
