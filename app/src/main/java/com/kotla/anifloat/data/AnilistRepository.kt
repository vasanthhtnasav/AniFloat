package com.kotla.anifloat.data

import com.kotla.anifloat.data.api.AnilistApi
import com.kotla.anifloat.data.model.GraphQLRequest
import com.kotla.anifloat.data.model.MediaListEntry
import com.kotla.anifloat.data.model.Viewer
import kotlinx.coroutines.flow.first

data class UserMediaListResult(
    val viewer: Viewer,
    val entries: List<MediaListEntry>
)

class AnilistRepository(
    private val api: AnilistApi,
    private val authRepository: AuthRepository
) {

    private suspend fun getToken(): String {
        val token = authRepository.accessToken.first() ?: throw IllegalStateException("No token found")
        return "Bearer $token"
    }

    suspend fun getCurrentUserAndList(): UserMediaListResult {
        val token = getToken()
        
        // 1. Get Viewer ID
        val viewerQuery = """
            query {
                Viewer {
                    id
                    name
                    avatar {
                        large
                    }
                }
            }
        """.trimIndent()
        
        val viewerResponse = api.getViewer(token, GraphQLRequest(viewerQuery, emptyMap()))
        val userId = viewerResponse.body()?.data?.Viewer?.id ?: throw Exception("Failed to get user")

        // 2. Get Media List
        val listQuery = """
            query(${"$"}userId: Int) {
                MediaListCollection(userId: ${"$"}userId, type: ANIME, status: CURRENT) {
                    lists {
                        entries {
                            id
                            progress
                            updatedAt
                            media {
                                id
                                title {
                                    userPreferred
                                }
                                coverImage {
                                    medium
                                }
                                episodes
                                relations {
                                    edges {
                                        relationType
                                        node {
                                            id
                                            title {
                                                userPreferred
                                            }
                                            coverImage {
                                                medium
                                            }
                                            episodes
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val listResponse = api.getUserMediaList(token, GraphQLRequest(listQuery, mapOf("userId" to userId)))
        val entries = listResponse.body()?.data?.MediaListCollection?.lists?.flatMap { it.entries } ?: emptyList()
        val viewer = viewerResponse.body()?.data?.Viewer ?: throw Exception("Failed to get user")
        return UserMediaListResult(viewer = viewer, entries = entries)
    }

    suspend fun updateProgress(entryId: Int, progress: Int, status: String? = null): MediaListEntry {
        val token = getToken()
        val mutation = """
            mutation(${"$"}id: Int, ${"$"}progress: Int, ${"$"}status: MediaListStatus) {
                SaveMediaListEntry(id: ${"$"}id, progress: ${"$"}progress, status: ${"$"}status) {
                    id
                    progress
                    media {
                        id
                        title {
                            userPreferred
                        }
                        coverImage {
                            medium
                        }
                        episodes
                        relations {
                            edges {
                                relationType
                                node {
                                    id
                                    title {
                                        userPreferred
                                    }
                                    coverImage {
                                        medium
                                    }
                                    episodes
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val variables = mutableMapOf<String, Any>("id" to entryId, "progress" to progress)
        if (status != null) {
            variables["status"] = status
        }
        
        val response = api.updateProgress(token, GraphQLRequest(mutation, variables))
        return response.body()?.data?.SaveMediaListEntry ?: throw Exception("Failed to update progress")
    }

    suspend fun saveMediaListEntry(mediaId: Int, progress: Int, status: String?): MediaListEntry {
        val token = getToken()
        val mutation = """
            mutation(${"$"}mediaId: Int, ${"$"}progress: Int, ${"$"}status: MediaListStatus) {
                SaveMediaListEntry(mediaId: ${"$"}mediaId, progress: ${"$"}progress, status: ${"$"}status) {
                    id
                    progress
                    media {
                        id
                        title {
                            userPreferred
                        }
                        coverImage {
                            medium
                        }
                        episodes
                        relations {
                            edges {
                                relationType
                                node {
                                    id
                                    title {
                                        userPreferred
                                    }
                                    coverImage {
                                        medium
                                    }
                                    episodes
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val variables = mutableMapOf<String, Any>(
            "mediaId" to mediaId,
            "progress" to progress
        )
        if (status != null) {
            variables["status"] = status
        }

        val response = api.updateProgress(token, GraphQLRequest(mutation, variables))
        return response.body()?.data?.SaveMediaListEntry ?: throw Exception("Failed to add sequel")
    }

    suspend fun logout() {
        authRepository.clearToken()
    }
}

