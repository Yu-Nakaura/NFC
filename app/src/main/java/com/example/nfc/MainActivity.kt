package com.example.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.NfcF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    val felica = FelicaReader(this, this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        felica.setListener(felicaListener)
    }
    override fun onResume() {
        super.onResume()
        felica.start()
    }
    override fun onPause() {
        super.onPause()
        felica.stop()
    }

    private val felicaListener = object : FelicaReaderInterface{
        override fun onReadTag(tag : Tag) {                     // データ受信イベント
            val tvMain = findViewById<TextView>(R.id.tvMain)
            val idm : ByteArray = tag.id
            tag.techList
            tvMain.text = byteToHex(idm)
            Log.d("Sample","${byteToHex(idm)}")

            var status : String = byteToHex(idm)
            var level : Int = byteToHex(idm).length
            Log.d("level", "${byteToHex(idm).length}")
        }

        override fun onConnect() {
            Log.d("Sample","onConnected")
        }
    }

    private fun byteToHex(b : ByteArray) : String{
        var s : String = ""
        for (i in 0..b.size-1){
            s += "[%02X]".format(b[i])
        }
        return s
    }
}
interface FelicaReaderInterface : FelicaReader.Listener {
    fun onReadTag(tag : Tag)                        // タグ受信イベント
    fun onConnect()
}

class FelicaReader(private val context: Context, private val activity : Activity) : android.os.Handler() {
    private var nfcmanager : NfcManager? = null
    private var nfcadapter : NfcAdapter? = null
    private var callback : CustomReaderCallback? = null

    private var listener: FelicaReaderInterface? = null
    interface Listener {}

    fun start(){
        callback = CustomReaderCallback()
        callback?.setHandler(this)
        nfcmanager = context.getSystemService(Context.NFC_SERVICE) as NfcManager?
        nfcadapter = nfcmanager!!.getDefaultAdapter()
        nfcadapter!!.enableReaderMode(activity,callback
            ,NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,null)
    }
    fun stop(){
        nfcadapter!!.disableReaderMode(activity)
        callback = null
    }

    override fun handleMessage(msg: Message) {                  // コールバックからのメッセージクラス
        if (msg.arg1 == 1){                                     // 読み取り終了
            listener?.onReadTag(msg.obj as Tag)                 // 拡張用
        }
        if (msg.arg1 == 2){                                     // 読み取り終了
            listener?.onConnect()                               // 拡張用
        }
    }

    fun setListener(listener: FelicaReader.Listener?) {         // イベント受け取り先を設定
        if (listener is FelicaReaderInterface) {
            this.listener = listener as FelicaReaderInterface
        }
    }

    private class CustomReaderCallback : NfcAdapter.ReaderCallback {
        private var handler : android.os.Handler? = null
        override fun onTagDiscovered(tag: Tag) {
            Log.d("Sample", tag.id.toString())
            val msg = Message.obtain()
            msg.arg1 = 1
            msg.obj = tag
            if (handler != null) handler?.sendMessage(msg)
            val nfc : NfcF = NfcF.get(tag) ?: return
            try {
                nfc.connect()
                //nfc.transceive()
                nfc.close()
                msg.arg1 = 2
                msg.obj = tag
                if (handler != null) handler?.sendMessage(msg)
            }catch (e : Exception){
                nfc.close()
            }
        }
        fun setHandler(handler  : android.os.Handler){
            this.handler = handler
        }
    }
}