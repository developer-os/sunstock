/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunzoft.sunstock.utils;

import java.math.*;
import java.util.*;

/**
 *
 * @author sunzhu
 */
public class RangeSearcher
{
    private final TreeMap<Long,BigDecimal> sortMap=new TreeMap();
    
    public void put(long start,BigDecimal value)
    {
        sortMap.put(start, value);
    }
    
    public void put(String start,BigDecimal value)
    {
        sortMap.put(Long.valueOf(start), value);
    }
    
    public BigDecimal getValue(long pos)
    {
        BigDecimal lastValue=new BigDecimal(0);
        for (Map.Entry<Long,BigDecimal> entry : sortMap.entrySet()) 
        {
            if(entry.getKey()>pos)
                break;
            lastValue=entry.getValue();
        }
        return lastValue;
    }
    
    public BigDecimal getValue(String pos)
    {
        return getValue(Long.valueOf(pos));
    }
}
