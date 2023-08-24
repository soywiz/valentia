package valentia.ast

// Modifiers

interface Modifier : ModifierOrAnnotation, ModifierLike {
    val id: String
    val index: Int

    companion object {
        inline val ENUM get() = ClassModifier.ENUM
        inline val SEALED get() = ClassModifier.SEALED
        inline val ANNOTATION get() = ClassModifier.ANNOTATION
        inline val DATA get() = ClassModifier.DATA
        inline val VALUE get() = ClassModifier.VALUE
        inline val INNER get() = ClassModifier.INNER
        inline val OVERRIDE get() = MemberModifier.OVERRIDE
        inline val LATE_INIT get() = MemberModifier.LATE_INIT
        inline val PUBLIC get() = VisibilityModifier.PUBLIC
        inline val PRIVATE get() = VisibilityModifier.PRIVATE
        inline val PROTECTED get() = VisibilityModifier.PROTECTED
        inline val INTERNAL get() = VisibilityModifier.INTERNAL
        inline val IN get() = VarianceModifier.IN
        inline val OUT get() = VarianceModifier.OUT
        inline val TAILREC get() = FunctionModifier.TAILREC
        inline val OPERATOR get() = FunctionModifier.OPERATOR
        inline val INFIX get() = FunctionModifier.INFIX
        inline val INLINE get() = FunctionModifier.INLINE
        inline val EXTERNAL get() = FunctionModifier.EXTERNAL
        inline val SUSPEND get() = FunctionModifier.SUSPEND
        inline val CONST get() = PropertyModifier.CONST
        inline val ABSTRACT get() = InheritanceModifier.ABSTRACT
        inline val FINAL get() = InheritanceModifier.FINAL
        inline val OPEN get() = InheritanceModifier.OPEN
        inline val VARARG get() = ParameterModifier.VARARG
        inline val NOINLINE get() = ParameterModifier.NOINLINE
        inline val CROSSINLINE get() = ParameterModifier.CROSSINLINE
        inline val REIFIED get() = ReificationModifier.REIFIED
        inline val EXPECT get() = PlatformModifier.EXPECT
        inline val ACTUAL get() = PlatformModifier.ACTUAL
    }
}
enum class ClassModifier(override val id: String, override val index: Int) : Modifier {
    ENUM("enum", 0),
    SEALED("sealed", 1),
    ANNOTATION("annotation", 2),
    DATA("data", 3),
    INNER("inner", 4),
    VALUE("value", 5)
    ;
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class MemberModifier(override val id: String, override val index: Int) : Modifier {
    OVERRIDE("override", 6),
    LATE_INIT("lateinit", 7)
    ;
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class VisibilityModifier(override val id: String, override val index: Int) : Modifier {
    PUBLIC("public", 8),
    PRIVATE("private", 9),
    INTERNAL("internal", 10),
    PROTECTED("protected", 11)
    ;
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class VarianceModifier(override val id: String, override val index: Int) : Modifier {
    IN("in", 12),
    OUT("out", 13)
    ;
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class FunctionModifier(override val id: String, override val index: Int) : Modifier {
    TAILREC("tailrec", 14),
    OPERATOR("operator", 15),
    INFIX("infix", 16),
    INLINE("inline", 17),
    EXTERNAL("external", 18),
    SUSPEND("suspend", 19)
    ;

    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class PropertyModifier(override val id: String, override val index: Int) : Modifier {
    CONST("const", 20)
    ;
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class InheritanceModifier(override val id: String, override val index: Int) : Modifier {
    ABSTRACT("abstract", 21),
    FINAL("final", 22),
    OPEN("open", 23)
    ;
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class ParameterModifier(override val id: String, override val index: Int) : Modifier {
    VARARG("vararg", 24),
    NOINLINE("noinline", 25),
    CROSSINLINE("crossinline", 26)
    ;
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class ReificationModifier(override val id: String, override val index: Int) : Modifier {
    REIFIED("reified", 27)
    ;
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class PlatformModifier(override val id: String, override val index: Int) : Modifier {
    EXPECT("expect", 28),
    ACTUAL("actual", 29)
    ;
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}

val ALL_MODIFIERS_LIST: List<Modifier> = ClassModifier.entries + MemberModifier.entries + VisibilityModifier.entries + VarianceModifier.entries + FunctionModifier.entries + PropertyModifier.entries + InheritanceModifier.entries + ParameterModifier.entries + ReificationModifier.entries + PlatformModifier.entries

val ALL_MODIFIERS_BY_INDEX = ALL_MODIFIERS_LIST.associateBy { it.index }

val ALL_MODIFIERS_BY_ID: Array<Modifier?> = Array(32) {
    ALL_MODIFIERS_BY_INDEX[it]
}

inline class ModifiersSet(val values: Int) {
    operator fun plus(other: ModifiersSet): ModifiersSet = ModifiersSet(this.values or other.values)
    operator fun contains(item: Modifier): Boolean = (values and (1 shl item.index)) != 0
    companion object {
        operator fun invoke(vararg modifiers: Modifier): ModifiersSet {
            var out = 0
            for (mod in modifiers) out = out or (1 shl mod.index)
            return ModifiersSet(out)
        }
    }
}
