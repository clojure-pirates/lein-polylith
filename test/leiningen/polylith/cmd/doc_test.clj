(ns leiningen.polylith.cmd.doc-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.file :as file]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith :as polylith]
            [clojure.string :as str]))

(use-fixtures :each helper/test-setup-and-tear-down)

(deftest polylith-doc--with-an-empty-workspace--do-nothing
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          output (with-out-str
                   (polylith/polylith nil "create" "w" "ws1" "" "-git")
                   (polylith/polylith project "doc" "-browse"))]

      (is (= [""]
             (helper/split-lines output))))))


(deftest polylith-doc--with-system--print-table
  (with-redefs [file/current-path (fn [] @helper/root-dir)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "")
          sys1-content ["(ns system1.core"
                        "  (:require [comp-one.interface :as comp-one]"
                        "            [interface1.interface :as component2]"
                        "            [logger.interface :as logger])"
                        "  (:gen-class))"
                        "(defn -main [& args]"
                        "  (comp-one/add-two 10)"
                        "  (component2/add-two 10)"
                        "  (logger/add-two 10)"
                        "  (println \"Hello world!\"))"]

          comp1-content ["(ns comp-one.core"
                         "  (:require [logger.interface :as logger]))"
                         "(defn add-two [x]\n  (logger/add-two x))"]]
      (polylith/polylith nil "create" "w" "ws1" "" "-git")
      (polylith/polylith project "create" "s" "system1")
      (polylith/polylith project "create" "c" "comp-one")
      (polylith/polylith project "create" "c" "component2" "interface1")
      (polylith/polylith project "create" "c" "logger")
      (polylith/polylith project "create" "c" "email")
      (polylith/polylith project "add" "comp-one" "system1")
      (polylith/polylith project "add" "component2" "system1")
      (polylith/polylith project "add" "logger" "system1")
      (polylith/polylith project "add" "email" "system1")
      (file/replace-file! (str ws-dir "/systems/system1/src/system1/core.clj") sys1-content)
      (file/replace-file! (str ws-dir "/components/comp-one/src/comp_one/core.clj") comp1-content)
      (polylith/polylith project "doc" "-browse")

      (is (= [""
              ""
              "<!DOCTYPE html>"
              "<html>"
              "<head>"
              "<title>ws1</title>"
              ""
              "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">"
              ""
              "</head>"
              "<body>"
              ""
              "<img src=\"../logo.png\" alt=\"Polylith\" style=\"width:200px;\">"
              ""
              "<h1>ws1</h1>"
              ""
              "<h3>Libraries</h3>"
              "<div class=\"library\" title=\"1.9.0\">org.clojure/clojure</div>"
              "<p class=\"clear\"/>"
              ""
              "<h3>Interfaces</h3>"
              "<div class=\"interface\">comp&#8209;one</div>"
              "<div class=\"interface\">email</div>"
              "<div class=\"interface\">interface1</div>"
              "<div class=\"interface\">logger</div>"
              "<p class=\"clear\"/>"
              ""
              "<h3>Components</h3>"
              "  <div class=\"component\">"
              "    <div class=\"component-impl\" title=\"\">comp-one</div>"
              "    <div class=\"pass-through-ifc-empty\">&nbsp;</div>"
              "  </div>"
              "  <div class=\"component\">"
              "    <div class=\"component-impl\" title=\"\">component2</div>"
              "    <div class=\"pass-through-ifc\">interface1</div>"
              "  </div>"
              "  <div class=\"component\">"
              "    <div class=\"component-impl\" title=\"\">email</div>"
              "    <div class=\"pass-through-ifc-empty\">&nbsp;</div>"
              "  </div>"
              "  <div class=\"component\">"
              "    <div class=\"component-impl\" title=\"\">logger</div>"
              "    <div class=\"pass-through-ifc-empty\">&nbsp;</div>"
              "  </div>"
              "<p class=\"clear\"/>"
              ""
              "<h3>Bases</h3>"
              "<div class=\"base\">system1</div>"
              "<p class=\"clear\"/>"
              ""
              "<h3>Environments</h3>"
              "<div class=\"environments\">"
              "  <h4>development:</h4>"
              "  <div class=\"component\">"
              "    <div class=\"component-impl\" title=\"\">comp-one</div>"
              "    <div class=\"pass-through-ifc-empty\">&nbsp;</div>"
              "  </div>"
              "  <div class=\"component\">"
              "    <div class=\"component-impl\" title=\"\">component2</div>"
              "    <div class=\"pass-through-ifc\">interface1</div>"
              "  </div>"
              "  <div class=\"component\">"
              "    <div class=\"component-impl\" title=\"\">email</div>"
              "    <div class=\"pass-through-ifc-empty\">&nbsp;</div>"
              "  </div>"
              "  <div class=\"component\">"
              "    <div class=\"component-impl\" title=\"\">logger</div>"
              "    <div class=\"pass-through-ifc-empty\">&nbsp;</div>"
              "  </div>"
              "    <div class=\"base\">system1</div>"
              "  <p class=\"clear\"/>"
              "</div>"
              ""
              "<h3>Systems</h3>"
              "<div class=\"systems\">"
              "  <h4>system1:</h4>"
              "  <div class=\"component\">"
              "    <div class=\"component-impl\" title=\"The component 'email' was added to 'system1' but has no references to it in the source code.\">email</div>"
              "    <div class=\"pass-through-ifc-empty\">&nbsp;</div>"
              "  </div>"
              "  <p class=\"clear\"/>"
              "  <table class=\"system-table\">"
              "    <tr>"
              "      <td class=\"tcomponent\">logger</td>"
              "      <td class=\"spc\"></td>"
              "      <td class=\"tcomponent\"></td>"
              "      <td class=\"spc\"></td>"
              "      <td class=\"tcomponent\"></td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"tinterface\">&nbsp;</td>"
              "      <td class=\"spc\"></td>"
              "      <td class=\"tcomponent\"></td>"
              "      <td class=\"spc\"></td>"
              "      <td class=\"tcomponent\"></td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"tcomponent\">comp&#8209;one</td>"
              "      <td class=\"spc\"></td>"
              "      <td class=\"tcomponent\">component2</td>"
              "      <td class=\"spc\"></td>"
              "      <td class=\"tcomponent\">logger</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"tinterface\">&nbsp;</td>"
              "      <td class=\"spc\"></td>"
              "      <td class=\"tinterface\">interface1</td>"
              "      <td class=\"spc\"></td>"
              "      <td class=\"tinterface\">&nbsp;</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"tbase\" colspan=5>system1</td>"
              "    </tr>"
              "  </table>"
              "</div>"
              "</body>"
              "</html>"]
             (helper/split-lines (slurp (str ws-dir "/doc/workspace.html")))))

      (is (= [""
              ""
              "<!DOCTYPE html>"
              "<html>"
              "<head>"
              "<title>ws1 - Components</title>"
              ""
              "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">"
              ""
              "</head>"
              "<body>"
              ""
              "<img src=\"../logo.png\" alt=\"Polylith\" style=\"width:200px;\">"
              ""
              "<h1>Components</h1>"
              ""
              "<h3>comp-one</h3>"
              "  <table class=\"system-table\">"
              "    <tr>"
              "      <td class=\"tinterface\">logger</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"tcomponent\">comp&#8209;one</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"tinterface-bottom\">&nbsp;</td>"
              "    </tr>"
              "  </table>"
              "<h3>component2</h3>"
              "  <table class=\"system-table\">"
              "    <tr>"
              "      <td class=\"tcomponent\">component2</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"tinterface-bottom\">interface1</td>"
              "    </tr>"
              "  </table>"
              "<h3>email</h3>"
              "  <table class=\"system-table\">"
              "    <tr>"
              "      <td class=\"tcomponent\">email</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"tinterface-bottom\">&nbsp;</td>"
              "    </tr>"
              "  </table>"
              "<h3>logger</h3>"
              "  <table class=\"system-table\">"
              "    <tr>"
              "      <td class=\"tcomponent\">logger</td>"
              "    </tr>"
              "    <tr>"
              "      <td class=\"tinterface-bottom\">&nbsp;</td>"
              "    </tr>"
              "  </table>"
              ""
              "</body>"
              "</html>"]
             (helper/split-lines (slurp (str ws-dir "/doc/components.html"))))))))
