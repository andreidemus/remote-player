(ns remote.core.handler
  (:use [clojure.java.shell :only [sh]]
        [clojure.java.io :only [file]])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(def root "/Users/andrei/Movies")
(def fifo "/tmp/remote_fifo")

(defn resp [r]
  (json/write-str r))

(defn ok []
  (resp {:status "ok"}))

(defn path-by-id[id]
  (let [fs (file-seq (file root))]
    (when-let [f (some #(when (= (hash %) id) %) fs)]
      (.getPath f))))

(defn cmd [& cs]
  (let [c (str "echo " (apply str (interpose " " cs)) " > " fifo)]
    (log/debug c)
    (future (sh "sh" "-c" c)))
  (ok))

(defn file-to-map [file]
  {:name (.getName file)
   :is-directory (.isDirectory file)
   :hash (hash file)})

(defn get-parent [path]
  (let [parent (.getParentFile (file path))]
    {(str (hash parent))
     {:name ".."
      :is-directory true}}))

(defn get-movies [path]
  (let [files (.listFiles (file path))
        names (map #(hash-map :name (.getName %) :is-directory (.isDirectory %)) files)
        ids (map #(str (hash %)) files)
        movies (zipmap ids names)]
    (if (= path root)
      movies
      (merge (get-parent path) movies))))

(defn play [id]
  (let [movie (path-by-id id)
        c (str "rm -f " fifo
            " && mkfifo " fifo
            " && mplayer -quiet -slave -input file=" fifo " '" movie "'")] ;; -fs
    (log/debug c)
    (future (log/debug (sh "sh" "-c" c)))
    (ok)))

(defn playlist []
  (resp (get-movies root)))

(defn dir-playlist [id]
  (resp (get-movies (path-by-id id))))

(defroutes app-routes
  (GET "/" [] (slurp "resources/index.html"))
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
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
