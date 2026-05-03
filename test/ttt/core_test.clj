(ns ttt.core-test
  (:require [clojure.test :refer [deftest is]]
            [ttt.core :as core]))

(deftest placeholder-message-is-short-and-explicit
  (is (= "Tic-tac-toe is not implemented yet."
         core/placeholder-message)))
