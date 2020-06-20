;;   Copyright (c) Metadata Partners, LLC and Contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns datomic.codeq.util
  (:require [datomic.client.api :as d]))

(set! *warn-on-reflection* true)

(defn index-get-id
  [db attr v]
  (let [d (first (d/index-range db {:attrid attr
                                    :start  v}))]
    (when (and d (= (:v d) v))
      (:e d))))

(defn tempid
  [prefix]
  (str (gensym (str "tempid-" prefix))))

(defn index->id-fn
  [db attr]
  (memoize
   (fn [x]
     (or (index-get-id db attr x)
         (tempid :db.part/user)))))

(def tempid? string? )

(defn entid
  [db ident]
  (ffirst (d/q '[:find ?e
                 :in $ ?ident
                 :where [?e :db/ident ?ident]]
               db ident)))