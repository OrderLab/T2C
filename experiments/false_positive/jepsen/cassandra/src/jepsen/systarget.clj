;; lein run test --node lift11 --node lift12 --node lift13 --node lift14 --node lift15 --ssh-private-key ~/.ssh/uva_ed &> test.log
;; lein run test --node node0 --node node1 --node node2 --node node3 --node node4 --ssh-private-key ~/.ssh/id_rsa &> test.log
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
  (:import [edu.uva.liftlab.cassandra JepsenT2C]))

(defn parse-long-nil
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (parse-long s)))

(def querynum 100)
(def systarget "cassandra")
(def username "vqx2dc")
(def password "")
(def tilde (str "/u/" username "/"))
(def privkey (str tilde ".ssh/uva_ed"))
(def dir (str "/localtmp/" systarget "/"))
(def binary "bin/cassandra")
(def pidfile (str dir systarget "_server.pid"))
(def datadir (str dir "data/"))
(def logfiles [(str dir "logs/debug.log")])

(def clientBuilder (JepsenT2C.))

(defn db
  "Target system DB"
  []
  (reify db/DB
    (setup! [_ test node]
      (info node (str "Starting " systarget))
      ;; (c/trace
      ;;   (c/su
      ;;     (c/cd dir
      ;;       (c/exec :bash binary :-p pidfile)
      ;;       (c/exec :sleep :60)
      ;;     )
      ;;   )
      ;; ) 
      (info node (str systarget " ready")))
    
      

    (teardown! [_ test node]
               (info node (str "Tearing down " systarget))
              ;;  (c/su
              ;;   (c/cd dir 
              ;;         (c/exec (c/lit "kill `cat cassandra_server.pid`" ))
              ;;         )
              ;;   (c/cd dir
              ;;         (c/exec :rm :-rf datadir)
              ;;         )
              ;;   )
               (info node (str systarget " stopped"))) 
    


    db/LogFiles
    (log-files [_ test node]
      logfiles)))
    
  


(defn r   [_ _] {:type :invoke, :f :read, :value (rand-int querynum)})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int querynum)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int querynum) (rand-int querynum)]})

(defn client
  "DB Client"
  [host]
  (reify client/Client
    (open! [this test node]
      (client node))
        
      

    (setup! [this test])
      ;; Populate table 
       
    

    (invoke! [this test op]
      (timeout 5000 (assoc op :type :info, :error :timeout)
               (case (:f op)
                 :read (assoc op :type :ok, :value (parse-long-nil (
                                                                    .read clientBuilder host (:value op) -1)))
                                                                    
                 :write (do (.update clientBuilder host (:value op))
                            (assoc op :type :ok)))))
                
      

    (close! [_ test]
      ;; (.close conn)
      (info "Closing session close"))

    (teardown! [_ test]
      (info "Closing session teardown"))
    
    ;; client/Reusable
    ;; (reusable? [this test]
    ;;   true)
   ))

  

(defn test
  "Given an options map from the command-line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts] 
  (info "Creating test" opts)
  (doto clientBuilder
    (.populate querynum))
  (merge tests/noop-test
         opts
         {:name    systarget
          :db      (db)
          :client  (client nil)
          :ssh {:username username
                :password password
                :private-key-path privkey
                :strict-host-key-checking :no}
          :nemesis (nemesis/partition-random-node)
          :generator (gen/phases
                      (->> (gen/cycle [(gen/limit querynum (gen/stagger 1 (gen/mix [r w])))
                                       (gen/sleep 180)])
                           (gen/nemesis
                            (cycle [(gen/sleep 300)
                                    {:type :info, :f :start}
                                    (gen/sleep 5)
                                    {:type :info, :f :stop}]))
                           (gen/time-limit 1800))
                      (gen/nemesis {:type :info, :f :stop})) 
          :checker (checker/compose
                    {:perf   (checker/perf)})
          }))
        
  

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn test})
                   (cli/serve-cmd))
           args))
