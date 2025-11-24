package com.kotla.anifloat.data.api

import com.kotla.anifloat.data.model.GraphQLRequest
import com.kotla.anifloat.data.model.GraphQLResponse
import com.kotla.anifloat.data.model.MediaListCollectionData
import com.kotla.anifloat.data.model.SaveMediaListEntryData
import com.kotla.anifloat.data.model.ViewerData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AnilistApi {
    
    @POST("/")
    suspend fun getViewer(
        @Header("Authorization") token: String,
        @Body request: GraphQLRequest
    ): Response<GraphQLResponse<ViewerData>>

    @POST("/")
    suspend fun getUserMediaList(
        @Header("Authorization") token: String,
        @Body request: GraphQLRequest
    ): Response<GraphQLResponse<MediaListCollectionData>>
    
    @POST("/")
    suspend fun updateProgress(
        @Header("Authorization") token: String,
        @Body request: GraphQLRequest
    ): Response<GraphQLResponse<SaveMediaListEntryData>>
}

