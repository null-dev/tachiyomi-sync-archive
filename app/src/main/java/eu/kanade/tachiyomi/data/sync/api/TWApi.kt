package eu.kanade.tachiyomi.data.sync.api

import android.accounts.Account
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.sync.api.models.AuthResponse
import eu.kanade.tachiyomi.data.sync.api.models.TestAuthenticatedResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import rx.Observable

/**
 * TachiWeb Retrofit API
 */

interface TWApi {
    @GET("auth")
    fun checkAuth(@Query("password") password: String): Observable<AuthResponse>

    @POST("diff_sync")
    fun syncDiff(@Header("TW-Session") token: String, @Body diff: String): Observable<String>

    @GET("test_auth")
    fun testAuthenticated(@Header("TW-Session") token: String): Observable<TestAuthenticatedResponse>

    companion object {
        fun create(client: OkHttpClient, baseUrl: String) = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(TWApi::class.java)!!

        fun apiFromAccount(networkService: NetworkHelper, account: Account) = TWApi.create(networkService.client, account.name)!!
    }
}