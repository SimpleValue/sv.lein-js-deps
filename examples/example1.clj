(ns example1
  (:require [sv.js-deps.core :as c]))

(def config
  {:dir "dev/lib"
   :dependencies
   {:videojs-dash
    {:js "src/js/videojs-dash.js"
     :css "src/css/videojs-dash.css"
     :git "git@github.com:videojs/videojs-contrib-dash.git"
     :tag "v2.0.0"
     :requires #{:videojs :dash-js}}
    :dash-js
    {:js "dist/dash.all.js"
     :git "git@github.com:Dash-Industry-Forum/dash.js.git"
     :tag "v1.5.1"}
    :videojs.ima
    {:js "src/videojs.ima.js"
     :css "src/videojs.ima.css"
     :git "git@github.com:googleads/videojs-ima.git"
     :tag "0.2.0"
     :requires #{:videojs}}
    :videojs
    {:js "dist/video.js"
     :css "dist/video-js.css"
     :git "git@github.com:videojs/video.js.git"
     :tag "v5.4.6"}}})

(defn download-dependencies []
  (c/download-dependencies config))

(defn minify-js []
  (c/minify-js config))

(defn minify-js-cmd []
  (c/minify-js-cmd config))

(defn deps-order []
  (c/deps-order config))

(defn minify-css-cmd []
  (c/minify-css-cmd config))

(defn minify-css []
  (c/minify-css config))

(defn dev-js []
  (c/dev-js config))
