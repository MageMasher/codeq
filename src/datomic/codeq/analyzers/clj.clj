;;   Copyright (c) Metadata Partners, LLC and Contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns datomic.codeq.analyzers.clj
  (:require [datomic.client.api :as d]
            [datomic.codeq.util :refer [index->id-fn tempid?]]
            [datomic.codeq.analyzer :as az]
            [datomic.codeq.util :as util]))

(defn analyze-1
  "returns [tx-data ctx]"
  [db f x {:keys [loc line col endline endcol]} seg ret {:keys [sha->id codename->id added ns] :as ctx}]
  (if loc
    (let [sha (-> seg az/ws-minify az/sha)
          codeid (sha->id sha)
          newcodeid (and (tempid? codeid) (not (added codeid)))
          ret (cond-> ret newcodeid (conj {:db/id codeid :code/sha sha :code/text seg}))
          added (cond-> added newcodeid (conj codeid))

          codeqid (or (ffirst (d/q '[:find ?e :in $ ?f ?loc
                                     :where [?e :codeq/file ?f] [?e :codeq/loc ?loc]]
                                   db f loc))
                      (util/tempid :db.part/user))

          op (first x)
          ns? (= op 'ns)
          defing (and ns
                      (symbol? op)
                      (.startsWith (name op) "def"))

          naming (let [nsym (second x)]
                   (cond
                    ns? (str nsym)
                    defing (if (namespace nsym)
                             (str nsym)
                             (str (symbol (name ns) (name nsym))))))

          nameid (when naming (codename->id naming))

          ret (cond-> ret
                      (tempid? codeqid)
                      (conj {:db/id codeqid
                             :codeq/file f
                             :codeq/loc loc
                             :codeq/loc+line line
                             :codeq/loc+col col
                             :codeq/loc+endline endline
                             :codeq/loc+endcol endcol
                             :codeq/code codeid})

                      ns?
                      (conj [:db/add codeqid :clj/ns nameid])

                      defing
                      (conj [:db/add codeqid :clj/def nameid]
                            [:db/add codeqid :clj/defop (str op)])

                      (tempid? nameid)
                      (conj [:db/add nameid :code/name naming]))]
      [ret (assoc ctx :added added)])
    [ret ctx]))

(defn analyze
   [db f src]
   (with-open [r (clojure.lang.LineNumberingPushbackReader. (java.io.StringReader. src))]
     (let [loffs (az/line-offsets src)
           eof (Object.)
           ctx {:sha->id (index->id-fn db :code/sha)
                :codename->id (index->id-fn db :code/name)
                :added #{}}]
       (loop [ret [], ctx ctx, x (read r false eof)]
         (if (= eof x)
           ret
           (let [{:keys [line column]} (meta x)
                 ctx (if (and (coll? x) (= (first x) 'ns))
                       (assoc ctx :ns (second x))
                       ctx)
                 endline (.getLineNumber r)
                 endcol (.getColumnNumber r)
                 [loc seg] (when (and line column)
                             [{:loc (str line " " column " " endline " " endcol)
                               :line line :col column :endline endline :endcol endcol}
                              (az/segment src loffs (dec line) (dec column) (dec endline) (dec endcol))])
                 [ret ctx] (analyze-1 db f x loc seg ret ctx)]
             (recur ret ctx (read r false eof))))))))

(defn schemas []
  {1 [{:db/ident :clj/ns
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/doc "codename of ns defined by expression"}
      {:db/ident :clj/def
       :db/valueType :db.type/ref
       :db/cardinality :db.cardinality/one
       :db/doc "codename defined by expression"}]
   2 [{:db/ident :clj/defop
       :db/valueType :db.type/string
       :db/cardinality :db.cardinality/one
       :db/doc "the def form (defn, defmacro etc) used to create this definition"}]})

(deftype CljAnalyzer []
  az/Analyzer
  (keyname [a] :clj)
  (revision [a] 2)
  (extensions [a] [".clj"])
  (schemas [a] (schemas))
  (analyze [a db f src] (analyze db f src)))

(defn impl [] (CljAnalyzer.))