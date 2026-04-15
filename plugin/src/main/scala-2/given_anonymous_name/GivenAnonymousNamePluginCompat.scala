package given_anonymous_name

private[given_anonymous_name] trait GivenAnonymousNamePluginCompat { self: GivenAnonymousNamePlugin.type =>
  implicit class DefOps(val self: sbt.Def.type) {
    def uncached[A](a: A): A = a
  }
}
