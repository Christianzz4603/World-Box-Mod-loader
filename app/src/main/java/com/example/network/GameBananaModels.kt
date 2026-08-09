package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GbSubfeedResponse(
    @Json(name = "_aRecords") val records: List<GbModItem>? = null,
    @Json(name = "_aMetadata") val metadata: GbMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GbMetadata(
    @Json(name = "_nRecordCount") val totalRecords: Int? = null,
    @Json(name = "_nPerpage") val perPage: Int? = null
)

@JsonClass(generateAdapter = true)
data class GbModItem(
    @Json(name = "_idRow") val id: Long,
    @Json(name = "_sName") val name: String? = "Unnamed Mod",
    @Json(name = "_sProfileUrl") val profileUrl: String? = null,
    @Json(name = "_aSubmitter") val submitter: GbSubmitter? = null,
    @Json(name = "_aCategory") val category: GbCategory? = null,
    @Json(name = "_aPreviewMedia") val previewMedia: GbPreviewMedia? = null,
    @Json(name = "_nLikeCount") val likeCount: Int? = 0,
    @Json(name = "_nViewCount") val viewCount: Int? = 0,
    @Json(name = "_nDownloadCount") val downloadCount: Int? = 0,
    @Json(name = "_tsDateAdded") val dateAdded: Long? = 0L,
    @Json(name = "_sVersion") val version: String? = "1.0.0",
    @Json(name = "_sTeaser") val teaser: String? = null
)

@JsonClass(generateAdapter = true)
data class GbSubmitter(
    @Json(name = "_idRow") val id: Long? = null,
    @Json(name = "_sName") val name: String? = "Unknown Author",
    @Json(name = "_sAvatarUrl") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GbCategory(
    @Json(name = "_idRow") val id: Long? = null,
    @Json(name = "_sName") val name: String? = "General"
)

@JsonClass(generateAdapter = true)
data class GbPreviewMedia(
    @Json(name = "_aImages") val images: List<GbImage>? = null
)

@JsonClass(generateAdapter = true)
data class GbImage(
    @Json(name = "_sBaseUrl") val baseUrl: String? = null,
    @Json(name = "_sFile") val file: String? = null,
    @Json(name = "_sFile100") val file100: String? = null,
    @Json(name = "_sFile220") val file220: String? = null,
    @Json(name = "_sFile530") val file530: String? = null
) {
    fun getFullUrl(): String? {
        return if (!baseUrl.isNullOrEmpty() && !file.isNullOrEmpty()) {
            "$baseUrl/$file"
        } else if (!baseUrl.isNullOrEmpty() && !file530.isNullOrEmpty()) {
            "$baseUrl/$file530"
        } else if (!baseUrl.isNullOrEmpty() && !file220.isNullOrEmpty()) {
            "$baseUrl/$file220"
        } else null
    }
}

@JsonClass(generateAdapter = true)
data class GbModProfileResponse(
    @Json(name = "_idRow") val id: Long,
    @Json(name = "_sName") val name: String? = null,
    @Json(name = "_sDescription") val description: String? = null,
    @Json(name = "_sText") val textHtml: String? = null,
    @Json(name = "_aFiles") val files: List<GbModFile>? = null,
    @Json(name = "_sVersion") val version: String? = null
)

@JsonClass(generateAdapter = true)
data class GbModFile(
    @Json(name = "_idRow") val id: Long,
    @Json(name = "_sFile") val fileName: String,
    @Json(name = "_nFilesize") val fileSize: Long = 0L,
    @Json(name = "_sDownloadUrl") val downloadUrl: String,
    @Json(name = "_sDescription") val description: String? = null,
    @Json(name = "_sMd5Checksum") val md5Checksum: String? = null,
    @Json(name = "_nDownloadCount") val downloadCount: Int = 0
)
