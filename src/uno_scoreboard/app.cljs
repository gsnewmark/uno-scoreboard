(ns uno-scoreboard.app
  (:require rum))


(enable-console-print!)


(rum/defc example [n text]
  [:h2 (repeat n text)])


(defn render []
  (if-let [root (.getElementById js/document "app")]
    (rum/mount (example 5 "abc") root)
    (.error js/console "Root element wasn't found")))

(defn reload [] (render))

(defn start [] (render))




