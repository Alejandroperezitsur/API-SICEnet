/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.marsphotos.data

import AddCookiesInterceptor
import ReceivedCookiesInterceptor
import android.content.Context
import com.example.marsphotos.network.MarsApiService
import com.example.marsphotos.network.SICENETWService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory


/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {

    val marsPhotosRepository: MarsPhotosRepository
    val snRepository: SNRepository
}

/**
 * Implementation for the Dependency Injection container at the application level.
 *
 * Variables are initialized lazily and the same instance is shared across the whole app.
 */
class DefaultAppContainer(applicationContext: Context) : AppContainer {
    private val baseUrl = "https://android-kotlin-fun-mars-server.appspot.com/"
    private val baseUrlSN = "https://sicenet.surguanajuato.tecnm.mx"
    private var client: OkHttpClient
    init {
        client = OkHttpClient()
        val builder = OkHttpClient.Builder()

        builder.addInterceptor(AddCookiesInterceptor(applicationContext)) // VERY VERY IMPORTANT

        builder.addInterceptor(ReceivedCookiesInterceptor(applicationContext)) // VERY VERY IMPORTANT

        client = builder.build()
    }
    /**
     * Use the Retrofit builder to build a retrofit object using a kotlinx.serialization converter
     */
    private val retrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .baseUrl(baseUrl)
        .build()

    private val retrofitSN: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrlSN)
        .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
        .client(client)
        .build()

    //bodyacceso.toRequestBody("text/xml; charset=utf-8".toMediaType())

    /**
     * Retrofit service object for creating api calls
     */
    private val retrofitService: MarsApiService by lazy {
        retrofit.create(MarsApiService::class.java)
    }

    /**
     * Retrofit service object for creating api calls
     */
    private val retrofitServiceSN: SICENETWService by lazy {
        retrofitSN.create(SICENETWService::class.java)
    }
    override val marsPhotosRepository: NetworkMarsPhotosRepository by lazy {
        NetworkMarsPhotosRepository(retrofitService)
    }
    /**
     * DI implementation for Mars photos repository
     */
    override val snRepository: NetworSNRepository by lazy {
        NetworSNRepository(retrofitServiceSN)
    }
}
