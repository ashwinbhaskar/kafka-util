(defproject io.github.ashwinbhaskar/kafka-util "0.1.2"
  :description "A simple interface to fetch data from kafka"
  :url "https://github.com/ashwinbhaskar/kafka-util"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.apache.kafka/kafka-clients "2.1.0"]
                 [dvlopt/kafka "1.3.1"]
                 [camel-snake-kebab "0.4.2"]
                 [org.clojure/core.async "1.5.648"]
                 [org.clojure/data.json "2.4.0"]
                 [com.taoensso/timbre "5.2.1"]]
  :license {:name         "Apache-2.0 License"
            :url          "https://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo
            :comments     ""}
  :repositories [["clojars" {:url   "https://repo.clojars.org"
                              :creds :gpg}]]
  :pom-addition ([:developers
                  [:developer
                   [:id "ashwinbhaskar"]
                   [:name "Ashwin Bhaskar"]
                   [:url "https://github.com/ashwinbhaskar"]
                   [:roles
                    [:role "developer"]
                    [:role "maintainer"]]]])
  :scm {:name "git" :url "https://github.com/ashwinbhaskar/kafka-util"}
  :repl-options {:init-ns kafka-util.core})
