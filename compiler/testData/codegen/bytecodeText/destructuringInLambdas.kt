data class A(val x: String, val y: String)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() = foo(A("O", "K")) { (x, y) -> x + y }

// 1 LOCALVARIABLE \$x_y LA; L. L. 1
// 1 LOCALVARIABLE x LA; L. L. 2
// 1 LOCALVARIABLE y LA; L. L. 3
