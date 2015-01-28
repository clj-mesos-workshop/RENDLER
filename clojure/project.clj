(defproject rendler "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :jvm-opts [~(str "-Djava.library.path=/usr/local/lib/:" (System/getProperty "java.library.path"))]
  :profiles {:uberjar {:aot :all}}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [enlive "1.1.5"]
                 [digest "1.4.4"]
                 [clj-mesos "0.20.5"]])
