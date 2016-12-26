(ns remote.core.handler
  (:use [clojure.java.shell :only [sh]]
        [clojure.java.io :only [file]])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [clojure.java.io :as io])
  (:gen-class))

(load-file "config.clj")
(def fifo "/tmp/remote_fifo")
(def root (atom (:path-to-media config)))
(def queue (atom nil))

(defn resp [r]
  (json/write-str r))

(defn ok []
  (resp {:status "ok"}))

(defn path-by-id [id]
  (let [fs (file-seq (file @root))]
    (when-let [f (some #(when (= (hash %) id) %) fs)]
      (.getPath f))))

(defn cmd [& cs]
  (let [c (str "echo " (apply str (interpose " " cs)) " > " fifo)]
    (future (sh "sh" "-c" c))
    (log/info c))
  (ok))

(defn file-to-map [file]
  {:id (hash file)
   :name (.getName file)
   :is-directory (.isDirectory file)})

(defn get-parent [path]
  (let [parent (.getParentFile (file path))]
    {:id (hash parent)
     :name "[...]"
     :is-directory true
     :is-parent true}))

(defn get-medias [path]
  (let [files (.listFiles (file path) (reify
                                        java.io.FileFilter
                                        (accept [this f]
                                          (not (.isHidden f)))))
        medias (map file-to-map files)]
    (if (= path @root)
      medias
      (cons (get-parent path) medias))))

(defn get-queue
  []
  (->> @root
       get-medias
       (map :id)
       shuffle))

(defn create-queue
  []
  (reset! queue (get-queue)))

(defn play
  [id]
  (cmd "quit")
  (let [media (path-by-id id)
        c (str "rm -f " fifo
               " && mkfifo " fifo
               " && " (:mplayer-cmd config) " -slave -input file=" fifo " '" media "'")]
    (log/debug c)
    (future (log/debug (sh "sh" "-c" c)))
    (ok)))

(defn play-item
  [id]
  (cmd "quit")
  (let [media (path-by-id id)
        c (str "rm -f " fifo
               " && mkfifo " fifo
               " && " (:mplayer-cmd config) " -slave -input file=" fifo " '" media "'")]
    (log/info c)
    (log/info (sh "sh" "-c" c))
    (log/info (str "item " id "played"))))

(defn play-queue
  []
  (log/info "play loop")
  (when-let [item (peek @queue)]
    (swap! queue pop)
    (play-item item)
    (recur)))

(defn playlist []
  (resp (get-medias @root)))

(defn handle-play-all
  []
  (log/info "Play all")
  (future (play-queue))                  
  (ok))

(defn dir-playlist [id]
  (resp (get-medias (path-by-id id))))

(defroutes app-routes
  (GET "/" [] (io/resource "index.html"))
  (GET "/play-all" [] (handle-play-all))
  (GET "/play/:id" [id] (play (read-string id)))
  (GET "/playlist" [] (playlist))
  (GET "/playlist/:id" [id] (dir-playlist (read-string id)))
  (GET "/pause" [] (cmd "pause"))
  (GET "/stop" [] (cmd "quit"))
  (GET "/volume-up" [] (cmd "volume +10"))
  (GET "/volume-down" [] (cmd "volume -10"))
  (GET "/mute" [] (cmd "mute"))
  (GET "/forward" [] (cmd "seek +10 0"))
  (GET "/backward" [] (cmd "seek -10 0"))
  (GET "/f-forward" [] (cmd "seek +150 0"))
  (GET "/f-backward" [] (cmd "seek -150 0"))
  (GET "/switch-subs" [] (cmd "sub_visibility"))
  (GET "/config" [] (resp config))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

(defn start
  []
  (create-queue)
  (jetty/run-jetty #'app {:port (:port config)
                          :join? false}))
(defn -main []
  (start))
