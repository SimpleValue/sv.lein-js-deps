(ns sv.js-deps.core
  (:require [clj-http.client :as h]
            [clojure.java.io :as io]
            [asset-minifier.core :as m]
            [com.stuartsierra.dependency :as dep]))

(defn split-git-url [git-url]
  (let [[_ hostname user repository] (re-find #"git@(.*):(.*)/(.*).git" git-url)]
    {:user user
     :repository repository
     :hostname hostname}))

(defn github-raw-url [git-url branch-tag-or-commit path]
  (let [{:keys [user repository]} (split-git-url git-url)]
    (apply
     str
     "https://raw.githubusercontent.com/"
     (interpose "/" [user repository branch-tag-or-commit path]))))

(defn download* [url dest]
  (println "downloading:" url)
  (io/copy
   (:body
    (h/request
     {:method :get
      :url url
      :as :stream}))
   dest))

(defn js-info [dependency]
  (let [{:keys [git tag]} dependency]
    {:path (str "js/" (name (:id dependency)) ".js")
     :url (github-raw-url git tag (:js dependency))}))

(defn css-info [dependency]
  (let [{:keys [git tag]} dependency]
    {:path (str "css/" (name (:id dependency)) ".css")
     :url (github-raw-url git tag (:css dependency))}))

(defn download-instructions [dependency]
  (cond-> []
    (:js dependency)
    (conj (js-info dependency))
    (:css dependency)
    (conj (css-info dependency))))

(defn download [config instructions]
  (doseq [instruction instructions]
    (let [dir (:dir config)
          dest-file (io/file dir (io/file (:path instruction)))]
      (.mkdirs (.getParentFile dest-file))
      (download* (:url instruction) dest-file))))

(defn download-dependencies [config]
  (let [instructions (mapcat download-instructions (:dependencies config))]
    (download
     config
     instructions)))

(defn- min-js-file-name [js-file-name]
  (.replaceAll js-file-name ".js$" ".min.js"))

(defn js-deps-order [config]
  (let [graph (reduce
               (fn [g dependency]
                 (reduce
                  (fn [g dep]
                    (dep/depend g (:id dependency) dep))
                  g
                  (cons ::nothing (:requires dependency))))
               (dep/graph)
               (filter
                :js
                (:dependencies config)))]
    (remove #(= % ::nothing) (dep/topo-sort graph))))

(defn- dependencies-by-id [config]
  (into
   {}
   (map
    (fn [dependency]
      [(:id dependency) dependency])
    (:dependencies config))))

(defn minify-js-cmd [config]
  (let [dest-file (:js-min-file config (str (:dir config) "/js/js-deps.min.js"))
        dependencies (dependencies-by-id config)
        order (map (comp js-info dependencies) (js-deps-order config))
        paths (vec (map #(str (:dir config) "/" (:path %)) order))]
    [m/minify-js
     paths
     dest-file]))

(defn minify-js [config]
  (let [[f paths dest-file] (minify-js-cmd config)]
    (println "minifying js files"
             (pr-str paths)
             "into"
             (pr-str dest-file))
    (f paths dest-file)))
