(ns cows.core
  (:require
    [clojure.java.io :as io]
    [rum.core :as rum :refer [defc]]))

(def h-style {:font-family "'Lato', 'Helvetica Neue', Helvetica, Arial, sans-serif"
              :font-weight 700})

(def default-head
  (list
    [:title "Mystery Cows"]
    [:meta {:name "author" :content "Jacob O'Bryant"}]
    [:meta {:name "description"
            :content (str "A board game written with Clojure. Part of "
                       "The Solo Hacker's Guide To Clojure.")}]
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet" :href "/css/bootstrap.css"}]
    [:link {:rel "stylesheet" :href "/css/main.css"}]
    [:link {:rel "apple-touch-icon" :sizes "180x180" :href "/apple-touch-icon.png"}]
    [:link {:rel "icon" :type "image/png" :sizes "32x32" :href "/favicon-32x32.png"}]
    [:link {:rel "icon" :type "image/png" :sizes "16x16" :href "/favicon-16x16.png"}]
    [:link {:rel "manifest" :href "/site.webmanifest"}]))

(def navbar
  [:nav.navbar.navbar-dark.bg-primary.static-top
   [:.container
    [:.navbar-brand "Mystery Cows"]]])

(def header
  [:header.masthead.text-white.text-center
   [:.overlay]
   [:.container
    [:.row
     [:.col-xl-9.mx-auto
      [:h1.masthead-h1.mb-3 {:style h-style}
       "There's been a murder at the dairy. Put your deductive "
       "skills to the test and find out hoof dunnit."]]]
    [:.d-flex.justify-content-center
     [:a.btn.btn-primary.btn-block.btn-lg
      {:style {:z-index 1
               :max-width "10rem"}
       :href "/login/"}
      "Sign in"]]]])

(defc testimonial-item [{:keys [img-src title text]}]
  [:.col-lg-4
   [:.mx-auto.mb-5.mb-lg-0 {:style {:max-width "18rem"}}
    [:img.img-fluid.rounded-circle.mb-3
     {:style {:max-width "12rem"
              :box-shadow "0px 5px 5px 0px #adb5bd"}
      :alt ""
      :src img-src}]
    [:h5 {:style h-style} title]
    [:p.font-weight-light.mb-0 text]]])

(def testimonials
  [:section.text-center.bg-light
   {:style {:padding-top "7rem"
            :padding-bottom "7rem"}}
   [:.container
    [:h2.mb-5 {:style h-style}
     "What bovines are saying..."]
    [:.row
     (for [[img-src title text] [["img/cow1.jpg"
                                  "Margaret E."
                                  "\"This is fantastic! Thanks so much guys!\""]
                                 ["img/cow2.jpg"
                                  "Fred S."
                                  "\"Moo\""]
                                 ["img/cow3.jpg"
                                  "Sarah W."
                                  "\"I knew it was Larry all along...\""]]]
       (testimonial-item {:img-src img-src
                          :title title
                          :text text}))]]])

(defn firebase-js [modules]
  (list
    [:script {:src "/__/firebase/7.8.0/firebase-app.js"}]
    (for [m modules]
      [:script {:src (str "/__/firebase/7.8.0/firebase-" (name m) ".js")}])
    [:script {:src "/__/firebase/init.js"}]))

(defc base-page [{:keys [firebase-modules head scripts]} & contents]
  [:html {:lang "en-US"
          :style {:min-height "100%"}}
   (into
     [:head
      default-head]
     head)
   (into
     [:body {:style {:font-family "'Helvetica Neue', Helvetica, Arial, sans-serif"}}
      navbar
      contents
      (when (not-empty firebase-modules)
        (firebase-js firebase-modules))]
     scripts)])

(def ensure-logged-in
  [:script
   {:dangerouslySetInnerHTML
    {:__html "firebase.auth().onAuthStateChanged(u => { if (!u) window.location.href = '/'; });"}}])

(def ensure-logged-out
  [:script
   {:dangerouslySetInnerHTML
    {:__html "firebase.auth().onAuthStateChanged(u => { if (u) window.location.href = '/app/'; });"}}])

(defc landing-page []
  (base-page {:firebase-modules [:auth]
              :scripts [ensure-logged-out]}
    header
    testimonials))

(defc login []
  (base-page {:head [[:link {:type "text/css"
                             :rel "stylesheet"
                             :href "/css/firebase-ui-auth-4.4.0.css"}]]
              :firebase-modules [:auth]
              :scripts [[:script {:src "/js/firebase-ui-auth-4.4.0.js"}]
                        [:script {:src "/js/main.js"}]
                        ensure-logged-out]}
    [:#firebaseui-auth-container
     {:style {:margin-top "7rem"}}]))

(defc app []
  (base-page {:firebase-modules [:auth :firestore]
              :scripts [ensure-logged-in
                        [:script {:src "/cljs/main.js"}]]}
    [:#app
     [:.d-flex.flex-column.align-items-center.mt-4
      [:.spinner-border.text-primary
       {:role "status"}
       [:span.sr-only
        "Loading..."]]]]))

(def pages
  {"/" landing-page
   "/login/" login
   "/app/" app})

(defn -main []
  (doseq [[path component] pages
          :let [full-path (str "public" path "index.html")]]
    (io/make-parents full-path)
    (spit full-path (rum/render-static-markup (component)))))
