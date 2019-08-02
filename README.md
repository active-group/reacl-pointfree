# Reacl pointfree style

This project demonstrates pointfree component composition. A style of
programming with Reacl components.

## Background: pointfree function composition

Functions can be composed by applying them to explicitly named
arguments inside a lambda:

```clojure
(defn not-zero? [x]
  (not (zero? x)))
```

Alternatively, you can also compose functions by using function
combinators such as `partial`, `comp`, etc.

```clojure
(def not-zero? (comp not zero?))
```

## Pointfree component composition

Reacl components can be composed in similar ways and in similar
styles. In most cases we take to the point-based style.

```clojure
(defclass to-do-item this item []

  render
  (div
   (checkbox
    (opt :embed-app-state (fn [i d] (assoc i :done? d)))
    (:done? item))

   (text-input
    (opt :embed-app-state (fn [i t] (assoc i :text t)))
    (:text item))))
```

But at some instances we may be better served by a pointfree
definition:

```clojure
(def to-do-item
  (p/div
   (p/focus :done? checkbox)
   (p/focus :text text-input)))
```

Here, `p/div` is a higher-order component that just wraps its arguments
in a `dom/div`. App-state is not transformed at all. In contrast,
`focus` is a higher-order component that takes a lens and a
subcomponent and returns a component that manages the subcomponent's
app-state according to the lens.

```clojure
(defn focus [lens component]
  (class
   "focus"
   this
   app-state
   [& args]

   handle-message
   (fn [sub]
     (return
      :app-state (lens/shove app-state lens sub)))

   render
   (apply
    component
    (opt
     :reaction
     (pass-through-reaction this))
    (lens/yank app-state lens)
    args)))
```

This project provides a set of combinators for Reacl components in
`pointfree.pointfree` and a simple example of their usage in
`pointfree.core`. The usual caveats of higher-order components apply.


## Discussion

With the point-based style we give the input app-state a name that is
bound to a value at runtime.

```clojure
(defclass to-do-item this item ...)
```

This is similar to naming arguments in a function definition and is
therefore easy to understand. However, components must do more work
than functions. In addition to an input ("downward") app-state we must
also specify what happens with output ("upward") app-state.

```clojure
...
render
...
 (checkbox
  (opt :embed-app-state (fn [i d] (assoc i :done? d)))
  (:done? item))
...
```

With `:embed-app-state` we specify the upward flow of app-state.
Still, syntactically, we specify this flow as an option that we pass
"down".

In pointfree style we can avoid this awkwardness with a careful design
of the combinators. `focus` is a perfect example. A lens is all it
takes to fully specify both upward and downward flow of app-state.


### Local state

We have not yet found a satisfying way of dealing with local-state in
pointfree style. We might need to get rid of the concept
entirely in order to find a more principled solution for the problems
that we currently try to solve with local-state.


### Names

Without rigid static types, the inputs of pointfree definitions are
harder to infer than with point-based definitions, which give the
reader the names of the inputs as one more data point. You should
probably either specify the input types via comments, `:pre`, specs or
by giving the definition itself a more descriptive name.

```clojure
(defrecord Todo [done? text id])

(def todo-component
  (p/div
   (p/focus :done? checkbox)
   (p/focus :text text-input)))
```
