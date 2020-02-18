var ui = new firebaseui.auth.AuthUI(firebase.auth());
var uiConfig = {
  signInSuccessUrl: '/app/',
  signInOptions: [
    {
      provider: firebase.auth.EmailAuthProvider.PROVIDER_ID,
      signInMethod: firebase.auth.EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD,
      requireDisplayName: false
    },
    firebase.auth.GoogleAuthProvider.PROVIDER_ID,
  ],
  tosUrl: '/tos.txt',
  privacyPolicyUrl: '/privacy.txt',
  credentialHelper: firebaseui.auth.CredentialHelper.NONE
};

ui.start('#firebaseui-auth-container', uiConfig);
