(ns leiningen.polylith.cmd.doc
  (:require [leiningen.polylith.cmd.deps :as cdeps]
            [leiningen.polylith.cmd.shared :as shared]
            [clojure.set :as set]
            [leiningen.polylith.file :as file]
            [clojure.java.browse :as browse]
            [leiningen.polylith.cmd.info :as info]
            [leiningen.polylith.freemarker :as freemarker]
            [clojure.string :as str]))

(defn dependencies [ws-path top-dir system]
  (let [used-entities (shared/used-entities ws-path top-dir system)
        used-components (set/intersection used-entities (shared/all-components ws-path))
        used-bases (set/intersection used-entities (shared/all-bases ws-path))]
    (cdeps/component-dependencies ws-path top-dir used-components used-bases)))

(defn entity-type [entity all-bases]
  (cond
    (contains? all-bases entity) "base"
    :else "component"))

(defn dependency-tree [entity deps all-bases]
  {:entity entity
   :type (entity-type entity all-bases)
   :children (mapv #(dependency-tree % deps all-bases) (deps entity))})

(defn count-cols [{:keys [_ _ children]}]
  (cond
    (empty? children) 1
    :else (apply + (map count-cols children))))

(defn count-columns [tree]
  (let [sections (count-cols tree)]
    (if (zero? sections)
      0
      (dec (* 2 sections)))))

(defn max-deps [{:keys [_ _ children]} depth]
  (if (empty? children)
    depth
    (apply max (map #(max-deps % (inc depth)) children))))


(def component-table [[{"entity" "email", "type" "interface", "columns" 1}]
                      [{"entity" "address", "type" "component", "columns" 1}]])

(def system-table [[{"entity" "util", "type" "component", "columns" 1}
                    {"type" "spc"}
                    {"entity" "util", "type" "component", "columns" 1}
                    {"type" "spc"}
                    {"entity" "", "type" "component", "columns" 1}
                    {"type" "spc"}
                    {"entity" "", "type" "component", "columns" 1}
                    {"type" "spc"}
                    {"entity" "", "type" "component", "columns" 1}]
                   [{"entity" "email", "type" "component", "columns" 1}
                    {"type" "spc"}
                    {"entity" "email", "type" "component", "columns" 1}
                    {"type" "spc"}
                    {"entity" "util", "type" "component", "columns" 1}
                    {"type" "spc"}
                    {"entity" "", "type" "component", "columns" 1}
                    {"type" "spc"} {"entity" "", "type" "component", "columns" 1}]
                   [{"entity" "address", "type" "component", "columns" 1}
                    {"type" "spc"}
                    {"entity" "user", "type" "component", "columns" 3}
                    {"type" "spc"}
                    {"entity" "util", "type" "component", "columns" 1}
                    {"type" "spc"}
                    {"entity" "volvo", "type" "component", "columns" 1}]
                   [{"entity" "rest&#8209;api", "type" "base", "columns" 9}]])

(def row [{"entity" "util", "type" "component", "columns" 1}
          {"type" "spc"}
          {"entity" "util", "type" "component", "columns" 1}
          {"type" "spc"}
          {"entity" "", "type" "component", "columns" 1}
          {"type" "spc"}
          {"entity" "", "type" "component", "columns" 1}
          {"type" "spc"}
          {"entity" "", "type" "component", "columns" 1}])

(def col {"entity" "util", "type" "component", "columns" 1})

;(def ws-path "/Users/joakimtengstrand/IdeaProjects/ws01")
;(def top-dir "")
;(def all-interfaces (shared/all-interfaces ws-path top-dir))


(defn +ifc [ws-path top-dir interfaces col]
  (let [entity (col "entity")]
    (condp = (col "type")
      "component" [(assoc col "entity" (shared/interface-of ws-path top-dir entity interfaces)
                              "type" "interface"
                              "top" true)]
      "spc" [col]
      [])))

(defn +ifc-row [ws-path top-dir all-interfaces row]
  (mapcat #(+ifc ws-path top-dir all-interfaces %) row))

(defn +ifc-table [ws-path top-dir all-interfaces table]
  (mapv #(+ifc-row ws-path top-dir all-interfaces %) table))


;(+ifc ws-path top-dir all-interfaces)
;(+ifc-row ws-path top-dir all-interfaces row)


;(def res
;  (mapv
;    vector
;    system-table
;    (+ifc-table ws-path top-dir all-interfaces system-table)))

;(defn reduce-table [result [v1 v2]]
;  (conj (conj result v1) v2))


;(+ifc-table ws-path top-dir all-interfaces system-table)


(defn calc-table
  ([ws-path top-dir tree]
   (let [maxy (max-deps tree 1)
         result (transient (vec (repeat maxy [])))
         _ (calc-table tree 0 maxy result)
         table (vec (reverse (persistent! result)))
         interfaces (shared/all-interfaces ws-path top-dir)

         result (+ifc-table ws-path top-dir interfaces
                            (mapv #(interpose {:type "spc"} %) table))]
     result))
  ([{:keys [entity type bottom children] :as tree} y maxy result]
   (assoc! result y (conj (get result y) {:entity entity
                                          :type type
                                          :bottom bottom
                                          :columns (count-columns tree)}))
   (if (empty? children)
     (doseq [yy (range (inc y) maxy)]
       (assoc! result yy (conj (get result yy) {:entity ""
                                                :type "component"
                                                :columns 1})))
     (doseq [child children]
       (calc-table child (inc y) maxy result)))))

(defn base-name [ws-path top-dir type-dir environment]
  (let [dir (shared/full-name top-dir "/" "")
        bases (shared/all-bases ws-path)
        directories (file/directories (str ws-path type-dir environment "/src/" dir))]
    (first (filterv #(contains? bases %) (map shared/path->file directories)))))

(defn entity-usages
  ([tree]
   (into {} (map (juxt first #(-> % second first second))
                 (group-by first (sort (entity-usages 0 [0 [] tree]))))))
  ([x [y result {:keys [entity children]}]]
   (conj (apply concat
                (map-indexed #(entity-usages % [(inc y)
                                                result
                                                %2])
                             children))
         (conj [entity [y x]]))))

(defn crop-branches [x [y {:keys [entity type children]} usages result]]
  (if (= [y x] (usages entity))
    (assoc result :entity entity
                  :type type
                  :children (vec (map-indexed #(crop-branches % [(inc y) %2 usages result]) children)))
    (assoc result :entity entity
                  :type type
                  :children [])))

(defn entity-deps [{:keys [entity _ children]} result]
  (concat (reduce concat (map #(entity-deps % result) children))
          (conj result entity)))

(defn unused->component [ws-path top-dir component]
  {"name" component
   "interface" (shared/interface-of ws-path top-dir component)
   "type" "component"})

(defn missing->interface [interface]
  {"name" interface
   "type" "interface"})

(defn deps->names [[_ symbols]]
  (mapv name symbols))

(defn system-info [ws-path top-dir all-bases type-dir system]
  (let [deps (dependencies ws-path top-dir system)
        base (base-name ws-path top-dir type-dir system)]
    (when base
      (let [tree (dependency-tree base deps all-bases)
            usages (entity-usages tree)
            cropped-tree (crop-branches 0 [0 tree usages {}])
            added-entities (set (shared/used-entities ws-path top-dir "systems" system))
            used-entities (set (entity-deps tree []))
            used-components (set/intersection used-entities (shared/all-components ws-path))
            used-interfaces (set (map #(shared/interface-of ws-path top-dir %) used-components))
            used-bases (set/intersection used-entities (shared/all-bases ws-path))
            referenced-interfaces (set (mapcat deps->names (cdeps/interface-dependencies ws-path top-dir used-components used-bases)))
            missing-ifss (set/difference referenced-interfaces used-interfaces)
            unused-entities (set/difference added-entities used-entities)
            table (vec (calc-table ws-path top-dir cropped-tree))
            unused-components (mapv #(unused->component ws-path top-dir %) unused-entities)
            missing-interfaces (mapv missing->interface missing-ifss)]
        {"name" (shared/htmlify system)
         "table" (freemarker/->map table)
         "entities" (vec (concat unused-components missing-interfaces))}))))

(def sorting {"component" 1
              "base" 2})

(defn ->child [interface]
  {:entity (str interface)
   :type "interface"
   :children #{}})

(defn ->entity [entity children all-bases]
  {:entity entity
   :type (entity-type entity all-bases)
   :bottom true
   :children (mapv ->child children)})

(defn entity-ifc-table [ws-path top-dir entity entity-deps all-bases]
  (let [dependencies (map str (entity-deps entity))
        tree (->entity entity dependencies all-bases)]
    (vec (calc-table ws-path top-dir tree))))

(defn ->entity-map [ws-path top-dir all-bases entity-deps entity]
  (let [interface (shared/interface-of ws-path top-dir entity)
        type (if (contains? all-bases entity)
               "base"
               "component")
        table (entity-ifc-table ws-path top-dir entity entity-deps all-bases)]
    {"name" entity
     "type" type
     "interface" interface
     "table" (freemarker/->map table)
     "sort-order" (str (sorting type) entity)}))

(defn base-or-component [bases components entity]
  (or (contains? bases entity)
      (contains? components entity)))

(defn pimped-entities [ws-path top-dir all-bases all-components entities]
  (let [entity-deps (cdeps/interface-dependencies ws-path top-dir all-components all-bases)]
    (sort-by #(% "sort-order")
             (mapv #(->entity-map ws-path top-dir all-bases entity-deps %) entities))))

(defn env-entities [ws-path top-dir environment all-bases all-components]
  (let [dir (str ws-path "/environments/" environment "/src/" (shared/full-name top-dir "/" ""))
        entities (sort (filter #(base-or-component all-bases all-components %)
                               (map file/path->dir-name (file/directories dir))))]
    {"name" (shared/htmlify environment)
     "entities" (pimped-entities ws-path top-dir all-bases all-components entities)}))

(defn environments [ws-path top-dir all-bases all-components]
  (mapv #(env-entities ws-path top-dir % all-bases all-components)
       (sort (shared/all-environments ws-path))))

(defn ->lib [[lib version]]
  {"name" (shared/htmlify lib)
   "version" version})

(defn template-data [ws-path top-dir]
  (let [libraries (map ->lib (sort (filter #(not= 'interfaces (first %))
                                           (shared/all-libraries ws-path))))
        interfaces (shared/all-interfaces ws-path top-dir)
        all-bases (shared/all-bases ws-path)
        all-components (shared/all-components ws-path)
        systems (mapv #(system-info ws-path top-dir all-bases "/systems/" %)
                      (sort (shared/all-systems ws-path)))
        components (pimped-entities ws-path top-dir all-bases all-components all-components)]
    {"workspace"    (shared/htmlify (last (str/split ws-path #"/")))
     "libraries"    libraries
     "interfaces"   (mapv shared/htmlify (sort interfaces))
     "components"   components
     "bases"        (map shared/htmlify (sort all-bases))
     "systems"      systems
     "environments" (environments ws-path top-dir all-bases all-components)}))

(def gen-doc-ok? (atom false))

(def in-out-files [{:template-file "components.ftl"
                    :output-file "components.html"}
                   {:template-file "workspace.ftl"
                    :output-file "workspace.html"}])

(defn html-file? [{:keys [output-file]}]
  (or
    (str/ends-with? output-file ".htm")
    (str/ends-with? output-file ".html")))

(defn first-html-file []
  (-> (filter html-file? in-out-files) first :output-file))

(defn generate-docs [doc-path data]
  (let [templates-root-dir (str doc-path "/templates")
        config (freemarker/configuration templates-root-dir)]
    (reset! gen-doc-ok? true)

    (doseq [{:keys [template-file output-file]} in-out-files]
      (when @gen-doc-ok?
        (let [output-path (str doc-path "/" output-file)
              [ok? message] (freemarker/write-file config templates-root-dir template-file output-path data)]
          (when (not ok?)
            (reset! gen-doc-ok? false)
            (println (str "  " message))))))))

(defn browse-file [browse? doc-path]
  (let [out-path (str doc-path "/" (first-html-file))]
    (when (and browse? (file/file-exists out-path))
      (browse/browse-url (file/url out-path)))))

(defn execute [ws-path top-dir args]
  (if (info/has-circular-dependencies? ws-path top-dir)
    (println (str "  Cannot generate documentation. Circular dependencies detected. "
                  "Run the 'info' command for details."))
    (let [browse? (not (shared/has-args? args "-browse"))
          generate? (not (shared/has-args? args "-generate"))
          doc-path (str ws-path "/doc")]
      (when generate?
        (generate-docs doc-path (template-data ws-path top-dir)))
      (browse-file browse? doc-path))))
