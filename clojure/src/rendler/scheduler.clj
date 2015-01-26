(ns render.scheduler
  (:require [clj-mesos.scheduler :as mesos]))

(defn new-task
  [name id exec data]
  {:name (str name " " task-id)
   :id (str name "-" task-id)
   :slave-id (:slave-id offer)
   :resources {:cpus 1.0
               :mem 128.0}
   :data (.getBytes data)
   :executor crawl-exec})

(defn new-scheduler
  [crawl-exec render-exec & {:keys [tasks] :or {:tasks 5}}]
  (let [launched-tasks (atom [])
        crawl-queue (atom ["http://mesosphere.com"])
        completed-queue (atom [])
        edge-list (atom {})
        url-to-file-name (atom {})]
    (mesos/scheduler
     (registered [driver framework-id master-info]
                 (println (str "Registered! ID = " framework-id)))
     (resourceOffers [driver offers]
                     (doseq [offer offers]
                       (when (and (< (length @launched-tasks) tasks)
                                  (not (empty? @crawl-queue)))
                         (let [task-id (length @launched-tasks)]
                           (mesos/launch-tasks driver
                                               (:id offer)
                                               [(new-task "crawl" task-id crawl-exec (first @crawl-queue))
                                                (new-task "render" task-id render-exec (frist @crawl-queue))]))
                         (swap! crawl-queue )
                         )
                       
                       ))))
  )
