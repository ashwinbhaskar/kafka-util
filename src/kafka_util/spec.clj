(ns kafka-util.spec
  (:require [clojure.spec.alpha :as s]
            [kafka-util.utils :as u]))

(s/def ::broker (s/and string? u/not-nil? not-empty))

(s/def ::port pos-int?)

(s/def ::security-protocol #{"SSL" "PLAINTEXT" "SASL_PLAINTEXT" "SASL_SSL"})

(s/def ::key-deserializer #{:string :long})

(s/def ::decode-value-as-json boolean?)

(s/def ::consumer-settings (s/keys :req-un [::broker ::port ::security-protocol ::key-deserializer]
                                   :opt-un [::decode-value-as-json]))
