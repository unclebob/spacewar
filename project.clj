(defproject spacewar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [figwheel-sidecar "0.5.15"]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [quil "2.7.1"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.clojure/tools.reader "1.3.2"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.15"]]
  :profiles {:dev {:dependencies [[midje "1.9.2"]]}
             ;; You can add dependencies that apply to `lein midje` below.
             ;; An example would be changing the logging destination for test runs.
             ;; Note that Midje itself is in the `dev` profile to support
             ;; running autotest in the repl.
             :midje {}}
  :main spacewar.core
  :clean-targets ^{:protect false} [:target-path "resources/public/js"]
  :cljsbuild
  {:builds [; development build with figwheel hot swap
            {:id "development"
             :source-paths ["src"]
             :figwheel true
             :compiler
             {:main "spacewar.core"
              :output-to "resources/public/js/main.js"
              :output-dir "resources/public/js/development"
              :asset-path "js/development"}}
            ; minified and bundled build for deployment
            {:id "optimized"
             :source-paths ["src"]
             :compiler
             {:main "spacewar.core"
              :output-to "resources/public/js/main.js"
              :output-dir "resources/public/js/optimized"
              :asset-path "js/optimized"
              :optimizations :advanced}}]})