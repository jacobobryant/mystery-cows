function elements(className, f) {
  let res = document.querySelectorAll("." + className);
  res.forEach(f);
}

function toggle(classes, whichEnabled) {
  [...Array(classes.length).keys()].forEach(i => {
    elements(classes[i], e => {
      e.style.display = (whichEnabled == i) ? '' : 'none';
    });
  });
}

function signup(e) {
  e.preventDefault();
  let email = document.getElementById("email").value;
  firebase
    .firestore()
    .collection('signups')
    .add({email: email})
    .then(x => toggle(["before-signup", "after-signup"], 1));
}
