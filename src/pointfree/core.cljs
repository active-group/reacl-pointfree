(ns ^:figwheel-hooks pointfree.core
  (:require
   [goog.dom :as gdom]
   [pointfree.pointfree :as p]
   [pointfree.basics :as basics]
   [reacl2.core :as reacl :include-macros true]
   [reacl2.dom :as dom :include-macros true]
   [active.clojure.lens :as lens]))

;; Data

(defrecord TodosApp [next-id next-text todos])
(defrecord Todo [id text done?])


;; Components

(defrecord Delete [todo])

;; app-state: Todo
;; outgoing actions: Delete
(def to-do-item
  (p/div
   (p/focus :done? basics/checkbox)
   (p/click->action
    ->Delete
    (p/eternally (dom/button "Zap")))
   (p/focus :text (p/pure str))))

;; app-state: TodosApp
(def to-do-app
  (p/div
   (p/h3 (p/eternally "TODO"))

   (p/focus
    :todos
    (p/action->app-state
     (fn [todos delete-action]
       (let [id (-> delete-action :todo :id)]
         (remove (fn [todo]
                   (= id (:id todo)))
                 todos)))
     (p/distribute
      to-do-item)))

   (p/focus
    :next-text
    basics/text-input)

   (p/click->app-state
    (fn [{:keys [next-id next-text todos] :as app-state}]
      (assoc app-state
             :todos
             (concat todos
                     [(->Todo next-id next-text false)])
             :next-id (inc next-id)
             :next-text ""))
    (p/button
     (p/pure #(str "Add #" (:next-id %)))))))


(defn get-app-element []
  (gdom/getElement "app"))

(reacl/render-component
 (get-app-element)
 to-do-app
 (->TodosApp
  0 "Next"
  [(->Todo 123 "foo" false)]))
