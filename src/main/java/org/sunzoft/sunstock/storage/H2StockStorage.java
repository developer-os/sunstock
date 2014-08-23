/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunzoft.sunstock.storage;

import java.util.*;
import org.sunzoft.sunstock.*;
import java.sql.*;

/**
 *
 * @author sunzhu
 */
public class H2StockStorage
{
    private static final String TB_QUOTA_DAY="quota_day";
    private static final String TB_STATUS="status";
    private static final String TB_DAILY_VALUE="daily_value";
    Connection con;
    PreparedStatement insertStmt;
    PreparedStatement getStmt;
    PreparedStatement getLastDateStmt;
    
    public void init() throws Exception
    {
        Class.forName("org.h2.Driver");
        con=DriverManager.getConnection("jdbc:h2:./data/sunstock");
        createTableIfNeeded();        
        insertStmt=con.prepareStatement("insert into "+TB_QUOTA_DAY+" values(?,?,?,?,?,?,?)");
        getStmt=con.prepareStatement("select day,open,close,hight,low,volumn from "+TB_QUOTA_DAY+" where code=? and day>=? and day<=? order by day");
        getLastDateStmt=con.prepareStatement("select max(day) from "+TB_DAILY_VALUE);
    }
    
    public void close()
    {
        try
        {
            if(insertStmt!=null)
                insertStmt.close();
        }
        catch (Exception e)
        {
        }
        try
        {
            if(getStmt!=null)
                getStmt.close();
        }
        catch (Exception e)
        {
        }
        try
        {
            if(con!=null)
                con.close();
        }
        catch (Exception e)
        {
        }
    }
    
    protected void createTableIfNeeded() throws Exception
    {
        Statement stmt = con.createStatement();      
        String sql = "create table if not exists "+TB_QUOTA_DAY +
                     "(code varchar(10), " +
                     " day CHAR(8), " + 
                     " open real, " +  
                     " close real, " +  
                     " hight real, " +  
                     " low real, " + 
                     " volumn INTEGER, " + 
                     " PRIMARY KEY (code,day))";
        stmt.executeUpdate(sql);
        sql = "create table if not exists "+TB_STATUS +
                     "(stat_key varchar(20), " +
                     " stat_value varchar(50), "+
                     " PRIMARY KEY (stat_key))";
        stmt.executeUpdate(sql);
        sql = "create table if not exists "+TB_DAILY_VALUE +
                     "(day CHAR(8), " +
                     " market_value real, "+
                     " capital_value real, "+
                     " index_value real, "+
                     " PRIMARY KEY (day))";
        stmt.executeUpdate(sql);
        stmt.close();
    }
    
    public void saveTradeSummary(SortedMap<String,TradeSummary> trades) throws Exception
    {
        for(String date:trades.keySet())
        {
            saveTradeSummary(date,trades.get(date));
        }
    }
    
    public void saveTradeSummary(String date, TradeSummary t) throws Exception
    {
        insertStmt.setString(1, t.code);
        insertStmt.setString(2, date);
        insertStmt.setFloat(3, t.open);
        insertStmt.setFloat(4, t.close);
        insertStmt.setFloat(5, t.high);
        insertStmt.setFloat(6, t.low);
        insertStmt.setInt(7, t.volumn);
        insertStmt.executeUpdate();
    }
    
    /**
     * Get the stock trade info
     * @param code 'shxxxxxx', 'szxxxxxx'
     * @param from format yyyymmdd
     * @param to format yyyymmdd
     * @return
     * @throws Exception 
     */
    public SortedMap<String,TradeSummary> getTradeSummary(String code,String from,String to) throws Exception
    {
        SortedMap<String,TradeSummary> trades=new TreeMap<String,TradeSummary>();
        
        getStmt.setString(1, code);
        getStmt.setString(2, from);
        getStmt.setString(3, to);
        ResultSet rs=getStmt.executeQuery();
        while(rs.next())
        {
            TradeSummary t=new TradeSummary();
            t.code=code;
            t.open=rs.getFloat(2);
            t.close=rs.getFloat(3);
            t.high=rs.getFloat(4);
            t.low=rs.getFloat(5);
            t.volumn=rs.getInt(6);
            trades.put(rs.getString(1), t);
        }
        rs.close();
        return trades;
    }
    
     /**
     * Get the stock trade info for single day
     * @param code 'shxxxxxx', 'szxxxxxx'
     * @param date format yyyymmdd
     * @return
     * @throws Exception 
     */
    public TradeSummary getTradeSummary(String code,String date) throws Exception
    {
        SortedMap<String,TradeSummary> trades=getTradeSummary(code,date,date);
        if(trades.isEmpty())
            return null;
        return (TradeSummary)trades.values().toArray()[0];
    }
    
    public String getCalculatedEndDate() throws Exception
    {
        String day=null;
        ResultSet rs=getLastDateStmt.executeQuery();
        if(rs.next())
            day=rs.getString(1);
        rs.close();
        return day;
    }
    
    public void saveCalculatedValues(String date,float market,float capital) throws Exception
    {
        System.out.println(date+"\t"+market+"\t"+capital);
    }
    
    public void saveAccountStatus(String date,Map<String,Stock> stocks,float money) throws Exception
    {
        System.out.println("==============Final stocks===============");
        for(Map.Entry<String,Stock> stk:stocks.entrySet())
        {
            Stock stock=stk.getValue();
            System.out.println(stk.getKey()+"\t"+stock.close+"*"+stock.volume+"="+(stock.close*stock.volume));
        }
        System.out.println("Money left: "+money);
    }
}
