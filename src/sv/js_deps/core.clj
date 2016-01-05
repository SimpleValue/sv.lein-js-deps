(ns sv.js-deps.core
  (:require [clj-http.client :as h]
            [clojure.java.io :as io]
            [asset-minifier.core :as m]
            [com.stuartsierra.dependency :as dep]
            [sv.js-deps.utils :as u]))

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

(defn- info [& args]
  (locking *out*
    (apply println args)))

(defn download* [url dest]
  (info "downloading:" url)
  (io/copy
   (:body
    (h/request
     {:method :get
      :url url
      :as :stream}))
   dest))

(defn- prepare-dependency [[id dependency]]
  (assoc dependency :id id))

(defn- get-dependencies [config]
  (map prepare-dependency (:dependencies config)))

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
  (u/io-map
   20
   (fn [instruction]
     (let [dir (:dir config)
           dest-file (io/file dir (io/file (:path instruction)))]
       (.mkdirs (.getParentFile dest-file))
       (download* (:url instruction) dest-file)))
   instructions))

(defn download-dependencies [config]
  (let [instructions (mapcat download-instructions (get-dependencies config))]
    (download
     config
     instructions)))

(defn deps-order [config]
  (let [graph (reduce
               (fn [g dependency]
                 (reduce
                  (fn [g dep]
                    (dep/depend g (:id dependency) dep))
                  g
                  (cons ::nothing (:requires dependency))))
               (dep/graph)
               (get-dependencies config))]
    (remove #(= % ::nothing) (dep/topo-sort graph))))

(defn- dependencies-by-id [config]
  (into
   {}
   (map
    (fn [dependency]
      [(:id dependency) dependency])
    (get-dependencies config))))

(defn- paths [key info-fn config]
  (let [dependencies (dependencies-by-id config)
        order (map
               info-fn
               (filter
                key
                (map
                 dependencies
                 (deps-order config))))]
    (vec (map #(str (:dir config) "/" (:path %)) order))))

(defn- get-js-file-path [config]
  (:js-file config (str (:dir config) "/js/js-deps.js")))

(defn minify-js-cmd [config]
  (let [dest-file (get-js-file-path config)]
    [m/minify-js
     (paths :js js-info config)
     dest-file]))

(defn minify-js [config]
  (let [[f paths dest-file] (minify-js-cmd config)]
    (info "minifying js files"
          (pr-str paths)
          "into"
          (pr-str dest-file))
    (f paths dest-file)))

(defn- get-css-file-path [config]
  (:css-file config (str (:dir config) "/css/js-deps.css")))

(defn minify-css-cmd [config]
  (let [dest-file (get-css-file-path config)]
    [m/minify-css
     (paths :css css-info config)
     dest-file]))

(defn minify-css [config]
  (let [[f paths dest-file] (minify-css-cmd config)]
    (info "minifying css files"
          (pr-str paths)
          "into"
          (pr-str dest-file))
    (f paths dest-file)))

(defn dev-js-script [config]
  (apply
   str
   (map
    (fn [path]
      (str "\n/* js-deps path: " path " */\n"
           (slurp path)))
    (paths :js js-info config))))

(defn dev-js [config]
  (let [js-file (io/file (get-js-file-path config))]
    (.mkdirs (.getParentFile js-file))
    (spit js-file (dev-js-script config))))

(defn dev-css-file [config]
  (apply
   str
   (map
    (fn [path]
      (str "\n/* js-deps path: " path " */\n"
           (slurp path)))
    (paths :css css-info config))))

(defn dev-css [config]
  (let [css-file (io/file (get-css-file-path config))]
    (.mkdirs (.getParentFile css-file))
    (spit css-file (dev-css-file config))))
