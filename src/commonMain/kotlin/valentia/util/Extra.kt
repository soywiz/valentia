package valentia.util

import kotlin.reflect.KProperty

@PublishedApi internal inline fun <T> Any?.fastCastTo(): T = this as T

interface Extra {
    var extra: MutableMap<String, Any?>?

    open class Mixin(override var extra: MutableMap<String, Any?>? = null) : Extra

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

    companion object {
        operator fun invoke() = Mixin()
    }

    class Property<T : Any?>(val name: String? = null, val defaultGen: () -> T) {
        inline operator fun getValue(thisRef: Extra, property: KProperty<*>): T {
            val res = (thisRef.extra?.get(name ?: property.name).fastCastTo<T?>())
            if (res == null) {
                val r = defaultGen()
                if (r != null || thisRef.hasExtra(name ?: property.name)) {
                    setValue(thisRef, property, r)
                }
                return r
            }
            return res
        }

        inline operator fun setValue(thisRef: Extra, property: KProperty<*>, value: T) {
            //beforeSet(value)
            thisRef.setExtra(name ?: property.name, value.fastCastTo<Any?>())
            //afterSet(value)
        }
    }

    class PropertyThis<T2 : Extra, T : Any?>(val name: String? = null, val defaultGen: T2.() -> T) {
        @PublishedApi internal var transform: (T2.(value: T) -> T) = { it }

        inline fun withTransform(noinline block: T2.(T) -> T): PropertyThis<T2, T> { transform = block; return this }

        inline operator fun getValue(thisRef: T2, property: KProperty<*>): T {
            val res = thisRef.getExtraTyped<T>(name ?: property.name)
            if (res == null) {
                val r = defaultGen(thisRef)
                setValueUntransformed(thisRef, property, r)
                return r
            }
            return res
        }

        inline fun setValueUntransformed(thisRef: T2, property: KProperty<*>, value: T) {
            thisRef.setExtra(name ?: property.name, value)
        }

        inline operator fun setValue(thisRef: T2, property: KProperty<*>, value: T) {
            setValueUntransformed(thisRef, property, transform(thisRef, value))
        }
    }
}

@PublishedApi internal fun <T : Any?> Extra.getExtraTyped(name: String): T? = extra?.get(name).fastCastTo<T?>()
@PublishedApi internal fun Extra.hasExtra(name: String): Boolean = extra?.contains(name) == true
@PublishedApi internal fun Extra.getExtra(name: String): Any? = extra?.get(name)
@PublishedApi internal fun Extra.setExtra(name: String, value: Any?) {
    if (extra == null) {
        if (value == null) return
        extra = LinkedHashMap<String, Any?>()
    }
    extra?.set(name, value)
}