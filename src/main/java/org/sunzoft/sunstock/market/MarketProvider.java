/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunzoft.sunstock.market;

import java.util.*;
import org.sunzoft.sunstock.*;

/**
 *
 * @author sunzhu
 */
public interface MarketProvider
{

    void close();

    /**
     * Get the stock trade info for a single day
     * @param code 'shxxxxxx', 'szxxxxxx'
     * @param date format yyyymmdd
     * @return
     * @throws Exception
     */
    TradeSummary getTradeSummary(String code, String date) throws Exception;

    /**
     * Get the stock trade info
     * @param code '600019', no market sign
     * @param from format yyyymmdd
     * @param to format yyyymmdd
     * @return
     * @throws Exception
     */
    SortedMap<String, TradeSummary> getTradeSummary(String code, String from, String to) throws Exception;

    void init() throws Exception;

}
