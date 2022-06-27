(ns kafka-util.utils
  (:require [camel-snake-kebab.core :as csk])
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

(defn append-to-file [file-path ^String content]
  (with-open [writer (clojure.java.io/writer file-path :append true)]
    (.write writer content)))

(defn equals-ignore-case [s1 s2]
  (= (clojure.string/lower-case s1)
     (clojure.string/lower-case s2)))

(defn slurp-lines [file-path]
  (-> file-path
      slurp
      (.split "\n")))



(defn ->long-or [str or]
  (try (Long/parseLong str)
       (catch Exception e or)))


(defn ->csv [m]
  "Takes in a map and converts the key values in it to comma separated values"
  (reduce-kv #(format "%s%s,%s\n" %1 %2 %3) "" m))

(defn not-nil?
  [x]
  (not (nil? x)))

(defn all-not-nil
  [& args]
  (every? not-nil? args))

(defn now-epoch []
  (.toEpochMilli (Instant/now)))

(defn hours->milli
  [hours]
  (* hours 60 60 1000))

(defn seconds->milli
  [seconds]
  (* seconds 1000))

(defn minutes->milli
  [minutes]
  (* minutes 60 1000))

(defn hours-ago-epoch
  [hours]
  (- (now-epoch) (hours->milli hours)))

(defn minutes-ago-epoch
  [minutes]
  (- (now-epoch) (minutes->milli minutes)))

(defn uid []
  (str (java.util.UUID/randomUUID)))

(defn seconds-ago-epoch
  [seconds]
  (- (now-epoch) (seconds->milli seconds)))