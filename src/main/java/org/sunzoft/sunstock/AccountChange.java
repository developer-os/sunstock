/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunzoft.sunstock;

import java.math.*;

/**
 *
 * @author sunzhu
 */
public class AccountChange implements Comparable
{
    public String date;
    public String code;
    public float price;
    public int stockDelta;
    public BigDecimal moneyDelta;

    public int compareTo(Object o)
    {
        AccountChange ac=(AccountChange)o;
        return date.compareTo(ac.date);
    }
    
    @Override
    public String toString()
    {
        return date+"\t"+code+"\t"+stockDelta+"\t"+moneyDelta;
    }
}
