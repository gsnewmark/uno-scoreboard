(ns uno-scoreboard.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require rum
            [cljs.core.async :refer [<! chan put!]]))


(enable-console-print!)


(defonce data (atom nil))

(defonce events (chan))

(def ^:private initial-data
  {:players [["Player 1" 0] ["Player 2" 0] ["Player 3" 0]]
   :max-score 500})


(rum/defcs input < (rum/local nil) [state type value callback]
  (let [v (:rum/local state)]
    [:span
     [:input {:type type
              :value (or @v value)
              :on-change #(reset! v (.. % -target -value))}]
     [:button {:on-click #(callback (or @v value))} "Save"]]))

(rum/defcs editable < (rum/local ::show) [state type label value callback]
  (let [mode (:rum/local state)]
    [:span.editable label
     (if (= ::show @mode)
       [:span
        {:on-click (fn [_] (reset! mode ::edit))}
        value]
       (input type value
              (fn [new-value]
                (callback new-value)
                (reset! mode ::show))))]))


(rum/defc max-score [score]
  [:div
   (editable "number" "Maximum score: " score
             #(put! events [:new-max-score (js/parseInt %)]))
   [:button.btn.btn-link.btn-xs.reset-scores
    {:on-click #(put! events [:reset-scores])}
    [:span.glyphicon.glyphicon-repeat {:aria-hidden true}]]])

(rum/defcs player-score < (rum/local 0) [state name max-score score]
  (let [s (:rum/local state)]
    [:div {:class "row"}
     [:div {:class "col-sm-8"}
      [:div {:class "progress"}
       [:div {:class "progress-bar"
              :role "progressbar"
              :aria-valuenow score
              :aria-valuemin "0"
              :aria-valuemax max-score
              :style {:width (str (* (/ score max-score) 100) "%")}}
        score]]]
     [:div {:class "col-sm-2"}
      [:input.form-control
       {:type "number"
        :value @s
        :on-change #(reset! s (js/parseInt (.. % -target -value)))}]]
     [:div {:class "col-sm-2"}
      [:button
       {:class "btn btn-success btn-lg btn-block"
        :on-click (fn [_]
                    (put! events [:inc-score name @s])
                    (reset! s 0))}
       [:i {:class "glyphicon glyphicon-chevron-up"}]]]]))

(rum/defc player [max-score name score]
  [:div.player
   (editable "type" "" name
             #(put! events [:new-player-name name %]))
   [:button.btn.btn-link.btn-xs.remove-player
    {:on-click #(put! events [:remove-player name])}
    [:span.glyphicon.glyphicon-remove-sign {:aria-hidden true}]]
   (player-score name max-score score)])

(rum/defc scoreboard < rum/reactive [data]
  [:div.scoreboard.container-fluid
   (max-score (:max-score (rum/react data)))
   [:hr]
   (for [[name score] (:players (rum/react data))]
     (rum/with-props player
       (:max-score (rum/react data)) name score :rum/key [name]))
   [:button.btn.btn-info
    {:on-click #(put! events [:add-new-player])}
    "Add new player"]])


(defn update-player [data predicate updater]
  (update-in data [:players]
             #(->> %
                   (map
                    (fn [player]
                      (if (predicate player)
                        (updater player)
                        player)))
                   (filterv (complement nil?)))))

(defn same-name? [name] (fn [[n _]] (= n name)))

(defn new-name [names]
  (loop [idx (count names)]
    (let [name (str "Player " idx)]
      (if (names name)
        (recur (inc idx))
        name))))

(defn start-event-listener [events-chan data]
  (go-loop []
    (let [[type & args :as event] (<! events-chan)]
      (case type
        :new-max-score
        (let [new-max-score (first args)]
          (when (and new-max-score (> new-max-score 0))
            (swap! data assoc :max-score new-max-score)))
        :new-player-name
        (let [[old new] args]
          (when (and old new)
            (swap! data update-player
                   (same-name? old)
                   (fn [[_ score]] [new score]))))
        :inc-score
        (let [[player score-addition] args]
          (when (and player score-addition (> score-addition 0))
            (swap! data update-player
                   (same-name? player)
                   (fn [[name score]] [name (+ score score-addition)]))))
        :reset-scores
        (swap! data update-in [:players] (partial mapv (fn [[p s]] [p 0])))
        :add-new-player
        (let [names (into #{} (map first (:players @data)))
              name (new-name names)]
          (swap! data update-in [:players] conj [name 0]))
        :remove-player
        (let [[player] args]
          (swap! data update-player (same-name? player) (constantly nil)))
        (.warn js/console (str "Don't know how to handle event: "
                               event)))
      (recur))))


(defn render []
  (if-let [root (.getElementById js/document "app")]
    (rum/mount (scoreboard data) root)
    (.error js/console "Root element wasn't found")))

(defn reload []
  (start-event-listener events data)
  (render))

(defn start []
  (reset! data initial-data)
  (start-event-listener events data)
  (render))
