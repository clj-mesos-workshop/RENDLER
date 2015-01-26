(ns rendler.core
  (:require [render.scheduler :refer [new-scheduler]]
            [clj-mesos.scheduler :as mesos]))

(def usage
  "")

(def artifact
  "rendler-1.0-SNAPSHOT-standalone.jar")

(defn crawl-cmd
  [uri]
  {:uri uri
   :value (str "java -cp " artifact " render.crawl_executor")})

(defn render-cmd
  [uri]
  {:uri uri
   :value (str "java -cp " artifact " render.crawl_executor")})

(defn jar-path
  []
  (str (System/getProperty "user.dir") "/target/" artifact))

(defn -main
  [& [master tasks & _]]
  (when-not task
    (println usage)
    (System/exit 1))
  (let [uri {:value (jar-path)
             :extract false}
        crawl-executor {:executor-id "CrawlExecutor"
                        :name "Crawl Executor (clojure)"
                        :command (crawl-cmd uri)}
        render-executor {:executor-id "RenderExecutor"
                         :name "Render Executor (clojure)"
                         :data (.getBytes (System/getProperty "user.dir"))
                         :command (render-cmd uri)}
        scheduler (new-scheduler crawl-executor render-executor :tasks (when tasks
                                                                         (Integer/parseInt tasks)))
        driver (mesos/driver scheduler
                             {:user "" :name "Rendler Framework (Java)"}
                             master)]
    (mesos/run driver)
    (mesos/stop driver)))
