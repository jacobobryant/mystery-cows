(ns cows.core
  (:require [rum.core :as rum :refer [defc]]))

(def head
  [:head
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
   [:link {:rel "manifest" :href "/site.webmanifest"}]])

(def navbar
  [:nav.navbar.navbar-light.bg-light.static-top
   [:.container
    [:.navbar-brand "Mystery Cows"]]])

(def signup-form
  [:header.masthead.text-white.text-center
   [:.overlay]
   [:.container
    [:.row
     [:.col-xl-9.mx-auto
      [:h1.mb-5
       "There's been a murder at the dairy. Put your deductive "
       "skills to the test and find out hoof dunnit."]]]
    [:.row.before-signup
     [:.col-md-10.col-lg-8.col-xl-7.mx-auto
      [:form
       [:.form-row
        [:.col-12.col-md-9.mb-2.mb-md-0
         [:input#email.form-control.form-control-lg
          {:placeholder "Enter email", :type "email"}]]
        [:.col-12.col-md-3
         [:button.btn.btn-block.btn-lg.btn-primary
          {:on-click "signup(event)"
           :type "submit"}
          "Sign up"]]]]]]
    [:.row.before-signup
     [:.col.mx-auto
      [:h2 "Coming soon"]]]
    [:.row.after-signup {:style {:display "none"}}
     [:.col.mx-auto
      [:h2 "Thanks for signup up. We'll notify you once Mystery Cows is ready to play."]]]]])

(defc testimonial-item [{:keys [img-src title text]}]
  [:.col-lg-4
   [:.mx-auto.mb-5.mb-lg-0 {:style {:max-width "18rem"}}
    [:img.img-fluid.rounded-circle.mb-3
     {:style {:max-width "12rem"
              :box-shadow "0px 5px 5px 0px #adb5bd"}
      :alt ""
      :src img-src}]
    [:h5 title]
    [:p.font-weight-light.mb-0 text]]])

(def testimonials
  [:section.text-center.bg-light
   {:style {:padding-top "7rem"
            :padding-bottom "7rem"}}
   [:.container
    [:h2.mb-5 "What bovines are saying..."]
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

(def scripts
  (list
    [:script {:src "/__/firebase/7.8.0/firebase-app.js"}]
    [:script {:src "/__/firebase/7.8.0/firebase-firestore.js"}]
    [:script {:src "/__/firebase/init.js"}]
    [:script {:src "/js/main.js"}]))

(defc landing-page []
  [:html {:lang "en-US"
          :style {:min-height "100%"}}
   head
   [:body {:style {:font-family "'Helvetica Neue', Helvetica, Arial, sans-serif"}}
    navbar
    signup-form
    testimonials
    scripts]])

(defn -main []
  (spit "public/index.html" (rum/render-static-markup (landing-page))))
