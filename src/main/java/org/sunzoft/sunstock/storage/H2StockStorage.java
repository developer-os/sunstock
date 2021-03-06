/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunzoft.sunstock.storage;

import java.util.*;
import org.sunzoft.sunstock.*;
import java.sql.*;
import org.slf4j.*;

/**
 *
 * @author sunzhu
 */
public class H2StockStorage
{
    private static final Logger logger=LoggerFactory.getLogger(H2StockStorage.class);
    private static final String TB_QUOTA_DAY="quota_day";
    private static final String TB_ACCOUNT_CASH="account_cash";
    private static final String TB_ACCOUNT_STATUS="account_status";
    private static final String TB_STOCK_HELD="stock_held";
    private static final String TB_APP_CONFIG="app_config";
    
    Connection con;
    PreparedStatement stmtSaveStockDailyQuota;
    PreparedStatement stmtGetStockQuota;
    PreparedStatement stmtGetMaxStatusDate;
    PreparedStatement stmtSaveAccountStatus;
    PreparedStatement stmtSaveStockHeld;
    PreparedStatement stmtGetStockHeld;
    PreparedStatement stmtSaveAccountCash;
    PreparedStatement stmtGetAccountCash;
    PreparedStatement stmtGetDailyStatus;
    PreparedStatement stmtDelAccountStatus;
    PreparedStatement stmtDelAccountCash;
    PreparedStatement stmtDelAccountStock;
    PreparedStatement stmtGetAppConfig;
    PreparedStatement stmtSaveAppConfig;
    PreparedStatement stmtUpdateAppConfig;
    
    public void init() throws Exception
    {
        Class.forName("org.h2.Driver");
        con=DriverManager.getConnection("jdbc:h2:./data/sunstock");
        createTableIfNeeded();        
        stmtSaveStockDailyQuota=con.prepareStatement("insert into "+TB_QUOTA_DAY+" values(?,?,?,?,?,?,?)");
        stmtGetStockQuota=con.prepareStatement("select day,open,close,hight,low,volume from "+TB_QUOTA_DAY+" where code=? and day>=? and day<=? order by day");
        stmtGetMaxStatusDate=con.prepareStatement("select max(day) from "+TB_ACCOUNT_CASH+" where day<=?");
        stmtSaveAccountStatus=con.prepareStatement("insert into "+TB_ACCOUNT_STATUS+" values(?,?,?)");
        stmtSaveStockHeld=con.prepareStatement("insert into "+TB_STOCK_HELD+" values(?,?,?,?)");
        stmtGetStockHeld=con.prepareStatement("select code,close,volume from "+TB_STOCK_HELD+" where day=?");
        stmtSaveAccountCash=con.prepareStatement("insert into "+TB_ACCOUNT_CASH+" values(?,?)");
        stmtGetAccountCash=con.prepareStatement("select cash from "+TB_ACCOUNT_CASH+" where day=?");
        stmtGetDailyStatus=con.prepareStatement("select day,market_value,capital_value from "+TB_ACCOUNT_STATUS+" where day>=? and day<=? order by day");
        stmtDelAccountStatus=con.prepareStatement("delete from "+TB_ACCOUNT_STATUS+" where day>?");
        stmtDelAccountCash=con.prepareStatement("delete from "+TB_ACCOUNT_CASH+" where day>?");
        stmtDelAccountStock=con.prepareStatement("delete from "+TB_STOCK_HELD+" where day>?");
        stmtGetAppConfig=con.prepareStatement("select cfg_value from "+TB_APP_CONFIG+" where cfg_name=?");
        stmtSaveAppConfig=con.prepareStatement("insert into "+TB_APP_CONFIG+" values(?,?)");
        stmtUpdateAppConfig=con.prepareStatement("update "+TB_APP_CONFIG+" set cfg_value=? where cfg_name=?");
    }
    
    public void close()
    {
        closeStatement(stmtSaveStockDailyQuota);
        closeStatement(stmtGetStockQuota);
        closeStatement(stmtGetMaxStatusDate);
        closeStatement(stmtSaveAccountStatus);
        closeStatement(stmtSaveStockHeld);
        closeStatement(stmtGetStockHeld);
        closeStatement(stmtSaveAccountCash);
        closeStatement(stmtGetAccountCash);
        closeStatement(stmtGetDailyStatus);
        closeStatement(stmtDelAccountStatus);
        closeStatement(stmtDelAccountCash);
        closeStatement(stmtDelAccountStock);
        closeStatement(stmtGetAppConfig);
        closeStatement(stmtSaveAppConfig);
        closeStatement(stmtUpdateAppConfig);
        try
        {
            if(con!=null)
                con.close();
        }
        catch (Exception e)
        {
        }
    }
    
    protected void closeStatement(Statement stmt)
    {
        try
        {
            if(stmt!=null)
                stmt.close();
        }
        catch (Exception e)
        {
            //do nothing
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
                     " volume INTEGER, " + 
                     " PRIMARY KEY (code,day))";
        stmt.executeUpdate(sql);
        sql = "create table if not exists "+TB_ACCOUNT_CASH +
                     "(day CHAR(8)," +
                     " cash real, "+
                     " PRIMARY KEY (day))";
        stmt.executeUpdate(sql);
        sql = "create table if not exists "+TB_ACCOUNT_STATUS +
                     "(day CHAR(8), " +
                     " market_value real, "+
                     " capital_value real, "+
                     " PRIMARY KEY (day))";
        stmt.executeUpdate(sql);
        sql = "create table if not exists "+TB_STOCK_HELD +
                     "(day CHAR(8), " +
                     " code varchar(10), " +
                     " close real, "+
                     " volume integer)";
        stmt.executeUpdate(sql);
        sql = "create index if not exists "+TB_STOCK_HELD+"_IDX1 on "+TB_STOCK_HELD+"(day)";
        stmt.executeUpdate(sql);
        sql = "create table if not exists "+TB_APP_CONFIG +
                     "(cfg_name varchar(20), " +
                     " cfg_value varchar(255), " +
                     " PRIMARY KEY (cfg_name))";
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
        stmtSaveStockDailyQuota.setString(1, t.code);
        stmtSaveStockDailyQuota.setString(2, date);
        stmtSaveStockDailyQuota.setFloat(3, t.open);
        stmtSaveStockDailyQuota.setFloat(4, t.close);
        stmtSaveStockDailyQuota.setFloat(5, t.high);
        stmtSaveStockDailyQuota.setFloat(6, t.low);
        stmtSaveStockDailyQuota.setInt(7, t.volumn);
        stmtSaveStockDailyQuota.executeUpdate();
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
        
        stmtGetStockQuota.setString(1, code);
        stmtGetStockQuota.setString(2, from);
        stmtGetStockQuota.setString(3, to);
        ResultSet rs=stmtGetStockQuota.executeQuery();
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
    
    public String getLastAccountDate(String last) throws Exception
    {
        String day=null;
        stmtGetMaxStatusDate.setString(1, last);
        ResultSet rs=stmtGetMaxStatusDate.executeQuery();
        if(rs.next())
            day=rs.getString(1);
        rs.close();
        return day;
    }
    
    /**
     * 保存账户的本金和市值
     * @param date
     * @param market
     * @param capital
     * @throws Exception 
     */
    public void saveAccountValues(String date,float market,float capital) throws Exception
    {
        logger.info("Saving account status: {}\t{}\t{}",date,market,capital);
        stmtSaveAccountStatus.setString(1, date);
        stmtSaveAccountStatus.setFloat(2, market);
        stmtSaveAccountStatus.setFloat(3, capital);
        stmtSaveAccountStatus.executeUpdate();
    }
    
    public void saveAccountStocks(String date,Map<String,Stock> stocks) throws Exception
    {
        logger.info("==============Final stocks({})===============",date);
        for(Map.Entry<String,Stock> stk:stocks.entrySet())
        {
            Stock stock=stk.getValue();
            logger.info(stk.getKey()+"\t"+stock.close+"*"+stock.volume+"="+(stock.close*stock.volume));
            stmtSaveStockHeld.setString(1, date);
            stmtSaveStockHeld.setString(2, stk.getKey());
            stmtSaveStockHeld.setFloat(3, stock.close);
            stmtSaveStockHeld.setInt(4, stock.volume);
            stmtSaveStockHeld.executeUpdate();
        }
    }
    
    public Map<String,Stock> getStockHeld(String date) throws Exception
    {
        logger.debug("Reading saved stock status...");
        Map<String,Stock> stocks=new HashMap<String,Stock>();        
        stmtGetStockHeld.setString(1, date);
        ResultSet rs=stmtGetStockHeld.executeQuery();
        while(rs.next())
        {
            Stock t=new Stock();
            t.code=rs.getString(1);
            t.close=rs.getFloat(2);
            t.volume=rs.getInt(3);
            stocks.put(t.code, t);
            logger.debug("{}\t{}\t{}",t.code,t.volume,t.close);
        }
        rs.close();
        return stocks;
    }
    
    public void saveAccountCash(String date,float money) throws Exception
    {
        logger.info("Saving money status: {}",money);
        stmtSaveAccountCash.setString(1, date);
        stmtSaveAccountCash.setFloat(2, money);
        stmtSaveAccountCash.executeUpdate();
    }
    
    public float getAccountCash(String date) throws Exception
    {
        stmtGetAccountCash.setString(1, date);
        ResultSet rs=stmtGetAccountCash.executeQuery();
        float cash=0;
        if(rs.next())
            cash=rs.getFloat(1);
        rs.close();
        return cash;
    }
    
    public List<AccountStatus> getAccountDailyStatus(String start,String end) throws Exception
    {
        List<AccountStatus> data=new ArrayList();
        stmtGetDailyStatus.setString(1, start);
        stmtGetDailyStatus.setString(2, end);
        ResultSet rs=stmtGetDailyStatus.executeQuery();
        while(rs.next())
        {
            AccountStatus d=new AccountStatus();
            d.date=rs.getString(1);
            d.market=rs.getFloat(2);
            d.capital=rs.getFloat(3);
            data.add(d);
        }
        rs.close();
        return data;
    }
    
    public void clearAccountStatusAfter(String lastDate) throws Exception
    {
        logger.info("Clearing account status after {}",lastDate);
        stmtDelAccountStatus.setString(1, lastDate);
        stmtDelAccountStatus.executeUpdate();
        stmtDelAccountCash.setString(1, lastDate);
        stmtDelAccountCash.executeUpdate();
        stmtDelAccountStock.setString(1, lastDate);
        stmtDelAccountStock.executeUpdate();
    }
    
    public void saveConfigItem(String name,String value) throws Exception
    {
        String existingValue=getConfigItem(name);
        if(existingValue==null&&value==null)
            return;
        if(existingValue!=null)
        {
            stmtUpdateAppConfig.setString(1, value);
            stmtUpdateAppConfig.setString(2, name);
            stmtUpdateAppConfig.executeUpdate();
        }
        else
        {
            stmtSaveAppConfig.setString(1, name);
            stmtSaveAppConfig.setString(2, value);
            stmtSaveAppConfig.executeUpdate();
        }
    }
    
    public String getConfigItem(String name) throws Exception
    {
        stmtGetAppConfig.setString(1, name);
        ResultSet rs=stmtGetAppConfig.executeQuery();
        String value=rs.next()?rs.getString(1):null;
        rs.close();
        return value;
    }
}
