(ns leiningen.js-deps
  (:require [sv.js-deps.core :as c]
            [clojure.pprint :as p]))

(defn js-deps [project & [command]]
  (let [cfg (:js-deps project)]
    (c/download-dependencies cfg)
    (case command
      "download"
      true
      "minify-js"
      (c/minify-js cfg)
      "minify-css"
      (c/minify-css cfg)
      "dev-js"
      (c/dev-js cfg)
      "dev-css"
      (c/dev-css cfg))))
