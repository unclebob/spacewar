(defproject spacewar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [quil "2.7.1"]
                 [org.clojure/math.combinatorics "0.1.4"]]
  :profiles {:dev {:dependencies [[midje "1.9.2"]]}
             ;; You can add dependencies that apply to `lein midje` below.
             ;; An example would be changing the logging destination for test runs.
             ;; Note that Midje itself is in the `dev` profile to support
             ;; running autotest in the repl.
             :midje {}}
  :main spacewar.core)


