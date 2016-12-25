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
    (log/debug c)
    (future (sh "sh" "-c" c)))
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

(defn get-movies [path]
  (let [files (.listFiles (file path) (reify
                                        java.io.FileFilter
                                        (accept [this f]
                                          (not (.isHidden f)))))
        movies (map file-to-map files)]
    (if (= path @root)
      movies
      (cons (get-parent path) movies))))

(defn play [id]
  (cmd "quit")
  (let [movie (path-by-id id)
        c (str "rm -f " fifo
            " && mkfifo " fifo
            " && " (:mplayer-cmd config) " -slave -input file=" fifo " '" movie "'")]
    (log/debug c)
    (future (log/debug (sh "sh" "-c" c)))
    (ok)))

(defn playlist []
  (resp (get-movies @root)))

(defn dir-playlist [id]
  (resp (get-movies (path-by-id id))))

(defroutes app-routes
  (GET "/" [] (io/resource "index.html"))
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

(defn -main []
  (jetty/run-jetty #'app {:port (:port config)
                          :join? false}))
