fun foo() {
    a1.filter { (x, y) -> }
    a2.filter { (x) -> }
    a3.filter { z, (x, y) -> }
    a4.filter { (x, y), z -> }
    a5.filter { q, (x, y), z -> }
    a6.filter { (x, y), (z, w) -> }

    a7.filter { (x, y): Type, (z: Type), (w, u: T) : V -> foo7() }

    a8.filter { ((x, y), z) -> foo8() }
    a9.filter { (x -> foo9() }
    a10.filter { (x, y :) : -> foo10() }
}
