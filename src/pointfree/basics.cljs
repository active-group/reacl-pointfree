(ns pointfree.basics
  (:require
   [reacl2.core :as reacl :include-macros true]
   [reacl2.dom :as dom :include-macros true]))

(reacl/defclass checkbox
  this
  checked?
  []

  handle-message
  (fn [c?]
    (reacl/return :app-state c?))

  render
  (dom/input
   {:type "checkbox"
    :value checked?
    :onchange (fn [e]
                (reacl/send-message!
                 this (.. e -target -checked)))}))

(reacl/defclass text-input
  this
  text
  []

  handle-message
  (fn [txt]
    (reacl/return :app-state txt))

  render
  (dom/input
   {:value text
    :onchange (fn [e]
                (reacl/send-message!
                 this (.. e -target -value)))}))
