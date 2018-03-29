package wiresegal.wob

import com.fasterxml.jackson.databind.node.ObjectNode
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import java.io.File

/**
 * @author WireSegal
 * Created at 10:38 PM on 2/15/18.
 */
fun fileInHome(name: String): File {
    val home = System.getProperty("user.home")
    return File(home, ".$name")
}

class FakeEmbedBuilder(val json: ObjectNode) : EmbedBuilder() {
    override fun toJsonNode(node: ObjectNode): ObjectNode {
        return node.setAll(json) as ObjectNode
    }
}

open class SavedMap(val location: File, private val backingMap: MutableMap<String, String> = mutableMapOf()) :
        MutableMap<String, String> by backingMap {

    private var lock = Any()

    init {
        @Suppress("LeakingThis")
        load()
    }

    override fun clear() = synchronized(lock) {
        backingMap.clear()
        save()
    }

    override fun put(key: String, value: String): String? = synchronized(lock) {
        load()
        val ret = backingMap.put(key, value)
        save()
        return ret
    }

    override fun putAll(from: Map<out String, String>) = synchronized(lock) {
        load()
        backingMap.putAll(from)
        save()
    }

    override fun remove(key: String): String? = synchronized(lock) {
        load()
        val ret = backingMap.remove(key)
        save()
        return ret
    }

    protected fun fillDirect(from: Map<out String, String>) {
        backingMap.clear()
        backingMap.putAll(from)
    }

    protected open fun save() {
        location.writeText(backingMap
                .map { it.key.replace(":", "\\:") + "::" + it.value.replace(":", "\\:") }
                .joinToString("\n") + if (backingMap.any()) "\n" else "")
    }

    protected open fun load() {
        location.createNewFile()
        val text = location.readText()
        val entries = text.split("\n").map { it.split("::").map { it.replace("\\:", ":") } }.filter { it.size > 1 }
        val map = entries.associate { it[0] to it[1] }.toMutableMap()
        fillDirect(map)
    }
}


class SavedTypedMap<K : Any, V : Any>(location: File,
                                      private val serializeKey: (K) -> String,
                                      private val deserializeKey: (String) -> K?,
                                      private val serializeValue: (K, V) -> String,
                                      private val deserializeValue: (K, String) -> V?,
                                      private val persistentLoad: Boolean = true,
                                      private val backingMap: MutableMap<K, V> = mutableMapOf()) : MutableMap<K, V> by backingMap {

    private val internalMap = SavedBackingMap(location)

    private var lock = Any()

    override fun clear() = synchronized(lock) {
        backingMap.clear()
        internalMap.save()
    }

    override fun put(key: K, value: V): V? = synchronized(lock) {
        if (persistentLoad)
            internalMap.load()
        val ret = backingMap.put(key, value)
        internalMap.save()
        return ret
    }

    override fun putAll(from: Map<out K, V>) = synchronized(lock) {
        if (persistentLoad)
            internalMap.load()
        backingMap.putAll(from)
        internalMap.save()
    }

    override fun remove(key: K): V? = synchronized(lock) {
        if (persistentLoad)
            internalMap.load()
        val ret = backingMap.remove(key)
        internalMap.save()
        return ret
    }

    private inner class SavedBackingMap(location: File) : SavedMap(location) {
        public override fun save() {
            fillDirect(backingMap.map { serializeKey(it.key) to serializeValue(it.key, it.value) }.toMap())
            super.save()
        }

        public override fun load() {
            super.load()
            backingMap.clear()
            backingMap.putAll(mapNotNull {
                val k = deserializeKey(it.key)
                if (k != null) {
                    val v = deserializeValue(k, it.value)
                    if (v != null)
                        k to v
                    else
                        null
                } else null
            }.toMap())
        }
    }
}
