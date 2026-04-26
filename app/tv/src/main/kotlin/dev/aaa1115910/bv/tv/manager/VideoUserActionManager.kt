package dev.aaa1115910.bv.tv.manager

import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.repositories.CoinRepository
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import dev.aaa1115910.biliapi.repositories.LikeRepository
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import java.util.concurrent.ConcurrentHashMap

data class VideoActionState(
    val liked: Boolean = false,
    val favorited: Boolean = false,
    val coin: Boolean = false,
    val favoriteFolderIds: List<Long> = emptyList()
)

/**
 * Simple in-memory manager for video user actions (like/favorite/coin).
 * Keyed by aid. Exposes a StateFlow per aid so UI can collect and share state across screens.
 * Network operations delegate to repositories from Koin and update the corresponding flow on success.
 */
object VideoUserActionManager {
    // key = Pair(uid, aid)
    private val stateMap = ConcurrentHashMap<Pair<Long, Long>, MutableStateFlow<VideoActionState>>()
    // key = uid, favorite folders are user-global
    private val favoriteFoldersMap = ConcurrentHashMap<Long, MutableStateFlow<List<FavoriteFolderMetadata>>>()
    private val fetchMutexMap = ConcurrentHashMap<Long, Mutex>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun key(uid: Long, aid: Long) = uid to aid

    private fun ensure(aid: Long, uid: Long = Prefs.uid): MutableStateFlow<VideoActionState> {
        val k = key(uid, aid)
        stateMap[k]?.let { return it }
        val newFlow = MutableStateFlow(VideoActionState())
        return stateMap.putIfAbsent(k, newFlow) ?: newFlow
    }

    fun getStateFlow(aid: Long, uid: Long = Prefs.uid): StateFlow<VideoActionState> = ensure(aid, uid)

    private fun ensureFavoriteFolders(uid: Long = Prefs.uid): MutableStateFlow<List<FavoriteFolderMetadata>> {
        val flow = favoriteFoldersMap.getOrPut(uid) { MutableStateFlow(emptyList()) }
        if (flow.value.isEmpty()) {
            scope.launch {
                val mutex = fetchMutexMap.getOrPut(uid) { Mutex() }
                mutex.withLock {
                    if (flow.value.isNotEmpty()) return@launch
                    runCatching {
                        flow.value = get<FavoriteRepository>(FavoriteRepository::class.java)
                            .getAllFavoriteFolderMetadataList(mid = uid, preferApiType = Prefs.apiType)
                    }
                }
            }
        }
        return flow
    }

    fun getFavoriteFoldersFlow(uid: Long = Prefs.uid): StateFlow<List<FavoriteFolderMetadata>> = ensureFavoriteFolders(uid)

    suspend fun updateFromLoadedData(aid: Long, liked: Boolean, favorited: Boolean, coin: Boolean, uid: Long = Prefs.uid) {
        val flow = ensure(aid, uid)
        flow.value = flow.value.copy(liked = liked, favorited = favorited, coin = coin)

        // load favorite folder ids for this video
        if (aid <= 0 || !Prefs.isLogin) return
        val favoriteRepository: FavoriteRepository = get(FavoriteRepository::class.java)
        val mutex = fetchMutexMap.getOrPut(uid) { Mutex() }
        mutex.withLock {
            runCatching {
                val folders = withContext(Dispatchers.IO) {
                    favoriteRepository.getAllFavoriteFolderMetadataList(
                        mid = uid,
                        rid = aid,
                        preferApiType = Prefs.apiType
                    )
                }
                // update folder list cache (superset of the no-rid call)
                favoriteFoldersMap.getOrPut(uid) { MutableStateFlow(emptyList()) }.value = folders
                // update video action state with folder ids
                val folderIds = folders.filter { it.videoInThisFav }.map { it.id }
                flow.value = flow.value.copy(favoriteFolderIds = folderIds)
            }
        }
    }

    suspend fun addLike(aid: Long, uid: Long = Prefs.uid): Boolean {
        if (aid <= 0) return false
        val likeRepository: LikeRepository = get(LikeRepository::class.java)
        return try {
            withContext(Dispatchers.IO) { likeRepository.addVideoLike(aid = aid) }
            ensure(aid, uid).value = ensure(aid, uid).value.copy(liked = true)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun delLike(aid: Long, uid: Long = Prefs.uid): Boolean {
        if (aid <= 0) return false
        val likeRepository: LikeRepository = get(LikeRepository::class.java)
        return try {
            withContext(Dispatchers.IO) { likeRepository.delVideoLike(aid = aid) }
            ensure(aid, uid).value = ensure(aid, uid).value.copy(liked = false)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun addCoin(aid: Long, uid: Long = Prefs.uid): Boolean {
        if (aid <= 0) return false
        val coinRepository: CoinRepository = get(CoinRepository::class.java)
        return try {
            withContext(Dispatchers.IO) { coinRepository.addVideoCoin(aid = aid) }
            ensure(aid, uid).value = ensure(aid, uid).value.copy(coin = true)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun addToView(aid: Long, uid: Long = Prefs.uid): Boolean {
        if (aid <= 0 || uid <= 0) return false
        val toViewRepository: ToViewRepository = get(ToViewRepository::class.java)
        return try {
            withContext(Dispatchers.IO) {
                toViewRepository.addToView(
                    avid = aid,
                    preferApiType = Prefs.apiType
                )
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateVideoFavoriteFolders(aid: Long, folderIds: List<Long>, uid: Long = Prefs.uid): Boolean {
        if (aid <= 0) return false
        val favoriteRepository: FavoriteRepository = get(FavoriteRepository::class.java)
        val currentFolders = ensureFavoriteFolders(uid).value
        return try {
            withContext(Dispatchers.IO) {
                require(currentFolders.isNotEmpty())
                favoriteRepository.updateVideoToFavoriteFolder(
                    aid = aid,
                    addMediaIds = folderIds,
                    delMediaIds = currentFolders.map { it.id } - folderIds.toSet()
                )
            }
            ensure(aid, uid).value = ensure(aid, uid).value.copy(
                favoriteFolderIds = folderIds,
                favorited = folderIds.isNotEmpty()
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun delVideoFromFavoriteFolder(aid: Long, folderId: Long, uid: Long = Prefs.uid): Boolean {
        if (aid <= 0) return false
        val favoriteRepository: FavoriteRepository = get(FavoriteRepository::class.java)
        return try {
            withContext(Dispatchers.IO) {
                favoriteRepository.delVideoFromFavoriteFolder(
                    aid = aid,
                    delMediaIds = listOf(folderId),
                    preferApiType = Prefs.apiType
                )
            }
            val flow = ensure(aid, uid)
            val updatedIds = flow.value.favoriteFolderIds - folderId
            flow.value = flow.value.copy(
                favoriteFolderIds = updatedIds,
                favorited = updatedIds.isNotEmpty()
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun addToDefaultFavoriteFolder(aid: Long, uid: Long = Prefs.uid): Boolean {
        if (aid <= 0) return false
        val flow = ensure(aid, uid)
        val default = ensureFavoriteFolders(uid).value.firstOrNull { it.title == "默认收藏夹" }
            ?: return false
        return updateVideoFavoriteFolders(aid, listOf(default.id), uid)
    }
}
