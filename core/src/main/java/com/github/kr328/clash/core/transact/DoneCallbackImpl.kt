package com.github.kr328.clash.core.transact

import bridge.DoneCallback
import java.util.concurrent.CompletableFuture

class DoneCallbackImpl : DoneCallback, CompletableFuture<Unit>() {
    override fun doneWithError(e: Exception?) {
        if (e == null)
            complete(Unit)
        else
            completeExceptionally(e)
    }

    override fun done() {
        complete(Unit)
    }
}