package eu.kanade.tachiyomi.data.sync.api

import eu.kanade.tachiyomi.data.sync.api.models.AuthResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

/**
 * TachiWeb Retrofit API
 */

interface TWApi {
    @GET("auth")
    fun checkAuth(@Query("password") password: String): Observable<AuthResponse>

    companion object {
        fun create(client: OkHttpClient, baseUrl: String) = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(TWApi::class.java)
    }
}