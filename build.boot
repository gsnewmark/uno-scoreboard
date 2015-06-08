(task-options!
  pom {:project     'uno-scoreboard
       :version     "0.1.0"
       :description "A simple scoreboad for the Uno game"
       :url         "https://gsnewmark.github.io/uno-scoreboard"
       :scm         {:url "https://github.com/gsnewmark/uno-scoreboard"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})

(set-env!
  :source-paths   #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[[org.clojure/clojure       "1.7.0-RC1"]
                  [org.clojure/clojurescript "0.0-3308"]
                  [org.clojure/core.async    "0.1.346.0-17112a-alpha"]
                  [rum                       "0.2.6"]
                  [org.webjars/bootstrap     "3.3.4"]

                  [adzerk/boot-cljs      "0.0-3269-2"      :scope "test"]
                  [adzerk/boot-reload    "0.2.6"           :scope "test"]
                  [pandeiro/boot-http    "0.6.3-SNAPSHOT"  :scope "test"]])

(require
  '[adzerk.boot-cljs   :refer [cljs]]
  '[adzerk.boot-reload :refer [reload]]
  '[pandeiro.boot-http :refer [serve]])

(def compiler-opts
  {:warnings {:single-segment-namespace false}})

(deftask none-opts "Set CLJS compiler options for development environment." []
  (task-options!
   cljs {:optimizations :none
         :source-map    true
         :compiler-options compiler-opts})
  identity)

(deftask advanced-opts "Set CLJS compiler options for production build." []
  (task-options!
   cljs {:optimizations    :advanced
         :compiler-options (merge compiler-opts
                                  {:closure-defines {:goog.DEBUG false}
                                   :elide-asserts   true})})
  identity)

(deftask dev "Start development environment." []
  (comp (none-opts)
        (serve :dir           "target/"
               :resource-root "META-INF/resources/")
        (watch)
        (speak)
        (reload :on-jsload 'uno-scoreboard.app/reload)
        (cljs)))

(deftask build "Start production build." []
  (comp (advanced-opts)
        (cljs)))
