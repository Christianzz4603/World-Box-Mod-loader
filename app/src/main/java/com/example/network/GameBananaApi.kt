package com.example.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GameBananaApi {

    @GET("apiv3/Subfeed")
    suspend fun getSubfeed(
        @Query("_idRow") gameId: Long = 11967, // WorldBox Game ID
        @Query("_sModelName") modelName: String = "Mod",
        @Query("_nPage") page: Int = 1,
        @Query("_sSort") sort: String = "new", // "new", "popular", "rating"
        @Query("_sSearch") search: String? = null
    ): Response<GbSubfeedResponse>

    @GET("apiv3/Mod/{id}/ProfilePage")
    suspend fun getModProfile(
        @Path("id") modId: Long
    ): Response<GbModProfileResponse>

    @GET("api/v1/Mod/{id}")
    suspend fun getModV1(
        @Path("id") modId: Long
    ): Response<Map<String, Any>>
}
