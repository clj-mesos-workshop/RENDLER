(ns rendler.graph-writer
  (:require [clojure.java.io :as io]
            [digest :refer [sha-256]]))

(defn graph-writer
  [filename url-to-file-name edge-list]
  (println filename)
  (with-open [w (io/writer filename)]
    (.write w "digraph G {\n")
    (.write w "  node [shape=box];\n")
    (doseq [[url file] url-to-file-name]
      (.write w (str "  a" (sha-256 url) " [label=\"\" image=\"" file "\"];\n")))
    (doseq [[url _] url-to-file-name]
      (doseq [link (edge-list url)]
        (when (some #{link} (keys url-to-file-name))
          (.write w (str "  a" (sha-256 url) " -> a" (sha-256 link) "\n")))))
    (.write w "}\n"))
  (println (str "Wrote results to " filename)))
