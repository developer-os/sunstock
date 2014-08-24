/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunzoft.sunstock;

import java.awt.*;
import java.text.*;
import javax.swing.*;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
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
public class StockMain extends JFrame
{
    StockDataSource dataSource;
    java.util.List<TimeData> profits;
    
    public static void main(String[] args) throws Exception
    {
        StockMain main = new StockMain();
        main.start();
    }

    public void start() throws Exception
    {
        initData();
        initChart();
        initGUI();
    }

    protected void initData() throws Exception
    {
        dataSource=new StockDataSource();
        dataSource.init();
        dataSource.readMoney();
        dataSource.readStock();
        dataSource.readTrade();
        //System.out.println("总盈亏: "+dataSource.getBalance());
        //dataSource.calculateAllAccountData();
        dataSource.calculateAccountData();
        profits=dataSource.getDailyProfit("20140101","20140901");
        dataSource.close();
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
				ChartPanel chartPanel = initChart();
				frame.getContentPane().add(chartPanel);
				frame.setVisible(true);
			}
		});
    }

    protected ChartPanel initChart()
    {
        /*
        Font titleFont = new Font("宋体", Font.BOLD, 22);
        JFreeChart chart = ChartFactory.createTimeSeriesChart("收益曲线", "日期", "盈利", initChartData(), true, true, false);
        chart.getTitle().setFont(titleFont);
        XYPlot xyPlot = (XYPlot) chart.getPlot();
        xyPlot.setDomainPannable(true);
        xyPlot.setRangePannable(false);
        xyPlot.setDomainCrosshairVisible(true);
        xyPlot.setRangeCrosshairVisible(true);
        Font axisFont = new Font("宋体", Font.PLAIN, 16);
        xyPlot.getDomainAxis().setLabelFont(axisFont);
        xyPlot.getRangeAxis().setLabelFont(axisFont);*/
        
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
		DateTickUnit dateTickUnit = null;
		if (dataset.getItemCount(0) < 16) {
			//刻度单位月,半年为间隔
			dateTickUnit = new DateTickUnit(DateTickUnitType.MONTH, 6, new SimpleDateFormat("yyyy-MM")); // 第二个参数是时间轴间距
		} else {// 数据过多,不显示数据
			XYLineAndShapeRenderer xyRenderer = (XYLineAndShapeRenderer) xyplot.getRenderer();
			xyRenderer.setBaseItemLabelsVisible(false);
			dateTickUnit = new DateTickUnit(DateTickUnitType.YEAR, 1, new SimpleDateFormat("yyyy")); // 第二个参数是时间轴间距
		}
		// 设置时间单位
		domainAxis.setTickUnit(dateTickUnit);
		ChartUtils.setLegendEmptyBorder(chart);
		// 设置图例位置
		// 6:使用chartPanel接收        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseZoomable(true);
        return chartPanel;
    }

    protected XYDataset initChartData()
    {
        TimeSeries ts1 = new TimeSeries("盈利金额");
        for(TimeData td:profits)
        {
            ts1.add(new Day(Integer.parseInt(td.date.substring(6)),
                            Integer.parseInt(td.date.substring(4,6)),
                            Integer.parseInt(td.date.substring(0,4))), td.value);
        }
        /*
        TimeSeries ts2 = new TimeSeries("L&G UK Index Trust");
        ts2.add(new Month(2, 2001), 129.59999999999999D);
        ts2.add(new Month(3, 2001), 123.2D);
        ts2.add(new Month(4, 2001), 117.2D);
        ts2.add(new Month(5, 2001), 124.09999999999999D);
        ts2.add(new Month(6, 2001), 122.59999999999999D);
        ts2.add(new Month(7, 2001), 119.2D);
        ts2.add(new Month(8, 2001), 116.5D);
        ts2.add(new Month(9, 2001), 112.7D);
        ts2.add(new Month(10, 2001), 101.5D);
        ts2.add(new Month(11, 2001), 106.09999999999999D);
        ts2.add(new Month(12, 2001), 110.3D);
        ts2.add(new Month(1, 2002), 111.7D);
        ts2.add(new Month(2, 2002), 111.0D);
        ts2.add(new Month(3, 2002), 109.59999999999999D);
        ts2.add(new Month(4, 2002), 113.2D);
        ts2.add(new Month(5, 2002), 111.59999999999999D);
        ts2.add(new Month(6, 2002), 108.8D);
        ts2.add(new Month(7, 2002), 101.59999999999999D);
        ts2.add(new Month(8, 2002), 90.950000000000003D);
        ts2.add(new Month(9, 2002), 91.019999999999996D);
        ts2.add(new Month(10, 2002), 82.370000000000005D);
        ts2.add(new Month(11, 2002), 86.319999999999993D);
        ts2.add(new Month(12, 2002), 91.0D);
        ts2.add(new Month(1, 2003), 86.0D);
        ts2.add(new Month(2, 2003), 80.040000000000006D);
        ts2.add(new Month(3, 2003), 80.400000000000006D);
        ts2.add(new Month(4, 2003), 80.280000000000001D);
        ts2.add(new Month(5, 2003), 86.420000000000002D);
        ts2.add(new Month(6, 2003), 91.400000000000006D);
        ts2.add(new Month(7, 2003), 90.519999999999996D);
        ts2.add(new Month(8, 2003), 93.109999999999999D);
        ts2.add(new Month(9, 2003), 96.799999999999997D);
        ts2.add(new Month(10, 2003), 94.780000000000001D);
        ts2.add(new Month(11, 2003), 99.560000000000002D);
        ts2.add(new Month(12, 2003), 100.8D);
        ts2.add(new Month(1, 2004), 103.40000000000001D);
        ts2.add(new Month(2, 2004), 102.09999999999999D);
        ts2.add(new Month(3, 2004), 105.3D);
        ts2.add(new Month(4, 2004), 103.7D);
        ts2.add(new Month(5, 2004), 105.2D);
        ts2.add(new Month(6, 2004), 103.7D);
        ts2.add(new Month(7, 2004), 105.7D);
        ts2.add(new Month(8, 2004), 103.59999999999999D);
        ts2.add(new Month(9, 2004), 106.09999999999999D);
        ts2.add(new Month(10, 2004), 109.3D);
        ts2.add(new Month(11, 2004), 110.3D);
        ts2.add(new Month(12, 2004), 112.59999999999999D);
        ts2.add(new Month(1, 2005), 116.0D);
        ts2.add(new Month(2, 2005), 117.3D);
        ts2.add(new Month(3, 2005), 120.09999999999999D);
        ts2.add(new Month(4, 2005), 119.3D);
        ts2.add(new Month(5, 2005), 116.2D);
        ts2.add(new Month(6, 2005), 120.8D);
        ts2.add(new Month(7, 2005), 125.2D);
        ts2.add(new Month(8, 2005), 127.7D);
        ts2.add(new Month(9, 2005), 130.80000000000001D);
        ts2.add(new Month(10, 2005), 131.0D);
        ts2.add(new Month(11, 2005), 135.30000000000001D);
        ts2.add(new Month(12, 2005), 141.19999999999999D);
        ts2.add(new Month(1, 2006), 144.69999999999999D);
        ts2.add(new Month(2, 2006), 146.40000000000001D);
        ts2.add(new Month(3, 2006), 151.90000000000001D);
        ts2.add(new Month(4, 2006), 153.5D);
        ts2.add(new Month(5, 2006), 144.5D);
        ts2.add(new Month(6, 2006), 150.09999999999999D);
        ts2.add(new Month(7, 2006), 148.69999999999999D);
        ts2.add(new Month(8, 2006), 150.09999999999999D);
        ts2.add(new Month(9, 2006), 151.59999999999999D);
        ts2.add(new Month(10, 2006), 153.40000000000001D);
        ts2.add(new Month(11, 2006), 158.30000000000001D);
        ts2.add(new Month(12, 2006), 157.59999999999999D);
        ts2.add(new Month(1, 2007), 163.90000000000001D);
        ts2.add(new Month(2, 2007), 163.80000000000001D);
        ts2.add(new Month(3, 2007), 162.0D);
        ts2.add(new Month(4, 2007), 167.09999999999999D);
        ts2.add(new Month(5, 2007), 170.0D);
        ts2.add(new Month(6, 2007), 175.69999999999999D);
        ts2.add(new Month(7, 2007), 171.90000000000001D);*/
        TimeSeriesCollection localTimeSeriesCollection = new TimeSeriesCollection();
        localTimeSeriesCollection.addSeries(ts1);
        //localTimeSeriesCollection.addSeries(ts2);
        return localTimeSeriesCollection;
    }
}
