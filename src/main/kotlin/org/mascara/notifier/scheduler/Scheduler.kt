package org.mascara.notifier.scheduler

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


object Scheduler {
    private var future: ScheduledFuture<*>? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    public fun schedule(task: Runnable) {
        if (future == null) {
            future = scheduler.scheduleWithFixedDelay(
                    task,
                    0,
                    2,
                    TimeUnit.MINUTES
            )
        }
    }
}
