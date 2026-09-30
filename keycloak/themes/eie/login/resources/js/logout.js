// Submit Keycloak's confirmation form while keeping the button as a fallback.
(function () {
    var form = document.getElementById('eie-logout-form');
    var button = document.getElementById('kc-logout');
    if (form && button) {
        form.requestSubmit(button);
    }
})();
