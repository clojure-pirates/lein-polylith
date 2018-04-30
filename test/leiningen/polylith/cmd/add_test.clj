(ns leiningen.polylith.cmd.add-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-add--add-component-to-system--component-added
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")

          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "s" "sys1" "base1")
                   (polylith/polylith project "create" "c" "comp1" "ifc1")
                   (polylith/polylith project "add" "comp1" "sys1")
                   (polylith/polylith project "info"))]

      (is (= (str "interfaces:\n"
                  "  ifc1\n"
                  "components:\n"
                  "  comp1 *   > ifc1\n"
                  "bases:\n"
                  "  base1 *\n"
                  "systems:\n"
                  "  sys1 *\n"
                  "    comp1 *   -> component\n"
                  "    base1 *   -> base\n"
                  "environments:\n"
                  "  development\n"
                  "    comp1 *   -> component\n"
                  "    base1 *   -> base\n")
             output))

      (is (= #{".gitignore"
               ".polylith"
               ".polylith/time.local.edn"
               "Readme.md"
               "bases"
               "bases/base1"
               "bases/base1/Readme.md"
               "bases/base1/project.clj"
               "bases/base1/resources"
               "bases/base1/resources/base1"
               "bases/base1/src"
               "bases/base1/src/my"
               "bases/base1/src/my/company"
               "bases/base1/src/my/company/base1"
               "bases/base1/src/my/company/base1/core.clj"
               "bases/base1/test"
               "bases/base1/test/my"
               "bases/base1/test/my/company"
               "bases/base1/test/my/company/base1"
               "bases/base1/test/my/company/base1/core_test.clj"
               "components"
               "components/comp1"
               "components/comp1/Readme.md"
               "components/comp1/project.clj"
               "components/comp1/resources"
               "components/comp1/resources/comp1"
               "components/comp1/src"
               "components/comp1/src/my"
               "components/comp1/src/my/company"
               "components/comp1/src/my/company/comp1"
               "components/comp1/src/my/company/comp1/core.clj"
               "components/comp1/src/my/company/ifc1"
               "components/comp1/src/my/company/ifc1/interface.clj"
               "components/comp1/test"
               "components/comp1/test/my"
               "components/comp1/test/my/company"
               "components/comp1/test/my/company/comp1"
               "components/comp1/test/my/company/comp1/core_test.clj"
               "environments"
               "environments/development"
               "environments/development/docs"
               "environments/development/docs/base1-Readme.md"
               "environments/development/docs/comp1-Readme.md"
               "environments/development/docs/sys1-Readme.md"
               "environments/development/interfaces"
               "environments/development/interfaces/my"
               "environments/development/interfaces/my/company"
               "environments/development/interfaces/my/company/ifc1"
               "environments/development/interfaces/my/company/ifc1/interface.clj"
               "environments/development/project-files"
               "environments/development/project-files/bases"
               "environments/development/project-files/bases/base1-project.clj"
               "environments/development/project-files/components"
               "environments/development/project-files/components/comp1-project.clj"
               "environments/development/project-files/interfaces-project.clj"
               "environments/development/project-files/systems"
               "environments/development/project-files/systems/sys1-project.clj"
               "environments/development/project-files/workspace-project.clj"
               "environments/development/project.clj"
               "environments/development/resources"
               "environments/development/resources/.keep"
               "environments/development/resources/base1"
               "environments/development/resources/comp1"
               "environments/development/src"
               "environments/development/src/my"
               "environments/development/src/my/company"
               "environments/development/src/my/company/base1"
               "environments/development/src/my/company/base1/core.clj"
               "environments/development/src/my/company/comp1"
               "environments/development/src/my/company/comp1/core.clj"
               "environments/development/src/my/company/ifc1"
               "environments/development/src/my/company/ifc1/interface.clj"
               "environments/development/test"
               "environments/development/test/my"
               "environments/development/test/my/company"
               "environments/development/test/my/company/base1"
               "environments/development/test/my/company/base1/core_test.clj"
               "environments/development/test/my/company/comp1"
               "environments/development/test/my/company/comp1/core_test.clj"
               "interfaces"
               "interfaces/project.clj"
               "interfaces/src"
               "interfaces/src/my"
               "interfaces/src/my/company"
               "interfaces/src/my/company/ifc1"
               "interfaces/src/my/company/ifc1/interface.clj"
               "logo.png"
               "project.clj"
               "systems"
               "systems/sys1"
               "systems/sys1/Readme.md"
               "systems/sys1/build.sh"
               "systems/sys1/project.clj"
               "systems/sys1/resources"
               "systems/sys1/resources/base1"
               "systems/sys1/resources/comp1"
               "systems/sys1/src"
               "systems/sys1/src/my"
               "systems/sys1/src/my/company"
               "systems/sys1/src/my/company/base1"
               "systems/sys1/src/my/company/base1/core.clj"
               "systems/sys1/src/my/company/comp1"
               "systems/sys1/src/my/company/comp1/core.clj"}
             (set (file/relative-paths ws-dir)))))))

(deftest polylith-add--add-two-components-to-system-with-same-interface--only-one-component-added
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company")

          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "my.company")
                   (polylith/polylith (helper/settings ws-dir "my.company")
                                      "create" "s" "sys1" "base1")
                   (polylith/polylith project "create" "c" "comp1" "ifc1")
                   (polylith/polylith project "create" "c" "comp2" "ifc1")
                   (polylith/polylith project "add" "comp1" "sys1")
                   (polylith/polylith project "add" "comp2" "sys1")
                   (polylith/polylith project "info"))]

      (is (= (str "Component comp2's interface ifc1 is also used by comp1 and can't be added\n"
                  "interfaces:\n"
                  "  ifc1\n"
                  "components:\n"
                  "  comp1 *   > ifc1\n"
                  "  comp2 *   > ifc1\n"
                  "bases:\n"
                  "  base1 *\n"
                  "systems:\n"
                  "  sys1 *\n"
                  "    comp1 *   -> component\n"
                  "    base1 *   -> base\n"
                  "environments:\n"
                  "  development\n"
                  "    comp1 *   -> component\n"
                  "    base1 *   -> base\n")
             output))

      (is (= #{".gitignore"
               ".polylith"
               ".polylith/time.local.edn"
               "Readme.md"
               "bases"
               "bases/base1"
               "bases/base1/Readme.md"
               "bases/base1/project.clj"
               "bases/base1/resources"
               "bases/base1/resources/base1"
               "bases/base1/src"
               "bases/base1/src/my"
               "bases/base1/src/my/company"
               "bases/base1/src/my/company/base1"
               "bases/base1/src/my/company/base1/core.clj"
               "bases/base1/test"
               "bases/base1/test/my"
               "bases/base1/test/my/company"
               "bases/base1/test/my/company/base1"
               "bases/base1/test/my/company/base1/core_test.clj"
               "components"
               "components/comp1"
               "components/comp1/Readme.md"
               "components/comp1/project.clj"
               "components/comp1/resources"
               "components/comp1/resources/comp1"
               "components/comp1/src"
               "components/comp1/src/my"
               "components/comp1/src/my/company"
               "components/comp1/src/my/company/comp1"
               "components/comp1/src/my/company/comp1/core.clj"
               "components/comp1/src/my/company/ifc1"
               "components/comp1/src/my/company/ifc1/interface.clj"
               "components/comp1/test"
               "components/comp1/test/my"
               "components/comp1/test/my/company"
               "components/comp1/test/my/company/comp1"
               "components/comp1/test/my/company/comp1/core_test.clj"
               "components/comp2"
               "components/comp2/Readme.md"
               "components/comp2/project.clj"
               "components/comp2/resources"
               "components/comp2/resources/comp2"
               "components/comp2/src"
               "components/comp2/src/my"
               "components/comp2/src/my/company"
               "components/comp2/src/my/company/comp2"
               "components/comp2/src/my/company/comp2/core.clj"
               "components/comp2/src/my/company/ifc1"
               "components/comp2/src/my/company/ifc1/interface.clj"
               "components/comp2/test"
               "components/comp2/test/my"
               "components/comp2/test/my/company"
               "components/comp2/test/my/company/comp2"
               "components/comp2/test/my/company/comp2/core_test.clj"
               "environments"
               "environments/development"
               "environments/development/docs"
               "environments/development/docs/base1-Readme.md"
               "environments/development/docs/comp1-Readme.md"
               "environments/development/docs/sys1-Readme.md"
               "environments/development/interfaces"
               "environments/development/interfaces/my"
               "environments/development/interfaces/my/company"
               "environments/development/interfaces/my/company/ifc1"
               "environments/development/interfaces/my/company/ifc1/interface.clj"
               "environments/development/project-files"
               "environments/development/project-files/bases"
               "environments/development/project-files/bases/base1-project.clj"
               "environments/development/project-files/components"
               "environments/development/project-files/components/comp1-project.clj"
               "environments/development/project-files/interfaces-project.clj"
               "environments/development/project-files/systems"
               "environments/development/project-files/systems/sys1-project.clj"
               "environments/development/project-files/workspace-project.clj"
               "environments/development/project.clj"
               "environments/development/resources"
               "environments/development/resources/.keep"
               "environments/development/resources/base1"
               "environments/development/resources/comp1"
               "environments/development/src"
               "environments/development/src/my"
               "environments/development/src/my/company"
               "environments/development/src/my/company/base1"
               "environments/development/src/my/company/base1/core.clj"
               "environments/development/src/my/company/comp1"
               "environments/development/src/my/company/comp1/core.clj"
               "environments/development/src/my/company/ifc1"
               "environments/development/src/my/company/ifc1/interface.clj"
               "environments/development/test"
               "environments/development/test/my"
               "environments/development/test/my/company"
               "environments/development/test/my/company/base1"
               "environments/development/test/my/company/base1/core_test.clj"
               "environments/development/test/my/company/comp1"
               "environments/development/test/my/company/comp1/core_test.clj"
               "interfaces"
               "interfaces/project.clj"
               "interfaces/src"
               "interfaces/src/my"
               "interfaces/src/my/company"
               "interfaces/src/my/company/ifc1"
               "interfaces/src/my/company/ifc1/interface.clj"
               "logo.png"
               "project.clj"
               "systems"
               "systems/sys1"
               "systems/sys1/Readme.md"
               "systems/sys1/build.sh"
               "systems/sys1/project.clj"
               "systems/sys1/resources"
               "systems/sys1/resources/base1"
               "systems/sys1/resources/comp1"
               "systems/sys1/src"
               "systems/sys1/src/my"
               "systems/sys1/src/my/company"
               "systems/sys1/src/my/company/base1"
               "systems/sys1/src/my/company/base1/core.clj"
               "systems/sys1/src/my/company/comp1"
               "systems/sys1/src/my/company/comp1/core.clj"}
             (set (file/relative-paths ws-dir)))))))
