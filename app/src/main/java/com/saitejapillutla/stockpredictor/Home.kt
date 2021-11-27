package com.saitejapillutla.stockpredictor

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.pacific.adapter.RecyclerAdapter
import kotlinx.android.synthetic.main.activity_home.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.HashMap

class Home : AppCompatActivity(){

    val coroutineContext: CoroutineContext
        get() = Dispatchers.IO +job
    private lateinit var job: Job
    private lateinit var job1: Job
    private  val TAG = "Home_"
    var client = OkHttpClient()
    val gainerRecycleradapter = RecyclerAdapter()
    val loserRecycleradapter = RecyclerAdapter()
    val adapter = RecyclerAdapter()
    @ExperimentalCoroutinesApi
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        gainerRecycler.adapter=gainerRecycleradapter
        looser_recycler.adapter =loserRecycleradapter

        textView.visibility = View.GONE
        gainerRecycler.visibility = View.GONE
        textView4.visibility = View.GONE
        looser_recycler.visibility = View.GONE


        CoroutineScope(Dispatchers.Main).launch{
            Log.d(TAG,"Gainer fetch Coroutine Started")
            val market_info =async(Dispatchers.IO) {fetchDBMarketInfo()}
            val gainers =async(Dispatchers.IO) { fetchDBGainers() }
            val losers = async(Dispatchers.IO ){ fetchDBLosers() }
            updateloserrecycler(losers)
            updateGainerrecycler(gainers)
            updateMarketInfoUI(market_info)
            if(gainers.await().size>0){
                textView.visibility = View.VISIBLE
                gainerRecycler.visibility = View.VISIBLE
                textView4.visibility = View.VISIBLE
                looser_recycler.visibility = View.VISIBLE
            }
          }
        val conditions = CustomModelDownloadConditions.Builder()
            .build()
        FirebaseModelDownloader.getInstance()
            .getModel("stockPredictionModel_v1", DownloadType.LOCAL_MODEL, conditions)
            .addOnCompleteListener {
                Log.d(TAG, "it completed" )

                // Download complete. Depending on your app, you could enable the ML
                // feature, or switch from the local model to the remote model, etc.
            }

       // CoroutineScope(newSingleThreadContext("retriveGainers")).launch(Dispatchers.Main){}
        //job1.start()
    }

    private suspend fun updateloserrecycler(losers: Deferred<HashMap<String, HashMap<String, String>>>) {
        for (i in 0 until  losers.await().size){
            val GainersClass =Gainers(
                losers.await().get(i.toString())?.get("symbol"),
                losers.await().get(i.toString())?.get("series"),
                losers.await().get(i.toString())?.get("open_price"),
                losers.await().get(i.toString())?.get("high_price"),
                losers.await().get(i.toString())?.get("low_price"),
                losers.await().get(i.toString())?.get("ltp"),
                losers.await().get(i.toString())?.get("prev_price"),
                losers.await().get(i.toString())?.get("net_price"),
                losers.await().get(i.toString())?.get("trade_quantity"),
                losers.await().get(i.toString())?.get("turnover"),
                losers.await().get(i.toString())?.get("market_type"),
                losers.await().get(i.toString())?.get("ca_ex_dt"),
                losers.await().get(i.toString())?.get("ca_purpose"),
                losers.await().get(i.toString())?.get("perChange"),
            )
            loserRecycleradapter.add(GainersClass)
        }
        looser_recycler.adapter =loserRecycleradapter
    }

    suspend fun fetchDBLosers(): HashMap<String, HashMap<String, String>> {
        val ref =FirebaseDatabase.getInstance().reference
        var losers = hashMapOf("0" to hashMapOf<String,String>())
        var callFetchLosers =true
        ref.child("losers").get().addOnSuccessListener {
            if (it.child("today").value != LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))){
                callFetchLosers =false
                Log.d(TAG,"calling FetchLosers")
            }

            for(i in 0 until it.childrenCount){
                Log.d(TAG," Fetched from DB Loers${it.key} ")
                losers.put(
                    i.toString(), hashMapOf(
                        "symbol" to it.child("symbol").value.toString(),
                        "series"  to  it.child("series").value.toString(),
                        "open_price"  to  it.child("open_price").value.toString(),
                        "high_price"  to  it.child("high_price").value.toString(),
                        "low_price"  to  it.child("low_price").value.toString(),
                        "ltp"  to  it.child("ltp").value.toString(),
                        "prev_price"  to it.child("prev_price").value.toString(),
                        "net_price"  to  it.child("net_price").value.toString(),
                        "trade_quantity"  to  it.child("trade_quantity").value.toString(),
                        "turnover"  to  it.child("turnover").value.toString(),
                        "market_type"  to it.child("market_type").value.toString(),
                        "ca_ex_dt"  to it.child("ca_ex_dt").value.toString(),
                        "ca_purpose"  to it.child("ca_purpose").value.toString(),
                        "perChange"  to  it.child("perChange").value.toString(),
                    ))
                Log.d(TAG," Fetched from DB Loers${losers} fffffffffffffffffffffffffffffff")
            }


        }
        if(callFetchLosers){
            Log.d(TAG,"calling Fetchlosers")
            withContext(Dispatchers.IO){  losers=  async { fetchlosers() }.await()
            }
            updateDBLosers(losers)
        }
        return losers


    }

    private fun updateDBLosers(losers: HashMap<String, HashMap<String, String>>) {
        val ref =FirebaseDatabase.getInstance().reference
        //var gainers = hashMapOf(0 to hashMapOf<String,String>())
        Log.d(TAG,"calling Fetchlosers")
        ref.child("losers").updateChildren(losers as Map<String, Any>) .addOnSuccessListener {
        }
    }

    private suspend fun fetchlosers(): HashMap<String, HashMap<String, String>> {


        var losers = hashMapOf("0" to hashMapOf<String,String>())
        withContext(Dispatchers.IO){
            val request = Request.Builder()
                .url("https://nse-data1.p.rapidapi.com/top_loosers")
                .get()
                .addHeader("x-rapidapi-host", "nse-data1.p.rapidapi.com")
                .addHeader("x-rapidapi-key", getString(R.string.x_rapidapi_key))
                .build()
            val response = client.newCall(request).execute()
            val jsonDataString = response.body?.string()
            val json = JSONObject(jsonDataString)
            val losers_array= json.getJSONObject("body").getJSONObject("NIFTYNEXT50")
                .getJSONArray("data")
            for (i in 0 until losers_array.length()) {
                Log.d(TAG,"called ${losers_array.getJSONObject(i)}")
                losers.put(i.toString(), hashMapOf(
                    "symbol" to losers_array.getJSONObject(i).getString("symbol"),
                    "series"  to  losers_array.getJSONObject(i).getString("series"),
                    "open_price"  to  losers_array.getJSONObject(i).getString("open_price"),
                    "high_price"  to  losers_array.getJSONObject(i).getString("high_price"),
                    "low_price"  to  losers_array.getJSONObject(i).getString("low_price"),
                    "ltp"  to  losers_array.getJSONObject(i).getString("ltp"),
                    "prev_price"  to losers_array.getJSONObject(i).getString("prev_price"),
                    "net_price"  to  losers_array.getJSONObject(i).getString("net_price"),
                    "trade_quantity"  to  losers_array.getJSONObject(i).getString("trade_quantity"),
                    "turnover"  to  losers_array.getJSONObject(i).getString("turnover"),
                    "market_type"  to  losers_array.getJSONObject(i).getString("market_type"),
                    "ca_ex_dt"  to  losers_array.getJSONObject(i).getString("ca_ex_dt"),
                    "ca_purpose"  to  losers_array.getJSONObject(i).getString("ca_purpose"),
                    "perChange"  to  losers_array.getJSONObject(i).getString("perChange"),
                ))
            }
            if (losers_array.getJSONObject(0).getString("symbol")!=""){
            }
        }
        return losers
    }

    private suspend fun updateGainerrecycler(gainers: Deferred<HashMap<String, HashMap<String, String>>>) {
        for (i in 0 until  gainers.await().size){
            val GainersClass =Gainers(
                gainers.await().get(i.toString())?.get("symbol"),
                gainers.await().get(i.toString())?.get("series"),
                gainers.await().get(i.toString())?.get("open_price"),
                gainers.await().get(i.toString())?.get("high_price"),
                gainers.await().get(i.toString())?.get("low_price"),
                gainers.await().get(i.toString())?.get("ltp"),
                gainers.await().get(i.toString())?.get("prev_price"),
                gainers.await().get(i.toString())?.get("net_price"),
                gainers.await().get(i.toString())?.get("trade_quantity"),
                gainers.await().get(i.toString())?.get("turnover"),
                gainers.await().get(i.toString())?.get("market_type"),
                gainers.await().get(i.toString())?.get("ca_ex_dt"),
                gainers.await().get(i.toString())?.get("ca_purpose"),
                gainers.await().get(i.toString())?.get("perChange"),
            )
             gainerRecycleradapter.add(GainersClass)
        }
        gainerRecycler.adapter =gainerRecycleradapter
    }

    private suspend fun updateMarketInfoUI(market_info: Deferred<HashMap<String, String>>) {
        if(market_info.await().get("marketStatus") != ""){
            if (market_info.await().get("marketStatus")=="Close"){
                marketStatus.text = "CLOSED"
                marketStatus.setTextColor( resources.getColor(R.color.light_red))
            }else{
                marketStatus.text = "OPEN"
                marketStatus.setTextColor( resources.getColor(R.color.light_green))
            }
            last.text = market_info.await().get("last")
            variation.text = "${market_info.await().get("variation")
            } ( ${market_info.await().get("percentChange")}% )"
            if (market_info.await().get("variation")?.toDouble()!!<0) {
                variation.setTextColor( resources.getColor(R.color.light_red))
            }else{
                variation.setTextColor( resources.getColor(R.color.light_green))
            }
        }
    }

    private suspend fun fetchDBGainers(): HashMap<String, HashMap<String, String>> {
        val ref =FirebaseDatabase.getInstance().reference
        var gainers = hashMapOf("0" to hashMapOf<String,String>())
        var callFetchGainers =true
        ref.child("gainers").get().addOnSuccessListener {
            Log.d(TAG,"Gainers taken from Firebase")
            if (it.child("today").value != LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))){
                callFetchGainers =false
                Log.d(TAG,"requested to call FetchGainers")
            }
            try {
                for ( i in 0 until it.childrenCount ){

                    if (it.child("symbol").value == null) {
                        Log.d(TAG,"${i} Nulled at  at ${it.child("symbol").value}")
                        continue}
                    gainers.put(
                        i.toString(), hashMapOf(
                        "symbol" to it.child("symbol").value as String,
                        "series"  to  it.child("series").value as String,
                        "open_price"  to  it.child("open_price").value as String,
                        "high_price"  to  it.child("high_price").value as String,
                        "low_price"  to  it.child("low_price").value as String,
                        "ltp"  to  it.child("ltp").value as String,
                        "prev_price"  to it.child("prev_price").value as String,
                        "net_price"  to  it.child("net_price").value as String,
                        "trade_quantity"  to  it.child("trade_quantity").value as String,
                        "turnover"  to  it.child("turnover").value as String,
                        "market_type"  to it.child("market_type").value as String,
                        "ca_ex_dt"  to it.child("ca_ex_dt").value as String,
                        "ca_purpose"  to it.child("ca_purpose").value as String,
                        "perChange"  to  it.child("perChange").value as String,
                    ))
                }
            } catch (e: Exception) {
            }


        }
        if(callFetchGainers){
            Log.d(TAG,"calling FetchGainers")
             withContext(Dispatchers.IO){  gainers=  async { fetchGainers() }.await()


             }
            updateDBGainers(gainers)
        }
        return gainers
    }



    private fun updateDBGainers(gainers: HashMap<String, HashMap<String, String>>) {
        val ref =FirebaseDatabase.getInstance().reference
        //var gainers = hashMapOf(0 to hashMapOf<String,String>())
        ref.child("gainers").updateChildren(gainers as Map<String, Any>) .addOnSuccessListener {
        }
    }

    suspend fun fetchGainers(): HashMap<String, HashMap<String, String>> {
        var gainers = hashMapOf("0" to hashMapOf<String,String>())

        withContext(Dispatchers.IO){
            val request = Request.Builder()
                .url("https://nse-data1.p.rapidapi.com/top_gainers")
                .get()
                .addHeader("x-rapidapi-host", "nse-data1.p.rapidapi.com")
                .addHeader("x-rapidapi-key", getString(R.string.x_rapidapi_key))
                .build()
            val response = client.newCall(request).execute()
            val jsonDataString = response.body?.string()
            val json = JSONObject(jsonDataString)
            val gainers_array= json.getJSONObject("body").getJSONObject("NIFTYNEXT50")
                .getJSONArray("data")
            for (i in 0 until gainers_array.length()) {
                gainers.put(i.toString(), hashMapOf(
                    "symbol" to gainers_array.getJSONObject(i).getString("symbol"),
                    "series"  to  gainers_array.getJSONObject(i).getString("series"),
                    "open_price"  to  gainers_array.getJSONObject(i).getString("open_price"),
                    "high_price"  to  gainers_array.getJSONObject(i).getString("high_price"),
                    "low_price"  to  gainers_array.getJSONObject(i).getString("low_price"),
                    "ltp"  to  gainers_array.getJSONObject(i).getString("ltp"),
                    "prev_price"  to gainers_array.getJSONObject(i).getString("prev_price"),
                    "net_price"  to  gainers_array.getJSONObject(i).getString("net_price"),
                    "trade_quantity"  to  gainers_array.getJSONObject(i).getString("trade_quantity"),
                    "turnover"  to  gainers_array.getJSONObject(i).getString("turnover"),
                    "market_type"  to  gainers_array.getJSONObject(i).getString("market_type"),
                    "ca_ex_dt"  to  gainers_array.getJSONObject(i).getString("ca_ex_dt"),
                    "ca_purpose"  to  gainers_array.getJSONObject(i).getString("ca_purpose"),
                    "perChange"  to  gainers_array.getJSONObject(i).getString("perChange"),
                ))
            }
            if (gainers_array.getJSONObject(0).getString("symbol")!=""){
            }
        }
        return gainers
    }

    suspend fun fetchMarketInfo(): HashMap<String, String> {
        val request = Request.Builder()
            .url("https://nse-data1.p.rapidapi.com/market_status")
            .get()
            .addHeader("x-rapidapi-host", "nse-data1.p.rapidapi.com")
            .addHeader("x-rapidapi-key", getString( R.string.x_rapidapi_key))
            .build()
        var market_info = hashMapOf(
            "market" to "",
            "marketStatus" to "",
            "tradeDate" to "",
            "index" to "",
            "last" to "",
            "variation" to  "",
            "percentChange" to "",
            "marketStatusMessage" to "",
            "todayDate" to  "",
        )
        try {
            val response = client.newCall(request).execute()
            val jsonDataString = response.body?.string()
            val json = JSONObject(jsonDataString)
            val market_info_jsonArray = json.getJSONObject("body").getJSONArray("marketState")
            market_info = hashMapOf(
                "market" to market_info_jsonArray.getJSONObject(0).getString("market"),
                "marketStatus" to market_info_jsonArray.getJSONObject(0).getString("marketStatus"),
                "tradeDate" to market_info_jsonArray.getJSONObject(0).getString("tradeDate"),
                "index" to market_info_jsonArray.getJSONObject(0).getString("index"),
                "last" to market_info_jsonArray.getJSONObject(0).getString("last"),
                "variation" to  Math.ceil(market_info_jsonArray.getJSONObject(0).getString("variation").toDouble()).toString(),
                "percentChange" to market_info_jsonArray.getJSONObject(0).getString("percentChange"),
                "marketStatusMessage" to market_info_jsonArray.getJSONObject(0).getString("marketStatusMessage"),
                "todayDate" to  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            )

            return  market_info
        }catch (e:Exception){
            Log.d(TAG,"Exception Occurred ${e}")
            return market_info
        }
    }

    suspend fun fetchDBMarketInfo(): HashMap<String, String> {
        val ref = FirebaseDatabase.getInstance().reference
        var market_info_retrive_data = hashMapOf(
            "market" to "",
            "marketStatus" to "",
            "tradeDate" to "",
            "index" to "",
            "last" to "",
            "variation" to  "",
            "percentChange" to "",
            "marketStatusMessage" to "",
            "todayDate" to  "",
        )
        ref.child("today").get().addOnSuccessListener {

             market_info_retrive_data = hashMapOf<String,String>(
                "market" to it.child("market").value as String,
                "marketStatus" to it.child("marketStatus").value as String,
                "tradeDate" to it.child("tradeDate").value as String,
                "index" to it.child("index").value as String,
                "last" to it.child("last").value as String,
                "variation" to  it.child("variation").value as String,
                "percentChange" to it.child("percentChange").value as String,
                "marketStatusMessage" to it.child("marketStatusMessage").value as String,
                "todayDate" to  it.child("todayDate").value as String,
            )
        }
            if (LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ==
                market_info_retrive_data.get("todayDate"))
            {
                return market_info_retrive_data
            }else {
                market_info_retrive_data =   fetchMarketInfo()
                return market_info_retrive_data
                updateDBMarketInfo(market_info_retrive_data)
            }
    }
    suspend fun updateDBMarketInfo(market_info: HashMap<String, String>) {
        val market_info_update_data = hashMapOf(
            "market" to market_info.get("market"),
            "marketStatus" to market_info.get("marketStatus"),
            "tradeDate" to market_info.get("tradeDate"),
            "index" to market_info.get("index"),
            "last" to market_info.get("last"),
            "variation" to  Math.ceil(market_info.get("variation")!!.toDouble()).toString(),
            "percentChange" to market_info.get("percentChange"),
            "marketStatusMessage" to market_info.get("marketStatusMessage"),
            "todayDate" to  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
        val ref = FirebaseDatabase.getInstance().reference
        ref.child("today").updateChildren(market_info_update_data as Map<String, Any>)
    }
    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }


}



