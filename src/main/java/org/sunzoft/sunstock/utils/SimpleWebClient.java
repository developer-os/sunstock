/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunzoft.sunstock.utils;

import java.util.*;
import org.apache.commons.lang3.*;
import org.apache.http.*;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.*;

/**
 *
 * @author sunzhu
 */
public class SimpleWebClient
{
    protected CloseableHttpClient client;
    protected Map<String,String> headers;
    protected RequestConfig requestConfig;
    protected String postParamEncoding="UTF-8";
    int perMaxRoute=200;//最大连接数
    int threadMaxTotal=1000;//最大线程连接数     
    public SimpleWebClient()
    {
        this(60, 60, 60); 
    }

    /**
     * 设置超时时间限制
     *
     * @param requestTimeout
     * @param connectTimeout
     * @param socketTimeout
     */
    public SimpleWebClient(Integer requestTimeout, Integer connectTimeout, Integer socketTimeout)
    {
        requestConfig = RequestConfig.custom().setConnectionRequestTimeout(requestTimeout * 1000).setConnectTimeout(connectTimeout * 1000).setSocketTimeout(socketTimeout * 1000).build();
        headers = new HashMap<String, String>();
        if (client == null)
        {
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setDefaultMaxPerRoute(perMaxRoute);
            cm.setMaxTotal(threadMaxTotal);
            client = HttpClients.custom().setConnectionManager(cm).build();
        }
    }
    
    /**
     * 为下一次请求增加http request header，header会在请求结束后清除
     * @param name
     * @param value 
     */
    public void addHeader(String name,String value)
    {
        headers.put(name, value);
    }
    
    /**
     * 发送get请求并获取回复内容
     * @param url
     * @return
     * @throws Exception 
     */
    public String getResponseForGet(String url) throws Exception
    {
        HttpGet httpget = new HttpGet(url);
        appendHeaders(httpget);
        CloseableHttpResponse response=null;
        try
        {
            response=client.execute(httpget);
            return EntityUtils.toString(response.getEntity());
        }
        finally
        {
            if(response!=null)
                response.close();
            httpget.releaseConnection();
        }
    }
    
    /**
     * 发送head请求并获取所有http response header
     * @param url
     * @return
     * @throws Exception 
     */
    public Header[] getHttpResponseHeaders(String url) throws Exception
    {
        HttpHead httphead = new HttpHead(url);
        appendHeaders(httphead);
        CloseableHttpResponse response=null;
        try
        {
            response=client.execute(httphead);
            return response.getAllHeaders();
        }
        finally
        {
            if(response!=null)
                response.close();
            httphead.releaseConnection();
        }
    }
    
    /**
     * 发送head请求并获取指定名称的http response header
     * @param url
     * @param headerName
     * @return
     * @throws Exception
     */
    public Header[] getHttpResponseHeader(String url,String headerName) throws Exception
    {
        HttpHead httphead = new HttpHead(url);
        appendHeaders(httphead);
        CloseableHttpResponse response=null;
        try
        {
            response=client.execute(httphead);
            return response.getHeaders(headerName);
        }
        finally
        {
            if(response!=null)
                response.close();
            httphead.releaseConnection();
        }
    }
    
    /**
     * 以默认编码对参数进行编码，发送post请求并获取回复内容
     * @param url
     * @param reqParams
     * @return
     * @throws Exception 
     */
    public String getResponseForPost(String url,List<NameValuePair> reqParams) throws Exception
    {
        return getResponseForPost(url,reqParams,postParamEncoding);
    }
    
    /**
     * 以指定编码对参数进行编码，发送post请求并获取回复内容
     * @param url
     * @param reqParams
     * @param paramEncoding
     * @return
     * @throws Exception 
     */
    public String getResponseForPost(String url,List<NameValuePair> reqParams,String paramEncoding) throws Exception
    {
        HttpPost req = new HttpPost(url);
        if (requestConfig != null)
            req.setConfig(requestConfig);
        appendHeaders(req);
        if(reqParams!=null&&reqParams.size()>0)
            req.setEntity(new UrlEncodedFormEntity(reqParams,paramEncoding));
        CloseableHttpResponse response=null;
        try
        { 
            response=client.execute(req);
            return EntityUtils.toString(response.getEntity());
        }
        finally
        {
            if(response!=null)
                response.close();
            req.releaseConnection();
        }
    }
    
    /**
     * 发送post请求并获取回复内容
     * @param url
     * @param requestBody
     * @return
     * @throws Exception 
     */
    public String getResponseForPost(String url,byte[] requestBody) throws Exception
    {
        HttpPost req = new HttpPost(url);
        appendHeaders(req);
        if(ArrayUtils.isNotEmpty(requestBody))
            req.setEntity(new ByteArrayEntity(requestBody));
        CloseableHttpResponse response=null;
        try
        {
            response=client.execute(req);
            return EntityUtils.toString(response.getEntity());
        }
        finally
        {
            if(response!=null)
                response.close();
            req.releaseConnection();
        }
    }
    
    /**
     * 给http请求增加设定的header
     * @param request 
     */
    protected void appendHeaders(HttpRequestBase request)
    {
        for(String name:headers.keySet())
            request.addHeader(name, headers.get(name));
    }
    
    /**
     * 清除request header
     */
    public void clearHeaders()
    {
        headers.clear();
    }
    
    /**
     * 释放资源
     */
    public void close()
    {
        try
        {
            client.close();
        }
        catch(Exception e)
        {
            //do nothing
        }
    }
    
    /**
     * 设置POST请求默认的参数编码
     * @param encoding 
     */
    public void setPostParamEncoding(String encoding)
    {
        postParamEncoding=encoding;
    }
}
