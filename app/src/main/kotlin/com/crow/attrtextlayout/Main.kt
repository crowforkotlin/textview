fun main() {
    println(reverse(1230))
}
fun reverse(x: Int): Long {
    var ans = 0L
    var x = x
    while(x != 0) {
        if (ans > Int.MAX_VALUE || ans < Int.MIN_VALUE) {
            return 0L
        }
        val a = x % 10
        ans = 10 * ans + a
        x /= 10
    }
    return ans
}

fun lastRemaining(n: Int, m: Int): Int {
    var ans = 0
    for (i in 2..n) {
        ans = (ans + m) % i
    }
    return ans + 1
}