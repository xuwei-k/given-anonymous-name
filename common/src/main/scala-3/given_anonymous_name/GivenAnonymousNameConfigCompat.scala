package given_anonymous_name

trait GivenAnonymousNameConfigCompat { self: GivenAnonymousNameConfig =>
  def asTupleOption = Option(Tuple.fromProductTyped(self))
}
