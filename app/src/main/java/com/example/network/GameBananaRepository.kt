package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GameBananaRepository {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val api: GameBananaApi = Retrofit.Builder()
        .baseUrl("https://gamebanana.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GameBananaApi::class.java)

    suspend fun fetchWorldBoxMods(
        page: Int = 1,
        sort: String = "new",
        search: String? = null
    ): Result<List<GbModItem>> {
        return try {
            val response = api.getSubfeed(
                gameId = 11967,
                page = page,
                sort = sort,
                search = if (search.isNullOrBlank()) null else search.trim()
            )
            if (response.isSuccessful) {
                val records = response.body()?.records ?: emptyList()
                Result.success(records)
            } else {
                Result.failure(Exception("GameBanana API HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getModFilesAndDetails(modId: Long): Result<Pair<GbModProfileResponse, List<GbModFile>>> {
        return try {
            val response = api.getModProfile(modId)
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                val files = profile.files ?: emptyList()
                if (files.isNotEmpty()) {
                    Result.success(Pair(profile, files))
                } else {
                    val fallbackFile = GbModFile(
                        id = modId,
                        fileName = "worldbox_mod_${modId}.zip",
                        fileSize = 0L,
                        downloadUrl = "https://gamebanana.com/dl/$modId",
                        description = "Direct Mod Package"
                    )
                    Result.success(Pair(profile, listOf(fallbackFile)))
                }
            } else {
                val fallbackProfile = GbModProfileResponse(id = modId, name = "Mod #$modId")
                val fallbackFile = GbModFile(
                    id = modId,
                    fileName = "worldbox_mod_${modId}.zip",
                    fileSize = 0L,
                    downloadUrl = "https://gamebanana.com/dl/$modId",
                    description = "Direct Download Package"
                )
                Result.success(Pair(fallbackProfile, listOf(fallbackFile)))
            }
        } catch (e: Exception) {
            val fallbackProfile = GbModProfileResponse(id = modId, name = "Mod #$modId")
            val fallbackFile = GbModFile(
                id = modId,
                fileName = "worldbox_mod_${modId}.zip",
                fileSize = 0L,
                downloadUrl = "https://gamebanana.com/dl/$modId",
                description = "Direct Download Package"
            )
            Result.success(Pair(fallbackProfile, listOf(fallbackFile)))
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
