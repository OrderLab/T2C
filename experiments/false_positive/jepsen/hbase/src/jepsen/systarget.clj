;; lein run test --node lift11 --node lift12 --node lift13 --node lift14 --node lift15 --ssh-username dimas --ssh-password LIFTLABUVA --ssh-private-key ~/.ssh/id_rsa &> test.log
;; lein run test --node lift11 --node lift12 --node lift13 --node lift14 --node lift15 --ssh-private-key ~/.ssh/uva_ed &> test.log
(ns jepsen.systarget
  (:gen-class)
  (:require [clojure.tools.logging :refer :all]
            [jepsen [db         :as db]
             [cli        :as cli]
             [checker    :as checker]
             [client     :as client]
             [control    :as c]
             [generator  :as gen]
             [nemesis    :as nemesis]
             [tests      :as tests]
             [util       :refer [timeout]]])
  (:import [edu.uva.liftlab.hbase JepsenT2C])
  )

(def querynum 10000)
(def systarget "hbase")
(def username "vqx2dc")
(def password "")
(def tilde (str "/u/" username "/"))
(def privkey (str tilde ".ssh/uva_ed"))
(def dir (str "/localtmp/" systarget "/"))
(def binary "bin/zkServer.sh")
(def pidfile (str dir "data/" systarget "_server.pid"))
(def datadir (str dir (c/lit "version-2")))
(def logfiles [(str dir systarget ".out")])

(def javaClient (JepsenT2C.))

(defn db
  "Target system DB"
  []
  (reify db/DB
    (setup! [_ test node]
      (info node (str "Starting " systarget))
      (info node (str systarget " ready"))
    )
      

    (teardown! [_ test node]
               (info node (str "Tearing down " systarget))
               (info node (str systarget " stopped")) 
    )


    db/LogFiles
    (log-files [_ test node]
      logfiles
    )
  )
)

(defn r   [_ _] {:type :invoke, :f :read, :value (rand-int querynum)})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int querynum)})

(defn client 
  "DB Client"
  [host] 
  (reify client/Client
    (open! [this test node]
      (client node)
      )

    (setup! [this test]
      )

    (invoke! [this test op] 
      (timeout 5000 (assoc op :type :info, :error :timeout)
               (case (:f op)
                 :read (assoc op :type :ok, :value (.read javaClient host (:value op)))
                 :write (do (.write javaClient host (:value op))
                            (assoc op :type :ok))
                 ))
      )

    (close! [_ test] 
      (info "Closing session close"))

    (teardown! [_ test]
      (info "Closing session teardown"))
    ))

(defn test
  "Given an options map from the command-line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (info "Creating test" opts)
  (doto javaClient
    (.populate))
  (merge tests/noop-test
         opts
         {:name    systarget
          :db      (db)
          :client  (client nil)
          :ssh {:username username
                :password password
                :private-key-path privkey
                :strict-host-key-checking :no}
          ;; :nemesis (nemesis/partition-random-node)
          ;; :nemesis #_{:clj-kondo/ignore [:unresolved-symbol]}
          ;;          (nemesis/node-start-stopper
          ;;           nodes
          ;;           (fn start [test node]
          ;;             (c/su
          ;;              (c/cd dir
          ;;                    (c/exec :bash binary :start)
          ;;                    (c/exec :sleep :20)
          ;;                    )
          ;;              )
          ;;             )
          ;;           (fn stop [test node]
          ;;             (c/su
          ;;              (c/cd dir
          ;;                    (c/exec :bash binary :stop)))
          ;;             )
          ;;           )
          :generator (gen/phases
                      (->> (gen/cycle [(gen/limit 3000 (gen/stagger 0.05 (gen/mix [r w])))
                                       (gen/sleep 180)])
                           (gen/nemesis
                            (cycle [(gen/sleep 300)
                                    {:type :info, :f :start}
                                    (gen/sleep 5)
                                    {:type :info, :f :stop}]))
                           (gen/time-limit 1800))
                      (gen/nemesis {:type :info, :f :stop})
                      )
          ;; :generator (gen/phases
          ;;             (->> (gen/mix [r w])
          ;;                  (gen/nemesis
          ;;                   (cycle [(gen/sleep 300)
          ;;                           {:type :info, :f :start}
          ;;                           (gen/sleep 5)
          ;;                           {:type :info, :f :stop}]))
          ;;                  (gen/time-limit 240))
          ;;             (gen/nemesis {:type :info, :f :stop}))
          :checker (checker/compose
                    {:perf   (checker/perf)})
        }
  ))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn test})
                   (cli/serve-cmd))
           args))
