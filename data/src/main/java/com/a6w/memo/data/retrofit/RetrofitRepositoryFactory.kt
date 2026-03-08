package com.a6w.memo.data.retrofit

import com.a6w.memo.data.repository.AddressSearchRepositoryImpl
import com.a6w.memo.data.retrofit.api.KakaoLocalAPI
import com.a6w.memo.data.retrofit.source.RetrofitAddressDataSource
import com.a6w.memo.domain.repository.AddressSearchRepository
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit Repository Factory
 * - Create each repository with Retrofit
 * - Each factory initializes Retrofit module, and inject to repository implementation
 */
object RetrofitRepositoryFactory {
    private const val KAKAO_BASE_URL = "https://dapi.kakao.com/"

    fun createAddressSearchRepository(apiKey: String): AddressSearchRepository {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "KakaoAK $apiKey")
                .build()
            chain.proceed(request)
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)

        val retrofit = Retrofit.Builder()
            .baseUrl(KAKAO_BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(KakaoLocalAPI::class.java)
        val dataSource = RetrofitAddressDataSource(api)

        // 내부 구현체를 조립해서 Domain 인터페이스로 캐스팅하여 반환
        return AddressSearchRepositoryImpl(dataSource)
    }
}