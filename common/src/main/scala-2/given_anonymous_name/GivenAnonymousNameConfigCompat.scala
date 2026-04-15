package given_anonymous_name

trait GivenAnonymousNameConfigCompat { self: GivenAnonymousNameConfig =>
  def asTupleOption = GivenAnonymousNameConfig.unapply(self)
}
