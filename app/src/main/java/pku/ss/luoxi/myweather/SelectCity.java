package pku.ss.luoxi.myweather;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pku.ss.luoxi.app.MyApplication;
import pku.ss.luoxi.bean.City;

/**
 * Created by admin on 2016/10/18.
 */
public class SelectCity extends Activity implements View.OnClickListener{
    private ImageView mBackBtn;
    private ListView cityList;
    private List<City> mCityList;
    private EditText mInputSearch;
    private Button mSearchBtn;

    private List<String> cityData;
    private Map<String,String> nameCode;
    private String cityCode;
    private String inputSearchString = "";
    private TextWatcher mTextWatcher;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_city);

        mContext = getApplicationContext();
        //初始化
        initSelectView();
        //返回按钮响应事件
        mBackBtn.setOnClickListener(this);
        //城市列表处理事件
        listViewEvent("");
        editTextWatcher();
        //输入框监听事件
        mInputSearch.addTextChangedListener(mTextWatcher);

    }
    //初始化控件及数据
    private void initSelectView(){
        //返回按钮
        mBackBtn = (ImageView) findViewById(R.id.title_back);
        //城市选择list
        cityList = (ListView) findViewById(R.id.city_listView);
        //搜索框
        mInputSearch = (EditText) findViewById(R.id.input_search);
        //搜索按钮
        //mSearchBtn = (Button) findViewById(R.id.search_btn);
        //获取初始的cityCode
        Intent intent = getIntent();
        if ( null != intent )
        {
            cityCode = intent.getStringExtra("cityCode"); //获取父Activity传来的字符串
        }
    }

    private void editTextWatcher(){
        mTextWatcher= new TextWatcher() {
            private CharSequence temp;
            private int editStart;
            private int editEnd;
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                temp = charSequence;
                Log.d("myapp","beforeTextChanged:"+temp) ;
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                //mInputSearch.setText(charSequence);
                listViewEvent(charSequence.toString());
                Log.d("myapp","onTextChanged:"+charSequence) ;
            }
            @Override
            public void afterTextChanged(Editable editable) {
                editStart= mInputSearch.getSelectionStart();
                editEnd= mInputSearch.getSelectionEnd();
                if (temp.length() > 10) {
                    Toast.makeText(SelectCity.this,"你输⼊的字数已经超过了限制！", Toast.LENGTH_SHORT)
                            .show();
                    editable.delete(editStart-1, editEnd);
                    int tempSelection= editStart;
                    mInputSearch.setText(editable);
                    mInputSearch.setSelection(tempSelection);
                }
                Log.d("myapp","afterTextChanged:") ;
            }
        };
    }
    /*
    * 城市列表处理事件
    */
    private void listViewEvent(String charSequence){
        //根据数据显示ListView
        cityList.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1,getCityData(charSequence)));
        //为每一个item添加监听器
        cityList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cityCode = nameCode.get(cityData.get(position));
                //统计搜索最多的城市代码
                HashMap<String,String> map = new HashMap<String,String>();
                map.put("cityCode",cityCode);
                MobclickAgent.onEvent(mContext, "cityCode", map);
                //传递数据给MainActivity
                Intent i = new Intent();
                i.putExtra("cityCode",cityCode);
                setResult(RESULT_OK,i);
                finish();
            }
        });
    }

    /*
    * 获取城市数据
    */
    private List<String> getCityData(String charSequence){
        cityData = new ArrayList<String>();
        nameCode = new HashMap<String,String>();
        mCityList = new ArrayList<City>();
        mCityList =  MyApplication.getInstance().getCityList();
        if(charSequence.isEmpty()){
            Log.d("TAG","NULL");
            for (City city : mCityList){
                String cityName = city.getCity();
                String cityCode = city.getNumber();
                cityData.add(cityName);
                nameCode.put(cityName,cityCode);
            }
        }else{
            Log.d("TAG",charSequence);
            for (City city : mCityList){
                String cityName = city.getCity();
                String cityCode = city.getNumber();
                String allPY = city.getAllPY();
                String allFirstPY = city.getAllFirstPY();
                if(cityName.contains(charSequence)){
                    cityData.add(cityName);
                    nameCode.put(cityName,cityCode);
                }else if(allPY.contains(charSequence.toUpperCase())){
                    cityData.add(cityName);
                    nameCode.put(cityName,cityCode);
                }else if(allFirstPY.contains(charSequence.toUpperCase())){
                    cityData.add(cityName);
                    nameCode.put(cityName,cityCode);
                }
            }
        }
        return cityData;
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_back:
                Intent i = new Intent();
                i.putExtra("cityCode",cityCode);
                setResult(RESULT_OK,i);
                finish();
                break;
//            case R.id.search_btn:
//                //获取用户输入的数据
//                inputSearchString = mInputSearch.getText().toString();
//                listViewEvent();
            default:
                break;
        }
    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
