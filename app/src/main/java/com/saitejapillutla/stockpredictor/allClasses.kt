package com.saitejapillutla.stockpredictor

import android.content.Intent
import android.graphics.Color
import com.google.firebase.database.IgnoreExtraProperties
import com.pacific.adapter.AdapterViewHolder
import com.pacific.adapter.SimpleRecyclerItem
import kotlinx.android.synthetic.main.gainer_looser_item.view.*

class allClasses {
}
@IgnoreExtraProperties
data class User(val username: String? = null, val email: String? = null,
                val lastSeen :String? = null,
                val uid :String) {


    // Null default values create a no-argument default constructor, which is needed
    // for deserialization from a DataSnapshot.
}

class Gainers(

    val symbol: String?,
    val series:String?,
    val open_price:String?,
    val high_price:String?,
    val low_price:String?,
    val ltp:String?,
    val prev_price:String?,
    val net_price:String?,
    val trade_quantity:String?,
    val turnover:String?,
    val market_type:String?,
    val ca_ex_dt:String?,
    val ca_purpose:String?,
    val perChange:String?,
    ) : SimpleRecyclerItem() {
    override fun bind(holder: AdapterViewHolder) {
        //Tlkndsal
    }
    override fun getLayout(): Int {
        return R.layout.gainer_looser_item
    }
    override fun unbind(holder: AdapterViewHolder) {
        super.unbind(holder)
    }

    override fun onViewAttachedToWindow(holder: AdapterViewHolder) {
        super.onViewAttachedToWindow(holder)
        val v=holder.itemView
        v.textView6.text ="${symbol}"
        v.textView9.text ="Oened at ${low_price}"
        v.textView8.text = "ltp at ${ltp}"


        if (perChange != null) {
            if (perChange.toDouble()<0){
                v.textView7.setTextColor(Color.RED)
                v.gainer_looser_items.setBackgroundResource(R.drawable.rounded_red)
            }else{
                v.textView7.setTextColor(Color.GREEN)
            }
        }
        v.textView7.text ="${perChange}"
        v.gainer_looser_items.setOnClickListener {
            val intent = Intent(v.context,stockDetails ::class.java)
            intent.putExtra( "symbol",symbol   )
            intent.putExtra( "series",series   )
            intent.putExtra( "open_price",open_price   )
            intent.putExtra( "high_price", high_price  )
            intent.putExtra( "low_price",  low_price )
            intent.putExtra( "ltp",   ltp)
            intent.putExtra( "prev_price",   prev_price)
            intent.putExtra( "net_price",   net_price)
            intent.putExtra( "trade_quantity",trade_quantity   )
            intent.putExtra( "turnover",turnover   )
            intent.putExtra( "market_type",market_type   )
            intent.putExtra( "ca_ex_dt",ca_ex_dt   )
            intent.putExtra( "ca_purpose",  ca_purpose )
            intent.putExtra( "perChange", perChange  )
            v.context.startActivity(intent)


        }

    }



}
