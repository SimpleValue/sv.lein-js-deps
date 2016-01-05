(ns sv.js-deps.utils)

(defn io-map
  ([number-of-threads f seq]
     (let [executor (java.util.concurrent.Executors/newFixedThreadPool number-of-threads)
           results (.invokeAll executor (map (fn [x] #(f x)) seq))
           result (doall (map deref results))]
       (.shutdown executor)
       result))
  ([f seq] (io-map 50 f seq)))
