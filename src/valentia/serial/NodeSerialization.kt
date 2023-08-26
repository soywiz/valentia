package valentia.serial

import valentia.ast.*
import valentia.util.BinaryReader
import valentia.util.BinaryWriter

interface NodeSerializationCommon {
    companion object {
        val MAGIC = "ValentiA"
        const val VERSION = 1

        const val ID_PROGRAM = 1
        const val ID_MODULE = 2
        const val ID_PACKAGE = 3
        const val ID_FILE = 4

        const val ID_NONE = 9

        const val ID_NULL = 10
        const val ID_TRUE = 11
        const val ID_FALSE = 12

        const val ID_CHAR = 13

        const val ID_INT = 14
        const val ID_LONG = 15
        const val ID_UINT = 16
        const val ID_ULONG = 17

        fun ID_INT(long: Boolean, unsigned: Boolean): Int = when {
            long -> if (unsigned) ID_ULONG else ID_LONG
            else -> if (unsigned) ID_UINT else ID_INT
        }

        const val ID_STRING = 18

        const val ID_INT_M1 = 19
        const val ID_INT_0 = 20
        const val ID_INT_1 = 21
        const val ID_INT_2 = 22

        const val ID_THIS_LABELLED = 23
        const val ID_THIS_UNLABELLED = 24
        const val ID_SUPER_LABELLED = 25
        const val ID_SUPER_UNLABELLED = 26

        const val ID_CAST = 30
        const val ID_CAST_SAFE = 31

        const val ID_INDEX_N = 32
        const val ID_INDEX_1 = 33
        const val ID_INDEX_2 = 34
        const val ID_INDEX_3 = 35

        const val ID_STMS = 49
        const val ID_RETURN = 50
        const val ID_RETURN_LABELLED = 51
        const val ID_IF_EXPR = 52
        const val ID_THROW = 53
        const val ID_BREAK = 54
        const val ID_BREAK_LABELLED = 55
        const val ID_CONTINUE = 56
        const val ID_CONTINUE_LABELLED = 57
        const val ID_EMPTY_STM = 58
        const val ID_WHILE = 59
        const val ID_DO_WHILE = 60
        const val ID_FOR = 61
        const val ID_EXPR_STM = 62

        const val ID_BINOP_PLUS = 80
        const val ID_BINOP_MINUS = 81
        const val ID_BINOP_TIMES = 82
        const val ID_BINOP_DIV = 83
        const val ID_BINOP_REM = 84

        const val ID_PRE_UNOP_INCR = 85
        const val ID_PRE_UNOP_DECR = 86
        const val ID_PRE_UNOP_MINUS = 87
        const val ID_PRE_UNOP_PLUS = 88
        const val ID_PRE_UNOP_EXCL = 89

        const val ID_POST_UNOP_INCR = 90
        const val ID_POST_UNOP_DECR = 91
        const val ID_POST_UNOP_MINUS = 92

        const val LABEL_NODE_ID = 100
        const val ANNOTATION_NODE_ID = 101
    }
}

// STRINGS: list of all string literals appearing in the module
// TYPES: list of all types appearing in the module
// DECLS: list of all decls, including classes, functions and non-local variables
// BODIES: list of all bodies
open class NodeSerializer : BinaryWriter(), NodeSerializationCommon {
    val stringPool = StringPool()
    val typePool = TypePool()

    fun getStringIndex(value: String?): Int = stringPool.getOrThrow(value)
    fun getTypeIndex(type: Type?): Int = typePool.getOrThrow(type)

    fun writeOp(op: Int) {
        writeIntVLQ(op)
    }

    fun writeStringIndex(str: String?) {
        writeIntVLQ(getStringIndex(str))
    }

    fun writeHeader() {
        writeFixedSizeString(NodeSerializationCommon.MAGIC)
        writeIntVLQ(NodeSerializationCommon.VERSION)
    }

    fun writeStringPool() {
    }

    fun writeAnnotation(annotation: AnnotationNode) {
        writeIntVLQ(annotation.useSite.ordinal)
        writeIntVLQ(getTypeIndex(annotation.type))
        writeIntVLQ(annotation.args?.size ?: 0)
        for (expr in (annotation.args ?: emptyList())) {
            writeExprOrStm(expr)
        }
    }

    fun writeModifiers(modifiers: Modifiers) {
        writeIntVLQ(modifiers.items.size)
        for (item in modifiers.items) {
            when (item) {
                is Modifier -> writeIntVLQ(item.index)
                is LabelNode -> {
                    writeIntVLQ(NodeSerializationCommon.LABEL_NODE_ID)
                    writeStringIndex(item.id)
                }
                is AnnotationNodes -> {
                    writeIntVLQ(NodeSerializationCommon.ANNOTATION_NODE_ID)
                    writeAnnotations(item)
                }
            }
        }
    }

    fun writeAnnotations(annotations: List<AnnotationNodes>) = writeAnnotations(annotations.merge())
    fun writeAnnotations(annotations: AnnotationNodes) {
        writeIntVLQ(annotations.annotations.size)
        for (annotation in annotations.annotations) {
            writeAnnotation(annotation)
        }
    }

    fun writeDecl(decl: Decl) {
        when (decl) {
            is Program -> {
                writeIntVLQ(NodeSerializationCommon.ID_PROGRAM)
                writeIntVLQ(decl.modulesById.size)
                for (module in decl.modulesById.values) writeDecl(module)
            }
            is Module -> {
                writeIntVLQ(NodeSerializationCommon.ID_MODULE)
                writeIntVLQ(getStringIndex(decl.id))
                writeIntVLQ(decl.packagesById.size)
                for (pack in decl.packagesById.values) writeDecl(pack)
            }
            is Package -> {
                writeIntVLQ(NodeSerializationCommon.ID_PACKAGE)
                writeIntVLQ(getStringIndex(decl.identifier?.fqname))
                writeIntVLQ(decl.files.size)
                for (file in decl.files) writeDecl(file)
            }
            is FileNode -> {
                writeIntVLQ(NodeSerializationCommon.ID_FILE)
                writeIntVLQ(getStringIndex(decl.filePath))
                writeAnnotations(decl.fileAnnotations)
                writeIntVLQ(decl.topLevelDecls.size)
                for (decl in decl.topLevelDecls) writeDecl(decl)
            }
            is BaseConstructorDecl -> TODO()
            is ClassLikeDecl -> TODO()
            is CompanionObjectDecl -> TODO()
            is FunDecl -> TODO()
            is InitDecl -> TODO()
            is TypeAliasDecl -> TODO()
            is MultiVariableDecl -> TODO()
            is VariableDecl -> TODO()
            is UnknownTypeDecl -> TODO()
            is TypeParameter -> TODO()
            is FuncValueParam -> TODO()
        }
    }

    fun writeExprOrStm(node: ExprOrStm?): NodeSerializer {
        when (node) {
            null -> writeOp(NodeSerializationCommon.ID_NONE)
            is Expr -> writeExpr(node)
            is Stm -> writeStm(node)
        }
        return this
    }

    fun writeStm(stm: Stm?): NodeSerializer {
        when (stm) {
            null -> writeOp(NodeSerializationCommon.ID_NONE)
            is EmptyStm -> {
                writeOp(NodeSerializationCommon.ID_EMPTY_STM)
            }
            is Stms -> {
                writeOp(NodeSerializationCommon.ID_STMS)
                writeIntVLQ(stm.stms.size)
                for (stm in stm.stms) writeExprOrStm(stm)
            }
            is ReturnStm -> TODO()
            is WhileLoopStm -> {
                writeOp(NodeSerializationCommon.ID_WHILE)
                writeModifiers(stm.modifiers)
                writeExprOrStm(stm.cond)
                writeExprOrStm(stm.body)
            }
            is DoWhileLoopStm -> {
                writeOp(NodeSerializationCommon.ID_DO_WHILE)
                writeModifiers(stm.modifiers)
                writeExprOrStm(stm.cond)
                writeExprOrStm(stm.body)
            }
            is ForLoopStm -> {
                writeOp(NodeSerializationCommon.ID_FOR)
                writeModifiers(stm.modifiers)
                //writeDecl(expr.vardecl)
                writeExprOrStm(stm.expr)
                writeExprOrStm(stm.body)
                TODO()
            }
            is ExprStm -> {
                writeOp(NodeSerializationCommon.ID_EXPR_STM)
                writeExprOrStm(stm.expr)
            }
            is AssignStm -> TODO()
            is BreakStm -> TODO()
            is ContinueStm -> TODO()
            is DeclStm -> TODO()
            is IfStm -> TODO()
            is ThrowStm -> TODO()
            is TryCatchStm -> TODO()
        }
        return this
    }

    fun writeExpr(expr: Expr?): NodeSerializer {
        when (expr) {
            null -> writeOp(NodeSerializationCommon.ID_NONE)
            is LiteralExpr -> {
                when (expr) {
                    is NullLiteralExpr -> writeOp(NodeSerializationCommon.ID_NULL)
                    is BoolLiteralExpr -> writeOp(if (expr.value) NodeSerializationCommon.ID_TRUE else NodeSerializationCommon.ID_FALSE)
                    is CharLiteralExpr -> {
                        writeOp(NodeSerializationCommon.ID_CHAR)
                        writeIntVLQ(expr.value.code)
                    }
                    is IntLiteralExpr -> {
                        when (expr.value) {
                            -1L, 0L, 1L, 2L -> {
                                writeOp(NodeSerializationCommon.ID_INT_0 + expr.value.toInt())
                            }
                            else -> {
                                writeOp(NodeSerializationCommon.ID_INT(expr.isLong, expr.isUnsigned))
                                writeLongSVLQ(expr.value)
                            }
                        }
                    }
                    is StringLiteralExpr -> {
                        writeOp(NodeSerializationCommon.ID_STRING)
                        writeStringIndex(expr.value)
                    }
                }
            }
            is ThisExpr -> {
                writeOp(if (expr.id != null) NodeSerializationCommon.ID_THIS_LABELLED else NodeSerializationCommon.ID_THIS_UNLABELLED)
                if (expr.id != null) writeStringIndex(expr.id)
            }
            is SuperExpr -> {
                writeOp(if (expr.label != null) NodeSerializationCommon.ID_SUPER_LABELLED else NodeSerializationCommon.ID_SUPER_UNLABELLED)
                if (expr.label != null) writeStringIndex(expr.label)
                writeIntVLQ(getTypeIndex(expr.type))
            }
            is ReturnExpr -> {
                writeOp(if (expr.label != null) NodeSerializationCommon.ID_RETURN_LABELLED else NodeSerializationCommon.ID_RETURN)
                if (expr.label != null) writeStringIndex(expr.label)
                writeExprOrStm(expr.expr)
            }
            is ThrowExpr -> {
                writeOp(NodeSerializationCommon.ID_THROW)
                writeExprOrStm(expr.expr)
            }

            is BreakExpr -> {
                writeOp(if (expr.label != null) NodeSerializationCommon.ID_BREAK_LABELLED else NodeSerializationCommon.ID_BREAK)
                if (expr.label != null) writeStringIndex(expr.label)
            }
            is ContinueExpr -> {
                writeOp(if (expr.label != null) NodeSerializationCommon.ID_CONTINUE_LABELLED else NodeSerializationCommon.ID_CONTINUE)
                if (expr.label != null) writeStringIndex(expr.label)
            }
            is IfExpr -> {
                writeOp(NodeSerializationCommon.ID_IF_EXPR)
                writeExprOrStm(expr.cond)
                writeExprOrStm(expr.trueBody)
                writeExprOrStm(expr.falseBody)
            }
            is CastExpr -> {
                writeOp(if (expr.safe) NodeSerializationCommon.ID_CAST_SAFE else NodeSerializationCommon.ID_CAST)
                writeIntVLQ(getTypeIndex(expr.targetType))
                writeExprOrStm(expr.expr)
            }
            is IndexedExpr -> {
                writeOp(when (expr.indices.size) {
                    0 -> error("Unexpected")
                    1 -> NodeSerializationCommon.ID_INDEX_1
                    2 -> NodeSerializationCommon.ID_INDEX_2
                    3 -> NodeSerializationCommon.ID_INDEX_3
                    else -> NodeSerializationCommon.ID_INDEX_N
                })
                writeExprOrStm(expr.expr)
                if (expr.indices.size > 3) writeIntVLQ(expr.indices.size)
                for (index in expr.indices) writeExprOrStm(index)
            }
            is BinaryOpExpr -> {
                when (expr.op) {
                    "+" -> writeOp(NodeSerializationCommon.ID_BINOP_PLUS)
                    "-" -> writeOp(NodeSerializationCommon.ID_BINOP_MINUS)
                    "*" -> writeOp(NodeSerializationCommon.ID_BINOP_TIMES)
                    "/" -> writeOp(NodeSerializationCommon.ID_BINOP_DIV)
                    "&" -> writeOp(NodeSerializationCommon.ID_BINOP_REM)
                    else -> TODO()
                }
                writeExprOrStm(expr.left)
                writeExprOrStm(expr.right)
            }
            is UnaryPreOpExpr -> {
                writeOp(when (expr.op) {
                    UnaryPreOp.INCR -> NodeSerializationCommon.ID_PRE_UNOP_INCR
                    UnaryPreOp.DECR -> NodeSerializationCommon.ID_PRE_UNOP_DECR
                    UnaryPreOp.MINUS -> NodeSerializationCommon.ID_PRE_UNOP_MINUS
                    UnaryPreOp.PLUS -> NodeSerializationCommon.ID_PRE_UNOP_PLUS
                    UnaryPreOp.EXCL -> NodeSerializationCommon.ID_PRE_UNOP_EXCL
                })
                writeExprOrStm(expr.expr)
            }
            is UnaryPostOpExpr -> {
                writeOp(when (expr.op) {
                    UnaryPostOp.INCR -> NodeSerializationCommon.ID_POST_UNOP_INCR
                    UnaryPostOp.DECR -> NodeSerializationCommon.ID_POST_UNOP_DECR
                    UnaryPostOp.NOT_NULL -> NodeSerializationCommon.ID_POST_UNOP_MINUS
                })
                writeExprOrStm(expr.expr)
            }
            is AnonymousFunctionExpr -> TODO()
            is IdentifierExpr -> TODO()
            is NavigationExpr -> TODO()
            is TempExpr -> TODO()
            is TypeArgumentsAssignableSuffixExpr -> TODO()
            is CallExpr -> TODO()
            is CallableReferenceExt -> TODO()
            is CollectionLiteralExpr -> TODO()
            is EmptyExpr -> TODO()
            is Identifier -> TODO()
            is IncompleteExpr -> TODO()
            is InterpolatedStringExpr -> TODO()
            is LambdaFunctionExpr -> TODO()
            is ObjectLiteralExpr -> TODO()
            is OpSeparatedBinaryExprs -> TODO()
            is RangeTestExpr -> TODO()
            is Temp -> TODO()
            is TernaryExpr -> TODO()
            is TryCatchExpr -> TODO()
            is TypeTestExpr -> TODO()
            is WhenExpr -> TODO()

        }
        return this
    }
}

open class NodeDeserializer(data: ByteArray) : BinaryReader(data), NodeSerializationCommon {
    fun readHeader() {
        val magic = readFixedSizeString(NodeSerializationCommon.MAGIC.length)
        check(magic == NodeSerializationCommon.MAGIC) { "Header '$magic'" }
        val version = readIntVLQ()
        check(version == NodeSerializationCommon.VERSION) { "Header version '$version'" }
    }

    fun readModifiers(): Modifiers {
        return Modifiers((0 until readIntVLQ()).map {
            val id = readIntVLQ()
            when (id) {
                NodeSerializationCommon.LABEL_NODE_ID -> {
                    LabelNode(readIndexedString()!!)
                }
                NodeSerializationCommon.ANNOTATION_NODE_ID -> {
                    TODO()
                }
                else -> ALL_MODIFIERS_BY_ID[id]!!
            }
        })
    }

    fun readOp(): Int = readIntVLQ()
    fun readIndexedString(): String? = TODO()

    fun readExpr(): Expr? = readExprOrStm() as Expr?
    fun readStm(): Stm? = readExprOrStm() as Stm?

    fun readExprOrStm(): ExprOrStm? {
        return when (val op = readOp()) {
            NodeSerializationCommon.ID_NONE -> null
            NodeSerializationCommon.ID_NULL -> NullLiteralExpr()
            NodeSerializationCommon.ID_TRUE -> BoolLiteralExpr(true)
            NodeSerializationCommon.ID_FALSE -> BoolLiteralExpr(false)
            NodeSerializationCommon.ID_CHAR -> CharLiteralExpr(readIntVLQ().toChar())
            NodeSerializationCommon.ID_INT -> IntLiteralExpr(readIntSVLQ().toLong())
            NodeSerializationCommon.ID_LONG -> IntLiteralExpr(readIntSVLQ().toLong(), isLong = true)
            NodeSerializationCommon.ID_UINT -> IntLiteralExpr(readIntSVLQ().toLong(), isUnsigned = true)
            NodeSerializationCommon.ID_ULONG -> IntLiteralExpr(readIntSVLQ().toLong(), isLong = true, isUnsigned = true)
            NodeSerializationCommon.ID_INT_M1 -> IntLiteralExpr(-1L)
            NodeSerializationCommon.ID_INT_0 -> IntLiteralExpr(0L)
            NodeSerializationCommon.ID_INT_1 -> IntLiteralExpr(1L)
            NodeSerializationCommon.ID_INT_2 -> IntLiteralExpr(2L)
            NodeSerializationCommon.ID_BINOP_PLUS -> BinaryOpExpr(readExprOrStm() as Expr, "+", readExprOrStm() as Expr)
            NodeSerializationCommon.ID_RETURN -> ReturnExpr(readExpr())
            NodeSerializationCommon.ID_RETURN_LABELLED -> ReturnExpr(label = readIndexedString(), expr = readExpr())
            NodeSerializationCommon.ID_THROW -> ThrowExpr(readExpr()!!)
            NodeSerializationCommon.ID_IF_EXPR -> IfExpr(readExpr()!!, readExprOrStm()!!, readExprOrStm())
            NodeSerializationCommon.ID_EMPTY_STM -> EmptyStm()
            NodeSerializationCommon.ID_EXPR_STM -> ExprStm(readExpr()!!)
            NodeSerializationCommon.ID_WHILE -> {
                val modifiers = readModifiers()
                val cond = readExpr()!!
                val body = readStm()!!
                WhileLoopStm(cond!!, body!!, modifiers)
            }
            else -> TODO("op=$op")
        }
    }
}

enum class NodeKind {
    MODULE, PACKAGE, FILE, CLASS,
}
