package com.storyteller_f.common_vm_ktx

/**
 * @author storyteller_f
 */
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
        this.first,
        this.second,
        this.third,
        this.fourth,
        this.fifth,
        this.sixth,
        this.seventh,
        this.eighth,
        this.ninth,
        that
    )

infix fun <A, B, C, D, E, F, G, H, I> Octuplet<A, B, C, D, E, F, G, H>.to(that: I) =
    Nonuple(
        this.first,
        this.second,
        this.third,
        this.fourth,
        this.fifth,
        this.sixth,
        this.seventh,
        this.eighth,
        that
    )

infix fun <A, B, C, D, E, F, G, H> Septuplet<A, B, C, D, E, F, G>.to(that: H) =
    Octuplet(
        this.first,
        this.second,
        this.third,
        this.fourth,
        this.fifth,
        this.sixth,
        this.seventh,
        that
    )

infix fun <A, B, C, D, E, F, G> Sextuplet<A, B, C, D, E, F>.to(that: G) =
    Septuplet(this.first, this.second, this.third, this.fourth, this.fifth, this.sixth, that)

infix fun <A, B, C, D, E, F> Quintuple<A, B, C, D, E>.to(that: F) =
    Sextuplet(this.first, this.second, this.third, this.fourth, this.fifth, that)

infix fun <A, B, C, D, E> Quadruple<A, B, C, D>.to(that: E) =
    Quintuple(this.first, this.second, this.third, this.fourth, that)

infix fun <A, B, C, D> Triple<A, B, C>.to(that: D) =
    Quadruple(this.first, this.second, this.third, that)

infix fun <A, B, C> Pair<A, B>.to(that: C) = Triple(this.first, this.second, that)

infix fun <A, B, C> Pair<A, B>.plus(that: Pair<A, C>) = mutableListOf(this, that)

infix fun <A, B> MutableList<Pair<A, Any?>>.plus(that: Pair<A, B>): List<Pair<A, Any?>> =
    this.apply {
        add(that)
    }

