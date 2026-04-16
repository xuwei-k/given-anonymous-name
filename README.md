# given-anonymous-name

## setup

### `project/plugins.sbt`

```scala
addSbtPlugin("com.github.xuwei-k" % "given-anonymous-name-plugin" % "0.1.1")
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
