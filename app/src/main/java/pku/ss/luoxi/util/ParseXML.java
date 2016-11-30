package pku.ss.luoxi.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import pku.ss.luoxi.bean.FutureWeather;
import pku.ss.luoxi.bean.TodayWeather;

/**
 * Created by admin on 2016/11/30.
 */
public class ParseXML {
    //解析今日天气
    public TodayWeather parseTodayXML(String xmldata){
        TodayWeather todayWeather = null;
        int fengxiangCount = 0;
        int fengliCount = 0;
        int dateCount = 0;
        int highCount = 0;
        int lowCount = 0;
        int typeCount = 0;

        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d("myWeather","parseXML");
            while (eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType) {
                    //判断是否为文档开始
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    //判断是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if(xmlPullParser.getName().equals("resp")){
                            todayWeather = new TodayWeather();
                        }
                        if (todayWeather != null) {
                            if (xmlPullParser.getName().equals("city")) {
                                eventType = xmlPullParser.next();
                                //Log.d("myWeather", "city: " + xmlPullParser.getText());
                                todayWeather.setCity(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                //Log.d("myWeather", "updatetime: " + xmlPullParser.getText());
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                //Log.d("myWeather", "shidu: " + xmlPullParser.getText());
                                todayWeather.setShidu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                //Log.d("myWeather", "wendu: " + xmlPullParser.getText());
                                todayWeather.setWendu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                //Log.d("myWeather", "pm25: " + xmlPullParser.getText());
                                todayWeather.setPm25(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                //Log.d("myWeather", "quality: " + xmlPullParser.getText());
                                todayWeather.setQuality(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                //Log.d("myWeather", "fengxiang: " + xmlPullParser.getText());
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                                eventType = xmlPullParser.next();
                                //Log.d("myWeather", "fengli: " + xmlPullParser.getText());
                                todayWeather.setFengli(xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                //Log.d("myWeather", "date: " + xmlPullParser.getText());
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                //Log.d("myWeather", "high: " + xmlPullParser.getText());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                //Log.d("myWeather", "low: " + xmlPullParser.getText());
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                //Log.d("myWeather", "type: " + xmlPullParser.getText());
                                typeCount++;
                            }
                        }
                        break;
                    //判断是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                //进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return todayWeather;
    }
    //解析未来六日天气
    public List<FutureWeather> parseFutureJSON(String jsondata){
        FutureWeather futureWeather = null;
        List<FutureWeather> list = new ArrayList<FutureWeather>();
        try {
            JSONObject jsonObject = new JSONObject(jsondata);
            //JSONObject weatherinfo = jsonObject.getJSONObject("weatherinfo");
            //Log.d("jsonObject", jsonObject.toString());
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            //Log.d("jsonArray", jsonArray.toString());
            for (int i=1;i<jsonArray.length();i++)
            {
                futureWeather = new FutureWeather();
                JSONObject jsonObjectSon= (JSONObject)jsonArray.opt(i);
                //Log.d("jsonObjectSon", jsonObjectSon.getString("week"));
                futureWeather.setDate(jsonObjectSon.getString("week"));
                futureWeather.setWendu(jsonObjectSon.getString("temperature"));
                futureWeather.setFengli(jsonObjectSon.getString("winp"));
                futureWeather.setType(jsonObjectSon.getString("weather"));
                list.add(futureWeather);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }
    //根据连接读入内容
    public String readData(String address) {
        HttpURLConnection con = null;
        ParseXML parseXML = new ParseXML();
        URL url = null;
        String responseStr = null;
        try {
            url = new URL(address);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(8000);
            con.setReadTimeout(8000);
            InputStream in = con.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder response = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                response.append(str);
                //Log.d("myWeather",str);
            }
            responseStr = response.toString();
            Log.d("responseStr", responseStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(con != null){
                con.disconnect();
            }
        }
        return responseStr;
    }

}
