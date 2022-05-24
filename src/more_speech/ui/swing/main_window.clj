(ns more-speech.ui.swing.main-window
  (:require [clojure.core.async :as async]
            [more-speech.nostr.events :as events]
            [more-speech.ui.swing.article-tree :as article-tree]
            [more-speech.ui.swing.article-panel :as article-panel]
            [more-speech.ui.swing.relay-panel :as relay-panel]
            [more-speech.ui.swing.ui-context :refer :all])
  (:use [seesaw core])
  (:import (javax.swing Timer)))

(defrecord seesawHandler []
  events/event-handler
  (handle-text-event [_handler event]
    (invoke-later (article-tree/add-event ui-context event)))
  (update-relay-panel [_handler]
    (invoke-later (relay-panel/update-relay-panel ui-context))))

(declare make-main-window)

(defn setup-main-window [event-agent]
  (invoke-now (make-main-window event-agent))
  (->seesawHandler))

(declare timer-action)

(defn make-tabs [event-agent main-frame]
  (let [tabs (:tabs @event-agent)]
    (loop [tab-names (keys tabs)
           header-tree-tabs []]
      (if (empty? tab-names)
        header-tree-tabs
        (let [tab-name (first tab-names)
              header-tree (article-tree/make-header-tree event-agent main-frame)
              _ (config! header-tree
                         :user-data (get tabs tab-name)
                         :id tab-name)
              tab-data {:title (name tab-name)
                        :content (scrollable header-tree)}]
          (recur (rest tab-names) (conj header-tree-tabs tab-data)))
        )))
  )
(defn make-main-window [event-agent]
  (let [main-frame (frame :title "More Speech" :size [1000 :by 1000])
        article-area (article-panel/make-article-area)
        header-tab-panel (tabbed-panel :tabs (make-tabs event-agent main-frame))
        relay-panel (relay-panel/make-relay-panel)
        header-panel (left-right-split (scrollable relay-panel)
                                       header-tab-panel)
        article-panel (border-panel :north (article-panel/make-article-info-panel)
                                    :center (scrollable article-area)
                                    :south (article-panel/make-control-panel event-agent))
        main-panel (top-bottom-split
                     header-panel
                     article-panel
                     :divider-location 1/2)
        timer (Timer. 100 nil)]
    (config! main-frame :content main-panel)
    (swap! ui-context assoc :frame main-frame :event-agent event-agent)

    (listen timer :action timer-action)

    (listen main-frame :window-closing
            (fn [_]
              (.stop timer)
              (async/>!! (:send-chan @event-agent)
                         [:closed])
              (.dispose main-frame)))

    (show! main-frame)
    (.start timer)))

(defn timer-action [_]
  ;nothing for now.
  )


