sass: ./task sass -w
firebase: GOOGLE_APPLICATION_CREDENTIALS=$PWD/credentials.json npx firebase emulators:start
shadow: npx shadow-cljs server
html: npx onchange -v 'src/**/*.clj' -- clj -m cows.core
