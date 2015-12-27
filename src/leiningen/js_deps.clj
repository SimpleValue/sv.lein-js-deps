(ns leiningen.js-deps
  (:require [sv.js-deps.core :as c]
            [clojure.pprint :as p]))

(defn js-deps [project & [command]]
  (let [cfg (:js-deps project)]
    (case command
      "download"
      (c/download-dependencies
       cfg)
      "minify-js"
      (c/minify-js cfg)
      "minify-css"
      (c/minify-css cfg))))
