package com.habitrpg.android.habitica.helpers

import android.util.Log
import com.habitrpg.android.habitica.BuildConfig
import com.habitrpg.android.habitica.proxy.AnalyticsManager
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.CoroutineExceptionHandler
import okhttp3.internal.http2.ConnectionShutdownException
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException

class ExceptionHandler {
    private var analyticsManager: AnalyticsManager? = null

    companion object {

        private var instance = ExceptionHandler()

        fun init(analyticsManager: AnalyticsManager) {
            instance.analyticsManager = analyticsManager
        }

        fun coroutine(handler: ((Throwable) -> Unit)? = null): CoroutineExceptionHandler {
            return CoroutineExceptionHandler { _, throwable ->
                reportError(throwable)
                handler?.invoke(throwable)
            }
        }

        fun rx(): Consumer<Throwable> {
            // Can't be turned into a lambda, because it then doesn't work for some reason.
            return Consumer { reportError(it) }
        }

        fun reportError(throwable: Throwable) {
            if (BuildConfig.DEBUG) {
                try {
                    Log.e("ObservableError", Log.getStackTraceString(throwable))
                } catch (ignored: Exception) {
                }
            } else {
                if (!IOException::class.java.isAssignableFrom(throwable.javaClass) &&
                    !HttpException::class.java.isAssignableFrom(throwable.javaClass) &&
                    !retrofit2.HttpException::class.java.isAssignableFrom(throwable.javaClass) &&
                    !EOFException::class.java.isAssignableFrom(throwable.javaClass) &&
                    !retrofit2.adapter.rxjava3.HttpException::class.java.isAssignableFrom(throwable.javaClass) &&
                    throwable !is ConnectionShutdownException
                ) {
                    instance.analyticsManager?.logException(throwable)
                }
            }
        }
    }
}
