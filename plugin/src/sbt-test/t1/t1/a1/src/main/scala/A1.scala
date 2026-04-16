object A1 {
  given x1: Int = 1

  given x2
    : Int = 2

  given x3[B]: List[B] = Nil

  given x4(using c: Int): Int = 4
}
