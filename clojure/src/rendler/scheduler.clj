(ns rendler.scheduler
  (:require [clj-mesos.scheduler :as mesos]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [rendler.graph-writer :refer [graph-writer]]))

(defn new-task
  [name id exec data slave-id]
  {:name (str name " " id)
   :task-id (str name "-" id)
   :slave-id slave-id
   :resources {:cpus 1.0
               :mem 128.0}
   :data (.getBytes data)
   :executor exec})

(defn new-scheduler
  [crawl-exec render-exec tasks]
  (let [launched-tasks (atom 0)
        finished-tasks (atom 0)
        crawl-queue (atom ["http://mesos.apache.org"])
        completed-queue (atom [])
        edge-list (atom {})
        url-to-file-name (atom {})]
    (mesos/scheduler
     (registered [driver framework-id master-info]
                 (println (str "Registered! ID = " framework-id)))
     (resourceOffers [driver offers]
                     (doseq [offer offers]
                       (when (and (< @launched-tasks tasks)
                                  (not (empty? @crawl-queue)))
                         (mesos/launch-tasks driver
                                             (:id offer)
                                             [(new-task "crawl"
                                                        @launched-tasks
                                                        crawl-exec
                                                        (first @crawl-queue)
                                                        (:slave-id offer))
                                              (new-task "render"
                                                        @launched-tasks
                                                        render-exec
                                                        (first @crawl-queue)
                                                        (:slave-id offer))])
                         (swap! crawl-queue rest)
                         (swap! launched-tasks inc))))
     (statusUpdate [driver task-info]
                   (let [{:keys [state task-id]} task-info]
                       (if (or (= state :task-finished)
                            (= state :task-lost))
                      (do
                        (println (str "Status update: task "
                                      task-id
                                      " has completed with state "
                                      state))
                        (swap! finished-tasks inc)
                        (println (str "Finished Tasks: " @finished-tasks))
                        (if (= @finished-tasks (* 2 tasks))
                          (graph-writer (str (.getParent (io/file (System/getProperty "user.dir")))
                                             "/result.dot")
                                        @url-to-file-name
                                        @edge-list)))
                      (println (str "Status update: task "
                                    task-id
                                    " is in state "
                                    state)))))
     (frameworkMessage [driver executor-id slave-id data]
                       (let [{:keys [crawl render]} (read-string (String. data))]
                         (cond
                           crawl (let [[source & urls] crawl]
                                   (doseq [url urls]
                                     (when (and (not (some #{url} @completed-queue))
                                                (not (some #{url} @crawl-queue)))
                                       (swap! crawl-queue conj url)))
                                   (doseq [[edge _] @edge-list]
                                     (swap! edge-list update-in [edge] (fn [url-set]
                                                                         (apply conj url-set urls))))
                                   (when-not (contains? @edge-list source)
                                     (swap! edge-list assoc source (set urls))))
                           render (swap! url-to-file-name
                                         assoc (first render) (second render))))))))
