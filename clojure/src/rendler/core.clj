(ns rendler.core
  (:require [rendler.scheduler :refer [new-scheduler]]
            [clj-mesos.scheduler :as mesos]))

(def usage
  "")

(def artifact
  "rendler-0.1.0-SNAPSHOT-standalone.jar")

(defn jar-path
  []
  (str (System/getProperty "user.dir") "/target/" artifact))

(defn cmd
  [uri cmd]
  {:uri uri
   :value (str "java -Djava.library.path="
               (System/getProperty "java.library.path")
               " -cp "
               (jar-path)
               " rendler."
               cmd)})

(defn -main
  [& [master tasks & _]]
  (when-not master 
    (println usage)
    (System/exit 1))
  (let [uri {:value (jar-path)
             :extract false}
        crawl-executor {:executor-id "CrawlExecutor"
                        :name "Crawl Executor (clojure)"
                        :command (cmd uri "crawl_executor")}
        render-executor {:executor-id "RenderExecutor"
                         :name "Render Executor (clojure)"
                         :data (.getBytes (System/getProperty "user.dir"))
                         :command (cmd uri "render_executor")}
        scheduler (new-scheduler crawl-executor render-executor (if tasks
                                                                  (Integer/parseInt tasks)
                                                                  5))
        driver (mesos/driver scheduler
                             {:user "" :name "Rendler Framework (clojure)"}
                             master)]
    (println crawl-executor)
    (mesos/run driver)
    (mesos/stop driver)))
