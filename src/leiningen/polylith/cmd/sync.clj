(ns leiningen.polylith.cmd.sync
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [leiningen.polylith.cmd.shared :as shared]
            [leiningen.polylith.cmd.deps :as deps]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.add :as add]
            [leiningen.polylith.cmd.sync.environments :as environments]
            [leiningen.polylith.cmd.sync.interfaces :as interfaces]
            [leiningen.polylith.cmd.sync.shared :as shared-env]))

(defn updated-entity-lib [entity-libs dev-lib]
  (if-let [index (shared-env/index-of-lib entity-libs dev-lib)]
    (assoc entity-libs index dev-lib)
    entity-libs))

(defn updated-entity-libs [entity-libs dev-libs]
  (vec (sort-by first (reduce updated-entity-lib entity-libs dev-libs))))

(defn updated-content [project-path updated-libs]
  (let [content (vec (first (file/read-file project-path)))
        index (inc (shared-env/deps-index content))]
    (seq (assoc content index updated-libs))))

(defn sync-entities! [ws-path dev-project-path entities-name entities]
  (let [dev-libs (shared/libraries dev-project-path)]
    (doseq [entity entities]
      (let [project-path (str entities-name "/" entity "/project.clj")
            full-project-path (str ws-path "/" project-path)
            entity-libs (shared/libraries full-project-path)
            updated-libs (updated-entity-libs entity-libs dev-libs)]
        (when-not (= entity-libs updated-libs)
          (println (str "updated: " project-path))
          (file/write-to-file full-project-path project-path
                              (updated-content full-project-path updated-libs)))))))

(defn updated-system-content [libs project-path]
  (let [content (vec (first (file/read-file project-path)))
        index (inc (shared-env/deps-index content))]
    (seq (assoc content index libs))))

(defn update-systems-libs! [ws-path top-dir]
  (let [components (shared/all-components ws-path)]
    (doseq [system (shared/all-systems ws-path)]
      (let [system-path (str ws-path "/systems/" system)
            project-path (str system-path "/project.clj")
            path (str "systems/" system "/project.clj")
            src-path (str system-path "/src/" top-dir)
            entities (file/directory-names src-path)
            libs (environments/entities-libs ws-path [] entities components)
            sys-libs (sort-by first (shared/libraries project-path))
            content (seq (updated-system-content libs project-path))]
        (when (not= libs sys-libs)
          (println (str "updated: " path))
          (file/write-to-file (str ws-path "/" path) path content))))))

(defn ifc-components [ws-path top-dir all-interfaces all-components interface]
  (filterv #(= interface (shared/interface-of ws-path top-dir % all-interfaces)) all-components))

(defn add-component [ws-path top-dir system component]
  (add/add-component-to-system ws-path top-dir component system)
  (println (str "Added component '" component "' to system '" system "'.")))

(defn missing-components [system components interface]
  (println (str "Missing component in system '" system "' for interface '" interface "'. Suggested components: " (str/join ", " (sort components)) ".")))

(defn add-missing-components-to-system! [ws-path top-dir system]
  (let [system-path (str ws-path "/systems/" system)
        src-path (str system-path "/src/" top-dir)
        entities (set (file/directory-names src-path))
        all-bases (shared/all-bases ws-path)
        all-components (shared/all-components ws-path)
        all-interfaces (shared/all-interfaces ws-path top-dir)
        ifc-deps (deps/interface-dependencies ws-path top-dir all-components all-bases)
        components (filter all-components entities)
        entity-interfaces (set (map str (flatten (map ifc-deps entities))))
        system-interfaces (set (map #(shared/interface-of ws-path top-dir %) components))
        missing-interfaces (set/difference entity-interfaces system-interfaces)
        missing-component-lists (mapv #(vector % (ifc-components ws-path top-dir all-interfaces all-components %)) missing-interfaces)]

    (doseq [[interface components] missing-component-lists]
      (let [cnt (count components)]
        (cond
          (= cnt 0) nil
          (= cnt 1) (add-component ws-path top-dir system (first components))
          (> cnt 1) (missing-components system components interface))))

    (empty? (filter #(> (-> % second count) 1)
                    missing-component-lists))))

(defn add-missing-components-to-systems! [ws-path top-dir]
  (every? true? (map #(add-missing-components-to-system! ws-path top-dir %)
                     (shared/all-systems ws-path))))

(defn do-sync [ws-path top-dir project-path dev-project-path]
  (environments/update-environments ws-path top-dir dev-project-path)
  (sync-entities! ws-path project-path "components" (shared/all-components ws-path))
  (sync-entities! ws-path project-path "bases" (shared/all-bases ws-path))
  (update-systems-libs! ws-path top-dir)
  (every? true?
    [(interfaces/sync-interfaces! ws-path top-dir)
     (add-missing-components-to-systems! ws-path top-dir)]))

(defn validate [arg]
  (condp = arg
    nil [true]
    "+deps" [true]
    [false (str "Invalid argument '" arg "'. Valid arguments are '+deps' or no arguments.")]))

(defn execute [ws-path top-dir [arg]]
  (let [[ok? message] (validate arg)
        dev-project-path "environments/development/project.clj"
        project-path (str ws-path "/" dev-project-path)]
    (if ok?
      (do-sync ws-path top-dir project-path dev-project-path)
      (do
        (println message)
        false))))

(defn sync-all [ws-path top-dir action]
  (or (execute ws-path top-dir [])
      (throw (Exception. ^String (str "Cannot " action ".")))))
