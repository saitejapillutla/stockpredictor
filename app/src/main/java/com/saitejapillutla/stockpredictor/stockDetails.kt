package com.saitejapillutla.stockpredictor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartAnimationType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartZoomType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import kotlinx.android.synthetic.main.activity_stock_details.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*

class stockDetails : AppCompatActivity() {
    //  val aaChartView = findViewById<AAChartView>(R.id.aa_chart_view)

    var client = OkHttpClient()
    val TAG = "stockdetailsTAG"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_details)


        var aaChartModel: AAChartModel = AAChartModel()
            .chartType(AAChartType.Area)
            .animationType(AAChartAnimationType.Elastic)
            .dataLabelsEnabled(false)
            .animationDuration(22)
            .title("title")
            .subtitle("subtitle")
            .colorsTheme(arrayOf("#0c9674", "#7dffc0", "#d11b5f", "#facd32", "#ffffa0",))
            .backgroundColor(R.color.black)
            .dataLabelsEnabled(true)
            .series(
                arrayOf(
                    AASeriesElement()
                        .name("Berlin")
                        .data(
                            arrayOf(
                                3.9,
                                4.2,
                                5.7,
                                8.5,
                                11.9,
                                15.2,
                                17.0,
                                16.6,
                                14.2,
                                10.3,
                                6.6,
                                4.8
                            )
                        )
                )
            )

        //The chart view object calls the instance object of AAChartModel and draws the final graphic

        aa_chart_view.aa_drawChartWithChartModel(aaChartModel)
        CoroutineScope(Dispatchers.Main).launch {
          var returndata =  async(Dispatchers.IO) {  fetchCompanyDetails("RELIANCE.BSE")}
            aaChartModel =AAChartModel()
                .chartType(AAChartType.Area)
                .title("title")
                .subtitle("subtitle")
                .gradientColorEnable(true)
                .zoomType(AAChartZoomType.XY)
                .backgroundColor(R.color.black)
                .dataLabelsEnabled(true)
                .series(
                    arrayOf(
                        AASeriesElement()
                            .name("Berlin")
                            .data(returndata.await() as Array<Any>)
                    )
                )

            aa_chart_view.aa_drawChartWithChartModel(aaChartModel)


        }


    }



    suspend fun fetchCompanyDetails(CompanyTag: String): Array<out Any> {
        val url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=${CompanyTag}&outputsize=full&apikey=${R.string.alpha_vintage_API_KEY}"
        var returndata = false


        var stackOpen = ArrayDeque<Double>()
        var stackhigh = ArrayDeque<Double>()
        var stackclose = ArrayDeque<Double>()
        var stackvolume = ArrayDeque<Double>()
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            val response = client.newCall(request).execute()
            val jsonDataString = response.body?.string()
            var json = JSONObject(jsonDataString)
            val items = json.getJSONObject("Time Series (Daily)")

            items.keys().forEachRemaining {
                stackOpen.push( items.getJSONObject(it).get("1. open").toString().toDouble())
                stackhigh.push( items.getJSONObject(it).get("2. high").toString().toDouble())
                stackclose.push( items.getJSONObject(it).get("4. close").toString().toDouble())
                stackvolume.push( items.getJSONObject(it).get("6. volume").toString().toDouble())
            }
            returndata =true
        }
        var open  = emptyArray<Double>()
        open= stackOpen.reversed().toTypedArray()
        return open

    }
}