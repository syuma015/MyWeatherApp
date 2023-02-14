package com.example.myweatherapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button //追加
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.SSLSessionBindingEvent


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //0)準備（APIキーと、URL（の基本部分）を定義）
        val apiKey ="hoge"
        val mainUrl = "https://api.openweathermap.org/data/2.5/weather?lang=ja"//urlの共通部分



        // 0)準備 viewを取得
        val btnIizuka:Button = findViewById(R.id.btnIizuka)
        val btnMiyazaki:Button = findViewById(R.id.btnMiyazaki)
        val tvCityName:TextView = findViewById(R.id.tvCityName)
        val tvCityWeather:TextView = findViewById(R.id.tvCityWeather)
        val tvMax:TextView = findViewById(R.id.tvMax)
        val tvMin:TextView = findViewById(R.id.tvMin)
        val btnClear:Button = findViewById(R.id.btnClear)

        //1)btnIizukaが押されたら
        btnIizuka.setOnClickListener {
            //[1-1]飯塚のお天気URLを取得して
            val weatherUrl = "$mainUrl&q=iizuka&appid=$apiKey"
            //[1-2]そのURLを元に得られた情報の結果を表示
            //2)コルーチンを作る→3)HTTP通信(ワーカースレッド)→4)お天気データ表示（メインスレッド）
            weatherTask(weatherUrl)//中身は2)へ
            //val intent = Intent(this,WeatherTask::class.java)
            //startActivity(intent)
        }
        //5)btnMiyazakiが押されたら
        btnMiyazaki.setOnClickListener {
            //[5-1]宮崎のURLを取得して

            val weatherUrl = "$mainUrl&q=miyazaki&appid=$apiKey"
            //[5-2]そのURLを元に得られた情報の結果を表示
            weatherTask(weatherUrl)//中身は2)へ

        }
        //6)　クリアボタンで元に戻す
        btnClear.setOnClickListener {
            tvCityName.text = "都市名"
            tvCityWeather.text = "都市の天気"
            tvMax.text = "最高気温"
            tvMin.text = "最低気温"
        }
    }
    //2)weatherTask()の中身
    //関数の基本構造
    private fun weatherTask(weatherUrl:String){
        //コルーチンスコープ（非同期処理の領域）を用意
    lifecycleScope.launch{
        //3)HTTP通信(ワーカースレッド)
        val result = weatherBackgroundTask(weatherUrl)

        //4)お天気データ表示（メインスレッド）
        weatherJsonTask(result)
    }
    }
    //3)HTTP通信(ワーカースレッド)の中身(※suspend=中断する可能性がある関数につける）
    //:Stringはreturnの中身の型の指定
    private suspend fun weatherBackgroundTask(weatherUrl:String):String{
        val response = withContext(Dispatchers.IO){
            //withContext=スレッドを分離しますよの意、Dispatchers.IO=ワーカースレッド、Dispatchers.main=メインスレッド

            //　天気情報サービスから取得した結果情報（JSON文字列）を後で入れるための変数（一旦空っぽ）を用意=だからvar
            var httpResult = ""

            try{
                //ただのURL文字列をURLオブジェクトに変換（文字列にリンクを付けるイメージ）
                val urlObj = URL(weatherUrl)
                //  アクセスしたAPIから情報を取得
                //　テキストファイルを読み込むクラス（文字コードを読めるようにする準備（URLオブジェクト))
                val br = BufferedReader(InputStreamReader(urlObj.openStream()))
                //httpResult = br.toString()→Stringにしたいがこれだとエミュレータでエラーになる
                //読み込んだデータを文字列に変換して代入
                httpResult = br.readText()
            }catch (e:IOException){//IOExceptionとは例外管理するクラス
                e.printStackTrace() //エラーが発生したよって言う
            }catch (e:JSONException){//JSONデータ構造に問題が発生した場合の例外
                e.printStackTrace()
            }
            //HTTP接続の結果、取得したJSON文字列httpResultを戻り値とする
            return@withContext httpResult
        }

        return response
    }
    //4) 3のHTTP通信を受けて、お天気データ(JSONデータ)を表示（UIスレッド）の中身
    private fun weatherJsonTask(result:String){
        val tvCityName:TextView = findViewById(R.id.tvCityName)
        val tvCityWeather:TextView = findViewById(R.id.tvCityWeather)
        val tvMax:TextView = findViewById(R.id.tvMax)
        val tvMin:TextView = findViewById(R.id.tvMin)

        //　まずは「３」で取得した、JSONオブジェクト一式を生成
        val jsonObj = JSONObject(result)

        //　JSONオブジェクトの、都市名のキーを取得。→tvに代入して表示
        val cityName = jsonObj.getString("name")
        tvCityName.text = cityName
        // JOSNオブジェクトの、天気情報JSON配列オブジェクトを取得
        val weatherJSONArray = jsonObj.getJSONArray("weather")
        // 現在の天気情報JSONオブジェクト（配列の0番目)を取得
        val weatherJSON = weatherJSONArray.getJSONObject(0)
        // お天気の説明（description)を取得
        val weather = weatherJSON.getString("description")
        tvCityWeather.text = weather

        // JSONオブジェクトの、mainオブジェクトを取得
        val main = jsonObj.getJSONObject("main")
        // tvMaxに最高気温を表示
        tvMax.text = "最高気温：${main.getInt("temp_max")- 273}°C"
        // tvMinに最低気温を表示
        tvMin.text = "最低気温：${main.getInt("temp_min")- 273}°C"

    }
}
