package com.storyteller_f.common_vm_ktx

import java.io.Serializable

/**
 * @author storyteller_f
 */

open class Dao1<out D1>(val d1: D1) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dao1<*>

        if (d1 != other.d1) return false

        return true
    }

    override fun hashCode(): Int {
        return d1?.hashCode() ?: 0
    }
}

open class Dao2<out D1, out D2>(d1: D1, val d2: D2) : Dao1<D1>(d1) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao2<*, *>

        if (d2 != other.d2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d2?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2> Dao1<D1>.dao(d2: D2) = Dao2(d1, d2)


open class Dao3<out D1, out D2, out D3>(d1: D1, d2: D2, val d3: D3) : Dao2<D1, D2>(d1, d2) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao3<*, *, *>

        if (d3 != other.d3) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d3?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3> Dao2<D1, D2>.dao(d3: D3) = Dao3(d1, d2, d3)


open class Dao4<out D1, out D2, out D3, out D4>(d1: D1, d2: D2, d3: D3, val d4: D4) : Dao3<D1, D2, D3>(d1, d2, d3) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao4<*, *, *, *>

        if (d4 != other.d4) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d4?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3, D4> Dao3<D1, D2, D3>.dao(d4: D4) = Dao4(d1, d2, d3, d4)


open class Dao5<out D1, out D2, out D3, out D4, out D5>(d1: D1, d2: D2, d3: D3, d4: D4, val d5: D5) : Dao4<D1, D2, D3, D4>(d1, d2, d3, d4) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao5<*, *, *, *, *>

        if (d5 != other.d5) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d5?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3, D4, D5> Dao4<D1, D2, D3, D4>.dao(d5: D5) = Dao5(d1, d2, d3, d4, d5)


open class Dao6<out D1, out D2, out D3, out D4, out D5, out D6>(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5, val d6: D6) : Dao5<D1, D2, D3, D4, D5>(d1, d2, d3, d4, d5) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao6<*, *, *, *, *, *>

        if (d6 != other.d6) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d6?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3, D4, D5, D6> Dao5<D1, D2, D3, D4, D5>.dao(d6: D6) = Dao6(d1, d2, d3, d4, d5, d6)


open class Dao7<out D1, out D2, out D3, out D4, out D5, out D6, out D7>(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5, d6: D6, val d7: D7) : Dao6<D1, D2, D3, D4, D5, D6>(d1, d2, d3, d4, d5, d6) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao7<*, *, *, *, *, *, *>

        if (d7 != other.d7) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d7?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3, D4, D5, D6, D7> Dao6<D1, D2, D3, D4, D5, D6>.dao(d7: D7) = Dao7(d1, d2, d3, d4, d5, d6, d7)


open class Dao8<out D1, out D2, out D3, out D4, out D5, out D6, out D7, out D8>(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5, d6: D6, d7: D7, val d8: D8) :
    Dao7<D1, D2, D3, D4, D5, D6, D7>(d1, d2, d3, d4, d5, d6, d7) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao8<*, *, *, *, *, *, *, *>

        if (d8 != other.d8) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d8?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3, D4, D5, D6, D7, D8> Dao7<D1, D2, D3, D4, D5, D6, D7>.dao(d8: D8) = Dao8(d1, d2, d3, d4, d5, d6, d7, d8)


open class Dao9<out D1, out D2, out D3, out D4, out D5, out D6, out D7, out D8, out D9>(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5, d6: D6, d7: D7, d8: D8, val d9: D9) :
    Dao8<D1, D2, D3, D4, D5, D6, D7, D8>(d1, d2, d3, d4, d5, d6, d7, d8) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao9<*, *, *, *, *, *, *, *, *>

        if (d9 != other.d9) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d9?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3, D4, D5, D6, D7, D8, D9> Dao8<D1, D2, D3, D4, D5, D6, D7, D8>.dao(d9: D9) = Dao9(d1, d2, d3, d4, d5, d6, d7, d8, d9)


open class Dao10<out D1, out D2, out D3, out D4, out D5, out D6, out D7, out D8, out D9, out D10>(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5, d6: D6, d7: D7, d8: D8, d9: D9, val d10: D10) :
    Dao9<D1, D2, D3, D4, D5, D6, D7, D8, D9>(d1, d2, d3, d4, d5, d6, d7, d8, d9) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao10<*, *, *, *, *, *, *, *, *, *>

        if (d10 != other.d10) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d10?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3, D4, D5, D6, D7, D8, D9, D10> Dao9<D1, D2, D3, D4, D5, D6, D7, D8, D9>.dao(d10: D10) = Dao10(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10)


open class Dao11<out D1, out D2, out D3, out D4, out D5, out D6, out D7, out D8, out D9, out D10, out D11>(
    d1: D1,
    d2: D2,
    d3: D3,
    d4: D4,
    d5: D5,
    d6: D6,
    d7: D7,
    d8: D8,
    d9: D9,
    d10: D10,
    val d11: D11
) : Dao10<D1, D2, D3, D4, D5, D6, D7, D8, D9, D10>(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao11<*, *, *, *, *, *, *, *, *, *, *>

        if (d11 != other.d11) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d11?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3, D4, D5, D6, D7, D8, D9, D10, D11> Dao10<D1, D2, D3, D4, D5, D6, D7, D8, D9, D10>.dao(d11: D11) = Dao11(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11)


open class Dao12<out D1, out D2, out D3, out D4, out D5, out D6, out D7, out D8, out D9, out D10, out D11, out D12>(
    d1: D1,
    d2: D2,
    d3: D3,
    d4: D4,
    d5: D5,
    d6: D6,
    d7: D7,
    d8: D8,
    d9: D9,
    d10: D10,
    d11: D11,
    val d12: D12
) : Dao11<D1, D2, D3, D4, D5, D6, D7, D8, D9, D10, D11>(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Dao12<*, *, *, *, *, *, *, *, *, *, *, *>

        if (d12 != other.d12) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (d12?.hashCode() ?: 0)
        return result
    }
}

infix fun <D1, D2, D3, D4, D5, D6, D7, D8, D9, D10, D11, D12> Dao11<D1, D2, D3, D4, D5, D6, D7, D8, D9, D10, D11>.dao(d12: D12) = Dao12(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12)

/**
 * 4
 */
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
) {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    public override fun toString(): String = "($first, $second, $third, $fourth)"
}

/**
 * 5
 */
data class Quintuple<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
) {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}

/**
 * 6
 */
data class Sextuplet<out A, out B, out C, out D, out E, out F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
) {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth)"
}

/**
 * 7
 */
data class Septuplet<out A, out B, out C, out D, out E, out F, G>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G
) {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh)"
}

/**
 * 8
 */
data class Octuplet<out A, out B, out C, out D, out E, out F, out G, out H>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G,
    val eighth: H
) {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    override fun toString(): String =
        "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth)"
}

/**
 * 9
 */
data class Nonuple<out A, out B, out C, out D, out E, out F, out G, out H, out I>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G,
    val eighth: H,
    val ninth: I
) {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    override fun toString(): String =
        "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth)"
}

/**
 * 10
 */
data class Decuple<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G,
    val eighth: H,
    val ninth: I,
    val tenth: J
) {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    override fun toString(): String =
        "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth)"
}

infix fun <A, B, C, D, E, F, G, H, I, J> Nonuple<A, B, C, D, E, F, G, H, I>.to(that: J) =
    Decuple(
        first,
        second,
        third,
        fourth,
        fifth,
        sixth,
        seventh,
        eighth,
        ninth,
        that
    )

infix fun <A, B, C, D, E, F, G, H, I> Octuplet<A, B, C, D, E, F, G, H>.to(that: I) =
    Nonuple(
        first,
        second,
        third,
        fourth,
        fifth,
        sixth,
        seventh,
        eighth,
        that
    )

infix fun <A, B, C, D, E, F, G, H> Septuplet<A, B, C, D, E, F, G>.to(that: H) =
    Octuplet(
        first,
        second,
        third,
        fourth,
        fifth,
        sixth,
        seventh,
        that
    )

infix fun <A, B, C, D, E, F, G> Sextuplet<A, B, C, D, E, F>.to(that: G) =
    Septuplet(first, second, third, fourth, fifth, sixth, that)

infix fun <A, B, C, D, E, F> Quintuple<A, B, C, D, E>.to(that: F) =
    Sextuplet(first, second, third, fourth, fifth, that)

infix fun <A, B, C, D, E> Quadruple<A, B, C, D>.to(that: E) =
    Quintuple(first, second, third, fourth, that)

infix fun <A, B, C, D> Triple<A, B, C>.to(that: D) =
    Quadruple(first, second, third, that)

infix fun <A, B, C> Pair<A, B>.to(that: C) = Triple(first, second, that)

infix fun <A, B, C> Pair<A, B>.plus(that: Pair<A, C>) = mutableListOf(this, that)

infix fun <A, B> MutableList<Pair<A, Any?>>.plus(that: Pair<A, B>): List<Pair<A, Any?>> =
    apply {
        add(that)
    }

