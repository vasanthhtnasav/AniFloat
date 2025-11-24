package com.kotla.anifloat.data.model

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any>
)

data class GraphQLResponse<T>(
    val data: T?,
    val errors: List<GraphQLError>?
)

data class GraphQLError(
    val message: String
)

// --- Viewer ---
data class ViewerData(
    val Viewer: Viewer
)

data class Viewer(
    val id: Int,
    val name: String,
    val avatar: Avatar?
)

data class Avatar(
    val large: String?
)

// --- Media List ---
data class MediaListCollectionData(
    val MediaListCollection: MediaListCollection
)

data class MediaListCollection(
    val lists: List<MediaListGroup>
)

data class MediaListGroup(
    val entries: List<MediaListEntry>
)

data class MediaListEntry(
    val id: Int,
    val progress: Int,
    val media: Media,
    val updatedAt: Long?
)

data class Media(
    val id: Int,
    val title: MediaTitle,
    val coverImage: MediaCoverImage,
    val episodes: Int?,
    val relations: MediaConnection? = null
)

data class MediaConnection(
    val edges: List<MediaEdge>
)

data class MediaEdge(
    val relationType: String,
    val node: MediaNode
)

data class MediaNode(
    val id: Int,
    val title: MediaTitle,
    val coverImage: MediaCoverImage,
    val episodes: Int?
)

data class MediaTitle(
    val userPreferred: String
)

data class MediaCoverImage(
    val medium: String
)

// --- Mutation Response ---
data class SaveMediaListEntryData(
    val SaveMediaListEntry: MediaListEntry
)

