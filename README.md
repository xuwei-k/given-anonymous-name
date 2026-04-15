# given-anonymous-name

## setup

### `project/plugins.sbt`

```scala
addSbtPlugin("com.github.xuwei-k" % "given-anonymous-name-plugin" % "version")
```

### sbt shell

```
> givenAnonymousName
```

and then

```
> scalafixAll GivenAnonymousNameWarn
```

or

```
> scalafixAll GivenAnonymousNameError
```

or

```
> scalafixAll GivenAnonymousNameRemove
```
