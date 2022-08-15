(ns kafka-util.utils
  (:require [camel-snake-kebab.core :as csk]
            [clojure.string :as st])
  (:import (java.time Instant)))

(defn kebab->camel [m]
  (->> m
       (reduce-kv (fn [i k v]
                    (let [key (csk/->camelCase k)
                          value (if (map? v)
                                  (kebab->camel v)
                                  v)]
                      (assoc i key value)))
                  {})))

(defn camel->kebab [m]
  (->> m
       (reduce-kv (fn [i k v]
                    (let [key (csk/->kebab-case-keyword k)
                          value (if (map? v)
                                  (camel->kebab v)
                                  v)]
                      (assoc i key value)))
                  {})))

(defn not-nil?
  [x]
  (not (nil? x)))

(defn now-epoch []
  (.toEpochMilli (Instant/now)))

(defn minutes->milli
  [minutes]
  (* minutes 60 1000))

(defn mk-string
  [separator collection]
  (->> collection
       (map str)
       (st/join separator)))

(defn minutes-ago-epoch
  [minutes]
  (- (now-epoch) (minutes->milli minutes)))

(defn uid []
  (str (java.util.UUID/randomUUID)))