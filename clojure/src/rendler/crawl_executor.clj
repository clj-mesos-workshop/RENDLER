(ns rendler.crawl-executor
  (:require [clj-mesos.executor :as mesos]
            [net.cgrand.enlive-html :as html])
  (:gen-class))

(defn get-url
  [url]
  (html/html-resource (java.net.URL. url)))

(defn get-links
  [page]
  (->> (html/select page [:a])
       (map #(get-in % [:attrs :href]))
       (filter (comp not empty?))
       (filter #(re-find #"http" %))))

(defn new-executor
  []
  (mesos/executor
   (registered [driver executor-info framework-info slave-info]
               (println (str "Registered executor on " (:hostname slave-info))))
   (launchTask [driver task-info]
               (mesos/send-status-update driver
                                         {:task-id (:task-id task-info)
                                          :state :task-running})
               (let [url (String. (:data task-info))]
                 (try
                   (->> {:crawl (cons url (get-links (get-url url)))}
                        pr-str
                        (.getBytes)
                        (mesos/send-framework-message driver))
                   (catch java.io.IOException e
                     (println "Link may not be valid. Error parsing html: " e))))
               (mesos/send-status-update driver
                                         {:task-id (:task-id task-info)
                                          :state :task-finished}))))

(defn -main
  [& args]
  (-> (mesos/driver (new-executor))
      (mesos/run)))
