(defproject kafka-util "0.1.0-SNAPSHOT"
  :description "A simple interface to fetch data from kafka"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.apache.kafka/kafka-clients "2.1.0"]
                 [dvlopt/kafka "1.3.1"]
                 [camel-snake-kebab "0.4.2"]
                 [org.clojure/core.async "1.5.648"]
                 [org.clojure/data.json "2.4.0"]
                 [com.taoensso/timbre "5.2.1"]]
  :repl-options {:init-ns kafka-util.core})
