package wiresegal.wob

import java.nio.channels.ClosedChannelException

/**
 * @author WireSegal
 * Created at 2:32 PM on 5/7/17.
 */

private val timeouts = mutableMapOf<Long, Thread>()
private val intervals = mutableMapOf<Long, Thread>()

fun setTimeout(millis: Long, code: () -> Unit): Long {
    val thread = Thread {
        try {
            Thread.sleep(millis)
            if (Thread.currentThread().id in timeouts)
                code()
            timeouts.remove(Thread.currentThread().id)
        } catch (e: Exception) {
            if (e !is ClosedChannelException) {
                val id = Thread.currentThread().id
                println("Critical failure in timeout thread $id!")
                e.printStackTrace(System.out)
                timeouts.remove(id)
            }
        }
    }

    timeouts.put(thread.id, thread)
    thread.start()
    return thread.id
}

fun setInterval(millis: Long, code: () -> Unit): Long {
    val thread = Thread {
        try {
            while (true) {
                Thread.sleep(millis)
                if (Thread.currentThread().id !in intervals)
                    break
                code()
            }
        } catch (e: Exception) {
            if (e !is ClosedChannelException) {
                val id = Thread.currentThread().id
                println("Critical failure in interval thread $id!")
                e.printStackTrace(System.out)
                intervals.remove(id)
            }
        }
    }
    intervals.put(thread.id, thread)
    thread.start()
    return thread.id
}

fun async(code: () -> Unit) {
    Thread {
        try {
            code()
        } catch (e: Exception) {
            if (e !is ClosedChannelException) {
                val id = Thread.currentThread().id
                println("Critical failure in asynchronous thread $id!")
                e.printStackTrace(System.out)
            }
        }
    }.start()
}

class Subscriptor {
    companion object {
        fun awaitInternal(code: () -> Unit): Subscriptor {
            val subscriptor = Subscriptor()
            val thread = Thread {
                code()
                subscriptor.isReady = true
                subscriptor()
            }
            subscriptor.thread = thread
            thread.start()
            return subscriptor
        }
    }

    private val subscriptions = mutableListOf<() -> Unit>()
    private lateinit var thread: Thread

    private operator fun invoke() = subscriptions.forEach { it() }

    fun subscribe(code: () -> Unit) {
        if (!isReady) subscriptions.add(code)
        else code()
    }

    fun join(code: () -> Unit) {
        while (!isReady) {
            // NO-OP
        }

        code()
    }

    private var isReady = false
}

fun await(code: () -> Unit) = Subscriptor.awaitInternal(code)


fun clearTimeout(idCode: Long): Boolean {
    val thread = timeouts[idCode] ?: return false
    if (!thread.isAlive) return false
    timeouts.remove(idCode)
    return true
}

fun clearInterval(idCode: Long): Boolean {
    val thread = intervals[idCode] ?: return false
    if (!thread.isAlive) return false
    intervals.remove(idCode)
    return true
}
