package org.sunzoft.sunstock;

import au.com.bytecode.opencsv.*;
import java.io.*;
import java.math.*;
import java.text.*;
import java.util.*;
import org.apache.commons.lang3.*;
import org.sunzoft.sunstock.market.*;
import org.sunzoft.sunstock.storage.*;
import org.sunzoft.sunstock.utils.*;

/**
 * Hello world!
 *
 */
public class StockDataSource 
{
    BigDecimal inMoney=new BigDecimal(0);
    BigDecimal totalStockValue=new BigDecimal(0);
    BigDecimal calculatedMoney=new BigDecimal(0);
        
    NumberFormat numberParser=NumberFormat.getNumberInstance(java.util.Locale.US);
    
    H2StockStorage storage=new H2StockStorage();
    MarketProvider marketProvider=new YahooMarketProvider();
    
    Map<String,Integer> stockHeld=new HashMap<String,Integer>();
    List<String[]> moneyRecords;
    DateFormat df=new SimpleDateFormat("yyyyMMdd");
    RangeSearcher capitalRange=new RangeSearcher();
    
    public static void main( String[] args ) throws Exception
    {
        StockDataSource dataSource=new StockDataSource();
        dataSource.init();
        dataSource.readMoney();
        dataSource.readStock();
        dataSource.readTrade();
        System.out.println("总盈亏: "+dataSource.getBalance());
        dataSource.close();
    }
    
    public void init() throws Exception
    {
        storage.init();
        marketProvider.init();
    }
    
    public void calculateData() throws Exception
    {
        String startDate=storage.getCalculatedEndDate();
        if(startDate==null)
            startDate=getMoneyStartDate();
        Date startDay=df.parse(startDate);
        Calendar cldStart=Calendar.getInstance();
        cldStart.setTime(startDay);
        Calendar cldEnd=Calendar.getInstance();
        float index=1.0f;
        while(cldStart.before(cldEnd))
        {
            String date=df.format(cldStart.getTime());
            BigDecimal market=getMarketValue(date);
            BigDecimal capital=getCapitalValue(date);
            cldStart.add(Calendar.DATE, 1);
            storage.saveCalculatedValues(date,market.floatValue(),capital.floatValue(),index);
        }
    }
    
    protected BigDecimal getMarketValue(String day) throws Exception
    {
        
    }
    
    protected BigDecimal getCapitalValue(String day) throws Exception
    {
        return capitalRange.getValue(day);
    }
    
    protected String getMoneyStartDate() throws Exception
    {
        return moneyRecords.get(0)[0];
    }
    
    public TradeSummary getDayTrade(String code,String date) throws Exception
    {
        TradeSummary ts=storage.getTradeSummary(code, date);
        if(ts!=null)
            return ts;
        ts=marketProvider.getTradeSummary(code, date);
        storage.saveTradeSummary(date, ts);
        return ts;
    }
    public void readMoney() throws Exception
    {
        CSVReader reader = new CSVReader(new FileReader("data/money.xls"),'\t','"', 1);
        int lineNum=0;
        moneyRecords=reverseRecords(reader);
        BigDecimal lastMoney=new BigDecimal("0.000");
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
                calculatedMoney=calculatedMoney.subtract(new BigDecimal(nextLine[6]));
            }
            else if("申购还款".equals(nextLine[1]))
            {
                calculatedMoney=calculatedMoney.add(new BigDecimal(nextLine[6]));
            }
            else
                System.out.println(moneyRecords.size()-lineNum+1+" - Unknow operation: "+nextLine[1]);
            if(!calculatedMoney.equals(curMoney))
                System.out.println(moneyRecords.size()-lineNum+1+" - "+nextLine[0]+" - Cal: "+calculatedMoney+"\tAct: "+nextLine[7]);       
            lastMoney=curMoney;
            lineNum++;
        }
        reader.close();
        System.out.println("总投入: "+inMoney);
        System.out.println("实际剩余资金: "+lastMoney);
        System.out.println("计算剩余资金: "+calculatedMoney);
    }
    
    public void readStock() throws Exception
    {
        CSVReader reader = new CSVReader(new FileReader("data/stock.xls"),'\t','"', 1);
        String[] nextLine;
        System.out.println("========股票持仓========");
        while ((nextLine = reader.readNext()) != null)
        {
            if(StringUtils.isBlank(nextLine[2]))
                continue;
            int count=numberParser.parse(nextLine[2]).intValue();
            float cost=numberParser.parse(nextLine[5]).floatValue();
            BigDecimal value=new BigDecimal(numberParser.parse(nextLine[7]).toString());
            totalStockValue=totalStockValue.add(value);
            System.out.println(nextLine[0]+"\t"+nextLine[1]+"\t"+count+"\t"+value);
        }
        reader.close();
        System.out.println("=======================");
        System.out.println("股票总市值: "+totalStockValue);
    }
    
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
                    if("买入".equals(line[3])||"转入".equals(line[3])||"送红股".equals(line[3]))
                    {
                        int cnt=numberParser.parse(line[4]).intValue();
                        if("买入".equals(line[3])&&cnt<100&&StringUtils.isBlank(line[5]))
                        {
                            System.out.println(line[0]+" Invalid trade "+line[3]+" "+line[4]);
                            continue;
                        }
                        currentHeld+=cnt;
                    }
                    else if("卖出".equals(line[3]))
                        currentHeld-=numberParser.parse(line[4]).intValue();
                    else
                        System.out.println(line[0]+" Unknow operation: "+line[3]);
                    if(currentHeld==0)
                        stockHeld.remove(line[1]);
                    else
                        stockHeld.put(line[1], currentHeld);
                }
            }
        }
        System.out.println("===============最终持仓===============");
        for(String code:stockHeld.keySet())
            System.out.println(code+"\t"+stockHeld.get(code));
    }
    
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
    
    private String toPublicCode(String code)
    {
        return code.substring(2);
    }
    
    public BigDecimal getBalance()
    {
        return calculatedMoney.add(totalStockValue).subtract(inMoney);
    }
    
    public void close()
    {
        storage.close();
        marketProvider.close();
    }
    
    public void calculateDailyMarketValues(String from,String to) throws Exception
    {
        
    }
}
