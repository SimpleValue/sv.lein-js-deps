(ns sv.js-deps.core
  (:require [clj-http.client :as h]
            [clojure.java.io :as io]
            [asset-minifier.core :as m]))

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

(defn foreign-libs [config]
  (let [dependency-path (fn [dependency]
                          (str (:dir config) "/" (:path (js-info dependency))))]
    {:foreign-libs (vec (map
                         (fn [dependency]
                           (let [path (dependency-path dependency)]
                             {:file path
                              :file-min (min-js-file-name path)
                              :provides [(name (:id dependency))]
                              :requires (vec (map name (:requires dependency)))}))
                         (filter :js (:dependencies config))))
     :externs (vec (map dependency-path (filter :js (:dependencies config))))}))

(defn require-parts [config]
  (vec (map (comp symbol name :id) (filter :js (:dependencies config)))))

(defn minify-js [config]
  (let [paths (map (comp :path js-info) (filter :js (:dependencies config)))]
    (doseq [path paths]
      (println "minifying" path)
      (let [dest-file (io/file (:dir config) (io/file path))
            min-file (io/file (.getParentFile dest-file) (min-js-file-name (.getName dest-file)))]
        (m/minify-js
         dest-file
         min-file)))))

(defn prepare [config]
  (download-dependencies config)
  (minify-js config)
  (foreign-libs config))
