package com.example.test_okhttp_request.ui.login

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.test_okhttp_request.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.util.KeyStoreUtils
import nl.altindag.ssl.util.TrustManagerUtils
import okhttp3.OkHttpClient
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class TestRequestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val url: EditText = binding.url!!
        val login = binding.executeRequest
        // This url can gets your ipv5 addr
        binding.url!!.setText("http://6.ipw.cn")
        binding.switchUrl?.setOnClickListener {
            val sw = it as SwitchCompat
            if (sw.isChecked) {
                binding.url!!.setText("http://4.ipw.cn")
            } else {
//                http://[2402:4e00:1013:e500:0:9671:f018:4947]
                binding.url!!.setText("http://6.ipw.cn")
            }
        }

        login?.setOnClickListener {
            MainScope().launch(context = Dispatchers.IO) {
                val clentBulder = OkHttpClient().newBuilder()
                try {
                    if (binding.proxyAddr!!.text.toString() != "") {
                        val proxyAddr = binding.proxyAddr!!.text.toString().split(":")
                        clentBulder.proxy(
                            Proxy(
                                Proxy.Type.SOCKS,
                                InetSocketAddress(proxyAddr[0], proxyAddr[1].toInt())
                            )
                        )
                    }
                    val request = okhttp3.Request.Builder().get().url(url.text.toString())
                        .build()
                    val client = clentBulder
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .writeTimeout(5, TimeUnit.SECONDS)
                        .build()
                    val resp = client.newCall(request).execute()
                    launch(Dispatchers.Main) {
                        if (resp.isSuccessful) {
                            binding.result?.text = "okhttp request:" + resp.body?.string()
                        } else {
                            binding.result?.text = "okhttp request:" + resp.code.toString()
                        }
                    }
                } catch (e: java.lang.Exception) {
                    launch(Dispatchers.Main) {
                        binding.result?.text = "okhttp request:" + e.message
                    }
                }
            }

            MainScope().launch(context = Dispatchers.IO) {
                try {
                    val process: Process = if (binding.proxyAddr!!.text.toString() == "") {
                        Runtime.getRuntime().exec("curl  ${binding.url!!.text.toString()}")
                    } else {
                        val command =
                            "curl -x socks5h://${binding.proxyAddr!!.text.toString()} ${binding.url!!.text.toString()}"
                        Runtime.getRuntime().exec(command)
                    }
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    val sb = java.lang.StringBuilder()
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    launch(context = Dispatchers.Main) {
                        binding.curlResult!!.text = "curl request: " + sb.toString()
                    }
                    reader.close()
                } catch (e: IOException) {
                    launch(context = Dispatchers.Main) {
                        binding.curlResult!!.text = "curl request: " + e.message
                    }
                }
            }
        }


    }


}
