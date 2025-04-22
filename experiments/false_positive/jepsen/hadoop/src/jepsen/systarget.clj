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
  (:import [edu.uva.liftlab.hadoop JepsenT2C]))

(defn parse-long-nil
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (parse-long s)))

(def numFiles 100)
(def systarget "hadoop")
(def username "vqx2dc")
(def password "")
(def tilde (str "/u/" username "/"))
(def privkey (str tilde ".ssh/uva_ed"))
(def dir (str tilde systarget "/"))
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
      (info node (str systarget " ready")))
    
      

    (teardown! [_ test node]
               (info node (str "Tearing down " systarget))
               (info node (str systarget " stopped"))) 
    


    db/LogFiles
    (log-files [_ test node]
      logfiles)))

(defn opRead   [_ _] {:type :invoke, :f :read, :value (rand-int numFiles)})
(defn opWrite   [_ _] {:type :invoke, :f :write, :value (rand-int numFiles)})
(defn opDelete   [_ _] {:type :invoke, :f :delete, :value (rand-int numFiles)})
(defn opTrunc   [_ _] {:type :invoke, :f :trunc, :value (rand-int numFiles)})
(defn opMkdirs   [_ _] {:type :invoke, :f :mkdirs, :value (rand-int numFiles)})
(defn opLs   [_ _] {:type :invoke, :f :ls, :value (rand-int numFiles)})
(defn opChksum   [_ _] {:type :invoke, :f :chksum, :value (rand-int numFiles)})
(defn opRename   [_ _] {:type :invoke, :f :rename, :value (rand-int numFiles)})
(defn opSymlinkCreate   [_ _] {:type :invoke, :f :symlinkCreate, :value (rand-int numFiles)})
(defn opSnapshotCreate   [_ _] {:type :invoke, :f :snapshotCreate, :value (rand-int numFiles)})
(defn opSnapshotDelete   [_ _] {:type :invoke, :f :snapshotDelete, :value (rand-int numFiles)})


(defn client
  "DB Client"
  [host]

  (reify client/Client
    (open! [_ test node]
      (client node))
      
      

    (setup! [this test]) 
      
      

    (invoke! [this test op]
      (timeout 20000 (assoc op :type :info, :error :timeout)
               (case (:f op)
                 :read (assoc op :type :ok, :value (.read javaClient host (:value op)))
                               
                 :write (do (.write javaClient host (:value op))
                            (assoc op :type :ok))
                 :delete (assoc op :type :ok, :value (.delete javaClient "lift11" (:value op)))
                 :trunc (assoc op :type :ok, :value (.truncate javaClient "lift11" (:value op))) 
                 :mkdirs (assoc op :type :ok, :value (.mkdirs javaClient "lift11" (:value op))) 
                 :ls (assoc op :type :ok, :value (.ls javaClient "lift11" (:value op))) 
                 :chksum (assoc op :type :ok, :value (.chksum javaClient "lift11" (:value op))) 
                 :rename (do (.rename javaClient "lift11" (:value op))
                            (assoc op :type :ok))
                 :symlinkCreate (do (.symlinkCreate javaClient host (:value op))
                                 (assoc op :type :ok))
                 :snapshotCreate (do (.snapshotCreate javaClient host (:value "/"))
                                  (assoc op :type :ok))
                 :snapshotDelete (do (.snapshotDelete javaClient host (:value "/"))
                                  (assoc op :type :ok))
                ))) 
      

    (close! [_ test]
      (info "Closing session close"))

    (teardown! [_ test]
      (info "Closing session teardown"))))
  

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
                      (->> (gen/cycle [(gen/limit 3000 (gen/stagger 0.05 (gen/mix [opRead opWrite opDelete opTrunc opMkdirs opLs opChksum opRename])))
                                       (gen/sleep 180)])
                           (gen/nemesis
                            (cycle [(gen/sleep 300)
                                    {:type :info, :f :start}
                                    (gen/sleep 5)
                                    {:type :info, :f :stop}]))
                           (gen/time-limit 1800))
                      (gen/nemesis {:type :info, :f :stop}))
                      
          :checker (checker/compose
                    {:perf   (checker/perf)})}))
        
  

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn test})
                   (cli/serve-cmd))
           args))
