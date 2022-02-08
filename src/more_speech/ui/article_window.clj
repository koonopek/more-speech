(ns more-speech.ui.article-window
  (:require
    [more-speech.ui.cursor :as cursor]
    [more-speech.content.article :as a]
    [more-speech.ui.widget :refer [widget]]
    [more-speech.ui.button :refer [map->button
                                   up-arrow
                                   down-arrow]]
    [more-speech.ui.graphics :as g]
    [more-speech.nostr.util :refer [num->hex-string]]))

(declare draw-article-window
         scroll-up
         scroll-down)

(defrecord article-window [x y w h page-up page-down display-position]
  widget
  (setup-widget [widget state]
    (let [scroll-up (partial scroll-up (:path widget))
          scroll-down (partial scroll-down (:path widget))]
      (assoc widget :display-position 0
                    :page-up (map->button {:x (+ x 20) :y (+ y h -30) :h 20 :w 20
                                           :left-down scroll-up
                                           :left-held scroll-up
                                           :draw up-arrow})
                    :page-down (map->button {:x (+ x w -20) :y (+ y h -30) :h 20 :w 20
                                             :left-down scroll-down
                                             :left-held scroll-down
                                             :draw down-arrow})
                    )))
  (update-widget [widget state]
    state)
  (draw-widget [widget state]
    (draw-article-window state widget))
  )

(defn- scroll-up [widget-path button state]
  (let [article-window (get-in state widget-path)
        articles (get-in state [:application :chronological-text-events])
        display-position (:display-position article-window)
        display-position (min (count articles) (inc display-position))
        article-window (assoc article-window :display-position display-position)
        state (assoc-in state widget-path article-window)]
    state))

(defn- scroll-down [widget-path button state]
  (let [article-window (get-in state widget-path)
        display-position (:display-position article-window)
        display-position (max 0 (dec display-position))
        article-window (assoc article-window :display-position display-position)
        state (assoc-in state widget-path article-window)]
    state))

(defn- open-button [cursor]
  (cursor/draw-text cursor "+"))

(defn- null-button [cursor]
  (cursor/draw-text cursor " "))

(defn draw-article [window cursor article]
  (let [g (:graphics cursor)]
    (g/text-align g [:left])
    (g/fill g [0 0 0])
    (cursor/render cursor window (a/markup-article article)
                   {:open-button open-button
                    :null-button null-button}))
  )

(defn draw-articles [state window]
  (let [application (:application state)
        g (:graphics application)
        nicknames (:nicknames application)
        article-map (:text-event-map application)
        articles (:chronological-text-events application)
        display-position (:display-position window)
        articles (drop display-position articles)
        articles (take 19 articles)]
    (loop [cursor (cursor/->cursor g 0 (g/line-height g) 5)
           articles articles]
      (if (empty? articles)
        cursor
        (let [article-id (first articles)
              text-event (get article-map article-id)
              {:keys [pubkey created-at content references]} text-event
              name (get nicknames pubkey (num->hex-string pubkey))
              ref-count (count references)
              article (a/make-article name created-at content ref-count)]
          (recur (draw-article window cursor article)
                 (rest articles)))))))

(defn draw-article-window [state window]
  (let [application (:application state)
        g (:graphics application)]
    (g/with-translation
      g [(:x window) (:y window)]
      (fn [g]
        (g/stroke g [0 0 0])
        (g/stroke-weight g 2)
        (g/fill g [255 255 255])
        (g/rect g [0 0 (:w window) (:h window)])
        (draw-articles state window))
      )))
