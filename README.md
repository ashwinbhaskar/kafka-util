# kafka-util

A Clojure library designed to provide a simple interface to consume from Kafka for debugging purposes. For example, you can quickly retrieve records for the queries like 
- *Give me latest records from partition 1*
- *Give me records from all partitions from 15 minutes ago*
- *Give me records for partition 1 starting from offset 23434*
- *Give me records for partition 1 from 45 minutes ago*
- *Compute the partition for a given key*
<br><br>The library uses `clojure.core.async` to communicate back the kafka records.

## Add Dependency

Add the following to the dependencies section of `project.clj`
```clojure
[io.github.ashwinbhaskar/kafka-util "0.1.1"]
```

## Usage

```clojure
(:require [kafka-util.core :as ku]
          [clojure.core.async :refer [thread chan >!! <!! close! timeout]])

(def consumer-settings {:broker               "localhost"
                        :port                 9092
                        :security-protocol    "PLAIN_TEXT"
                        :decode-value-as-json true
                        :key-deserializer     :string})
(defn process-records
  [records]
  (->> records 
       (run! (fn [{:keys [value partition offset topic headers]}] 
               (println value)))))

(comment
  (let [topic "my-topic"
        channel (timeout 1000000)
        minutes-ago 60
        partition 2
        offset 564646
        total-partitions 8]
    (thread
      (ku/consume-records-latest consumer-settings topic channel)
      (ku/consume-records-latest consumer-settings topic channel partition)
      (ku/consume-records-minutes-ago consumer-settings topic channel minutes-ago)
      (ku/consume-records-minutes-ago consumer-settings topic channel minutes-ago partition)
      (ku/consume-records-offset consumer-settings topic channel partition offset)
      (ku/compute-partition (.getBytes "my-key") total-partitions))
    (loop []
      (if-let [records (<!! channel)]
        (do
          (process-records records)
          (recur))
        (println "Channel is closed!")))))

```
