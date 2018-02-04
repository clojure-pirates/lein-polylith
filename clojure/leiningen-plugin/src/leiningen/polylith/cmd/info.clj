(ns leiningen.polylith.cmd.info
  (:require [clojure.string :as str]
            [clojure.java.shell :as shell]
            [leiningen.polylith.cmd.diff :as diff]
            [leiningen.polylith.file :as file]))

(defn changed-dirs [dir file-paths]
  (let [f #(and (str/starts-with? % (str dir "/"))
                (> (count (str/split % #"/")) 2))]
    (vec (sort (set (map #(second (str/split % #"/"))
                         (filter f file-paths)))))))

(defn changed-base? [ws-path path changed-bases]
  (let [bases-path (str ws-path "/bases")
        base? (str/starts-with? path bases-path)
        changed? (and
                   base?
                   (let [base (second (str/split (subs path (count bases-path)) #"/"))]
                     (contains? (set changed-bases) base)))]
    {:base?  base?
     :changed? changed?}))

(defn changed-component? [ws-path path changed-components]
  (let [components-path (str ws-path "/components")
        component? (str/starts-with? path components-path)
        changed? (and
                   component?
                   (let [component (second (str/split (subs path (count components-path)) #"/"))]
                     (contains? (set changed-components) component)))]
    {:component? component?
     :changed?   changed?}))

(defn changed? [ws-path file changed-bases changed-components]
  (let [path (file/file-path->real-path file)
        changed-base (changed-base? ws-path path changed-bases)
        changed-component (changed-component? ws-path path changed-components)]
    {:name     (file/path->dir-name path)
     :type     (cond
                 (:base? changed-base) "-> base"
                 (:component? changed-component) "-> component"
                 :else "?")
     :changed? (cond
                 (:base? changed-base) (:changed? changed-base)
                 (:component? changed-component) (:changed? changed-component)
                 :else false)}))


(defn build-links [ws-path top-dir system changed-bases changed-components]
  (let [dir (if (zero? (count top-dir)) "/src" (str "/src/" top-dir))]
    (mapv #(changed? ws-path % changed-bases changed-components)
          (file/directories (str ws-path "/builds/" system dir)))))

(defn build-info [ws-path top-dir builds changed-bases changed-components]
  (into {} (mapv (juxt identity #(build-links ws-path top-dir % changed-bases changed-components)) builds)))

(defn any-changes? [builds-info system]
  (or (some true? (map :changed? (builds-info system))) false))

(defn base-or-component-changed? [builds-info changed-builds]
  (let [base-changes (map (juxt identity #(any-changes? builds-info %)) (keys builds-info))]
    (map (juxt first #(or (last %) (contains? changed-builds (first %)))) base-changes)))

(defn all-interfaces [ws-path top-dir]
  (let [dir (if (zero? (count top-dir))
              "/interfaces/src"
              (str "/interfaces/src/" top-dir))]
    (set (file/directory-names (str ws-path dir)))))

(defn all-builds [ws-path]
  (file/directory-names (str ws-path "/builds")))

(defn all-components [ws-path]
  (set (file/directory-names (str ws-path "/components"))))

(defn all-bases [ws-path]
  (set (file/directory-names (str ws-path "/bases"))))

(defn all-changed-build-dirs
  ([paths bases]
   (set (filter bases (changed-dirs "builds" paths)))))

(defn changed-interfaces
  ([ws-path paths top-dir]
   (changed-interfaces ws-path paths (all-interfaces ws-path top-dir) top-dir))
  ([ws-path paths interfaces top-dir]
   (set (filter interfaces (set (changed-dirs "interfaces" paths))))))

(defn changed-components
  ([ws-path paths]
   (changed-components ws-path paths (all-components ws-path)))
  ([ws-path paths components]
   (set (filter components (changed-dirs "components" paths)))))

(defn changed-bases
  ([ws-path paths]
   (changed-bases ws-path paths (all-bases ws-path)))
  ([ws-path paths bases]
   (set (filter bases (set (changed-dirs "bases" paths))))))

(defn changed-builds
  ([ws-path paths top-dir bases]
   (changed-builds (build-info ws-path
                               top-dir
                               (all-builds ws-path)
                               (changed-bases ws-path paths)
                               (changed-components ws-path paths))
                   (all-changed-build-dirs paths bases)))
  ([builds-info changed-build-dirs]
   (mapv first (filter second (base-or-component-changed? builds-info (set changed-build-dirs))))))

(defn info
  ([ws-path top-dir]
   (info ws-path top-dir []))
  ([ws-path top-dir last-success-sha1 current-sha1]
   (info ws-path top-dir (diff/diff ws-path last-success-sha1 current-sha1)))
  ([ws-path top-dir paths]
   (let [interfaces (all-interfaces ws-path top-dir)
         builds (all-builds ws-path)
         components (all-components ws-path)
         bases (all-bases ws-path)
         ch-interfaces (changed-interfaces ws-path paths interfaces)
         ch-builds (changed-builds ws-path paths top-dir bases)
         ch-components (changed-components ws-path paths components)
         ch-bases (changed-bases ws-path paths bases)]
     {:interfaces         (-> interfaces sort vec)
      :builds             (-> builds sort vec)
      :components         (-> components sort vec)
      :bases              (-> bases sort vec)
      :diff               paths
      :changed-interfaces ch-interfaces
      :changed-builds     ch-builds
      :changed-components ch-components
      :changed-bases      ch-bases
      :changed-builds-dir (all-changed-build-dirs paths bases)
      :builds-info        (build-info ws-path top-dir builds ch-bases ch-components)})))

(defn print-entity
  ([spaces entity changes show-changed? show-unchanged?]
   (let [changed? (contains? changes entity)
         star (if (and show-changed? changed?) " *" "")]
     (print-entity (str spaces entity star) changed? show-changed? show-unchanged?)))
  ([spaces entity type maxlength changed? show-changed? show-unchanged?]
   (let [star (if (and show-changed? changed?) " *" "")
         star-spaces (str/join (repeat (- maxlength (count (str entity star))) " "))
         string (str spaces entity star star-spaces type)]
     (print-entity string changed? show-changed?  show-unchanged?)))
  ([string changed? show-changed? show-unchanged?]
   (if (or (and changed? show-changed?)
           (and (not changed?) show-unchanged?))
     (println string))))

(defn print-info [{:keys [interfaces
                          components
                          bases
                          changed-interfaces
                          changed-bases
                          changed-components
                          changed-builds-dir
                          builds-info]}
                  show-changed?
                  show-unchanged?
                  show-interfaces?]
  (let [builds (keys builds-info)
        name-counts (map #(+ 3 (count (:name %)) (if (:changed? %) 2 0))
                         (filter #(or show-unchanged? (:changed? %))
                                 (mapcat second builds-info)))
        maxlength (if (empty? name-counts) 150 (apply max name-counts))]

    (when (or show-interfaces?
              (and show-unchanged? (not show-changed?))
              (and show-changed? (not show-unchanged?))
              (-> changed-interfaces empty? not))
      (println "interfaces:")
      (doseq [interface interfaces]
        (print-entity "  " interface changed-interfaces show-changed? show-unchanged?)))

    (println "components:")
    (doseq [component components]
      (print-entity "  " component changed-components show-changed? show-unchanged?))

    (println "bases:")
    (doseq [base bases]
      (print-entity "  " base changed-bases show-changed? show-unchanged?))

    (println "builds:")
    (doseq [build builds]
      (let [infos (sort-by :name
                           (filter #(or (and (:changed? %) show-changed?)
                                        (and (not (:changed? %)) show-unchanged?))
                                   (builds-info build)))]
        (when (or (-> infos empty? not)
                  (contains? changed-builds-dir build))
          (if show-changed?
            (print-entity "  " build changed-builds-dir true true)
            (println " " build)))
        (doseq [{:keys [name type changed?]} infos]
          (print-entity "    " name type maxlength changed? show-changed? show-unchanged?))))))

(defn execute [ws-path top-dir args]
  (let [cmd (first args)
        a? (= "a" cmd)
        filter? (= 1 (count cmd))
        [show-changed?
         show-unchanged?
         show-interfaces?] (if filter?
                             [(or a? (= "c" cmd))
                              (or a? (= "u" cmd))
                              a?]
                             [true true false])
        [last-success-sha1
         current-sha1] (if filter? (rest args) args)
        data (if (and last-success-sha1 current-sha1)
               (info ws-path top-dir last-success-sha1 current-sha1)
               (info ws-path top-dir))]
    (print-info data show-changed? show-unchanged? show-interfaces?)))

;(execute "/Users/joakimtengstrand/IdeaProjects/clojure-ring-mongodb-starter"
;         "com/furkanbayraktar"
;         ["a" "2f0d00fa0f3de5274aef084d4d830bc2587ffed" "6e1fadab3bbc8f5cd3dd820bf7f82e471a3cffc"])
