(ns rendler.render-executor
  (:require [clj-mesos.executor :as mesos]
            [clojure.java.shell :as sh])
  (:gen-class))

(defn new-executor
  []
  (let [cwd (atom "")]
    (mesos/executor
     (registered [driver executor-info framework-info slave-info]
                 (println (str "Registered executor on " (:hostname slave-info)))
                 (swap! cwd str (String. (:data executor-info))))
     (launchTask [driver task-info]
                 (mesos/send-status-update driver
                                           {:task-id (:task-id task-info)
                                            :state :task-running})
                 (let [url (String. (:data task-info))
                       render-js (str (.getParent (java.io.File. @cwd)) "/render.js")
                       work-path-dir (str @cwd "/renderoutput/")
                       filename (str work-path-dir (:task-id task-info) ".png")
                       cmd ["phantomjs" render-js url filename]]
                   (try
                     (apply sh/sh cmd)

                     (if (.exists (java.io.File. filename))
                       (->> {:render [url filename]}
                            pr-str
                            (.getBytes)
                            (mesos/send-framework-message driver)))
                     (catch Exception e
                       (println (str "Exception in PhantomJS: " e))))
                   (mesos/send-status-update driver
                                             {:task-id (:task-id task-info)
                                              :state :task-finished}))))))

(defn -main
  [& args]
  (-> (mesos/driver (new-executor))
      (mesos/run)))
