package com.habitrpg.android.habitica.data.implementation

import android.content.Context
import com.habitrpg.android.habitica.data.ApiClient
import com.habitrpg.android.habitica.data.ContentRepository
import com.habitrpg.android.habitica.data.local.ContentLocalRepository
import com.habitrpg.android.habitica.helpers.AprilFoolsHandler
import com.habitrpg.android.habitica.models.ContentResult
import com.habitrpg.android.habitica.models.WorldState
import com.habitrpg.android.habitica.models.inventory.SpecialItem
import io.reactivex.rxjava3.core.Flowable
import io.realm.RealmList
import java.util.Date

class ContentRepositoryImpl<T : ContentLocalRepository>(
    localRepository: T,
    apiClient: ApiClient,
    context: Context
) : BaseRepositoryImpl<T>(localRepository, apiClient), ContentRepository {

    private val mysteryItem = SpecialItem.makeMysteryItem(context)

    private var lastContentSync = 0L
    private var lastWorldStateSync = 0L

    override suspend fun retrieveContent(forced: Boolean): ContentResult? {
        val now = Date().time
        if (forced || now - this.lastContentSync > 300000) {
            val content = apiClient.getContent() ?: return null
            lastContentSync = now
            content.special = RealmList()
            content.special.add(mysteryItem)
            localRepository.saveContent(content)
            return content
        }
        return null
    }

    override suspend fun retrieveWorldState(): WorldState? {
        val now = Date().time
        if (now - this.lastWorldStateSync > 3600000) {
            val state = apiClient.getWorldState() ?: return null
            lastWorldStateSync = now
            localRepository.save(state)
            for (event in state.events) {
                if (event.aprilFools != null && event.isCurrentlyActive) {
                    AprilFoolsHandler.handle(event.aprilFools, event.end)
                }
            }
            return state
        }
        return null
    }

    override fun getWorldState(): Flowable<WorldState> {
        return localRepository.getWorldState()
    }
}
