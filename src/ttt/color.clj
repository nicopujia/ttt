(ns ttt.color)

(def ^:private ansi-reset "\u001B[0m")

(def ^:private ansi-colors
  {:bright-red "\u001B[91m"
   :bright-blue "\u001B[94m"
   :dim "\u001B[2m"
   :yellow-bg "\u001B[43m"
   :black-fg "\u001B[30m"
   :bold "\u001B[1m"
   :green "\u001B[32m"
   :yellow "\u001B[33m"})

(defn colorize
  [text color-key]
  (if-let [code (get ansi-colors color-key)]
    (str code text ansi-reset)
    text))

(defn x-style
  [text]
  (colorize text :bright-red))

(defn o-style
  [text]
  (colorize text :bright-blue))

(defn dim-style
  [text]
  (colorize text :dim))

(defn win-highlight
  [text]
  (str (:yellow-bg ansi-colors) (:black-fg ansi-colors) text ansi-reset))

(defn success-style
  [text]
  (colorize text :green))

(defn warning-style
  [text]
  (colorize text :yellow))

(defn bold-style
  [text]
  (colorize text :bold))
