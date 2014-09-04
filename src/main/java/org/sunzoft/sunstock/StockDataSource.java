package org.sunzoft.sunstock;

import au.com.bytecode.opencsv.*;
import java.io.*;
import java.math.*;
import java.text.*;
import java.util.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.sunzoft.sunstock.market.*;
import org.sunzoft.sunstock.storage.*;
import org.sunzoft.sunstock.utils.*;

/**
 * Hello world!
 *
 */
public class StockDataSource 
{
    private static final Logger logger=LoggerFactory.getLogger(StockDataSource.class);
    BigDecimal inMoney=new BigDecimal(0);
    BigDecimal totalStockValue=new BigDecimal(0);
    BigDecimal calculatedMoney=new BigDecimal("0.000");
        
    NumberFormat numberParser=NumberFormat.getNumberInstance(java.util.Locale.US);
    
    H2StockStorage storage=new H2StockStorage();
    MarketProvider marketProvider=new SinaMarketProvider();//new YahooMarketProvider();
    
    Map<String,Integer> stockHeld=new HashMap<String,Integer>();
    List<String[]> moneyRecords;
    DateFormat df=new SimpleDateFormat("yyyyMMdd");
    RangeSearcher capitalRange=new RangeSearcher();
    List<AccountChange> accountChanges=new ArrayList();
    
    Map<String,Stock> currentStock;
    BigDecimal currentMoney;
    
    public static void main( String[] args ) throws Exception
    {
        StockDataSource dataSource=new StockDataSource();
        dataSource.init();
        dataSource.readMoney();
        dataSource.readStock();
        dataSource.readTrade();
        logger.info("总盈亏: "+dataSource.getBalance());
        dataSource.refreshAllAccountData();
        //dataSource.calculateAccountData();
        dataSource.close();
    }
    
    /**
     * 初始化
     * @throws Exception 
     */
    public void init() throws Exception
    {
        storage.init();
        marketProvider.init();
    }
    
    /**
     * 重新计算账户所有数据
     * @throws Exception 
     */
    public void refreshAllAccountData() throws Exception
    {
        refreshData(true);
    }
    
    /**
     * 计算账户增量数据
     * @throws Exception 
     */
    public void updateAccountData() throws Exception
    {
        refreshData(false);
    }
    
    /**
     * 计算账户数据
     * @param fullMode 是否重新刷新所有数据
     * @throws Exception 
     */
    protected void refreshData(boolean fullMode) throws Exception
    {
        String lastStatusDate=storage.getLastAccountDate();
        String lastFileEndDate=storage.getConfigItem(Config.LAST_FILE_DATE);
        logger.info("Last file date: {}. Last saved status: {}",lastFileEndDate,lastStatusDate);
        if(lastStatusDate==null||lastFileEndDate==null)
            fullMode=true;
        
        Calendar cld = Calendar.getInstance();
        if(cld.get(Calendar.HOUR_OF_DAY)<18)
            cld.add(Calendar.DATE, -1);
        String end=df.format(cld.getTime());
        
        
        String currentFileEndDate=getMoneyEndDate();
        String start;
        if(fullMode)
        {
            logger.info("Doing full refresh...");
            start=getMoneyStartDate();
            currentStock=new HashMap<String,Stock>();    
            currentMoney=new BigDecimal("0.000");
            storage.clearAccountStatusAfter("19900101");
        }
        else
        {
            storage.clearAccountStatusAfter(lastFileEndDate);
            cld = Calendar.getInstance();
            cld.setTime(df.parse(lastFileEndDate));
            cld.add(Calendar.DATE, 1);
            start = df.format(cld.getTime());
            if(start.compareTo(end)>0)
            {
                logger.info("Start date {} is later than today. No need to do any account calculation.",start);
                return;
            }
            currentStock=storage.getStockHeld(lastFileEndDate);
            currentMoney=new BigDecimal(storage.getAccountCash(lastFileEndDate));
            logger.debug("Saved money status: {}",currentMoney);
        }
        backdateAccountChanges(start,end);
        if(fullMode||currentFileEndDate.compareTo(lastFileEndDate)>0)
            saveAccountStatus(currentFileEndDate);
        storage.saveConfigItem(Config.LAST_FILE_DATE, currentFileEndDate);
    }
    
    /**
     * 获取历史任意一天的实际成本
     * @param day
     * @return
     * @throws Exception 
     */
    protected BigDecimal getCapitalValue(String day) throws Exception
    {
        return capitalRange.getValue(day);
    }
    
    /**
     * 获取交易流水的起始日期
     * @return
     * @throws Exception 
     */
    protected String getMoneyStartDate() throws Exception
    {
        return moneyRecords.get(0)[0];
    }
    
    /**
     * 获取交易流水的最后日期
     * @return
     * @throws Exception 
     */
    protected String getMoneyEndDate() throws Exception
    {
        return moneyRecords.get(moneyRecords.size()-1)[0];
    }
    
    /**
     * 获取股票的日线信息
     * 如果本地有，则直接返回本地记录。如果本地没有，从网络获取并保存到本地记录。
     * @param code
     * @param date
     * @return
     * @throws Exception 
     */
    public TradeSummary getDayTrade(String code,String date) throws Exception
    {
        TradeSummary ts=storage.getTradeSummary(code, date);
        if(ts!=null)
            return ts;
        ts=marketProvider.getTradeSummary(code, date);
        storage.saveTradeSummary(date, ts);
        return ts;
    }
    
    /**
     * 读取资金流水
     * @throws Exception 
     */
    public void readMoney() throws Exception
    {
        CSVReader reader = new CSVReader(new FileReader("data/money.xls"),'\t','"', 1);
        int lineNum=0;
        moneyRecords=reverseRecords(reader);
        BigDecimal lastMoney=new BigDecimal("0.000");
        BigDecimal lastCalculatedMoney=new BigDecimal("0.000");
        for(String[] nextLine:moneyRecords)
        {
            BigDecimal curMoney=new BigDecimal("0.000");
            if(!StringUtils.isBlank(nextLine[7]))
                curMoney=new BigDecimal(nextLine[7]);
            if("取出".equals(nextLine[1]))
            {
                inMoney=inMoney.subtract(new BigDecimal(nextLine[6]));
                calculatedMoney=calculatedMoney.subtract(new BigDecimal(nextLine[6]));
                capitalRange.put(nextLine[0], inMoney);
            }
            else if("存入".equals(nextLine[1]))
            {
                inMoney=inMoney.add(new BigDecimal(nextLine[6]));
                calculatedMoney=calculatedMoney.add(new BigDecimal(nextLine[6]));
                capitalRange.put(nextLine[0], inMoney);
            }
            else if("买入".equals(nextLine[1]))
            {
                calculatedMoney=calculatedMoney.subtract(new BigDecimal(nextLine[6]));
            }
            else if("卖出".equals(nextLine[1]))
            {
                calculatedMoney=calculatedMoney.add(new BigDecimal(nextLine[6]));
            }
            else if("派息".equals(nextLine[1]))
            {
                BigDecimal change=new BigDecimal(nextLine[6]);
                if(curMoney.equals(lastMoney.add(change)))//小财神会把派息扣税记成派息
                    calculatedMoney=calculatedMoney.add(change);
                else
                    calculatedMoney=calculatedMoney.subtract(change);
            }
            else if("利息归本".equals(nextLine[1]))
            {
                calculatedMoney=calculatedMoney.add(new BigDecimal(nextLine[6]));
            }
            else if("申购中签".equals(nextLine[1]))
            {
                calculatedMoney=calculatedMoney.subtract(new BigDecimal(nextLine[6]));
            }
            else if("利息税".equals(nextLine[1]))
            {
                calculatedMoney=calculatedMoney.subtract(new BigDecimal(nextLine[6]));
            }
            else if("新股申购".equals(nextLine[1]))
            {
                //calculatedMoney=calculatedMoney.subtract(new BigDecimal(nextLine[6]));
            }
            else if("申购还款".equals(nextLine[1]))
            {
                //calculatedMoney=calculatedMoney.add(new BigDecimal(nextLine[6]));
            }
            else
                logger.debug(moneyRecords.size()-lineNum+1+" - Unknow operation: "+nextLine[1]);
            /*
            if(!calculatedMoney.equals(curMoney))
                System.out.println(moneyRecords.size()-lineNum+1+" - "+nextLine[0]+" - Cal: "+calculatedMoney+"\tAct: "+nextLine[7]);   */    
            lastMoney=curMoney;
            if(!lastCalculatedMoney.equals(calculatedMoney))
            {
                AccountChange ac=new AccountChange();
                ac.date=nextLine[0];
                ac.moneyDelta=calculatedMoney.subtract(lastCalculatedMoney);
                accountChanges.add(ac);
            }
            lastCalculatedMoney=calculatedMoney;
            lineNum++;
        }
        reader.close();
        logger.info("总投入: "+inMoney);
        logger.info("实际剩余资金: "+lastMoney);
        logger.info("计算剩余资金: "+calculatedMoney);
    }
    
    /**
     * 读取股票持仓
     * @throws Exception 
     */
    public void readStock() throws Exception
    {
        CSVReader reader = new CSVReader(new FileReader("data/stock.xls"),'\t','"', 1);
        String[] nextLine;
        logger.info("========股票持仓========");
        while ((nextLine = reader.readNext()) != null)
        {
            if(StringUtils.isBlank(nextLine[2]))
                continue;
            try
            {
                int count=numberParser.parse(nextLine[2]).intValue();
                BigDecimal value=new BigDecimal(numberParser.parse(nextLine[7]).toString());
                totalStockValue=totalStockValue.add(value);
                logger.info(nextLine[0]+"\t"+nextLine[1]+"\t"+count+"\t"+value);
            }
            catch (Exception e)
            {
                logger.error("Failed to parse stock info for "+nextLine[0], e);
            }            
        }
        reader.close();
        logger.info("=======================");
        logger.info("股票总市值: "+totalStockValue);
    }
    
    /**
     * 读取交易流水
     * @throws Exception 
     */
    public void readTrade() throws Exception
    {
        CSVReader reader = new CSVReader(new FileReader("data/trade.xls"),'\t','"', 1);
        SortedMap<String,List<String[]>> sortMap=sortRecords(reader);
        reader.close();
        for(String date:sortMap.keySet())
        {
            List<String[]> lines=sortMap.get(date);
            for(String[] line:lines)
            {
                if(!StringUtils.isBlank(line[4]))
                {
                    Integer v=stockHeld.get(line[1]);
                    int currentHeld=(v==null?0:v);
                    int lastHeld=currentHeld;
                    if("买入".equals(line[3])||"转入".equals(line[3])||"送红股".equals(line[3]))
                    {
                        int cnt=numberParser.parse(line[4]).intValue();
                        if("买入".equals(line[3])&&cnt<100&&StringUtils.isBlank(line[5]))
                        {
                            logger.debug(line[0]+" Invalid trade "+line[3]+" "+line[4]);
                            continue;
                        }
                        currentHeld+=cnt;
                    }
                    else if("卖出".equals(line[3]))
                        currentHeld-=numberParser.parse(line[4]).intValue();
                    else
                        logger.debug(line[0]+" Unknow operation: "+line[3]);
                    if(currentHeld==0)
                        stockHeld.remove(line[1]);
                    else
                        stockHeld.put(line[1], currentHeld);
                    if(currentHeld!=lastHeld)
                    {
                        AccountChange ac=new AccountChange();
                        ac.date=line[0];
                        ac.code=line[1];
                        try
                        {
                            ac.price=Float.parseFloat(line[5]);
                        }
                        catch (Exception e)
                        {
                        }
                        ac.stockDelta=currentHeld-lastHeld;
                        accountChanges.add(ac);
                    }
                }
            }
        }
        logger.info("===============最终持仓===============");
        for(String code:stockHeld.keySet())
            logger.info(code+"\t"+stockHeld.get(code));
    }
    
    /**
     * 顺排序记录
     * @param reader
     * @return
     * @throws Exception 
     */
    private SortedMap<String,List<String[]>> sortRecords(CSVReader reader) throws Exception
    {
        String[] nextLine;
        TreeMap<String,List<String[]>> sortMap=new TreeMap();
        String lastDate="";
        List<String[]> lines=new ArrayList<String[]>();
        while ((nextLine = reader.readNext()) != null)
        {
            if(!nextLine[0].equals(lastDate))
            {
                if(!lines.isEmpty())
                    sortMap.put(lastDate, lines);
                lines=new ArrayList<String[]>();
                lastDate=nextLine[0];
            }
            lines.add(nextLine);
        }
        if(!lines.isEmpty())
            sortMap.put(lastDate, lines);
        return sortMap;
    }
    
    /**
     * 逆序记录
     * 由于excel里的记录是倒排序的，调用一次这个方法以后相当于正排序
     * @param reader
     * @return
     * @throws Exception 
     */
    private List<String[]> reverseRecords(CSVReader reader) throws Exception
    {
        String[] nextLine;
        List<String[]> lines=new ArrayList<String[]>();
        while ((nextLine = reader.readNext()) != null)
        {
            lines.add(nextLine);
        }
        Collections.reverse(lines);
        return lines;
    }
    
    /**
     * 获取最终盈亏
     * @return 
     */
    public BigDecimal getBalance()
    {
        return calculatedMoney.add(totalStockValue).subtract(inMoney);
    }
    
    /**
     * 关闭资源
     */
    public void close()
    {
        storage.close();
        marketProvider.close();
    }
    
    /**
     * 回溯各种交易及资金流水，获得每日的资金及股票持仓状态
     * @param from
     * @param to
     * @throws Exception 
     */
    public void backdateAccountChanges(String from,String to) throws Exception
    {
        Collections.sort(accountChanges);
        Calendar cld=Calendar.getInstance();
        cld.setTime(df.parse(from));
        Calendar cldEnd=Calendar.getInstance();
        cldEnd.setTime(df.parse(to));
        AccountChange nextChange=null;
        int nextIndex=0;
        while(nextIndex<accountChanges.size())
        {
            nextChange=accountChanges.get(nextIndex++);
            if(nextChange.date.compareTo(from)>=0)
                break;
        }
        logger.info("Total account changes from {} to {}: {}",from,to,accountChanges.size()-nextIndex);
        while(cld.compareTo(cldEnd)<=0)
        {
            int wkday=cld.get(Calendar.DAY_OF_WEEK);
            if(wkday!=Calendar.SATURDAY&&wkday!=Calendar.SUNDAY)
            {
                String day=df.format(cld.getTime());               
                while(day.equals(nextChange.date))//get all changes for the day
                {
                    logger.debug("Applying change: code {}, volume {}, money {}",nextChange.code,nextChange.stockDelta,nextChange.moneyDelta);
                    if(nextChange.code!=null)//stock change
                    {
                        Stock v=currentStock.get(nextChange.code);
                        if(v==null)
                            v=new Stock();
                        v.volume+=nextChange.stockDelta;
                        if(v.volume==0)
                            currentStock.remove(nextChange.code);
                        else
                            currentStock.put(nextChange.code, v);
                    }
                    else//money change
                    {
                        currentMoney=currentMoney.add(nextChange.moneyDelta);
                    }
                    if(nextIndex>=accountChanges.size())
                        break;
                    nextChange=accountChanges.get((nextIndex++));
                }
                BigDecimal totalValue=currentMoney.add(getTotalStockValue(day,currentStock,nextChange));
                storage.saveAccountStatus(day, totalValue.floatValue(), getCapitalValue(day).floatValue());
            }
            cld.add(Calendar.DATE, 1);
        }
    }
    
    /**
     * 保存计算的账户最终状态
     * @param day
     * @throws Exception 
     */
    public void saveAccountStatus(String day) throws Exception
    {        
        storage.saveAccountStocks(day, currentStock);
        storage.saveAccountCash(day, currentMoney.floatValue());
    }
    
    /**
     * 计算股票市值
     * @param day
     * @param stocks
     * @param change
     * @return
     * @throws Exception 
     */
    protected BigDecimal getTotalStockValue(String day,Map<String,Stock> stocks,AccountChange change) throws Exception
    {
        //System.out.println("stock count: "+stocks.size());
        BigDecimal value=new BigDecimal("0.000");
        for(Map.Entry<String,Stock> stk:stocks.entrySet())
        {
            Stock stock=stk.getValue();
            try
            {
                stock.close=getDayTrade(stk.getKey(),day).close;
            }
            catch (Exception e)
            {                
                logger.warn("Failed to get stock price for "+day+" - "+stk.getKey(), e);
            }
            if(stock.close==0&&stk.getKey().equals(change.code))
                stock.close=change.price;
            value=value.add(new BigDecimal(stock.close*stock.volume));
        }
        logger.debug("{} Total stock value: {}",day,value);
        return value;
    }
    
    public List<AccountStatus> getDailyProfit(String start,String end) throws Exception
    {
        return storage.getAccountDailyStatus(start,end);
    }
}
