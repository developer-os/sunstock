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
import java.util.*;
import javax.swing.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.sunzoft.sunstock.visual.*;

/**
 *
 * @author sunzhu
 */
public class StockMain implements ActionListener
{

    StockDataSource dataSource;
    java.util.List<TimeData> profits;
    JTextField startInput = new JTextField();
    JTextField endInput = new JTextField();
    JLabel statusLabel;
    ChartPanel chartPanel;

    public static void main(String[] args) throws Exception
    {
        StockMain main = new StockMain();
        main.start();
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
        //dataSource.calculateAllAccountData();
        dataSource.calculateAccountData();
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
                statusLabel=new JLabel("起始日盈利："+profits.get(0).value
                        +"，终止日盈利："+profits.get(profits.size()-1).value
                        +"，本阶段盈利："+(profits.get(profits.size()-1).value-profits.get(0).value));
                pStatus.add(statusLabel);
                contentPane.add(pStatus, BorderLayout.SOUTH);

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
        // 3:设置抗锯齿，防止字体显示不清楚
        ChartUtils.setAntiAlias(chart);// 抗锯齿
        // 4:对柱子进行渲染[创建不同图形]
        ChartUtils.setTimeSeriesRender(chart.getPlot(), true, true);
        // 5:对其他部分进行渲染
        XYPlot xyplot = (XYPlot) chart.getPlot();
        ChartUtils.setXY_XAixs(xyplot);
        ChartUtils.setXY_YAixs(xyplot);
        // 日期X坐标轴
        DateAxis domainAxis = (DateAxis) xyplot.getDomainAxis();
        domainAxis.setAutoTickUnitSelection(false);
        
        // 数据过多,不显示数据
        XYLineAndShapeRenderer xyRenderer = (XYLineAndShapeRenderer) xyplot.getRenderer();
        xyRenderer.setBaseItemLabelsVisible(false);
        
        DateTickUnit dateTickUnit = null;
        if (dataset.getItemCount(0) < 30)
            dateTickUnit = new DateTickUnit(DateTickUnitType.DAY, 5, new SimpleDateFormat("yyyy-MM-dd")); // 第二个参数是时间轴间距
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
        ChartUtils.setLegendEmptyBorder(chart);
		// 设置图例位置
        return chart;
    }

    protected XYDataset initChartData()
    {
        TimeSeries ts1 = new TimeSeries("盈利金额");
        for (TimeData td : profits)
        {
            ts1.add(new Day(Integer.parseInt(td.date.substring(6)),
                    Integer.parseInt(td.date.substring(4, 6)),
                    Integer.parseInt(td.date.substring(0, 4))), td.value);
        }
        TimeSeriesCollection localTimeSeriesCollection = new TimeSeriesCollection();
        localTimeSeriesCollection.addSeries(ts1);
        //localTimeSeriesCollection.addSeries(ts2);
        return localTimeSeriesCollection;
    }

    public void actionPerformed(ActionEvent e)
    {
        String start=startInput.getText();
        String end=endInput.getText();
        try
        {
            profits = dataSource.getDailyProfit(start, end);
            chartPanel.setChart(createChart());
            statusLabel.setText("起始日盈利："+profits.get(0).value
                        +"，终止日盈利："+profits.get(profits.size()-1).value
                        +"，本阶段盈利："+(profits.get(profits.size()-1).value-profits.get(0).value));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }        
    }
}
