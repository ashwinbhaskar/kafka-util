(ns kafka-util.core
  (:require [dvlopt.kafka :as K]
            [dvlopt.kafka.in :as K.in]
            [taoensso.timbre :as timbre]
            [clojure.core.async :refer [thread chan >!! <!! close! timeout alts!!]]
            [kafka-util.utils :as u]
            [clojure.spec.alpha :as s]
            [kafka-util.spec :as kafka-spec]
            [clojure.data.json :as j]
            [camel-snake-kebab.core :as csk])
  (:import (org.apache.kafka.common.utils Utils)))


(defn- consumer-config
  ([consumer-group-id {:keys [key-deserializer security-protocol broker port] :as settings}]
   (if (s/valid? ::kafka-spec/consumer-settings settings)
     {::K/nodes              [[broker port]]
      ::K/deserializer.key   key-deserializer
      ::K/deserializer.value :string
      ::K.in/configuration   {"auto.offset.reset"  "latest"
                              "enable.auto.commit" false
                              "group.id"           consumer-group-id
                              "security.protocol"  security-protocol}}
     (do
       (throw (IllegalArgumentException. ^String (s/explain-str ::kafka-spec/consumer-settings settings)))))))


(defn compute-default-partition
  [key-bytes partitions]
  (mod (Utils/toPositive (Utils/murmur2 key-bytes))
       partitions))

(defn- consume
  [consumer channel decode-value-as-json]
  (loop []
    (let [records (K.in/poll consumer {::K/timeout [100 :seconds]})]
      (if (nil? records)
        (do
          (close! channel)
          (timbre/info "Stopping consumer due to timeout"))
        (if (>!! channel (->> records
                              (map (fn [m]
                                     (let [v (if decode-value-as-json
                                               (-> (::K/value m)
                                                   (j/read-str :key-fn csk/->kebab-case-keyword))
                                               (::K/value m))]
                                       {:headers   (::K/headers m)
                                        :key       (::K/key m)
                                        :value     v
                                        :offset    (::K/offset m)
                                        :partition (::K/partition m)
                                        :topic     (::K/topic m)
                                        :timestamp (::K/timestamp m)})))))
          (recur)
          (timbre/info "Stopped consuming as channel is closed"))))))

(defmacro with-consumer-group-id [id & body]
  `(let [~id ~(u/uid)]
     (timbre/debug "Consumer group id used " ~id)
     ~@body))


(defn consume-records-latest
  "consumer-settings is a map
  {:key-deserializer :string/:long,
   :security-protocol : SSL/PLAINTEXT/SASL_PLAINTEXT/SASL_SSL,
   :broker <string>,
   :port <int>,
    :decode-value-as-json true/false"
  ([{:keys [decode-value-as-json] :as consumer-settings} topic channel]
   (with-consumer-group-id
     group-id
     (with-open [consumer (K.in/consumer (consumer-config group-id consumer-settings))]
       (timbre/debug "Will consume from latest offsets in all partitions of topic " topic)
       (K.in/register-for consumer (re-pattern topic))
       (consume consumer channel decode-value-as-json))))
  ([{:keys [decode-value-as-json] :as consumer-settings} topic channel partition]
   (with-consumer-group-id
     group-id
     (with-open [consumer (K.in/consumer (consumer-config group-id consumer-settings))]
       (timbre/debug "Will consume from latest offset for topic " topic " and partition " partition)
       (K.in/register-for consumer [[topic partition]])
       (consume consumer channel decode-value-as-json)))))

(defn consume-records-minutes-ago
  "consumer-settings is a map
  {:key-deserializer :string/:long,
   :security-protocol : SSL/PLAINTEXT/SASL_PLAINTEXT/SASL_SSL,
   :broker <string>,
   :port <int>,
    :decode-value-as-json true/false"
  ([{:keys [decode-value-as-json] :as consumer-settings} topic channel minutes-ago]
   (with-consumer-group-id
     group-id
     (with-open [consumer (K.in/consumer (consumer-config group-id consumer-settings))]
       (timbre/debug (format "Will consume from topic %s from all partitions (with offset calculated for %d minutes ago)" topic minutes-ago))
       (let [start-time (u/minutes-ago-epoch minutes-ago)
             partitions (count (K.in/partitions consumer topic))
             _ (timbre/debug "Number of partitions for topic " topic " is " partitions)
             topic-partitions (->> (range 0 partitions)
                                   (map #(vec [topic %])))
             _ (timbre/debug "topic-partitions are " (doall topic-partitions))
             offsets-for-timestamps-payload (->> topic-partitions
                                                 (reduce (fn [acc topic-partition]
                                                           (assoc acc topic-partition start-time))
                                                         {}))
             topic-partitions-offsets (K.in/offsets-for-timestamps consumer offsets-for-timestamps-payload)
             seek-payload (->> topic-partitions-offsets
                               (map (fn [[topic-partition {offset ::K/offset}]]
                                      [topic-partition offset]))
                               (into {}))
             _ (timbre/debug "seek pay load is " (doall seek-payload))]
         (K.in/register-for consumer topic-partitions)
         (timbre/debug "Consumer registered for topic " topic " all partitions")
         (K.in/seek consumer seek-payload)
         (consume consumer channel decode-value-as-json)))))
  ([{:keys [decode-value-as-json] :as consumer-settings} topic channel minutes-ago partition]
   (with-consumer-group-id
     group-id
     (with-open [consumer (K.in/consumer (consumer-config group-id consumer-settings))]
       (timbre/debug (format "Will consume from topic %s from partition %d with offset calculated for %d minutes ago" topic partition minutes-ago))
       (K.in/register-for consumer [[topic partition]])
       (let [topic-partition-offset (K.in/offsets-for-timestamps consumer {[topic partition] (u/minutes-ago-epoch minutes-ago)})
             {offset ::K/offset} (get topic-partition-offset [topic partition])]
         (K.in/seek consumer {[topic partition] offset})
         (timbre/debug (format "Computed offset for %d minutes ago as %d" minutes-ago offset))
         (consume consumer channel decode-value-as-json))))))

(defn consume-records-offset
  "consumer-settings is a map
  {:key-deserializer :string/:long,
   :security-protocol : SSL/PLAINTEXT/SASL_PLAINTEXT/SASL_SSL,
   :broker <string>,
   :port <int>,
   :decode-value-as-json true/false}"
  [{:keys [decode-value-as-json] :as consumer-settings} topic channel partition offset]
  (with-consumer-group-id
    group-id
    (with-open [consumer (K.in/consumer (consumer-config group-id consumer-settings))]
      (timbre/debug (format "Will consume from topic %s from partition %d and offset %d" topic partition offset))
      (K.in/register-for consumer [[topic partition]])
      (K.in/seek consumer {[topic partition] offset})
      (consume consumer channel decode-value-as-json))))