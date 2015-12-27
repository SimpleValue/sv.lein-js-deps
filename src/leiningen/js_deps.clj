(ns leiningen.js-deps
  (:require [sv.js-deps.core :as c]
            [clojure.pprint :as p]))

(defn js-deps [project & [command]]
  (let [cfg (:js-deps project)]
    (case command
      "download"
      (c/download-dependencies
       cfg)
      "libs"
      (p/pprint
       (c/foreign-libs
        cfg))
      "require"
      (println (pr-str (c/require-parts cfg)))
      "prepare"
      (p/pprint (c/prepare cfg))
      "minify-js"
      (c/minify-js cfg))))
