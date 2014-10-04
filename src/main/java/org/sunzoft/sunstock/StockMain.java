/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunzoft.sunstock;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.slf4j.*;
import org.sunzoft.sunstock.visual.*;

/**
 *
 * @author sunzhu
 */
public class StockMain implements ActionListener
{
    private static final Logger logger=LoggerFactory.getLogger(StockMain.class);
    StockDataSource dataSource;
    java.util.List<AccountStatus> profits;
    JTextField startInput = new JTextField();
    JTextField endInput = new JTextField();
    JLabel statusLabel;
    ChartPanel chartPanel;

    public static void main(String[] args)
    {
        try
        {
            StockMain main = new StockMain();
            main.start();            
        }
        catch (Exception e)
        {
            logger.error("Failed to init application!", e);
        }
    }

    public void start() throws Exception
    {
        initData();
        createChart();
        initGUI();
    }

    protected void initData() throws Exception
    {
        dataSource = new StockDataSource();
        dataSource.init();
        dataSource.readMoney();
        dataSource.readStock();
        dataSource.readTrade();
        //System.out.println("总盈亏: "+dataSource.getBalance());
        //dataSource.refreshAllAccountData();
        dataSource.updateAccountData();
        Calendar cld=Calendar.getInstance();        
        profits = dataSource.getDailyProfit(cld.get(Calendar.YEAR)+"0101", cld.get(Calendar.YEAR)+"1231");
    }

    protected void initGUI()
    {
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1024, 600);
        frame.setLocationRelativeTo(null);

        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                // 创建图形
                Container contentPane = frame.getContentPane();
                contentPane.setLayout(new BorderLayout(0, 0));

                JFreeChart chart = createChart();
                // 6:使用chartPanel接收        
                chartPanel = new ChartPanel(chart);
                chartPanel.setMouseZoomable(true);
                contentPane.add(chartPanel, BorderLayout.CENTER);

                JPanel pCtrl = new JPanel();
                if(profits.size()>0)
                {
                    startInput.setText(profits.get(0).date);
                    endInput.setText(profits.get(profits.size()-1).date);
                }
                else
                {
                    startInput.setText("20100101");
                    endInput.setText("20100101");
                }
                pCtrl.add(new JLabel("起始："));
                pCtrl.add(startInput);
                pCtrl.add(new JLabel("终止："));
                pCtrl.add(endInput);                
                JButton confirm = new JButton("刷新");
                confirm.addActionListener(StockMain.this);
                pCtrl.add(confirm);
                contentPane.add(pCtrl, BorderLayout.NORTH);
                
                JPanel pStatus = new JPanel();
                statusLabel=new JLabel(getStatusText());
                pStatus.add(statusLabel);
                contentPane.add(pStatus, BorderLayout.SOUTH);
                
                frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
                frame.setVisible(true);
                //dataSource.close();
            }
        });
    }

    protected JFreeChart createChart()
    {
        // 2：创建Chart[创建不同图形]
        XYDataset dataset = initChartData();
        JFreeChart chart = ChartFactory.createTimeSeriesChart("收益曲线", "时间", "盈利", dataset);
        
        XYPlot xyplot = (XYPlot) chart.getPlot();
        
        //addIndexChart(xyplot);        
        
        ChartUtils.setAntiAlias(chart);// 抗锯齿
        ChartUtils.setTimeSeriesStyle(xyplot, false, true);
        ChartUtils.setLegendEmptyBorder(chart);
        
        // 日期X坐标轴
        DateAxis domainAxis = (DateAxis) xyplot.getDomainAxis();
        domainAxis.setAutoTickUnitSelection(false);
        domainAxis.setTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());
        
        DateTickUnit dateTickUnit;
        if (dataset.getItemCount(0) < 30)
            dateTickUnit = new DateTickUnit(DateTickUnitType.DAY, 5, new SimpleDateFormat("yyyy-MM-dd")); // 第二个参数是时间轴间距
        else if (dataset.getItemCount(0) < 100)
            dateTickUnit = new DateTickUnit(DateTickUnitType.DAY, 10, new SimpleDateFormat("yyyy-MM-dd")); // 第二个参数是时间轴间距
        else if (dataset.getItemCount(0)< 200)
            dateTickUnit = new DateTickUnit(DateTickUnitType.MONTH, 1, new SimpleDateFormat("yyyy/MM")); // 第二个参数是时间轴间距
        else if (dataset.getItemCount(0)< 500)
            dateTickUnit = new DateTickUnit(DateTickUnitType.MONTH, 3, new SimpleDateFormat("yyyy/MM")); // 第二个参数是时间轴间距
        else if (dataset.getItemCount(0)< 1000)
            dateTickUnit = new DateTickUnit(DateTickUnitType.MONTH, 6, new SimpleDateFormat("yyyy/MM")); // 第二个参数是时间轴间距
        else
            dateTickUnit = new DateTickUnit(DateTickUnitType.YEAR, 1, new SimpleDateFormat("yyyy")); // 第二个参数是时间轴间距
        // 设置时间单位
        domainAxis.setTickUnit(dateTickUnit);
        return chart;
    }
    
    protected void addIndexChart(XYPlot xyplot)
    {
        NumberAxis localNumberAxis1 = new NumberAxis("指数");
        //localNumberAxis1.setLabelPaint(Color.red);
        //localNumberAxis1.setTickLabelPaint(Color.red);        
        xyplot.setRangeAxis(1, localNumberAxis1);
        xyplot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
        xyplot.setDataset(1,initIndexData());
        xyplot.mapDatasetToRangeAxis(1, 1);
    }

    protected XYDataset initChartData()
    {
        TimeSeries ts1 = new TimeSeries("盈利金额");
        for (AccountStatus td : profits)
        {
            ts1.add(new Day(Integer.parseInt(td.date.substring(6)),
                    Integer.parseInt(td.date.substring(4, 6)),
                    Integer.parseInt(td.date.substring(0, 4))), (td.market-td.capital));
        }
        TimeSeriesCollection localTimeSeriesCollection = new TimeSeriesCollection();
        localTimeSeriesCollection.addSeries(ts1);
        //localTimeSeriesCollection.addSeries(ts2);
        return localTimeSeriesCollection;
    }

    protected XYDataset initIndexData()
    {
        TimeSeries ts1 = new TimeSeries("盈利指数");
        float c=profits.get(0).capital;
        for (AccountStatus td : profits)
        {
            ts1.add(new Day(Integer.parseInt(td.date.substring(6)),
                    Integer.parseInt(td.date.substring(4, 6)),
                    Integer.parseInt(td.date.substring(0, 4))), 1+(td.market-td.capital)/c);
        }
        TimeSeriesCollection localTimeSeriesCollection = new TimeSeriesCollection();
        localTimeSeriesCollection.addSeries(ts1);
        return localTimeSeriesCollection;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String start=startInput.getText();
        String end=endInput.getText();
        try
        {
            profits = dataSource.getDailyProfit(start, end);
            chartPanel.setChart(createChart());
            statusLabel.setText(getStatusText());
        }
        catch (Exception ex)
        {
            logger.error("Falied to handle data update!",ex);
        }        
    }
    
    protected String getStatusText()
    {
        AccountStatus startStatus = profits.get(0);
        AccountStatus endStatus = profits.get(profits.size() - 1);
        float win=endStatus.market - endStatus.capital - startStatus.market + startStatus.capital;
        DecimalFormat formater=new DecimalFormat("0.00");
        return "起始日成本：" + startStatus.capital
                +"，起始日盈利：" + formater.format(startStatus.market - startStatus.capital)
                + "，终止日成本：" + endStatus.capital
                + "，终止日盈利：" + formater.format(endStatus.market - endStatus.capital)
                + "，本阶段盈利：" + formater.format(win)
                + "，" + formater.format(win* 100 / endStatus.capital)+"%";
    }
}
