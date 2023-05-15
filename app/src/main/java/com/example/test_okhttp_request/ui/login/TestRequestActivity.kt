package com.example.test_okhttp_request.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.test_okhttp_request.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.IOException
import java.io.InputStream
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
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
        val self = this
        binding.switchUrl?.setOnClickListener {
            val sw = it as SwitchCompat
            if (sw.isChecked) {
                binding?.url!!.setText("https://cert.pgyer.eu.org:4443/1.json")
            } else {
                binding?.url!!.setText("https://bing.com")
            }
        }

        login?.setOnClickListener {
            MainScope().launch(context = Dispatchers.IO) {
                try {
                    val request = okhttp3.Request.Builder().get().url(url.text.toString()).build()
                    val clent = OkHttpClient().newBuilder()
                        .setSSLCertificate(self.assets.open("rootCA.pem"))
                        .connectTimeout(1, TimeUnit.SECONDS)
                        .readTimeout(1, TimeUnit.SECONDS)
                        .writeTimeout(1, TimeUnit.SECONDS)
/*
                        .hostnameVerifier(object : HostnameVerifier {
                            @SuppressLint("BadHostnameVerifier")
                            override fun verify(hostname: String?, session: SSLSession?): Boolean {
                                // ignore domain
                                return true
                            }
                        })
*/
                        .build()
                    val resp = clent.newCall(request).execute()
                    launch(Dispatchers.Main) {
                        if (resp.isSuccessful) {
                            binding.result?.text = resp.body?.string()
                        }
                    }
                } catch (e: java.lang.Exception) {
                    launch(Dispatchers.Main) {
                        binding.result?.text = e.message
                    }
                }
            }
        }
    }

    fun OkHttpClient.Builder.setSSLCertificate(
        vararg certificates: InputStream,
        bksFile: InputStream? = null,
        password: String? = null
    ) = apply {
        val trustManager = prepareTrustManager(*certificates)?.let { chooseTrustManager(it) }
        setSSLCertificate(trustManager!!, bksFile, password)
    }

    fun OkHttpClient.Builder.setSSLCertificate(
        trustManager: X509TrustManager,
        bksFile: InputStream? = null,
        password: String? = null,
    ) = apply {
        try {
            val trustManagerFinal: X509TrustManager = trustManager
            val keyManagers = prepareKeyManager(bksFile, password)
            val sslContext = SSLContext.getInstance("TLS")
            // 用上面得到的trustManagers初始化SSLContext，这样sslContext就会信任keyStore中的证书
            // 第一个参数是授权的密钥管理器，用来授权验证，比如授权自签名的证书验证。第二个是被授权的证书管理器，用来验证服务器端的证书
            sslContext.init(keyManagers, arrayOf<TrustManager?>(trustManagerFinal), null)
            // 通过sslContext获取SSLSocketFactory对象
            sslSocketFactory(sslContext.socketFactory, trustManagerFinal)
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        } catch (e: KeyManagementException) {
            throw AssertionError(e)
        }
    }

    internal fun prepareKeyManager(bksFile: InputStream?, password: String?): Array<KeyManager>? {
        try {
            if (bksFile == null || password == null) return null
            val clientKeyStore = KeyStore.getInstance("BKS")
            clientKeyStore.load(bksFile, password.toCharArray())
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(clientKeyStore, password.toCharArray())
            return kmf.keyManagers
        } catch (e: Exception) {

        }
        return null
    }


    fun prepareTrustManager(vararg certificates: InputStream?): Array<TrustManager>? {
        if (certificates.isEmpty()) return null
        try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            // 创建一个默认类型的KeyStore，存储我们信任的证书
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            cload(null)
            for ((index, certStream) in certificates.withIndex()) {
                val certificateAlias = (index).toString()
                // 证书工厂根据证书文件的流生成证书 cert
                val cert = certificateFactory.generateCertificate(certStream)
                // 将cert作为可信证书放入到keyStore中
                keyStore.setCertificateEntry(certificateAlias, cert)
                try {
                    certStream?.close()
                } catch (e: IOException) {
                }
            }
            // 我们创建一个默认类型的TrustManagerFactory
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            // 用我们之前的keyStore实例初始化TrustManagerFactory，这样tmf就会信任keyStore中的证书
            tmf.init(keyStore)
            // 通过tmf获取TrustManager数组，TrustManager也会信任keyStore中的证书
            return tmf.trustManagers
        } catch (e: Exception) {
        }
        return null
    }

    fun chooseTrustManager(trustManagers: Array<TrustManager>): X509TrustManager? {
        for (trustManager in trustManagers) {
            if (trustManager is X509TrustManager) {
                return trustManager
            }
        }
        return null
    }

}
