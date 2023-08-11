package valentia.util

inline class Disjunction2<T1, T2>(val value: Any)
inline fun <reified T1, reified T2> Disjunction2<T1, T2>.optT1(): T1? = value as? T1?
inline fun <reified T1, reified T2> Disjunction2<T1, T2>.optT2(): T2? = value as? T2?

inline class Disjunction3<T1, T2, T3>(val value: Any)
inline fun <reified T1, reified T2, reified T3> Disjunction3<T1, T2, T3>.optT1(): T1? = value as? T1?
inline fun <reified T1, reified T2, reified T3> Disjunction3<T1, T2, T3>.optT2(): T2? = value as? T2?
inline fun <reified T1, reified T2, reified T3> Disjunction3<T1, T2, T3>.optT3(): T3? = value as? T3?
