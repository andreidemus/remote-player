(ns remote.core.handler
  (:use [clojure.java.shell :only [sh]]
        [clojure.java.io :only [file]])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(def movies "/Users/andrei/Movies/")
(def fifo "/tmp/fifofile2")

(defn cmd [& cs]
  (let [c (str "echo " (apply str (interpose " " cs)) " > " fifo)]
    (log/debug c)
    (sh "sh" "-c" c)))

(defn file-to-map [file]
  {:name (.getName file)
   :is-directory (.isDirectory file)
   :hash (hash file)})

(defn get-movies [path]
  (let [files (.listFiles (clojure.java.io/file (str movies path)))
        names (map #(.getName %) files)
        ids (map #(str (hash %)) files)]
  (zipmap ids names)))

(defn resp [r]
  (json/write-str r))

(defn ok []
  (resp {:status "ok"}))

(defn pause []
  (cmd "pause")
  (ok))

(defn play [id]
  (let [movie (get (get-movies "") id)
        c (str "mplayer -quiet -slave -input file=" fifo " " movies movie)] ;; -fs
    (log/debug c)
    (future (sh "sh" "-c" c))
    (ok)))

(defn stop []
  (cmd "quit")
  (ok))

(defn volume-up []
  (cmd "volume +10")
  (ok))

(defn volume-down []
  (cmd "volume -10")
  (ok))

(defn mute []
  (cmd "mute")
  (ok))

(defn forward []
  (cmd "seek +10 0")
  (ok))

(defn backward []
  (cmd "seek -10 0")
  (ok))

(defn f-forward []
  (cmd "seek +150 0")
  (ok))

(defn f-backward []
  (cmd "seek -150 0")
  (ok))

(defn playlist []
  (resp (get-movies "")))

(defroutes app-routes
  (GET "/" [] "Remote")
  (GET "/play/:id" [id] (play id))
  (GET "/playlist" [id] (playlist))
  (GET "/pause" [] (pause))
  (GET "/stop" [] (stop))
  (GET "/volume-up" [] (volume-up))
  (GET "/volume-down" [] (volume-down))
  (GET "/mute" [] (mute))
  (GET "/forward" [] (forward))
  (GET "/backward" [] (backward))
  (GET "/f-forward" [] (f-forward))
  (GET "/f-backward" [] (f-backward))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
