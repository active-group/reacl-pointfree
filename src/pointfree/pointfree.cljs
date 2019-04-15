(ns pointfree.pointfree
  (:require [reacl2.core :as reacl :include-macros true]
            [reacl2.dom :as dom :include-macros true]
            [active.clojure.lens :as lens]))

(defn- take-em [app-state]
  (reacl/return :app-state app-state))

(def wrap
  (memoize
   (fn [wrapper maybe-attrs & components]
     (reacl/class
      "wrap"
      this
      app-state
      [& args]

      handle-message
      take-em

      render
      (let [[attrs components]
            (if (map? maybe-attrs)
              [maybe-attrs components]
              [{} (cons maybe-attrs components)])
            attrs (if (fn? attrs)
                    (attrs app-state)
                    attrs)]
        (wrapper
         attrs
         (map (fn [component idx]
                (dom/keyed
                 idx
                 (apply component
                        (reacl/opt
                         :reaction
                         (reacl/pass-through-reaction this))
                        app-state
                        args)))
              components
              (range))))))))

(def div (partial wrap dom/div))
(def h3 (partial wrap dom/h3))
(def button (partial wrap dom/button))

(defn click->return [return-fn component]
  (reacl/class
   "click->return"
    this
    app-state
    [& args]

    handle-message
    (fn [msg]
      (case msg
        :click
        (apply return-fn app-state args)

        (reacl/return :app-state msg)))

    render
    (dom/span
     {:onclick (fn [e]
                 (.preventDefault e)
                 (.stopPropagation e)
                 (reacl/send-message! this :click))}
     (apply
      component
      (reacl/opt
       :reaction
       (reacl/pass-through-reaction this))
      app-state
      args))))

(defn click->action
  [action component]
  (click->return
   (fn [app-state]
     (let [action (if (fn? action)
                    (action app-state)
                    action)]
       (reacl/return :action action)))
   component))

(defn click->app-state
  [f component]
  (click->return
   (fn [app-state]
     (let [new-app-state (if (fn? f)
                           (f app-state)
                           f)]
       (reacl/return :app-state new-app-state)))
   component))

(defn handle-action
  [f component]
  (reacl/class
   "handle-action"
   this
   app-state
   [& args]

   handle-message
   take-em

   render
   (apply
    component
    (reacl/opt
     :reaction
     (reacl/pass-through-reaction this)
     :reduce-action f)
    app-state
    args
    )))

(defn action->app-state
  [f component]
  (handle-action
   (fn [app-state action]
     (reacl/return :app-state (f app-state action)))
   component))


(def focus
  (memoize
   (fn [lens component]
     (reacl/class
      "focus"
      this
      app-state
      [& args]

      handle-message
      (fn [sub]
        (reacl/return
         :app-state (lens/shove app-state lens sub)))

      render
      (apply
       component
       (reacl/opt
        :reaction
        (reacl/pass-through-reaction this))
       (lens/yank app-state lens)
       args)))))

(def map-arguments
  (memoize
   (fn [downf component]
     (reacl/class
      "transform-arguments"
      this
      app-state
      [& args]

      handle-message
      take-em

      render
      (apply
       component
       (reacl/opt
        :reaction
        (reacl/pass-through-reaction this))
       app-state
       (downf args))))))

(def if
  (memoize
   (fn [pred then else]
     (reacl/class
      "if"
      this
      app-state
      [& args]

      handle-message
      take-em

      render
      (let [pred (if (fn? pred)
                   (pred app-state args)
                   pred)
            app (fn [component]
                  (apply
                   component
                   (reacl/opt
                    :reaction
                    (reacl/pass-through-reaction this))
                   app-state
                   args))]
        (if pred
          (app then)
          (app else)
          ))))))

(def with-args
  (memoize
   (fn [args component]
     (reacl/class
      "with-args"
      this
      app-state
      [& _args]

      handle-message
      take-em

      render
      (apply
       component
       (reacl/opt
        :reaction
        (reacl/pass-through-reaction this))
       app-state
       args
       )))))

(def pure
  (memoize
   (fn [f]
     (reacl/class
      "pure"
      this
      app-state
      [& args]

      render
      (f app-state)))))

(def eternally (comp pure constantly))

(def distribute
  (memoize
   (fn [component]
     (reacl/class
      "distribute"
      this
      app-state
      [& args]

      handle-message
      take-em

      render
      (dom/div
       (map (fn [idx]
              (dom/keyed
               idx
               (apply component
                      (reacl/opt
                       :reaction
                       (reacl/reaction
                        this
                        (fn [sub]
                          (lens/shove app-state (lens/pos idx) sub))))
                      (lens/yank app-state (lens/pos idx))
                      args)))
            (range (count app-state))))))))



;; --- Map over flow of app-state ---------

(def map-app-state
  (memoize
   (fn [downf upf component]
     (reacl/class
      "map-app-state"
      this
      app-state
      [& args]

      handle-message
      (fn [new-app-state]
        (reacl/return :app-state (apply upf new-app-state args)))

      render
      (apply
       component
       (reacl/opt
        :reaction
        (reacl/pass-through-reaction this))
       (apply downf app-state args)
       args
       )))))
